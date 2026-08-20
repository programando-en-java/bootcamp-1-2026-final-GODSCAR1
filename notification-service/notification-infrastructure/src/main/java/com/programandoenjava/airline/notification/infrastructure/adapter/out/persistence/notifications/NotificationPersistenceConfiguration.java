package com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.notifications;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationPersistenceConfiguration {

    @Bean
    NotificationPersistenceAdapter notificationPersistenceAdapter(
            final NotificationJpaRepository notificationJpaRepository) {

        return new NotificationPersistenceAdapter(notificationJpaRepository);
    }
}
