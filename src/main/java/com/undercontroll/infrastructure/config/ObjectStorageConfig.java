package com.undercontroll.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class ObjectStorageConfig {

    private final String endpoint;
    private final String publicEndpoint;
    private final String region;
    private final boolean pathStyle;
    private final String accessKey;
    private final String secretKey;
    private final String sessionToken;

    public ObjectStorageConfig(
            @Value("${undercontroll.storage.endpoint:}") String endpoint,
            @Value("${undercontroll.storage.public-endpoint:}") String publicEndpoint,
            @Value("${undercontroll.storage.region:us-east-1}") String region,
            @Value("${undercontroll.storage.path-style:false}") boolean pathStyle,
            @Value("${undercontroll.storage.access-key:}") String accessKey,
            @Value("${undercontroll.storage.secret-key:}") String secretKey,
            @Value("${undercontroll.storage.session-token:}") String sessionToken
    ) {
        this.endpoint = endpoint;
        this.publicEndpoint = publicEndpoint;
        this.region = region;
        this.pathStyle = pathStyle;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.sessionToken = sessionToken;
    }

    @Bean(destroyMethod = "close")
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        String signingEndpoint = publicEndpoint.isBlank() ? endpoint : publicEndpoint;
        if (!signingEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(signingEndpoint));
        }
        return builder.build();
    }

    AwsCredentialsProvider credentials() {
        if (accessKey.isBlank() || secretKey.isBlank()) {
            return DefaultCredentialsProvider.builder().build();
        }
        if (sessionToken.isBlank()) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        return StaticCredentialsProvider.create(AwsSessionCredentials.create(accessKey, secretKey, sessionToken));
    }
}
