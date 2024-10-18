package LinkerBell.campus_market_spring.global.jwt;

import LinkerBell.campus_market_spring.domain.Role;
import LinkerBell.campus_market_spring.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

    @InjectMocks
    JwtUtils jwtUtils;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(jwtUtils, "secretKey", "TESTKEY1TESTKEY2TESTKEY3TESTKEY4TESTKEY5TESTKEY6");
        ReflectionTestUtils.setField(jwtUtils, "accessExpiredTime", Long.valueOf(360000));
        ReflectionTestUtils.setField(jwtUtils, "refreshExpiredTime", Long.valueOf(120960000));
    }

    @Test
    @DisplayName("jwt 발급 테스트")
    public void jwtGenerateTest() {
        // given
        User user = createUser();
        // when
        String accessToken = jwtUtils.generateAccessToken(user.getLoginEmail(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getLoginEmail(), user.getRole());

        // then
        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();
    }

    @Test
    @DisplayName("jwt 유효성 확인 테스트")
    public void jwtValidationTest() {
        // given
        User user = createUser();
        String accessToken = jwtUtils.generateAccessToken(user.getLoginEmail(), user.getRole());
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
        String accessToken = jwtUtils.generateAccessToken(user.getLoginEmail(), user.getRole());
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
        String token = jwtUtils.generateAccessToken(user.getLoginEmail(), user.getRole());
        // when
        Authentication authentication = jwtUtils.getAuthentication(token);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        GrantedAuthority authority = authorities.iterator().next();
        String authorityString = authority.getAuthority();
        // then
        assertThat(userDetails.getUsername()).isEqualTo(user.getLoginEmail());
        assertThat(authorityString).isEqualTo("ROLE_GUEST");
    }

    @Test
    @DisplayName("jwt에서 email 정보 가져오기 테스트")
    public void getEmailFromJwtTest() {
        // given
        User user = createUser();
        String token = jwtUtils.generateAccessToken(user.getLoginEmail(), user.getRole());
        // when
        String email = jwtUtils.getEmail(token);
        // then
        assertThat(email).isEqualTo(user.getLoginEmail());
    }

    private User createUser() {
        return User.builder()
                .loginEmail("abc@gmail.com")
                .role(Role.GUEST)
                .build();
    }
}