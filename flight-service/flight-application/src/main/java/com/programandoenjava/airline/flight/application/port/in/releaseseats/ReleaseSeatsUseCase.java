package com.programandoenjava.airline.flight.application.port.in.releaseseats;

/**
 * Gives seats back to the flight they were held on. Releasing a hold that is not
 * there succeeds, because the saga retries this step until the seats are back.
 */
public interface ReleaseSeatsUseCase {

    void release(ReleaseSeatsCommand command);
}
