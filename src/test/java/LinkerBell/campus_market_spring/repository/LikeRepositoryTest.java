package LinkerBell.campus_market_spring.repository;

import static org.assertj.core.api.Assertions.*;

import LinkerBell.campus_market_spring.domain.Category;
import LinkerBell.campus_market_spring.domain.Item;
import LinkerBell.campus_market_spring.domain.ItemStatus;
import LinkerBell.campus_market_spring.domain.Like;
import LinkerBell.campus_market_spring.domain.Role;
import LinkerBell.campus_market_spring.domain.User;
import java.util.Optional;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

@DataJpaTest
class LikeRepositoryTest {

    @Autowired
    LikeRepository likeRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ItemRepository itemRepository;

    private User user;

    private Item item;
    private Item item2;

    @BeforeEach
    public void setUp() {
        user = createUser(1L);
        User other = createUser(2L);

        user = userRepository.save(user);
        other = userRepository.save(other);
        item = createItem(other, 1L);
        item2 = createItem(other, 2L);

        item = itemRepository.save(item);
        item2 = itemRepository.save(item2);
    }

    @Test
    @DisplayName("좋아요 추가하기")
    public void doLikesTest() {
        // given
        Like like = new Like(1L, user, item);
        // when
        Like savedLike = likeRepository.save(like);
        // then
        assertThat(savedLike).isNotNull();
        assertThat(savedLike.getUser().getUserId()).isEqualTo(user.getUserId());
        assertThat(savedLike.getItem().getItemId()).isEqualTo(item.getItemId());
    }

    @Test
    @DisplayName("좋아요 목록 가져오기")
    public void getLikeListTest() {
        // given
        Like like = new Like(2L, user, item2);
        Like like2 = new Like(3L, user, item2);

        likeRepository.saveAll(Lists.newArrayList(like, like2));
        Sort sort = Sort.by("createdDate").descending();
        PageRequest pageRequest = PageRequest.of(0, 4, sort);
        // when
        Slice<Like> likes = likeRepository.findAllByUser(user, pageRequest);
        // then

        assertThat(likes).isNotNull();
        assertThat(likes.getNumberOfElements()).isEqualTo(2);
        assertThat(likes.getSize()).isEqualTo(pageRequest.getPageSize());
        assertThat(likes.getSort().isSorted()).isTrue();
    }

    @Test
    @DisplayName("좋아요 취소하기")
    public void deleteLikesTest() {
        // given
        Like like = new Like(1L, user, item);

        like = likeRepository.save(like);
        // when
        likeRepository.deleteById(like.getLikeId());
        Optional<Like> likeOpt = likeRepository.findById(like.getLikeId());
        // then
        assertThat(likeOpt.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("사용자와 상품으로 좋아요 정보 가져오기")
    public void getLikeByUserAndItem() {
        // given
        Like like = new Like(1L, user, item);

        like = likeRepository.save(like);
        // when
        Optional<Like> likeOpt = likeRepository.findByUserAndItem(user, item);

        // then
        assertThat(likeOpt.isPresent()).isTrue();
        assertThat(likeOpt.get().getLikeId()).isEqualTo(like.getLikeId());
    }

    @Test
    @DisplayName("사용자와 상품으로 좋아요 정보 가져오기 그러나 좋아요를 누른 상품이 아닌 경우")
    public void getLikeByUserAndItemButNotLiked() {
        // given
        // when
        Optional<Like> likeOpt = likeRepository.findByUserAndItem(user, item);

        // then
        assertThat(likeOpt.isEmpty()).isTrue();
    }

    private User createUser(Long id) {
        return User.builder()
            .userId(id)
            .role(Role.GUEST)
            .loginEmail("user" + id + "@example.com")
            .schoolEmail("user" + id + "@ajou.ac.kr")
            .nickname("i'm user" + id)
            .build();
    }

    private Item createItem(User user, Long id) {
        return Item.builder()
            .itemId(id)
            .category(Category.ELECTRONICS_IT)
            .price(40000 + id.intValue())
            .title("dummy item" + id)
            .description("for testing" + id)
            .itemStatus(ItemStatus.FORSALE)
            .thumbnail("default image" + id)
            .user(user)
            .build();
    }

}