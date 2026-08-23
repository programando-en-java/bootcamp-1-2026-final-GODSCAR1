# Walks a whole journey against a running stack and shows what the passenger was
# told at each step: the booking, the payment, and the check-in.
#
#   docker compose up -d --build --wait
#   .\scripts\smoke-notifications.ps1
#
# Nothing here calls notification-service. It is told what happened by three
# other services over three topics, which is the point.

# CmdletBinding, so PowerShell refuses a switch this script does not have
# instead of putting it in $args and running the wrong path in silence.
[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"

# Every service is behind the gateway now. Nothing else is published (ADR-024).
$gateway = "http://localhost:8080"

$authUrl       = $gateway
$authContainer = "airline-gateway"
$demoEmail  = "passenger@airline.test"
$demoSecret = "passenger123"

# Everything past the flight insert needs a token now. The passenger is whoever
# this logs in as: no request can name one any more.
function Login($email, $password) {
    $answer = Invoke-RestMethod -Method Post -Uri "$authUrl/api/v1/auth/login" `
        -ContentType "application/json" `
        -Body (@{ email = $email; password = $password } | ConvertTo-Json)

    return $answer.accessToken
}

$flightDb       = "airline-flight-db"
$bookingDb      = "airline-booking-db"
$notificationDb = "airline-notification-db"
$bookingUrl     = $gateway
$paymentUrl     = $gateway
$checkinUrl     = $gateway

$goodCard   = "4242424242424242"
$seats      = 1
$capacity   = 120
$hoursAhead = 3

function Step($message) {
    Write-Host ""
    Write-Host "== $message" -ForegroundColor Cyan
}

function Query($container, $database, $sql) {
    return (docker exec $container psql -U airline -d $database -tAc $sql).Trim()
}

# A failed docker exec sets $LASTEXITCODE and carries on: ErrorActionPreference
# does not cover a native command's exit code. Without this the script announces
# a flight it never created and only falls over at the first HTTP call.
function RequireStack($containers) {
    foreach ($name in $containers) {
        $state = docker inspect --format "{{.State.Running}}" $name 2>$null

        if ($LASTEXITCODE -ne 0 -or $state -ne "true") {
            throw "The stack is not up: $name is not running. Start it with: docker compose up -d --build --wait"
        }
    }
}

function Exec($container, $database, $sql) {
    docker exec $container psql -U airline -d $database -c $sql | Out-Null

    if ($LASTEXITCODE -ne 0) {
        throw "Could not run a statement against $database on $container"
    }
}

# Notifications arrive after a relay sweep, a broker delivery and a consumer
# run, none of which has happened by the time the request has answered.
function AwaitNotification($bookingId, $type) {
    $deadline = (Get-Date).AddSeconds(30)
    $count = "0"

    while ((Get-Date) -lt $deadline -and $count -ne "1") {
        Start-Sleep -Milliseconds 500
        $count = Query $notificationDb "airline_notification" `
            "SELECT count(*) FROM notifications WHERE booking_id = '$bookingId' AND type = '$type'"
    }

    if ($count -eq "1") {
        $body = Query $notificationDb "airline_notification" `
            "SELECT body FROM notifications WHERE booking_id = '$bookingId' AND type = '$type'"
        Write-Host "$type" -ForegroundColor Green
        Write-Host "  $body"
    } else {
        Write-Host "$type never arrived" -ForegroundColor Red
    }
}

# --- a flight leaving inside the check-in window -----------------------------

RequireStack @($authContainer, $flightDb, $bookingDb, $notificationDb)

Step "Logging in"

$token   = Login $demoEmail $demoSecret
$bearer  = @{ Authorization = "Bearer $token" }

Write-Host "as       $demoEmail"

Step "Creating a flight that leaves in $hoursAhead hours"

$flightId  = [guid]::NewGuid().ToString()
$suffix    = Get-Random -Minimum 1000 -Maximum 9999
$departure = (Get-Date).ToUniversalTime().AddHours($hoursAhead).ToString("yyyy-MM-dd HH:mm:sszzz")
$arrival   = (Get-Date).ToUniversalTime().AddHours($hoursAhead + 2).ToString("yyyy-MM-dd HH:mm:sszzz")

$insert = @"
INSERT INTO flights (id, flight_number, origin, destination,
                     departure_time, arrival_time,
                     total_seats, available_seats, price_amount, price_currency)
VALUES ('$flightId', 'NT$suffix', 'BOG', 'MDE',
        '$departure', '$arrival', $capacity, $capacity, 250000.00, 'COP');
"@

Exec $flightDb "airline_flight" $insert
Write-Host "flight   NT$suffix"

# --- booking ----------------------------------------------------------------

Step "Booking a seat"

$booking = Invoke-RestMethod -Method Post -Uri "$bookingUrl/api/v1/bookings" `
    -Headers ($bearer + @{ "Idempotency-Key" = [guid]::NewGuid().ToString() }) `
    -ContentType "application/json" `
    -Body (@{
        flightId    = $flightId
        seats       = $seats
    } | ConvertTo-Json)

$bookingId = $booking.bookingId
Write-Host "booking  $bookingId"

Step "What the passenger was told about the booking"
AwaitNotification $bookingId "BOOKING_CREATED"

# --- payment ----------------------------------------------------------------

Step "Paying"

$payment = Invoke-RestMethod -Method Post -Uri "$paymentUrl/api/v1/payments" `
    -Headers $bearer `
    -ContentType "application/json" `
    -Body (@{ bookingId = $bookingId; cardNumber = $goodCard } | ConvertTo-Json)

Write-Host "payment  $($payment.status)"

$deadline = (Get-Date).AddSeconds(30)
$status = "PENDING"
while ((Get-Date) -lt $deadline -and $status -eq "PENDING") {
    Start-Sleep -Milliseconds 500
    $status = Query $bookingDb "airline_booking" `
        "SELECT status FROM bookings WHERE id = '$bookingId'"
}
Write-Host "booking  $status"

Step "What the passenger was told about the payment"
AwaitNotification $bookingId "PAYMENT_SUCCEEDED"

# --- check-in ---------------------------------------------------------------

Step "Checking in"

$pass = Invoke-RestMethod -Method Post -Uri "$checkinUrl/api/v1/boarding-passes" `
    -Headers $bearer `
    -ContentType "application/json" `
    -Body (@{ bookingId = $bookingId } | ConvertTo-Json)

Write-Host "pass     $($pass.boardingPassId), boarding $($pass.boardingSequence)"

Step "What the passenger was told about the check-in"
AwaitNotification $bookingId "CHECK_IN_COMPLETED"

# --- the whole journey ------------------------------------------------------

Step "Everything this passenger was told"

docker exec $notificationDb psql -U airline -d airline_notification -c @"
SELECT type, subject, sent_at IS NOT NULL AS sent
FROM notifications
WHERE booking_id = '$bookingId'
ORDER BY created_at;
"@

$total  = Query $notificationDb "airline_notification" `
    "SELECT count(*) FROM notifications WHERE booking_id = '$bookingId'"
$unsent = Query $notificationDb "airline_notification" `
    "SELECT count(*) FROM notifications WHERE booking_id = '$bookingId' AND sent_at IS NULL"

if ($total -eq "3" -and $unsent -eq "0") {
    Write-Host "three notifications, all of them out" -ForegroundColor Green
} else {
    Write-Host "$total notifications, $unsent still unsent" -ForegroundColor Red
}
