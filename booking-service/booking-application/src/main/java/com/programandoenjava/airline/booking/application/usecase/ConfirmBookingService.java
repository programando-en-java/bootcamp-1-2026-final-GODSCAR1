package com.programandoenjava.airline.booking.application.usecase;

import com.programandoenjava.airline.booking.application.port.in.readbooking.exception.BookingNotFoundException;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.ConfirmBookingUseCase;
import com.programandoenjava.airline.booking.application.port.in.settlebooking.SettleBookingCommand;
import com.programandoenjava.airline.booking.application.port.out.findbooking.FindBookingPort;
import com.programandoenjava.airline.booking.application.port.out.processedevents.ProcessedEventsPort;
import com.programandoenjava.airline.booking.application.port.out.savebooking.SaveBookingPort;
import com.programandoenjava.airline.booking.domain.booking.Booking;

/*
 * Claiming the event first is what makes a redelivery cost nothing. The domain
 * tolerates a second confirmation anyway, but the claim covers the work around
 * it too, and says plainly that this message has been dealt with.
 */
public class ConfirmBookingService implements ConfirmBookingUseCase {

    private final ProcessedEventsPort processedEvents;
    private final FindBookingPort findBooking;
    private final SaveBookingPort saveBooking;

    public ConfirmBookingService(final ProcessedEventsPort processedEvents,
                                 final FindBookingPort findBooking,
                                 final SaveBookingPort saveBooking) {
        this.processedEvents = processedEvents;
        this.findBooking = findBooking;
        this.saveBooking = saveBooking;
    }

    @Override
    public void confirm(final SettleBookingCommand command) {
        if (!processedEvents.claim(command.eventId())) {
            return;
        }

        Booking booking = findBooking.byId(command.bookingId())
                .orElseThrow(() -> new BookingNotFoundException(command.bookingId()));

        Booking confirmed = booking.confirm();

        saveBooking.updateStatus(confirmed);
    }
}
