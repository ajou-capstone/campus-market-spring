package LinkerBell.campus_market_spring.service;

import LinkerBell.campus_market_spring.global.error.ErrorCode;
import LinkerBell.campus_market_spring.global.error.exception.CustomException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    @Value("${google.client}")
    private String GOOGLE_CLIENT_ID;

    public String getEmailWithVerifyIdToken(String idToken) {
        GoogleIdToken googleIdToken = getGoogleIdToken(idToken);

        if (idToken == null) {
            throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }

        return getEmailFromGoogleIdToken(googleIdToken);
    }

    private GoogleIdToken getGoogleIdToken(String googleToken) {
        GoogleIdToken idToken = null;
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
            throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
        }
        return idToken;
    }

    private String getEmailFromGoogleIdToken(GoogleIdToken idToken) {
        GoogleIdToken.Payload payload = idToken.getPayload();

        boolean emailVerified = payload.getEmailVerified();
        if (!emailVerified) {
            throw new CustomException(ErrorCode.NOT_VERIFIED_EMAIL);
        }

        return payload.getEmail();
    }
}
