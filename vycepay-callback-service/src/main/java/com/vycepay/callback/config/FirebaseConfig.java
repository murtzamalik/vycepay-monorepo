package com.vycepay.callback.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Initializes Firebase Admin when {@code vycepay.firebase.enabled=true}.
 * Credentials: {@code FIREBASE_CREDENTIALS_JSON} env (raw JSON) or
 * {@code vycepay.firebase.credentials-path} file path. Never commit the service account.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${vycepay.firebase.credentials-json:}")
    private String credentialsJson;

    @Value("${vycepay.firebase.credentials-path:}")
    private String credentialsPath;

    @Bean
    @ConditionalOnProperty(name = "vycepay.firebase.enabled", havingValue = "true")
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        GoogleCredentials credentials = loadCredentials();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp app = FirebaseApp.initializeApp(options);
        log.info("Firebase Admin initialized for push notifications");
        return app;
    }

    @Bean
    @ConditionalOnProperty(name = "vycepay.firebase.enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (StringUtils.hasText(credentialsJson)) {
            try (InputStream in = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8))) {
                return GoogleCredentials.fromStream(in);
            }
        }
        if (StringUtils.hasText(credentialsPath)) {
            try (InputStream in = Files.newInputStream(Path.of(credentialsPath))) {
                return GoogleCredentials.fromStream(in);
            }
        }
        // Application Default Credentials (GCP / GOOGLE_APPLICATION_CREDENTIALS)
        return GoogleCredentials.getApplicationDefault();
    }
}
