package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.domain.ChatMessage;
import LinkerBell.campus_market_spring.domain.ChatProperties;
import LinkerBell.campus_market_spring.domain.ChatRoom;
import LinkerBell.campus_market_spring.domain.Item;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.dto.AuthUserDto;
import LinkerBell.campus_market_spring.dto.ChatRoomDataResponseDto;
import LinkerBell.campus_market_spring.dto.ChatRoomRequestDto;
import LinkerBell.campus_market_spring.dto.ChatRoomResponseDto;
import LinkerBell.campus_market_spring.global.error.ErrorCode;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import LinkerBell.campus_market_spring.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ChatPropertiesRepository chatPropertiesRepository;
    private final ChatMessageRepository chatMessageRepository;

    // 채팅방 만들기. 채팅방 설정도 2개 만듦
    @Transactional
    public ChatRoomResponseDto addChatRoom(AuthUserDto user,
        ChatRoomRequestDto chatRoomRequestDto) {
        User buyer = userRepository.findById(user.getUserId())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Item item = itemRepository.findById(chatRoomRequestDto.getItemId())
            .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
        User seller = item.getUser();

        // 아이템, 구매자가 모두 같은 채팅방이 이미 존재하면 채팅방을 만들지 않음
        if (chatRoomRepository.existsByUser_userIdAndItem_itemId(user.getUserId(),
            item.getItemId())) {
            log.error("아이템, 구매자가 모두 같은 채팅방이 존재함");
            throw new CustomException(ErrorCode.DUPLICATE_CHATROOM);
        }

        ChatRoom chatRoom = ChatRoom.builder()
            .user(buyer)
            .item(item)
            .userCount(1)
            .build();

        chatRoomRepository.save(chatRoom);

        // 채팅방 설정 2개 만들기
        ChatProperties buyerChatProperties = ChatProperties.builder().user(buyer).chatRoom(chatRoom)
            .isAlarm(true).title(seller.getNickname()).isExited(false).build();
        chatPropertiesRepository.save(buyerChatProperties);

        ChatProperties sellerChatProperties = ChatProperties.builder().user(seller)
            .chatRoom(chatRoom).isAlarm(true).title(buyer.getNickname()).isExited(false).build();
        chatPropertiesRepository.save(sellerChatProperties);

        ChatRoomResponseDto chatRoomResponseDto = ChatRoomResponseDto.builder()
            .chatRoomId(chatRoom.getChatRoomId()).userId(chatRoom.getUser().getUserId())
            .itemId(chatRoom.getItem().getItemId()).title(chatRoom.getUser().getNickname())
            .thumbnail(chatRoom.getItem().getThumbnail())
            .isAlarm(true).build();

        return chatRoomResponseDto;
    }

    // 채팅방 목록 가져오기
    @Transactional(readOnly = true)
    public List<ChatRoomDataResponseDto> getChatRooms(AuthUserDto authUserDto) {
        List<ChatRoomDataResponseDto> chatRoomDataResponseDtoList = new ArrayList<>();
        User user = userRepository.findById(authUserDto.getUserId())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        chatRoomRepository.findAll().forEach(chatRoom -> {
            // 나간 채팅방인 경우, 유저가 포함되지 않은 채팅방인 경우
            ChatProperties chatProperties = chatPropertiesRepository.findByUserAndChatRoom(user,
                chatRoom);
            if (chatProperties == null || chatProperties.isExited()) {
                return;
            }

            // 채팅방에 메시지가 없는 경우
            ChatMessage recentChatMessage = chatMessageRepository.findTopByIsReadTrueOrderByCreatedDateDesc();
            Long messageId;
            if (recentChatMessage == null) {
                messageId = -1L;
            } else {
                messageId = recentChatMessage.getMessageId();
            }

            // 내가 구매자인 경우
            if (chatRoom.getUser().getUserId().equals(user.getUserId())) {
                ChatRoomDataResponseDto tempChatRoomDataResponseDto = ChatRoomDataResponseDto.builder()
                    .chatRoomId(chatRoom.getChatRoomId())
                    .userId(chatRoom.getItem().getUser().getUserId())
                    .itemId(chatRoom.getItem().getItemId())
                    .title(chatRoom.getItem().getUser().getNickname()) // 판매자의 닉네임이 제목에 보이게
                    .thumbnail(chatRoom.getItem().getThumbnail())
                    .isAlarm(
                        chatPropertiesRepository.findByUserAndChatRoom(user, chatRoom).isAlarm())
                    .messageId(messageId).build();

                chatRoomDataResponseDtoList.add(tempChatRoomDataResponseDto);
            } else if (chatRoom.getItem().getUser().getUserId()
                .equals(user.getUserId())) { // 내가 판매자인 경우
                ChatRoomDataResponseDto tempChatRoomDataResponseDto = ChatRoomDataResponseDto.builder()
                    .chatRoomId(chatRoom.getChatRoomId()).userId(chatRoom.getUser().getUserId())
                    .itemId(chatRoom.getItem().getItemId())
                    .title(chatRoom.getUser().getNickname()) // 구매자의 닉네임이 제목에 보이게
                    .thumbnail(chatRoom.getItem().getThumbnail())
                    .isAlarm(
                        chatPropertiesRepository.findByUserAndChatRoom(user, chatRoom).isAlarm())
                    .messageId(messageId).build();

                chatRoomDataResponseDtoList.add(tempChatRoomDataResponseDto);
            }
        });

        return chatRoomDataResponseDtoList;
    }

    // 채팅방 나가기
    @Transactional
    public void leaveChatRoom(Long chatRoomId, AuthUserDto authUserDto) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));
        User user = userRepository.findById(authUserDto.getUserId())
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ChatProperties chatProperties = chatPropertiesRepository.findByUserAndChatRoom(user,
            chatRoom);

        // isExited = true로 바꾸기
        chatProperties.setExited(true);

        // 채팅방에서 userCount - 1 하기
        chatRoom.setUserCount(chatRoom.getUserCount() - 1);
    }

    // 채팅방 1개 정보 가져오기
    @Transactional(readOnly = true)
    public ChatRoomDataResponseDto getChatRoom(Long userId, Long chatRoomId) {
        ChatRoomDataResponseDto chatRoomDataResponseDto;

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
            .orElseThrow(() -> new CustomException(ErrorCode.CHATROOM_NOT_FOUND));

        // 채팅방에 메시지가 없는 경우
        ChatMessage recentChatMessage = chatMessageRepository.findTopByIsReadTrueOrderByCreatedDateDesc();
        Long messageId;
        if (recentChatMessage == null) {
            messageId = -1L;
        } else {
            messageId = recentChatMessage.getMessageId();
        }

        // 내가 구매자인 경우
        if (chatRoom.getUser().getUserId().equals(user.getUserId())) {
            chatRoomDataResponseDto = ChatRoomDataResponseDto.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .userId(chatRoom.getItem().getUser().getUserId())
                .itemId(chatRoom.getItem().getItemId())
                .title(chatRoom.getItem().getUser().getNickname()) // 판매자의 닉네임이 제목에 보이게
                .thumbnail(chatRoom.getItem().getThumbnail())
                .isAlarm(
                    chatPropertiesRepository.findByUserAndChatRoom(user, chatRoom).isAlarm())
                .messageId(messageId).build();
        } else { // 내가 판매자인 경우

            chatRoomDataResponseDto = ChatRoomDataResponseDto.builder()
                .chatRoomId(chatRoom.getChatRoomId()).userId(chatRoom.getUser().getUserId())
                .itemId(chatRoom.getItem().getItemId())
                .title(chatRoom.getUser().getNickname()) // 구매자의 닉네임이 제목에 보이게
                .thumbnail(chatRoom.getItem().getThumbnail())
                .isAlarm(
                    chatPropertiesRepository.findByUserAndChatRoom(user, chatRoom).isAlarm())
                .messageId(messageId).build();
        }

        return chatRoomDataResponseDto;
    }
}
