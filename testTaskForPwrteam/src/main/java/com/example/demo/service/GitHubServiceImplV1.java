package com.example.demo.service;

import com.example.demo.client.GitHubClient;
import com.example.demo.client.RepoFilter;
import com.example.demo.mapper.BranchDetailsMapper;
import com.example.demo.mapper.RepoDetailsMapper;
import com.example.demo.model.api.RepoDetails;
import com.example.demo.model.client.GitHubBranch;
import com.example.demo.model.client.GitHubRepo;
import com.example.demo.model.client.Page;
import com.example.demo.problem.NotFoundException;
import com.example.demo.problem.RetryExhaustedException;
import com.example.demo.problem.ServiceException;
import com.example.demo.problem.UnauthorizedException;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ServiceVersion("1.0.0")
@Slf4j
@Setter
@Service("gitHubService")
public class GitHubServiceImplV1 implements GitHubService {

    @Value("#{gitHubConfig.linkNextPattern}")
    private String linkNextPattern;
    @Value("#{gitHubConfig.placeholderPattern}")
    private String placeholderPattern;
    @Value("#{gitHubConfig.client.pageSize}")
    private int clientPageSize;

    private final GitHubClient gitHubClient;
    private final BranchDetailsMapper branchDetailsMapper;
    private final RepoDetailsMapper repoDetailsMapper;
    private final CacheManager cacheManager;
    private Cache etagsCache;
    private Cache pagesCache;
    private Cache nextUrisCache;
    private Pattern nextPattern;

    public GitHubServiceImplV1(GitHubClient gitHubClient, CacheManager cacheManager,
                               BranchDetailsMapper branchDetailsMapper, RepoDetailsMapper repoDetailsMapper) {
        this.gitHubClient = gitHubClient;
        this.cacheManager = cacheManager;
        this.branchDetailsMapper = branchDetailsMapper;
        this.repoDetailsMapper = repoDetailsMapper;

    }

    @PostConstruct
    public void postConstruct() {
        pagesCache = cacheManager.getCache("pages");
        etagsCache = cacheManager.getCache("etags");
        nextUrisCache = cacheManager.getCache("nextUris");

        nextPattern = Pattern.compile(linkNextPattern);
    }

    public Flux<RepoDetails> getRepoDetails(String username, RepoFilter filter) {
        return getRepositories(username, filter).flatMap(this::fillRepoDetails).onErrorResume(throwable ->
        {
            if (throwable instanceof UnauthorizedException || throwable instanceof NotFoundException ||
            throwable instanceof RetryExhaustedException) {
                return Mono.error(throwable);
            } else {
                return Mono.error(
                        new ServiceException("Unexpected exception while getting data", throwable)
                );
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<RepoDetails> fillRepoDetails(GitHubRepo gitHubRepo) {
        return getBranches(gitHubRepo.getBranchesUrl())
                .collectList()
                .map(branchDetailsMapper::clientListToApiList)
                .map(branches -> {
                            RepoDetails repoDetails = repoDetailsMapper.toRepoDetails(gitHubRepo);
                            repoDetails.setBranchDetailsList(branches);
                            return repoDetails;
                        }
                );
    }

    private Flux<GitHubRepo> getRepositories(String username, RepoFilter filter) {
        String uri = gitHubClient.constructRepoUri(username);
        String pageEtag = etagsCache.get(uri, String.class);
        FilterPredicate filterPredicate = new FilterPredicate(filter);
        return getEntities(uri, pageEtag, GitHubRepo.class).filter(filterPredicate);
    }

    private Flux<GitHubBranch> getBranches(String branchesUrl) {
        String uri = branchesUrl.replaceAll(placeholderPattern, "") +
                "?per_page=" + clientPageSize;
        String pageEtag = etagsCache.get(uri, String.class);
        return getEntities(uri, pageEtag, GitHubBranch.class);
    }

    private <T> Flux<T> getEntities(String uri, String pageEtag, Class<T> responseType) {
        return gitHubClient.fetchPage(uri, pageEtag, responseType)
                .onErrorResume(throwable -> handlePageFetchError(throwable, uri, responseType))
                .expand(response -> {
                    Optional<String> nextUrl = getNextUrl(response);

                    if (nextUrl.isPresent() && response.getStatusCode().is2xxSuccessful()) {
                        nextUrisCache.put(response.getUri(), nextUrl.get());
                    }

                    Optional<String> nextPageEtag = nextUrl.map(next -> etagsCache.get(next, String.class));

                    return nextUrl.map(next_url -> gitHubClient.fetchPage(next_url, nextPageEtag.orElse(null), responseType)
                            .onErrorResume(throwable -> handlePageFetchError(throwable, next_url, responseType))).orElseGet(Mono::empty);
                })
                .flatMap(this::parseData);

    }

    private <T> Mono<Page<T>> handlePageFetchError(Throwable throwable, String uri, Class<T> responseType) {
        if(throwable instanceof WebClientResponseException){
           HttpStatusCode statusCode = ((WebClientResponseException) throwable).getStatusCode();
           if(statusCode.value() == 401){
               return Mono.error(new UnauthorizedException("failed  authorization on github, please check your token ", throwable));
           } else if (statusCode.value() == 404){
               return Mono.error(new NotFoundException("resource which you trying to obtain does not exist", throwable));
           }
            return Mono.error(throwable);
        } else if (throwable instanceof RetryExhaustedException) {
            // Attempt to retrieve cached data from the cache
            List<T> cachedData = pagesCache.get(uri, List.class);
            if (cachedData != null) {
                log.info("Returning cached data for URI due to RetryExhaustedException: " + uri);
                Page<T> cachedPage = new Page<>(cachedData, new HttpHeaders(), HttpStatusCode.valueOf(304), uri);
                return Mono.just(cachedPage);
            }
            //it is possible to return page from cache but need to clarify with business
            return Mono.error(throwable);
        }
        return Mono.error(throwable);
    }

    private <T> Flux<T> parseData(Page<T> response) {
        Optional<List<T>> data = Optional.ofNullable(response.getBody());
        HttpHeaders headers = response.getHeaders();
        HttpStatusCode statusCode = response.getStatusCode();
        String uri = response.getUri();
        Optional<String> eTag = Optional.ofNullable(headers.getETag());

        if (statusCode.value() == 304) {
            List<T> cachedRepos = pagesCache.get(uri, List.class);
            if(cachedRepos != null){
                eTag.ifPresent(tag -> etagsCache.put(uri, tag));
                log.info("getting cached page for uri: " + uri);
                return Flux.fromIterable(cachedRepos);
            }
        }

        if (data.isEmpty() || data.get().isEmpty()) {
            return Flux.empty();
        }

        eTag.ifPresent(tag -> {
            etagsCache.put(uri, tag);
            pagesCache.put(uri, data.get());
        });

        return Flux.fromIterable(data.get());
    }

    private Optional<String> getNextUrl(Page<?> response) {
        if (response.getStatusCode().is3xxRedirection()) {
            return Optional.ofNullable(nextUrisCache.get(response.getUri(), String.class));
        }
        String linkHeader = response.getHeaders().getFirst(HttpHeaders.LINK);
        if (linkHeader == null) {
            return Optional.empty();
        }
        Matcher matcher = nextPattern.matcher(linkHeader);
        return Optional.ofNullable(matcher.find() ? matcher.group() : null);
    }
}





