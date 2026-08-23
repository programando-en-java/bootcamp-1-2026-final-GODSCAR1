package com.programandoenjava.airline.checkin.infrastructure.adapter.in.web;

import com.programandoenjava.airline.checkin.application.port.in.checkin.CheckInCommand;
import com.programandoenjava.airline.checkin.application.port.shared.Caller;
import com.programandoenjava.airline.checkin.domain.boardingpass.BookingId;
import com.programandoenjava.airline.checkin.infrastructure.adapter.in.web.dto.CheckInRequest;

final class CheckInRequestMapper {

    private CheckInRequestMapper() {
    }

    static CheckInCommand toCommand(final CheckInRequest request, final Caller caller) {
        BookingId booking = new BookingId(request.bookingId());

        return new CheckInCommand(booking, caller);
    }
}
