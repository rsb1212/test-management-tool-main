package com.testmgmt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private JwtProperties jwt = new JwtProperties();
    private RateLimitProperties rateLimit = new RateLimitProperties();
    private JiraProperties jira = new JiraProperties();
    private StorageProperties storage = new StorageProperties();

    @Data
    public static class JwtProperties {
        private String secret;
        private Long expirationMs = 3600000L;
        private Long refreshExpirationMs = 86400000L;
    }

    @Data
    public static class RateLimitProperties {
        private Integer requestsPerMinute = 100;
    }

    @Data
    public static class JiraProperties {
        private String baseUrl;
        private String username;
        private String apiToken;
        private String projectKey;
    }

    @Data
    public static class StorageProperties {
        private String type = "local";
        private LocalStorageProperties local = new LocalStorageProperties();
        private S3StorageProperties s3 = new S3StorageProperties();

        @Data
        public static class LocalStorageProperties {
            private String path = "/tmp/test-management/uploads";
        }

        @Data
        public static class S3StorageProperties {
            private String bucket;
            private String region = "ap-south-1";
        }
    }
}
