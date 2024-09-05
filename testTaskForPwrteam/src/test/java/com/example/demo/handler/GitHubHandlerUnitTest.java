package com.example.demo.handler;

import com.example.demo.client.RepoFilter;
import com.example.demo.model.api.BranchDetails;
import com.example.demo.model.api.RepoDetails;
import com.example.demo.service.GitHubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class GitHubHandlerUnitTest {
    private static final String  ID = "11245";
    private static final  String REPO_NAME = "one";
    private static final  String OWNER_LOGIN = "two";
    private static final  String BRANCH_NAME = "branch";
    private static final  String BRANCH_SHA = "sha";

    @InjectMocks
    private GitHubHandler gitHubHandler;

    @Mock
    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getRepoDetails_validFilter_returnsRepoDetails() {
        // Prepare mock data
        String username = "testuser";
        RepoFilter filter = RepoFilter.ALL; // or RepoFilter.fromString("validFilterValue")
        BranchDetails branchDetails1 = new BranchDetails(BRANCH_NAME, BRANCH_SHA);
        BranchDetails branchDetails2 = new BranchDetails(BRANCH_NAME + 1, BRANCH_SHA + 1);
        List<BranchDetails> branchDetailsList = new ArrayList<>(2);
        branchDetailsList.add(branchDetails1);
        branchDetailsList.add(branchDetails2);
        RepoDetails repoDetails = new RepoDetails(ID, REPO_NAME, OWNER_LOGIN, branchDetailsList);

        Flux<RepoDetails> repoDetailsFlux = Flux.just(repoDetails);
        // Mock the service call
        when(gitHubService.getRepoDetails(username, filter))
                .thenReturn(repoDetailsFlux);

        // Create a mock ServerRequest
        ServerRequest serverRequest = mock(ServerRequest.class);
        when(serverRequest.queryParam("filter")).thenReturn(Optional.of("all"));
        when(serverRequest.pathVariable("username")).thenReturn(username);

        // Call the handler method
        Mono<ServerResponse> responseMono = gitHubHandler.getRepoDetails(serverRequest);

        // Verify the response
        StepVerifier.create(responseMono)
                .expectNextMatches(response -> {
                    // Check the status code
                    return response.statusCode().equals(HttpStatus.OK);
                })
                .verifyComplete();
    }

    @Test
    void getRepoDetails_invalidFilter_returnsBadRequest() {
        // Prepare mock data
        String username = "testuser";
        String invalidFilter = "invalidFilter";
        String expectedErrorMessage = "Invalid filter";

        // Create a mock ServerRequest
        ServerRequest serverRequest = mock(ServerRequest.class);
        when(serverRequest.queryParam("filter")).thenReturn(Optional.of("invalid"));
        when(serverRequest.pathVariable("username")).thenReturn(username);

        // Call the handler method
        Mono<ServerResponse> responseMono = gitHubHandler.getRepoDetails(serverRequest);

        // Verify the response
        StepVerifier.create(responseMono)
                .expectNextMatches(response -> {
                    // Check the status code
                    return response.statusCode().equals(HttpStatus.BAD_REQUEST);
                })
                .verifyComplete();
    }
}
