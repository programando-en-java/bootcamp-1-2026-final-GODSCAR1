package com.programandoenjava.airline.booking.application.port.in.settlebooking;

public interface FailBookingUseCase {

    void fail(SettleBookingCommand command);
}
