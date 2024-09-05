package com.example.demo;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("#{gitHubConfig.baseUrl}")
    String gitHubBaseUrl;
    @Value("#{gitHubConfig.token}")
    String gitHubToken;
    @Value("#{gitHubConfig.client.connectTimeoutInMilliseconds}")
    int connectTimeoutInMilliseconds;
    @Value("#{gitHubConfig.client.responseTimeoutInSeconds}")
    long responseTimeoutInSeconds;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {

        HttpClient client = HttpClient.create().responseTimeout(Duration.ofSeconds(responseTimeoutInSeconds))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutInMilliseconds);

            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(client))
                    .baseUrl(gitHubBaseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "token " + gitHubToken)
                    .build();
    }
}
