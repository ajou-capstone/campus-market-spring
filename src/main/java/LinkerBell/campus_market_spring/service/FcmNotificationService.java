package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.dto.FcmMessageDto;
import LinkerBell.campus_market_spring.repository.UserFcmTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FcmNotificationService {

    private final UserFcmTokenRepository userFcmTokenRepository;

    public void sendNotification(FcmMessageDto fcmMessageDto) {
        Message.Builder messageBuilder = Message.builder()
            .setToken(fcmMessageDto.getTargetToken())
            .setNotification(Notification.builder()
                .setTitle(fcmMessageDto.getTitle())
                .setBody(fcmMessageDto.getBody())
                .build());
        log.info("sendNotification fcm token={}", fcmMessageDto.getTargetToken());
        log.info("sendNotification title={}", fcmMessageDto.getTitle());
        log.info("sendNotification body={}", fcmMessageDto.getBody());
        log.info("sendNotification deeplink={}", fcmMessageDto.getDeeplinkUrl());
        if (fcmMessageDto.getDeeplinkUrl() != null) {
            messageBuilder.putData("deeplink", fcmMessageDto.getDeeplinkUrl());
        }
        log.info("fcm deeplink={}", fcmMessageDto.getDeeplinkUrl());
        Message message = messageBuilder.build();
        try {
            String response = FirebaseMessaging.getInstance().sendAsync(message).get();
            log.info("Successfully sent message with deeplink: {}", response);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof FirebaseMessagingException messagingException) {
                handleFirebaseMessagingException(messagingException,
                    fcmMessageDto.getTargetToken());
            } else {
                log.error("Unexpected error occurred while sending notification", e);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Notification sending was interrupted", e);
        } catch (Throwable e) {
            log.error("invalid error={}", e.getMessage());
        }
    }

    private void handleFirebaseMessagingException(FirebaseMessagingException ex,
        String targetToken) {
        switch (ex.getMessagingErrorCode()) {
            case INVALID_ARGUMENT -> {
                log.error("Invalid FCM token, removing token: {}", targetToken);
                userFcmTokenRepository.deleteByFcmToken(targetToken);
            }
            case UNREGISTERED -> {
                log.error("Unregistered FCM token, removing token: {}", targetToken);
                userFcmTokenRepository.deleteByFcmToken(targetToken);
            }
            default -> log.error("Unexpected FirebaseMessagingException occurred", ex);
        }
    }
}
