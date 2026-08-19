# Walks the whole journey against a running stack: a flight, a booking, a
# payment, and the message the payment puts on Kafka.
#
#   .\mvnw.cmd -B clean package
#   docker compose up -d --build --wait
#   .\scripts\smoke-payment.ps1
#
# Pass -Decline to use the card the gateway refuses, which is the US-006 path.

param(
    [switch]$Decline
)

$ErrorActionPreference = "Stop"

$flightDb      = "airline-flight-db"
$bookingUrl    = "http://localhost:8082"
$paymentUrl    = "http://localhost:8083"
$kafka         = "airline-kafka"

$goodCard      = "4242424242424242"
$decliningCard = "4000000000000002"
$card          = if ($Decline) { $decliningCard } else { $goodCard }
$topic         = if ($Decline) { "payment.failed.v1" } else { "payment.succeeded.v1" }

function Step($message) {
    Write-Host ""
    Write-Host "== $message" -ForegroundColor Cyan
}

# --- a flight to book -------------------------------------------------------
# There is no API for creating one, and a running instance starts with an empty
# catalogue, so it goes straight into flight-service's own database.

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
        '$departure', '$arrival', 120, 120, 250000.00, 'COP');
"@

docker exec $flightDb psql -U airline -d airline_flight -c $insert | Out-Null
Write-Host "flight   $flightId (SM$suffix)"

# --- a booking --------------------------------------------------------------

Step "Creating a booking"

$booking = Invoke-RestMethod -Method Post -Uri "$bookingUrl/api/v1/bookings" `
    -Headers @{ "Idempotency-Key" = [guid]::NewGuid().ToString() } `
    -ContentType "application/json" `
    -Body (@{
        passengerId = [guid]::NewGuid().ToString()
        flightId    = $flightId
        seats       = 2
    } | ConvertTo-Json)

Write-Host "booking  $($booking.bookingId)"
Write-Host "total    $($booking.total) $($booking.currency)"
Write-Host "status   $($booking.status)"

# --- the payment ------------------------------------------------------------
# No amount in the request: payment-service asks booking-service what is owed.

Step "Paying with $card"

$payment = Invoke-RestMethod -Method Post -Uri "$paymentUrl/api/v1/payments" `
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
# The relay polls every second, so give it a moment before looking.

Step "Reading $topic"

Start-Sleep -Seconds 3

docker exec $kafka /opt/kafka/bin/kafka-console-consumer.sh `
    --bootstrap-server localhost:9092 `
    --topic $topic `
    --from-beginning `
    --timeout-ms 5000

Step "Outbox rows"

docker exec airline-payment-db psql -U airline -d airline_payment -c `
    "SELECT topic, aggregate_id, published_at IS NOT NULL AS sent FROM outbox ORDER BY created_at;"
