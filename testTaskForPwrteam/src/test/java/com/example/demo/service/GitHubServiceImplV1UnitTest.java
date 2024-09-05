package com.example.demo.service;

import com.example.demo.client.GitHubClient;
import com.example.demo.client.RepoFilter;
import com.example.demo.mapper.BranchDetailsMapper;
import com.example.demo.mapper.RepoDetailsMapper;
import com.example.demo.model.api.BranchDetails;
import com.example.demo.model.api.RepoDetails;
import com.example.demo.model.client.GitHubBranch;
import com.example.demo.model.client.GitHubRepo;
import com.example.demo.model.client.Page;
import com.example.demo.problem.RetryExhaustedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.mockito.Mock;
import org.springframework.http.HttpStatusCode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.http.HttpHeaders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

public class GitHubServiceImplV1UnitTest {
    private static final String LINK_NEXT_PATTERN = "(?<=<)([^>]+)(?=>;\\s*rel=\"next\")";
    private  static final String PLACEHOLDER_PATTERN = " \\{.*?\\}";
    private  static final int CLIENT_PAGE_SIZE = 3;

    private static final String REPO_ID = "12345";
    private static final String BRANCH_SHA = "5678";
    @Mock
    private GitHubClient gitHubClient;

    @Mock
    private BranchDetailsMapper branchDetailsMapper;

    @Mock
    private RepoDetailsMapper repoDetailsMapper;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache etagsCache;

    @Mock
    private Cache pagesCache;

    @Mock
    private Cache nextUrisCache;

    @InjectMocks
    private GitHubServiceImplV1 gitHubService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(cacheManager.getCache("etags")).thenReturn(etagsCache);
        when(cacheManager.getCache("pages")).thenReturn(pagesCache);
        when(cacheManager.getCache("nextUris")).thenReturn(nextUrisCache);

        when(etagsCache.get(any(String.class), eq(String.class))).thenReturn("etag");

        gitHubService.setLinkNextPattern(LINK_NEXT_PATTERN);
        gitHubService.setPlaceholderPattern(PLACEHOLDER_PATTERN);
        gitHubService.setClientPageSize(CLIENT_PAGE_SIZE);

        gitHubService.postConstruct();
    }

    @Test
    void testGetRepoDetails_Success() {
        String username = "testUser";
        RepoFilter filter = RepoFilter.ALL;
        GitHubRepo gitHubRepo = new GitHubRepo();
        gitHubRepo.setBranchesUrl("https://api.github.com/repos/apache/abdera/branches");
        gitHubRepo.setId(REPO_ID);

        RepoDetails repoDetails = new RepoDetails();
        repoDetails.setId(REPO_ID);
        GitHubBranch gitHubBranch = new GitHubBranch();
        List<GitHubBranch> branchList = Collections.singletonList(gitHubBranch);
        List<GitHubRepo> repoList = Collections.singletonList(gitHubRepo);
        Page repoPage = new Page(repoList, new HttpHeaders(), HttpStatusCode.valueOf(200), "repoUri");
        Page branchPage = new Page(branchList, new HttpHeaders(), HttpStatusCode.valueOf(200), "branchUri");

        when(gitHubClient.constructRepoUri(username)).thenReturn("repoUri");
        when(gitHubClient.fetchPage(any(String.class), any(String.class), eq(GitHubRepo.class)))
                .thenReturn(Mono.just(repoPage));
        when(gitHubClient.fetchPage(any(String.class), any(String.class), eq(GitHubBranch.class)))
                .thenReturn(Mono.just(branchPage));
        when(branchDetailsMapper.clientListToApiList(branchList)).
                thenReturn(Collections.singletonList(new BranchDetails("one", BRANCH_SHA)));
        when(repoDetailsMapper.toRepoDetails(gitHubRepo)).thenReturn(repoDetails);

        Flux<RepoDetails> result = gitHubService.getRepoDetails(username, filter);

        StepVerifier.create(result)
                .expectNextMatches(details -> details.getId().equals(REPO_ID) &&
                        details.getBranchDetailsList().get(0).branchSha().equals(BRANCH_SHA))
                .verifyComplete();
    }
}
