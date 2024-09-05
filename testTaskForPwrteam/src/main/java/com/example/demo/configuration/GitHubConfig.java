package com.example.demo.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("gitHubConfig")
@ConfigurationProperties(prefix = "github")
@Data
public class GitHubConfig {
    private String baseUrl;
    private String endpoint;
    private String token;
    private String XGitHubApiVersion;
    private String linkNextPattern;
    private String placeholderPattern;
    private Client client;

    @Data
    public static class Client {
        private int pageSize;
        private int connectTimeoutInMilliseconds;
        private long responseTimeoutInSeconds;
        private long maxRetryAttempts;
        private long minBackOff;
        private long maxBackoffInSeconds;

    }
}
