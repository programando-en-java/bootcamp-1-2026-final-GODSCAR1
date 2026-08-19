package com.programandoenjava.airline.booking.application.port.in.settlebooking;

public interface ConfirmBookingUseCase {

    void confirm(SettleBookingCommand command);
}
