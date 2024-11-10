package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.domain.ChatMessage;
import LinkerBell.campus_market_spring.domain.ChatRoom;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.dto.ChattingRequestDto;
import LinkerBell.campus_market_spring.dto.ChattingResponseDto;
import LinkerBell.campus_market_spring.global.error.ErrorCode;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import LinkerBell.campus_market_spring.repository.ChatMessageRepository;
import LinkerBell.campus_market_spring.repository.ChatRoomRepository;
import LinkerBell.campus_market_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChattingService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChattingResponseDto makeChattingResponseDto(Long userId, Long chatRoomId,
        ChattingRequestDto chattingRequestDto) {
        // 메시지 정보를 db에 저장
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));

        ChatMessage chatMessage = ChatMessage.builder()
            .chatRoom(chatRoom)
            .user(user)
            .content(chattingRequestDto.getContent())
            .contentType(chattingRequestDto.getContentType())
            .isRead(false)
            .build();

        ChatMessage savedChatMessage = chatMessageRepository.save(chatMessage);

        // chattingResponseDto 리턴
        ChattingResponseDto chattingResponseDto = ChattingResponseDto.builder()
            .chattingId(savedChatMessage.getMessageId())
            .chatRoomId(chatRoomId)
            .userId(userId)
            .content(savedChatMessage.getContent())
            .contentType(chattingRequestDto.getContentType())
            .createdAt(savedChatMessage.getCreatedDate())
            .build();

        return chattingResponseDto;
    }
}