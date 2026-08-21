package com.programandoenjava.airline.booking.application.usecase;

import com.programandoenjava.airline.booking.application.port.in.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.FailBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.SettleBookingCommand;
import com.programandoenjava.airline.booking.application.port.out.findbooking.FindBookingPort;
import com.programandoenjava.airline.booking.application.port.out.processedevents.ProcessedEventsPort;
import com.programandoenjava.airline.booking.application.port.out.releaseseats.ReleaseSeatsPort;
import com.programandoenjava.airline.booking.application.port.out.savebooking.SaveBookingPort;
import com.programandoenjava.airline.booking.domain.booking.Booking;

import java.time.Clock;
import java.time.Instant;

public class FailBookingService implements FailBookingUseCase {

    private final ProcessedEventsPort processedEvents;
    private final FindBookingPort findBooking;
    private final SaveBookingPort saveBooking;
    private final ReleaseSeatsPort releaseSeats;
    private final Clock clock;

    public FailBookingService(final ProcessedEventsPort processedEvents,
                              final FindBookingPort findBooking,
                              final SaveBookingPort saveBooking,
                              final ReleaseSeatsPort releaseSeats,
                              final Clock clock) {
        this.processedEvents = processedEvents;
        this.findBooking = findBooking;
        this.saveBooking = saveBooking;
        this.releaseSeats = releaseSeats;
        this.clock = clock;
    }

    @Override
    public void fail(final SettleBookingCommand command) {
        boolean claimed = processedEvents.claim(command.eventId());

        if (!claimed) {
            return;
        }

        Booking booking = findBooking.byId(command.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

        Booking failed = booking.fail();

        saveBooking.updateStatus(failed);

        releaseSeats.release(failed.flightId(), failed.seatBlockId());

        Instant now = clock.instant();
        saveBooking.markSeatsReleased(failed, now);
    }
}
