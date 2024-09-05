package com.example.demo.client;

import com.example.demo.model.client.Page;
import reactor.core.publisher.Mono;

public interface GitHubClient {
    String constructRepoUri(String username);
    <T> Mono<Page<T>> fetchPage(String uri, String pageEtag, Class<T> responseType);
}
