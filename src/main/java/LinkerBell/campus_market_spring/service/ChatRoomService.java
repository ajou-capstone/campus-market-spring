package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.domain.ChatProperties;
import LinkerBell.campus_market_spring.domain.ChatRoom;
import LinkerBell.campus_market_spring.domain.Item;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.dto.AuthUserDto;
import LinkerBell.campus_market_spring.dto.ChatRoomRequestDto;
import LinkerBell.campus_market_spring.dto.ChatRoomResponseDto;
import LinkerBell.campus_market_spring.global.error.ErrorCode;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import LinkerBell.campus_market_spring.repository.ChatPropertiesRepository;
import LinkerBell.campus_market_spring.repository.ChatRoomRepository;
import LinkerBell.campus_market_spring.repository.ItemRepository;
import LinkerBell.campus_market_spring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ChatPropertiesRepository chatPropertiesRepository;

    // 채팅방 만들기. 채팅방 설정도 2개 만듦
    @Transactional
    public ChatRoomResponseDto addChatRoom(AuthUserDto user, ChatRoomRequestDto chatRoomRequestDto) {
        User buyer = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Item item = itemRepository.findById(chatRoomRequestDto.getItemId())
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
        User seller = item.getUser();

        ChatRoom chatRoom = ChatRoom.builder()
                .user(buyer)
                .item(item)
                .build();

        chatRoomRepository.save(chatRoom);

        // 채팅방 설정 2개 만들기
        ChatProperties buyerChatProperties = ChatProperties.builder()
                .user(buyer)
                .chatRoom(chatRoom)
                .isAlarm(true)
                .title(seller.getNickname())
                .isExited(false)
                .build();
        chatPropertiesRepository.save(buyerChatProperties);

        ChatProperties sellerChatProperties = ChatProperties.builder()
                .user(seller)
                .chatRoom(chatRoom)
                .isAlarm(true)
                .title(buyer.getNickname())
                .isExited(false)
                .build();
        chatPropertiesRepository.save(sellerChatProperties);

        ChatRoomResponseDto chatRoomResponseDto = ChatRoomResponseDto.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .build();

        return chatRoomResponseDto;
    }
}
