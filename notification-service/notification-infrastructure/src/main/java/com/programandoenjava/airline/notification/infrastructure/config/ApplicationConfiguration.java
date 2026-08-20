package com.programandoenjava.airline.notification.infrastructure.config;

import com.programandoenjava.airline.notification.application.port.in.notify.NotifyPassengerUseCase;
import com.programandoenjava.airline.notification.application.port.out.channel.NotificationChannel;
import com.programandoenjava.airline.notification.application.port.out.notifications.SaveNotificationPort;
import com.programandoenjava.airline.notification.application.port.out.processedevents.ProcessedEventsPort;
import com.programandoenjava.airline.notification.application.usecase.NotificationRecorder;
import com.programandoenjava.airline.notification.application.usecase.NotifyPassengerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    NotificationRecorder notificationRecorder(final ProcessedEventsPort processedEventsPort,
                                              final SaveNotificationPort saveNotificationPort) {
        return new NotificationRecorder(processedEventsPort, saveNotificationPort);
    }

    @Bean
    NotifyPassengerUseCase notifyPassengerUseCase(final NotificationRecorder notificationRecorder,
                                                  final NotificationChannel notificationChannel,
                                                  final SaveNotificationPort saveNotificationPort,
                                                  final Clock clock) {
        return new NotifyPassengerService(notificationRecorder, notificationChannel,
                saveNotificationPort, clock);
    }
}
