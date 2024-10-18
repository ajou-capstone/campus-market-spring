package LinkerBell.campus_market_spring.dto;

import LinkerBell.campus_market_spring.domain.Role;
import lombok.Builder;
import lombok.Getter;


@Getter
public class AuthUserDto {

    private Long userId;
    private String loginEmail;
    private Role role;

    @Builder
    public AuthUserDto(Long userId, String loginEmail, Role role) {
        this.userId = userId;
        this.loginEmail = loginEmail;
        this.role = role;
    }

}
