# Walks the whole journey against a running stack: a flight, a booking, a
# payment, the message it puts on Kafka, and what booking-service does when it
# reads that message.
#
#   docker compose up -d --build --wait
#   .\scripts\smoke-payment.ps1
#
# Pass -Decline to use the card the payment gateway refuses, which is the US-006 path:
# the booking should end up FAILED and the seats should go back on the flight.

# CmdletBinding, so PowerShell refuses a switch this script does not have
# instead of putting it in $args and running the wrong path in silence.
[CmdletBinding()]
param(
    [switch]$Decline
)

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

$flightDb      = "airline-flight-db"
$bookingDb     = "airline-booking-db"
$bookingUrl    = $gateway
$paymentUrl    = $gateway
$kafka         = "airline-kafka"

$goodCard      = "4242424242424242"
$decliningCard = "4000000000000002"
$card          = if ($Decline) { $decliningCard } else { $goodCard }
$topic         = if ($Decline) { "payment.failed.v1" } else { "payment.succeeded.v1" }
$expected      = if ($Decline) { "FAILED" } else { "CONFIRMED" }

$seats         = 2
$capacity      = 120

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

# --- a flight to book -------------------------------------------------------
# There is no API for creating one, and a running instance starts with an empty
# catalogue, so it goes straight into flight-service's own database.

RequireStack @($authContainer, $flightDb, $bookingDb, $kafka)

Step "Logging in"

$token   = Login $demoEmail $demoSecret
$bearer  = @{ Authorization = "Bearer $token" }

Write-Host "as       $demoEmail"

Step "Creating a flight"

$flightId  = [guid]::NewGuid().ToString()
$suffix    = Get-Random -Minimum 1000 -Maximum 9999
$departure = (Get-Date).ToUniversalTime().AddDays(1).ToString("yyyy-MM-dd HH:mm:sszzz")
$arrival   = (Get-Date).ToUniversalTime().AddDays(1).AddHours(2).ToString("yyyy-MM-dd HH:mm:sszzz")

$insert = @"
INSERT INTO flights (id, flight_number, origin, destination,
                     departure_time, arrival_time,
                     total_seats, available_seats, price_amount, price_currency)
VALUES ('$flightId', 'SM$suffix', 'BOG', 'MDE',
        '$departure', '$arrival', $capacity, $capacity, 250000.00, 'COP');
"@

Exec $flightDb "airline_flight" $insert
Write-Host "flight   $flightId (SM$suffix), $capacity seats"

# --- a booking --------------------------------------------------------------

Step "Creating a booking"

$booking = Invoke-RestMethod -Method Post -Uri "$bookingUrl/api/v1/bookings" `
    -Headers ($bearer + @{ "Idempotency-Key" = [guid]::NewGuid().ToString() }) `
    -ContentType "application/json" `
    -Body (@{
        flightId    = $flightId
        seats       = $seats
    } | ConvertTo-Json)

Write-Host "booking  $($booking.bookingId)"
Write-Host "total    $($booking.total) $($booking.currency)"
Write-Host "status   $($booking.status)"

$heldSeats = Query $flightDb "airline_flight" `
    "SELECT available_seats FROM flights WHERE id = '$flightId'"
Write-Host "seats    $heldSeats left on the flight"

# --- the payment ------------------------------------------------------------
# No amount in the request: payment-service asks booking-service what is owed.

Step "Paying with $card"

$payment = Invoke-RestMethod -Method Post -Uri "$paymentUrl/api/v1/payments" `
    -Headers $bearer `
    -ContentType "application/json" `
    -Body (@{
        bookingId  = $booking.bookingId
        cardNumber = $card
    } | ConvertTo-Json)

Write-Host "payment  $($payment.paymentId)"
Write-Host "amount   $($payment.amount) $($payment.currency)"
Write-Host "card     ****$($payment.cardLastFourDigits)"
Write-Host "status   $($payment.status)" -ForegroundColor Yellow

# --- what came out the other side -------------------------------------------

Step "Reading $topic"

Start-Sleep -Seconds 2

docker exec $kafka /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server localhost:9092 `
    --topic $topic `
    --from-beginning `
    --timeout-ms 5000

# --- where the saga ends ----------------------------------------------------
# The booking settles when booking-service consumes the message, which happens
# some time after the payment answered. Polling rather than sleeping, so a slow
# run waits and a fast one does not.

Step "Waiting for the booking to settle"

$deadline = (Get-Date).AddSeconds(30)
$status = $booking.status

while ((Get-Date) -lt $deadline -and $status -eq "PENDING") {
    Start-Sleep -Milliseconds 500
    $status = Query $bookingDb "airline_booking" `
        "SELECT status FROM bookings WHERE id = '$($booking.bookingId)'"
}

if ($status -eq $expected) {
    Write-Host "booking  $status" -ForegroundColor Green
} else {
    Write-Host "booking  $status, expected $expected" -ForegroundColor Red
}

Step "Where the seats ended up"

$finalSeats = Query $flightDb "airline_flight" `
    "SELECT available_seats FROM flights WHERE id = '$flightId'"
$blocks = Query $flightDb "airline_flight" `
    "SELECT count(*) FROM seat_blocks WHERE flight_id = '$flightId'"
$released = Query $bookingDb "airline_booking" `
    "SELECT seats_released_at IS NOT NULL FROM bookings WHERE id = '$($booking.bookingId)'"

Write-Host "seats    $finalSeats left on the flight (was $heldSeats while held)"
Write-Host "holds    $blocks on this flight"
Write-Host "released $released"

if ($Decline) {
    # US-006: the seats a failed booking held must go back.
    if ($finalSeats -eq $capacity -and $blocks -eq "0") {
        Write-Host "the seats went back" -ForegroundColor Green
    } else {
        Write-Host "the seats are still held" -ForegroundColor Red
    }
} else {
    # US-005: a confirmed booking keeps its seats.
    if ($finalSeats -eq ($capacity - $seats) -and $blocks -eq "1") {
        Write-Host "the seats are held for the confirmed booking" -ForegroundColor Green
    } else {
        Write-Host "the seats are not where they should be" -ForegroundColor Red
    }
}

Step "Events booking-service has acted on"

docker exec $bookingDb psql -U airline -d airline_booking -c `
    "SELECT count(*) AS processed FROM processed_events;"
