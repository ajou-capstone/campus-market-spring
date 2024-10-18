package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.domain.Role;
import LinkerBell.campus_market_spring.domain.User;
import LinkerBell.campus_market_spring.dto.AuthResponseDto;
import LinkerBell.campus_market_spring.dto.AuthUserDto;
import LinkerBell.campus_market_spring.global.error.ErrorCode;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import LinkerBell.campus_market_spring.global.jwt.JwtUtils;
import LinkerBell.campus_market_spring.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${google.client}")
    private String GOOGLE_CLIENT_ID;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponseDto googleLogin(String googleToken) {
        GoogleIdToken idToken = null;
        log.info("google Token: " + googleToken);
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID))
                .build();
            idToken = verifier.verify(googleToken);

        } catch (GeneralSecurityException | IOException e) {
            throw new CustomException(ErrorCode.UNVERIFIED_GOOGLE_TOKEN);
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException");
            throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        if (idToken == null) {
            log.error("idToken is null");
            throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        String email = payload.getEmail();
        boolean emailVerified = payload.getEmailVerified();
        if (!emailVerified) {
            throw new CustomException(ErrorCode.NOT_VERIFIED_EMAIL);
        }

        User user = userRepository.findByLoginEmail(email)
            .orElseGet(() -> User.builder()
                .loginEmail(email)
                .role(Role.GUEST)
                .build());

        String accessToken = jwtUtils.generateAccessToken(user.getLoginEmail(), user.getRole());
        String refreshToken = jwtUtils.generateRefreshToken(user.getLoginEmail(), user.getRole());

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        AuthResponseDto authResponseDto = AuthResponseDto.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();

        return authResponseDto;
    }

    public AuthUserDto getUserByLoginEmail(String loginEmail) {
        User user = userRepository.findByLoginEmail(loginEmail)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return AuthUserDto.builder()
            .userId(user.getUserId())
            .loginEmail(user.getLoginEmail())
            .role(user.getRole())
            .build();
    }
}
