package com.example.demo.client;

import com.example.demo.model.client.Page;
import com.example.demo.problem.RetryExhaustedException;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Slf4j
@ClientVersion("1.0.0")
@Component("gitHubClient")
@Setter
public class GitHubClientImplV1 implements GitHubClient {

    @Value("#{gitHubConfig.baseUrl}")
    String baseUrl;
    @Value("#{gitHubConfig.endpoint}")
    String endpoint;
    @Value("#{gitHubConfig.XGitHubApiVersion}")
    private String XGitHubApiVersion;
    @Value("#{gitHubConfig.client.pageSize}")
    private int clientPageSize;
    @Value("#{gitHubConfig.client.maxRetryAttempts}")
    private int maxRetryAttempts;
    @Value("#{gitHubConfig.client.minBackOff}")
    private int minBackOff;
    @Value("#{gitHubConfig.client.maxBackoffInSeconds}")
    private int maxBackoffInSeconds;
    @Value("#{gitHubConfig.placeholderPattern}")
    private String placeholderPattern;
    private final WebClient webClient;

    public GitHubClientImplV1(WebClient webClient) {
        this.webClient = webClient;
    }

    public String constructRepoUri(String username) {
        return baseUrl + endpoint.replaceAll(placeholderPattern, username) + "?per_page=" + clientPageSize;
    }
    public <T> Mono<Page<T>> fetchPage(String uri, String pageEtag, Class<T> responseType) {
        Retry retrySpec = Retry.backoff(maxRetryAttempts, Duration.ofMillis(minBackOff))
                .maxBackoff(Duration.ofSeconds(maxBackoffInSeconds))  // Maximum backoff per retry
                .filter(throwable ->
                        throwable instanceof ReadTimeoutException ||
                                throwable instanceof ConnectTimeoutException ||
                                throwable instanceof WebClientRequestException
                )
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                    Throwable lastThrowable = retrySignal.failure();
                    throw new RetryExhaustedException("Retries exhausted: " + retrySignal.totalRetriesInARow() +
                            " attempts", lastThrowable, uri, responseType);
                });

        WebClient.RequestHeadersSpec<?> requestSpec = webClient.get()
                .uri(uri)
                .header("X-GitHub-Api-Version", XGitHubApiVersion);

        if (StringUtils.hasLength(pageEtag)) {
            requestSpec.header("if-none-match", pageEtag);
        }

        return requestSpec.retrieve()
                .toEntityList(responseType)
                .retryWhen(retrySpec)
                .map(responseEntity -> {
                    List<T> body = responseEntity.getBody();
                    HttpHeaders headers = responseEntity.getHeaders();
                    HttpStatusCode statusCode = responseEntity.getStatusCode();
                    return new Page<>(body, headers, statusCode, uri);
                });
    }
}
