package LinkerBell.campus_market_spring.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import LinkerBell.campus_market_spring.domain.Role;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.dto.AuthUserDto;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import LinkerBell.campus_market_spring.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    AuthService authService;

    @Test
    @DisplayName("사용자 정보 찾기 테스트")
    public void getUserByLoginEmailTest() {
        User user = User.builder()
            .userId(1L)
            .loginEmail("abc@gmail.com")
            .role(Role.GUEST)
            .build();

        when(userRepository.findByLoginEmail(Mockito.anyString())).thenReturn(
            Optional.ofNullable(user));

        AuthUserDto userDto = authService.getUserByLoginEmail("abc@gmail.com");

        assertThat(userDto.getUserId()).isEqualTo(1L);
        assertThat(userDto.getLoginEmail()).isEqualTo("abc@gmail.com");
        assertThat(userDto.getRole()).isEqualTo(Role.GUEST);
    }

    @Test
    @DisplayName("사용자 정보 찾기 예외 테스트")
    public void getExceptionFromGetUserTest() {
        User user = User.builder()
            .userId(1L)
            .loginEmail("abc@gmail.com")
            .role(Role.GUEST)
            .build();

        when(userRepository.findByLoginEmail(Mockito.anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> authService.getUserByLoginEmail(user.getLoginEmail())).isInstanceOf(
            CustomException.class);
    }
}