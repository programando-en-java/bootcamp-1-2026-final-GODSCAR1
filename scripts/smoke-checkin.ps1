# Walks the check-in journey against a running stack: a flight leaving soon, a
# booking, a payment that confirms it, the boarding pass, and the message
# check-in puts on Kafka for whoever notifies the passenger.
#
#   .\mvnw.cmd -B clean package
#   docker compose up -d --build --wait
#   .\scripts\smoke-checkin.ps1
#
# Pass -TooEarly to put the flight two days out, which is the US-008 path: the
# window has not opened and no pass is printed.

param(
    [switch]$TooEarly
)

$ErrorActionPreference = "Stop"

$flightDb   = "airline-flight-db"
$bookingDb  = "airline-booking-db"
$checkinDb  = "airline-checkin-db"
$bookingUrl = "http://localhost:8082"
$paymentUrl = "http://localhost:8083"
$checkinUrl = "http://localhost:8084"
$kafka      = "airline-kafka"

$goodCard   = "4242424242424242"
$topic      = "checkin.completed.v1"

$seats      = 1
$capacity   = 120
$hoursAhead = if ($TooEarly) { 48 } else { 3 }

function Step($message) {
    Write-Host ""
    Write-Host "== $message" -ForegroundColor Cyan
}

function Query($container, $database, $sql) {
    return (docker exec $container psql -U airline -d $database -tAc $sql).Trim()
}

# --- a flight leaving soon --------------------------------------------------
# Three hours out by default, which is inside the window: check-in opens a day
# before departure and closes an hour before it.

Step "Creating a flight that leaves in $hoursAhead hours"

$flightId  = [guid]::NewGuid().ToString()
$suffix    = Get-Random -Minimum 1000 -Maximum 9999
$departure = (Get-Date).ToUniversalTime().AddHours($hoursAhead).ToString("yyyy-MM-dd HH:mm:sszzz")
$arrival   = (Get-Date).ToUniversalTime().AddHours($hoursAhead + 2).ToString("yyyy-MM-dd HH:mm:sszzz")

$insert = @"
INSERT INTO flights (id, flight_number, origin, destination,
                     departure_time, arrival_time,
                     total_seats, available_seats, price_amount, price_currency)
VALUES ('$flightId', 'SM$suffix', 'BOG', 'MDE',
        '$departure', '$arrival', $capacity, $capacity, 250000.00, 'COP');
"@

docker exec $flightDb psql -U airline -d airline_flight -c $insert | Out-Null
Write-Host "flight   $flightId (SM$suffix)"
Write-Host "departs  $departure"

# --- a booking, paid for ----------------------------------------------------

Step "Creating a booking"

$booking = Invoke-RestMethod -Method Post -Uri "$bookingUrl/api/v1/bookings" `
    -Headers @{ "Idempotency-Key" = [guid]::NewGuid().ToString() } `
    -ContentType "application/json" `
    -Body (@{
        passengerId = [guid]::NewGuid().ToString()
        flightId    = $flightId
        seats       = $seats
    } | ConvertTo-Json)

Write-Host "booking  $($booking.bookingId)"
Write-Host "status   $($booking.status)"

Step "Paying for it"

$payment = Invoke-RestMethod -Method Post -Uri "$paymentUrl/api/v1/payments" `
    -ContentType "application/json" `
    -Body (@{
        bookingId  = $booking.bookingId
        cardNumber = $goodCard
    } | ConvertTo-Json)

Write-Host "payment  $($payment.status)"

# The booking is confirmed when booking-service consumes the payment message,
# some time after the payment answered. Checking in before that would be
# refused for a booking that is about to be confirmed.

Step "Waiting for the booking to be confirmed"

$deadline = (Get-Date).AddSeconds(30)
$status = $booking.status

while ((Get-Date) -lt $deadline -and $status -eq "PENDING") {
    Start-Sleep -Milliseconds 500
    $status = Query $bookingDb "airline_booking" `
        "SELECT status FROM bookings WHERE id = '$($booking.bookingId)'"
}

Write-Host "booking  $status" -ForegroundColor Green

# --- check-in ---------------------------------------------------------------

Step "Checking in"

$body = @{ bookingId = $booking.bookingId } | ConvertTo-Json

try {
    $pass = Invoke-RestMethod -Method Post -Uri "$checkinUrl/api/v1/boarding-passes" `
        -ContentType "application/json" -Body $body

    Write-Host "pass     $($pass.boardingPassId)"
    Write-Host "flight   $($pass.flightNumber) $($pass.origin) to $($pass.destination)"
    Write-Host "departs  $($pass.departureTime)"
    Write-Host "boarding $($pass.boardingSequence)" -ForegroundColor Yellow
    $refused = $null
} catch {
    $refused = $_.ErrorDetails.Message
    Write-Host "refused  $refused" -ForegroundColor Yellow
}

# --- checking in twice ------------------------------------------------------
# The same pass, not a second one. Only meaningful on the path where one was
# issued at all.

if (-not $TooEarly) {
    Step "Checking in again"

    $again = Invoke-RestMethod -Method Post -Uri "$checkinUrl/api/v1/boarding-passes" `
        -ContentType "application/json" -Body $body

    if ($again.boardingPassId -eq $pass.boardingPassId) {
        Write-Host "pass     $($again.boardingPassId), the same one" -ForegroundColor Green
    } else {
        Write-Host "pass     $($again.boardingPassId), a second one" -ForegroundColor Red
    }
}

# --- what it left behind ----------------------------------------------------

Step "What check-in recorded"

$passes = Query $checkinDb "airline_checkin" `
    "SELECT count(*) FROM boarding_passes WHERE booking_id = '$($booking.bookingId)'"
$messages = Query $checkinDb "airline_checkin" `
    "SELECT count(*) FROM outbox WHERE aggregate_id = '$($booking.bookingId)'"
$unsent = Query $checkinDb "airline_checkin" `
    "SELECT count(*) FROM outbox WHERE aggregate_id = '$($booking.bookingId)' AND published_at IS NULL"

Write-Host "passes   $passes"
Write-Host "messages $messages"
Write-Host "unsent   $unsent"

if ($TooEarly) {
    # US-008: the window is shut, so nothing was printed and nothing announced.
    if ($passes -eq "0" -and $messages -eq "0") {
        Write-Host "nothing was issued, which is the point" -ForegroundColor Green
    } else {
        Write-Host "something was issued outside the window" -ForegroundColor Red
    }
} else {
    # US-007: one pass, announced once, and the relay has sent it.
    if ($passes -eq "1" -and $messages -eq "1" -and $unsent -eq "0") {
        Write-Host "one pass, announced once and sent" -ForegroundColor Green
    } else {
        Write-Host "the pass and its message are not where they should be" -ForegroundColor Red
    }
}

if (-not $TooEarly) {
    Step "Reading $topic"

    docker exec $kafka /opt/kafka/bin/kafka-console-consumer.sh `
        --bootstrap-server localhost:9092 `
        --topic $topic `
        --from-beginning `
        --timeout-ms 5000
}
