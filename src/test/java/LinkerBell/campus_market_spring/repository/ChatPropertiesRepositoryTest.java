package LinkerBell.campus_market_spring.repository;

import LinkerBell.campus_market_spring.domain.ChatProperties;
import LinkerBell.campus_market_spring.domain.ChatRoom;
import LinkerBell.campus_market_spring.domain.Item;
import LinkerBell.campus_market_spring.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
//@AutoConfigureTestDatabase(replace = Replace.NONE) // 실제 DB 설정 사용 시 필요
class ChatPropertiesRepositoryTest {

    @Autowired
    private ChatPropertiesRepository chatPropertiesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    @DisplayName("findByUserAndChatRoom()으로 특정 사용자와 채팅방에 해당하는 ChatProperties를 조회")
    void findByUserAndChatRoom_ShouldReturnChatProperties() {
        // given
        // 1. 테스트용 User, Item, ChatRoom 객체 생성 및 저장
        User user = new User();
        user.setNickname("testUser");
        userRepository.save(user);

        Item item = new Item();
        item.setUser(user);
        itemRepository.save(item);

        ChatRoom chatRoom = ChatRoom.builder()
                .user(user)
                .item(item)
                .build();
        chatRoomRepository.save(chatRoom);

        // 2. ChatProperties 객체를 생성하고 저장
        ChatProperties chatProperties = ChatProperties.builder()
                .user(user)
                .chatRoom(chatRoom)
                .isAlarm(true)
                .title("testChatRoom")
                .isExited(false)
                .build();
        chatPropertiesRepository.save(chatProperties);

        // when
        ChatProperties foundChatProperties = chatPropertiesRepository.findByUserAndChatRoom(user, chatRoom);

        // then
        assertThat(foundChatProperties).isNotNull(); // 조회 결과가 null이 아닌지 확인
        assertThat(foundChatProperties.getUser().getUserId()).isEqualTo(user.getUserId()); // User ID 확인
        assertThat(foundChatProperties.getChatRoom().getChatRoomId()).isEqualTo(chatRoom.getChatRoomId()); // ChatRoom ID 확인
        assertThat(foundChatProperties.getTitle()).isEqualTo("testChatRoom"); // title 확인
        assertThat(foundChatProperties.isAlarm()).isTrue(); // isAlarm 확인
        assertThat(foundChatProperties.isExited()).isFalse(); // isExited 확인
    }

    @Test
    @DisplayName("updateIsExitedTrueByUserAndChatRoom()으로 특정 사용자와 채팅방의 isExited를 true로 업데이트")
    void updateIsExitedTrueByUserAndChatRoom_ShouldUpdateIsExitedToTrue() {
        // given
        // 1. 테스트용 User, Item, ChatRoom 객체 생성 및 저장
        User user = new User();
        user.setNickname("testUser");
        userRepository.save(user);

        Item item = new Item();
        item.setUser(user);
        itemRepository.save(item);

        ChatRoom chatRoom = ChatRoom.builder()
                .user(user)
                .item(item)
                .build();
        chatRoomRepository.save(chatRoom);

        // 2. ChatProperties 객체를 생성하고 저장
        ChatProperties chatProperties = ChatProperties.builder()
                .user(user)
                .chatRoom(chatRoom)
                .isAlarm(true)
                .title("testChatRoom")
                .isExited(false)
                .build();
        chatPropertiesRepository.save(chatProperties);

        // when
        // `isExited`가 false임을 확인한 후, 해당 메서드로 `isExited`를 true로 업데이트
        assertThat(chatProperties.isExited()).isFalse(); // 초기 값 확인
        chatPropertiesRepository.updateIsExitedTrueByUserAndChatRoom(user, chatRoom);

        // 영속성 컨텍스트를 새로고침하여 업데이트 사항 반영
        ChatProperties updatedChatProperties = chatPropertiesRepository.findByUserAndChatRoom(user, chatRoom);

        // then
        assertThat(updatedChatProperties.isExited()).isTrue(); // 업데이트가 성공적으로 수행되었는지 확인
    }
}
