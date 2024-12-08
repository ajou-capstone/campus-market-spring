package LinkerBell.campus_market_spring.global.jwt;

import static org.assertj.core.api.Assertions.*;

import LinkerBell.campus_market_spring.domain.Role;
import LinkerBell.campus_market_spring.domain.User;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {
    @InjectMocks
    JwtUtils jwtUtils;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "secretKey", "TESTTESTTESTTESTTESTTESTTESTTESTTEST");
        ReflectionTestUtils.setField(jwtUtils, "accessExpiredTime", Long.valueOf(360000));
        ReflectionTestUtils.setField(jwtUtils, "refreshExpiredTime", Long.valueOf(2109600000));
    }

    @Test
    @DisplayName("jwt 발급 테스트")
    public void jwtGenerateTest() {
        // given
        User user = createUser();
        // when
        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getLoginEmail(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getUserId(),user.getLoginEmail(), user.getRole());
        // then
        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();
    }

    @Test
    @DisplayName("jwt 유효성 확인 테스트")
    public void jwtValidationTest() {
        // given
        User user = createUser();
        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getLoginEmail(), user.getRole());
        // when
        boolean isValidated = jwtUtils.validateToken(accessToken);
        // then
        assertThat(isValidated).isTrue();
    }

    @Test
    @DisplayName("jwt 추출 테스트")
    public void jwtResolveTest() {
        // given
        User user = createUser();
        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getLoginEmail(), user.getRole());
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("Authorization", "Bearer " + accessToken);
        // when
        String resolvedToken = jwtUtils.resolveToken(mockHttpServletRequest);
        // then
        assertThat(resolvedToken).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("authentication 생성 테스트")
    public void jwtAuthenticationTest() {
        // given
        User user = createUser();
        String token = jwtUtils.generateAccessToken(user.getUserId(), user.getLoginEmail(), user.getRole());
        // when
        Authentication authentication = jwtUtils.getAuthentication(token);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        GrantedAuthority authority = authorities.iterator().next();
        String authorityString = authority.getAuthority();
        // then
        assertThat(userDetails.getUsername()).isEqualTo(user.getUserId().toString());
        assertThat(authorityString).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("jwt에서 email 정보 가져오기 테스트")
    public void getEmailFromJwtTest() {
        // given
        User user = createUser();
        String token = jwtUtils.generateAccessToken(user.getUserId(), user.getLoginEmail(), user.getRole());
        // when
        String email = jwtUtils.getEmail(token);
        // then
        assertThat(email).isEqualTo(user.getLoginEmail());
    }

    @Test
    @DisplayName("refresh token 헤더에서 가져오기 테스트")
    public void resolveRefreshTokenTest() {
        // given
        User user = createUser();
        String accessToken = jwtUtils.generateRefreshToken(user.getUserId(), user.getLoginEmail(), user.getRole());
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        mockHttpServletRequest.addHeader("refresh", "Bearer " + accessToken);
        // when
        String resolvedToken = jwtUtils.resolveRefreshToken(mockHttpServletRequest);
        // then
        assertThat(resolvedToken).isEqualTo(accessToken);
    }

    @Test
    @DisplayName("token 유효기간 확인 테스트")
    public void tokenExpiredTimeTest() {
        // given
        User user = createUser();
        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getLoginEmail(), user.getRole());
        // when
        Long expiredTime = jwtUtils.getExpirationTime(accessToken);
        // then
        assertThat(expiredTime).isNotNull();
        assertThat(expiredTime).isGreaterThan(0);
    }

    @Test
    @DisplayName("token에서 userId 가져오기 테스트")
    public void getUserIdFromJwtTest() {
        // given
        User user = createUser();
        String accessToken = jwtUtils.generateAccessToken(user.getUserId(), user.getLoginEmail(), user.getRole());
        // when
        Long userId = jwtUtils.getUserId(accessToken);
        // then
        assertThat(userId).isNotNull();
        assertThat(userId).isEqualTo(user.getUserId());
    }


    private User createUser() {
        return User.builder()
            .userId(1L)
            .loginEmail("test@example.com")
            .role(Role.USER)
            .build();
    }
}