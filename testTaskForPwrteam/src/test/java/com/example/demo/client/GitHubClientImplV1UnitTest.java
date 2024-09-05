package com.example.demo.client;

import com.example.demo.model.client.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


public class GitHubClientImplV1UnitTest {
    private static final String X_GITHUB_API_VERSION = "2022-11-28";
    private static final int CLIENT_PAGE_SIZE = 3;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int MIN_BACKOFF = 500;
    private static final int MAX_BACKOFF_IN_SECONDS = 2;
    private static final String BASE_URL = "https://api.github.com";
    private static final String ENDPOINT = "/users/{username}/repos";
    private static final String PLACEHOLDER_PATTERN =  "\\{.*?\\}";

    @Mock
    private WebClient webClient;
    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @InjectMocks
    private GitHubClientImplV1 gitHubClient;

    @BeforeEach
    void setGitHubConfigProperties(){
        MockitoAnnotations.openMocks(this);
        gitHubClient.setXGitHubApiVersion(X_GITHUB_API_VERSION);
        gitHubClient.setClientPageSize(CLIENT_PAGE_SIZE);
        gitHubClient.setMaxRetryAttempts(MAX_RETRY_ATTEMPTS);
        gitHubClient.setMinBackOff(MIN_BACKOFF);
        gitHubClient.setMaxBackoffInSeconds(MAX_BACKOFF_IN_SECONDS);
        gitHubClient.setBaseUrl(BASE_URL);
        gitHubClient.setEndpoint(ENDPOINT);
        gitHubClient.setPlaceholderPattern(PLACEHOLDER_PATTERN);

    }

    @Test
    public void testConstructRepoUri() {
        String username = "testuser";
        String expectedUri = "https://api.github.com/users/testuser/repos?per_page=3";

        String constructedUri = gitHubClient.constructRepoUri(username);

        assertEquals(expectedUri, constructedUri);
    }

    @Test
    void fetchPage_shouldReturnPage() {
        // Prepare mock data
        String uri = "http://example.com";
        String pageEtag = "etag";
        Class<String> responseType = String.class;



        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.LINK, "<http://example.com/next>; rel=\"next\"");
        Page<String> mockPage = new Page<>();
        mockPage.setBody(List.of("item1", "item2"));
        mockPage.setHeaders(headers);
        mockPage.setStatusCode(HttpStatus.OK);
        mockPage.setUri(uri);

        // Mock WebClient behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq(uri))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq("X-GitHub-Api-Version"), eq(X_GITHUB_API_VERSION))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(eq("if-none-match"), eq(pageEtag))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntityList(eq(responseType)))
                .thenReturn(Mono.just(new ResponseEntity<>(mockPage.getBody(), headers, HttpStatus.OK)));

        // Call the method and verify results
        StepVerifier.create(gitHubClient.fetchPage(uri, pageEtag, responseType))
                .expectNextMatches(page -> page.getBody().equals(mockPage.getBody()) &&
                        page.getHeaders().equals(mockPage.getHeaders()) &&
                        page.getStatusCode().equals(mockPage.getStatusCode()) &&
                        page.getUri().equals(mockPage.getUri()))
                .verifyComplete();
    }
}
