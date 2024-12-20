package LinkerBell.campus_market_spring.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import LinkerBell.campus_market_spring.domain.Campus;
import LinkerBell.campus_market_spring.domain.Item;
import LinkerBell.campus_market_spring.domain.ItemReportCategory;
import LinkerBell.campus_market_spring.domain.Role;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.domain.UserReportCategory;
import LinkerBell.campus_market_spring.global.error.ErrorCode;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import LinkerBell.campus_market_spring.repository.ItemReportRepository;
import LinkerBell.campus_market_spring.repository.ItemRepository;
import LinkerBell.campus_market_spring.repository.UserReportRepository;
import LinkerBell.campus_market_spring.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ItemReportRepository itemReportRepository;
    @Mock
    private UserReportRepository userReportRepository;

    private User user;
    private User other;
    private Item item;
    private Campus campus;
    private Campus otherCampus;

    @BeforeEach
    public void setUp() {
        campus = createCampus(1L);
        otherCampus = createCampus(2L);
        user = createUser(1L, campus);
        other = createUser(2L, campus);
        item = createItem(other);
    }

    @Test
    @DisplayName("상품 신고 테스트")
    public void reportItemTest() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.ofNullable(user));
        given(itemRepository.findById(anyLong())).willReturn(Optional.ofNullable(item));
        // when
        reportService.reportItem(user.getUserId(), item.getItemId(), "test reason",
            ItemReportCategory.PROHIBITED_ITEM);
        // then
        then(itemReportRepository).should(times(1)).save(assertArg(report -> {
            assertThat(report).isNotNull();
            assertThat(report.getItem()).isEqualTo(item);
            assertThat(report.getUser()).isEqualTo(user);
            assertThat(report.getItem().getUser()).isEqualTo(other);
            assertThat(report.getCategory()).isEqualTo(ItemReportCategory.PROHIBITED_ITEM);
        }));
    }

    @Test
    @DisplayName("다른 캠퍼스 상품 신고 에러 테스트")
    public void reportOtherCampusItemErrorTest() {
        // given
        other.setCampus(otherCampus);
        item.setUser(other);
        item.setCampus(other.getCampus());
        given(userRepository.findById(anyLong())).willReturn(Optional.ofNullable(user));
        given(itemRepository.findById(anyLong())).willReturn(Optional.ofNullable(item));

        // when & then
        assertThatThrownBy(() -> reportService.reportItem(user.getUserId(), item.getItemId(),
            "test reason", ItemReportCategory.FRAUD))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.NOT_MATCH_USER_CAMPUS.getMessage());
    }

    @Test
    @DisplayName("자기 자신의 상품 신고 에러 테스트")
    public void reportOwnItemErrorTest() {
        // given
        Item newItem = createItem(user);
        given(userRepository.findById(anyLong())).willReturn(Optional.ofNullable(user));
        given(itemRepository.findById(anyLong())).willReturn(Optional.ofNullable(newItem));
        // when & then
        assertThatThrownBy(() -> reportService.reportItem(user.getUserId(), newItem.getItemId(),
            "test reason", ItemReportCategory.FRAUD))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.NOT_REPORT_OWN.getMessage());
    }

    @Test
    @DisplayName("사용자 신고 테스트")
    public void reportUserTest() {
        // given
        given(userRepository.findById(user.getUserId())).willReturn(Optional.ofNullable(user));
        given(userRepository.findById(other.getUserId())).willReturn(Optional.ofNullable(other));
        // when
        reportService.reportUser(user.getUserId(), other.getUserId(), "test reason",
            UserReportCategory.FRAUD);
        // then
        then(userReportRepository).should(times(1)).save(assertArg(report -> {
            assertThat(report).isNotNull();
            assertThat(report.getTarget()).isEqualTo(other);
            assertThat(report.getUser()).isEqualTo(user);
            assertThat(report.getCategory()).isEqualTo(UserReportCategory.FRAUD);
        }));
    }

    @Test
    @DisplayName("다른 대학교 사용자 신고 에러 테스트")
    public void reportOtherUnivUserErrorTest() {
        // given
        other.setCampus(otherCampus);
        given(userRepository.findById(user.getUserId())).willReturn(Optional.ofNullable(user));
        given(userRepository.findById(other.getUserId())).willReturn(Optional.ofNullable(other));

        // when & then
        assertThatThrownBy(() -> reportService.reportUser(user.getUserId(), other.getUserId(),
            "test reason", UserReportCategory.FRAUD))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.NOT_MATCH_USER_CAMPUS.getMessage());
    }

    @Test
    @DisplayName("자기 자신 신고 에러 테스트")
    public void reportOwnErrorTest() {
        // given

        // when & then
        assertThatThrownBy(() -> reportService.reportUser(user.getUserId(), user.getUserId(),
            "test reason", UserReportCategory.FRAUD))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining(ErrorCode.NOT_REPORT_OWN.getMessage());
    }

    private Campus createCampus(Long campusId) {
        return Campus.builder()
            .campusId(campusId)
            .email(String.format("testUniv@example%d.com", campusId))
            .region("korea")
            .universityName(String.format("testUniv%d", campusId))
            .build();
    }

    private User createUser(Long userId, Campus campus) {
        return User.builder()
            .userId(userId)
            .role(Role.USER)
            .loginEmail("testEmail")
            .campus(campus)
            .schoolEmail("testSchool")
            .nickname("user" + userId).build();
    }

    private Item createItem(User user) {
        return Item.builder()
            .itemId(1L)
            .campus(user.getCampus())
            .user(user)
            .title("testItem")
            .description("testItemDescription")
            .price(10000000).build();
    }
}