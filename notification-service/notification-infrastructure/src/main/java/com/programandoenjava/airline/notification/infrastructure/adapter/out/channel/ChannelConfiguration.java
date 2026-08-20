package com.programandoenjava.airline.notification.infrastructure.adapter.out.channel;

import com.programandoenjava.airline.notification.application.port.out.channel.NotificationChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChannelConfiguration {

    @Bean
    NotificationChannel notificationChannel() {
        return new LoggingNotificationChannel();
    }
}
