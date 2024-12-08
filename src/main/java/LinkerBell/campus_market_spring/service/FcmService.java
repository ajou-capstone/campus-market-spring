package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.domain.Item;
import LinkerBell.campus_market_spring.domain.Keyword;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.domain.UserFcmToken;
import LinkerBell.campus_market_spring.dto.FcmMessageDto;
import LinkerBell.campus_market_spring.repository.UserFcmTokenRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class FcmService {

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final FcmNotificationService fcmNotificationService;

    @Value("${deeplink.keyword_url}")
    private String deeplinkKeywordUrl;
    @Value("${deeplink.chat_url}")
    private String deeplinkChatUrl;

    public void sendFcmMessageWithKeywords(List<Keyword> sendingKeywords, Item savedItem) {
        for (Keyword sendingKeyword : sendingKeywords) {
            List<String> fcmTokens = userFcmTokenRepository.findFcmTokenByUser_UserId(
                sendingKeyword.getUser().getUserId());
            for (String fcmToken : fcmTokens) {
                FcmMessageDto sendingKeywordMessage = createKeywordFcmMessage(sendingKeyword,
                    fcmToken,
                    savedItem);

                fcmNotificationService.sendNotification(sendingKeywordMessage);
            }

        }
    }

    private FcmMessageDto createKeywordFcmMessage(Keyword sendingKeyword, String fcmToken,
        Item savedItem) {
        return FcmMessageDto.builder()
            .targetToken(fcmToken)
            .title(sendingKeyword.getKeywordName() + " 키워드 알림")
            .body(savedItem.getTitle())
            .deeplinkUrl(deeplinkKeywordUrl + savedItem.getItemId())
            .build();
    }

    public void saveUserFcmToken(String firebaseToken, User user) {
        userFcmTokenRepository.findByFcmToken(firebaseToken).ifPresentOrElse(userFcmToken -> {
                userFcmToken.setLastModifiedDate(LocalDateTime.now());
            },
            () -> {
                UserFcmToken userFcmToken = UserFcmToken.builder().fcmToken(firebaseToken)
                    .user(user).build();
                userFcmTokenRepository.save(userFcmToken);
            });
    }

    public void sendFcmMessageWithChat(Long userId, Long chatRoomId, String title, String content) {
        List<String> fcmTokens = userFcmTokenRepository.findFcmTokenByUser_UserId(userId);

        for (String fcmToken : fcmTokens) {
            FcmMessageDto fcmMessageDto = FcmMessageDto.builder()
                .targetToken(fcmToken)
                .title(title)
                .body(content)
                .deeplinkUrl(deeplinkChatUrl + chatRoomId)
                .build();

            fcmNotificationService.sendNotification(fcmMessageDto);
        }
    }

    public void deleteFcmTokenAllByUserId(Long userId) {
        userFcmTokenRepository.deleteByUser_UserId(userId);
    }

}
