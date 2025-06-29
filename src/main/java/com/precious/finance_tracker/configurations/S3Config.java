package com.precious.finance_tracker.configurations;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3Config {
    @Value("${s3.access-key-id}")
    private String s3AccessKeyId;

    @Value("${s3.secret-access-key}")
    private String s3SecretAccessKey;

    @Value("${s3.endpoint-url}")
    private String s3EndpointUrl;

    @Value("${s3.region}")
    private String s3Region;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                s3AccessKeyId,
                s3SecretAccessKey
        );

        return S3Client.builder()
                .region(Region.of(s3Region))
                .endpointOverride(URI.create(s3EndpointUrl))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();
    }
}
