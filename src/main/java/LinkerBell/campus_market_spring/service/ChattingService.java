package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.domain.ChatMessage;
import LinkerBell.campus_market_spring.domain.ChatProperties;
import LinkerBell.campus_market_spring.domain.ChatRoom;
import LinkerBell.campus_market_spring.domain.ContentType;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.dto.ChattingRequestDto;
import LinkerBell.campus_market_spring.dto.ChattingResponseDto;
import LinkerBell.campus_market_spring.global.error.ErrorCode;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import LinkerBell.campus_market_spring.repository.ChatMessageRepository;
import LinkerBell.campus_market_spring.repository.ChatPropertiesRepository;
import LinkerBell.campus_market_spring.repository.ChatRoomRepository;
import LinkerBell.campus_market_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChattingService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatPropertiesRepository chatPropertiesRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Transactional
    public ChattingResponseDto makeChattingResponseDto(Long userId, Long chatRoomId,
        ChattingRequestDto chattingRequestDto) {
        // 메시지 정보를 db에 저장
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));

        if (chattingRequestDto.getContentType() == null) {
            log.error("makeChattingResponseDto: contentType is null");
            throw new CustomException(ErrorCode.INVALID_CONTENT_TYPE);
        }

        String content = "";

        if (chattingRequestDto.getContentType() == ContentType.TIMETABLE) {

        } else {
            content = chattingRequestDto.getContent();
        }

        ChatMessage chatMessage = ChatMessage.builder()
            .chatRoom(chatRoom)
            .user(user)
            .content(content)
            .contentType(chattingRequestDto.getContentType())
            .isRead(false)
            .build();

        ChatMessage savedChatMessage = chatMessageRepository.save(chatMessage);

        // chattingResponseDto 리턴
        ChattingResponseDto chattingResponseDto = ChattingResponseDto.builder()
            .messageId(savedChatMessage.getMessageId())
            .chatRoomId(chatRoomId)
            .userId(userId)
            .content(savedChatMessage.getContent())
            .contentType(chattingRequestDto.getContentType())
            .createdAt(savedChatMessage.getCreatedDate())
            .build();

        return chattingResponseDto;
    }

    @Transactional(readOnly = true)
    public void sendNotification(Long userId, Long chatRoomId,
        ChattingRequestDto chattingRequestDto) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));

        String title;
        Long targetUserId;

        if (chatRoom.getUser().getUserId().equals(userId)) { // 내가 구매자
            title = chatRoom.getUser().getNickname();
            targetUserId = chatRoom.getItem().getUser().getUserId();
        } else { // 내가 판매자
            title = chatRoom.getItem().getUser().getNickname();
            targetUserId = chatRoom.getUser().getUserId();
        }

        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ChatProperties chatProperties = chatPropertiesRepository.findByUserAndChatRoom(targetUser,
            chatRoom);

        if (chatProperties == null) {
            log.error("sendNotification: chatProperties is null");
            throw new CustomException(ErrorCode.CHAT_PROPERTIES_NOT_FOUND);
        }

        if (!chatProperties.isAlarm()) {
            return;
        }

        String content = chattingRequestDto.getContent();

        fcmService.sendFcmMessageWithChat(targetUserId, chatRoomId, title, content);
    }
}
