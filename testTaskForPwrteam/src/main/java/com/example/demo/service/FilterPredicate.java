package com.example.demo.service;

import com.example.demo.client.RepoFilter;
import com.example.demo.model.client.GitHubRepo;
import java.util.function.Predicate;

public class FilterPredicate implements Predicate<GitHubRepo> {
    private final RepoFilter filter;
    public FilterPredicate(RepoFilter filter){
        this.filter = filter;
    }
    @Override
    public boolean test(GitHubRepo repo) {
        return switch (filter) {
            case FORKED -> repo.isFork();
            case NONFORKED -> !repo.isFork();
            default -> true;
        };
    }
}
