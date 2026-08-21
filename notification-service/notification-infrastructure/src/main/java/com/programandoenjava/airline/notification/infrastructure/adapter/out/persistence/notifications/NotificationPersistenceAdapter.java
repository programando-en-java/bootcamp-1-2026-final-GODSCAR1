package com.programandoenjava.airline.notification.infrastructure.adapter.out.persistence.notifications;

import com.programandoenjava.airline.notification.application.port.out.notifications.SaveNotificationPort;
import com.programandoenjava.airline.notification.domain.notification.Notification;
import com.programandoenjava.airline.notification.domain.notification.NotificationMessage;
import org.springframework.transaction.annotation.Transactional;

class NotificationPersistenceAdapter implements SaveNotificationPort {

    private final NotificationJpaRepository notificationJpaRepository;

    NotificationPersistenceAdapter(final NotificationJpaRepository notificationJpaRepository) {
        this.notificationJpaRepository = notificationJpaRepository;
    }

    @Override
    @Transactional
    public void save(final Notification notification) {
        NotificationMessage message = notification.message();

        NotificationEntity entity = new NotificationEntity(
                notification.id().value(),
                notification.passengerId().value(),
                notification.bookingId().value(),
                NotificationTypeEntity.valueOf(notification.type().name()),
                message.subject(),
                message.body(),
                notification.createdAt());

        notificationJpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void markSent(final Notification notification) {
        notificationJpaRepository.findById(notification.id().value())
                .ifPresent(entity -> entity.markSent(notification.sentAt()));
    }
}
