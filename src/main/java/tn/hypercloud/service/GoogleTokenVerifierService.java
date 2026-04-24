package tn.hypercloud.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleTokenVerifierService {

    private final String googleClientId;

    public GoogleTokenVerifierService(@Value("${app.security.google.client-id:}") String googleClientId) {
        this.googleClientId = googleClientId;
    }

    public GoogleIdToken.Payload verify(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new RuntimeException("Google Client ID non configure dans le backend");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new RuntimeException("Token Google invalide ou expire");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            Object emailVerified = payload.get("email_verified");
            if (!(emailVerified instanceof Boolean) || !((Boolean) emailVerified)) {
                throw new RuntimeException("Email Google non verifie");
            }

            return payload;
        } catch (GeneralSecurityException | IOException ex) {
            throw new RuntimeException("Impossible de verifier le token Google", ex);
        }
    }
}