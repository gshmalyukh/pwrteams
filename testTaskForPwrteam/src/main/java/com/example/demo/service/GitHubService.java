package com.example.demo.service;

import com.example.demo.client.RepoFilter;
import com.example.demo.model.api.RepoDetails;
import reactor.core.publisher.Flux;

public interface GitHubService {
    Flux<RepoDetails> getRepoDetails(String username, RepoFilter repoFilter);
}
