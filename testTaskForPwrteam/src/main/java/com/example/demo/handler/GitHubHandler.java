package com.example.demo.handler;

import com.example.demo.client.RepoFilter;
import com.example.demo.model.api.RepoDetails;
import com.example.demo.service.GitHubService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.ServerResponse.ok;


@Slf4j
@Component
public class GitHubHandler {
    private final GitHubService gitHubService;

    public GitHubHandler(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    public Mono<ServerResponse> getRepoDetails(ServerRequest serverRequest) {
        String filterParam = serverRequest.queryParam("filter").orElse("all");
        RepoFilter filter;
        try {
            filter = RepoFilter.fromString(filterParam);
        } catch (IllegalArgumentException ex) {
            return ServerResponse.badRequest()
                    .bodyValue("{\"status\": 400, \"message\": \"" + ex.getMessage() + "\"}");
        }

        final String username = serverRequest.pathVariable("username");
        return ok()
                .contentType(MediaType.APPLICATION_JSON).body(gitHubService.getRepoDetails(username, filter),
                        RepoDetails.class);
    }
}
