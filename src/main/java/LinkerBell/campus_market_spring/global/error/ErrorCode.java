package LinkerBell.campus_market_spring.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_GOOGLE_TOKEN(HttpStatus.UNAUTHORIZED, 4001, "Invalid Google idToken"),
    INVALID_JWT(HttpStatus.UNAUTHORIZED, 4002, "Invalid JWT"),
    EXPIRED_JWT(HttpStatus.UNAUTHORIZED, 4003, "JWT가 만료되었습니다."),
    LOGOUT_JWT(HttpStatus.UNAUTHORIZED, 4004, "Logout한 JWT"),
    UNVERIFIED_GOOGLE_TOKEN(HttpStatus.UNAUTHORIZED, 4005, "Google idToken이 확인되지 않습니다."),
    NOT_VERIFIED_EMAIL(HttpStatus.UNAUTHORIZED, 4006, "Google email is not verified"),
    JWT_IS_NULL(HttpStatus.UNAUTHORIZED, 4007, "jwt의 값이 null입니다."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 4011, "사용자가 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final int code;
    private final String message;
}
