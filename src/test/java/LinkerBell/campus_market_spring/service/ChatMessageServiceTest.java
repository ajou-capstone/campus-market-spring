package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.domain.ChatMessage;
import LinkerBell.campus_market_spring.domain.ChatRoom;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.dto.ChatMessageResponseDto;
import LinkerBell.campus_market_spring.dto.RecentChatMessageResponseDto;
import LinkerBell.campus_market_spring.repository.ChatMessageRepository;
import LinkerBell.campus_market_spring.repository.ChatRoomRepository;
import LinkerBell.campus_market_spring.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    void getRecentMessageList_Success() {
        // given
        Long userId = 1L;
        Long chatRoomId = 10L;
        Long messageId1 = 100L;
        Long messageId2 = 101L;

        User user = new User();
        user.setUserId(userId);

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setChatRoomId(chatRoomId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chatRoomRepository.findByUser(user)).thenReturn(Collections.singletonList(chatRoom));
        when(chatMessageRepository.findMessageIdsByChatRoomAndRecentDays(eq(chatRoom), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(messageId1, messageId2));

        // when
        List<RecentChatMessageResponseDto> result = chatMessageService.getRecentMessageList(userId);

        // then
        assertThat(result).hasSize(1);
        RecentChatMessageResponseDto responseDto = result.get(0);
        assertThat(responseDto.getChatRoomId()).isEqualTo(chatRoomId);
        assertThat(responseDto.getMessageIdList()).containsExactly(messageId1, messageId2);
    }


    @Test
    void getRecentMessageList_UserNotFound() {
        // given
        Long userId = 1L;

        // Mock userRepository to return empty (user not found)
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when
        List<RecentChatMessageResponseDto> result = chatMessageService.getRecentMessageList(userId);

        // then
        assertThat(result).isEmpty(); // 사용자 없으면 빈 리스트 반환
    }

    @Test
    void getRecentMessageList_NoMessagesInChatRoom() {
        // given
        Long userId = 1L;
        Long chatRoomId = 10L;
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // Mock User and ChatRoom
        User user = new User();
        user.setUserId(userId);

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setChatRoomId(chatRoomId);

        // Mocking repositories
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(chatRoomRepository.findByUser(user)).thenReturn(Collections.singletonList(chatRoom));
        when(chatMessageRepository.findMessageIdsByChatRoomAndRecentDays(eq(chatRoom), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // when
        List<RecentChatMessageResponseDto> result = chatMessageService.getRecentMessageList(userId);

        // then
        assertThat(result).hasSize(1);
        RecentChatMessageResponseDto responseDto = result.get(0);
        assertThat(responseDto.getChatRoomId()).isEqualTo(chatRoomId);
        assertThat(responseDto.getMessageIdList()).isEmpty(); // 메시지가 없는 경우 빈 리스트 반환
    }

    @Test
    void readMessage_Success() {
        // given
        Long messageId = 100L;

        // when
        chatMessageService.readMessage(messageId);

        // then
        verify(chatMessageRepository, times(1)).updateByIsReadTrueByMessageId(messageId);
    }

    @Test
    void getMessageContents_Success() {
        // given
        Long messageId1 = 100L;
        Long messageId2 = 101L;
        Long chatRoomId = 10L;
        Long userId = 1L;

        ChatRoom chatRoom = ChatRoom.builder().chatRoomId(chatRoomId).build();
        User user = User.builder().userId(userId).build();

        ChatMessage chatMessage1 = new ChatMessage();
        chatMessage1.setMessageId(messageId1);
        chatMessage1.setContent("Hello");
        chatMessage1.setChatRoom(chatRoom);
        chatMessage1.setUser(user);

        ChatMessage chatMessage2 = new ChatMessage();
        chatMessage2.setMessageId(messageId2);
        chatMessage2.setContent("Hi there!");
        chatMessage2.setChatRoom(chatRoom);
        chatMessage2.setUser(user);

        List<Long> messageIdList = Arrays.asList(messageId1, messageId2);
        when(chatMessageRepository.findById(messageId1)).thenReturn(Optional.of(chatMessage1));
        when(chatMessageRepository.findById(messageId2)).thenReturn(Optional.of(chatMessage2));

        // when
        List<ChatMessageResponseDto> result = chatMessageService.getMessageContents(messageIdList);

        // then
        assertThat(result).hasSize(2);

        ChatMessageResponseDto responseDto1 = result.get(0);
        assertThat(responseDto1.getMessageId()).isEqualTo(messageId1);
        assertThat(responseDto1.getContent()).isEqualTo("Hello");

        ChatMessageResponseDto responseDto2 = result.get(1);
        assertThat(responseDto2.getMessageId()).isEqualTo(messageId2);
        assertThat(responseDto2.getContent()).isEqualTo("Hi there!");

        verify(chatMessageRepository, times(2)).findById(anyLong());
    }

    @Test
    void getMessageContents_EmptyList() {
        // given
        List<Long> messageIdList = Collections.emptyList();

        // when
        List<ChatMessageResponseDto> result = chatMessageService.getMessageContents(messageIdList);

        // then
        assertThat(result).isEmpty();
        verify(chatMessageRepository, never()).findById(anyLong());
    }
}

