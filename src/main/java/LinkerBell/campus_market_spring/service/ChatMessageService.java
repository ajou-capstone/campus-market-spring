package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.domain.ChatMessage;
import LinkerBell.campus_market_spring.domain.ChatRoom;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.dto.ChatMessageResponseDto;
import LinkerBell.campus_market_spring.dto.RecentChatMessageResponseDto;
import LinkerBell.campus_market_spring.global.error.ErrorCode;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import LinkerBell.campus_market_spring.repository.ChatMessageRepository;
import LinkerBell.campus_market_spring.repository.ChatRoomRepository;
import LinkerBell.campus_market_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    // 최근 7일간 메시지 목록들 가져오기
    @Transactional(readOnly = true)
    public RecentChatMessageResponseDto getRecentMessageList(Long userId) {
        List<Long> recentChatMessageIdList = new ArrayList<>();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 유저가 속한 채팅방들 찾기
        List<ChatRoom> chatRoomList = chatRoomRepository.findByUserOrItemSeller(user);

        // 채팅방들마다 7일간 메시지들 찾기
        for (ChatRoom chatRoom : chatRoomList) {
            recentChatMessageIdList.addAll(
                chatMessageRepository.findMessageIdsByChatRoomAndRecentDays(chatRoom,
                    sevenDaysAgo));
        }


        RecentChatMessageResponseDto recentChatMessageResponseDto = RecentChatMessageResponseDto
            .builder()
            .messageIdList(recentChatMessageIdList)
            .build();

        return recentChatMessageResponseDto;
    }

    // 메시지 읽음으로 표시하기
    @Transactional
    public void readMessage(Long messageId) {
        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

        chatMessage.setRead(true);
    }

    // 메시지 내용들 가져오기
    @Transactional(readOnly = true)
    public List<ChatMessageResponseDto> getMessageContents(List<Long> messageIdList) {
        List<ChatMessageResponseDto> chatMessageResponseDtoList = new ArrayList<>();
        for (Long messageId : messageIdList) {
            ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new CustomException(ErrorCode.MESSAGE_NOT_FOUND));

            ChatMessageResponseDto chatMessageResponseDto = ChatMessageResponseDto.builder()
                .messageId(chatMessage.getMessageId())
                .chatRoomId(chatMessage.getChatRoom().getChatRoomId())
                .userId(chatMessage.getUser().getUserId())
                .content(chatMessage.getContent())
                .contentType((chatMessage.getContentType()))
                .createdAt(chatMessage.getCreatedDate())
                .build();
            chatMessageResponseDtoList.add(chatMessageResponseDto);
        }

        return chatMessageResponseDtoList;
    }
}
