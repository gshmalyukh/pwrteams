package com.example.demo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class PwrteamApplicationTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(PwrteamApplicationTests.class);

    private static final String GITHUB_REPO_RESPONSE = """
            [
                {
                    "id": 822280514,
                    "name": "catalog-service",
                    "owner": {
                        "login": "gshmalyukh"
                    },
                     "fork": false,
                     "branches_url": "http://localhost:8080/repos/gshmalyukh/catalog-service/branches{/branch}"
                }
            ]
            """;

    private static final String GITHUB_REPO_RESPONSE_WITH_FOCKED_NONFOCKED = """
            [
                {
                    "id": 822280514,
                    "name": "catalog-service",
                    "owner": {
                        "login": "gshmalyukh"
                    },
                     "fork": false,
                     "branches_url": "http://localhost:8080/repos/gshmalyukh/catalog-service/branches{/branch}"
                },
                {
                    "id": 822280515,
                    "name": "catalog-client",
                    "owner": {
                        "login": "gshmalyukh"
                    },
                     "fork": true,
                     "branches_url": "http://localhost:8080/repos/gshmalyukh/catalog-client/branches{/branch}"
                }
            ]
            """;

    private static final String GITHUB_BRANCH_RESPONSE = """
            [
                {
                    "name": "main",
                     "commit": {
                        "sha": "catalog-service-sha"
                                }
                }
            ]
            """;
    private static final String GITHUB_BRANCH_RESPONSE_FOR_CATALOG_CLIENT = """
            [
                {
                    "name": "main",
                     "commit": {
                        "sha": "catalog-client-sha"
                                }
                }
            ]
            """;

    private static final String APP_EXPECTED_RESPONSE = "[{\"repoName\":\"catalog-service\",\"ownerLogin\":\"gshmalyukh\","
            + "\"branchDetailsList\":[{\"branchName\":\"main\",\"branchSha\":\"catalog-service-sha\"}]}]";
    private static final String APP_EXPECTED_RESPONSE_ALL = "[{\"repoName\":\"catalog-client\",\"ownerLogin\":\"gshmalyukh\",\"branchDetailsList\":[{\"branchName\":\"main\",\"branchSha\":\"catalog-client-sha\"}]},{\"repoName\":\"catalog-service\",\"ownerLogin\":\"gshmalyukh\",\"branchDetailsList\":[{\"branchName\":\"main\",\"branchSha\":\"catalog-service-sha\"}]}]";
    private static final String APP_EXPECTED_RESPONSE_NONFORKED = "[{\"repoName\":\"catalog-service\",\"ownerLogin\":\"gshmalyukh\",\"branchDetailsList\":[{\"branchName\":\"main\",\"branchSha\":\"catalog-service-sha\"}]}]";
    private static final String APP_EXPECTED_RESPONSE_FORKED = "[{\"repoName\":\"catalog-client\",\"ownerLogin\":\"gshmalyukh\",\"branchDetailsList\":[{\"branchName\":\"main\",\"branchSha\":\"catalog-client-sha\"}]}]";
    private static final String APP_NOT_FOUND_RESPONSE = "{\"message\":\"resource which you trying to obtain does not exist\",\"status\":404}";
    private static final String APP_NOT_ACCEPTABLE_RESPONSE = "{\"message\":\"Application response only in application/json\",\"status\":406}";
    private static final String APP_UNAUTHORIZED_RESPONSE = "{\"message\":\"failed  authorization on github, please check your token \",\"status\":401}";
    @LocalServerPort
    private int port;
    @Autowired
    private WebTestClient webClient;
    private static WireMockServer wireMockServer;

    @BeforeAll
    static void setWiremokServer() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldReturnSuccessful() {
        WireMock.configureFor("localhost", 8080);
        stubFor(get(urlEqualTo("/users/gshmalyukh/repos?per_page=3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "repoetag")
                        .withBody(GITHUB_REPO_RESPONSE)
                ));

        stubFor(get(urlEqualTo("/repos/gshmalyukh/catalog-service/branches?per_page=3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "branchetag")
                        .withBody(GITHUB_BRANCH_RESPONSE)
                ));

        this.webClient.get().
                uri("http://localhost:8081/users/gshmalyukh/repositories").
                exchange().expectStatus()
                .isOk()
                .expectBody(String.class).
                isEqualTo(APP_EXPECTED_RESPONSE);

        WireMock.reset();

        stubFor(get(urlEqualTo("/users/gshmalyukh/repos?per_page=3"))
                .willReturn(aResponse()
                        .withStatus(304)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "repoetag")
                ));

        stubFor(get(urlEqualTo("/repos/gshmalyukh/catalog-service/branches?per_page=3"))
                .willReturn(aResponse()
                        .withStatus(304)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "branchetag")
                ));

        this.webClient.get().
                uri("http://localhost:8081/users/gshmalyukh/repositories").
                exchange().expectStatus()
                .isOk()
                .expectBody(String.class).
                isEqualTo(APP_EXPECTED_RESPONSE);
    }

    @Test
    void shouldCheckFilter() {
        WireMock.configureFor("localhost", 8080);
        stubFor(get(urlEqualTo("/users/gshmalyukh/repos?per_page=3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "repoetag")
                        .withBody(GITHUB_REPO_RESPONSE_WITH_FOCKED_NONFOCKED)
                ));
        stubFor(get(urlEqualTo("/repos/gshmalyukh/catalog-service/branches?per_page=3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "branchetag")
                        .withBody(GITHUB_BRANCH_RESPONSE)
                ));
        stubFor(get(urlEqualTo("/repos/gshmalyukh/catalog-client/branches?per_page=3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("ETag", "branchetag")
                        .withBody(GITHUB_BRANCH_RESPONSE_FOR_CATALOG_CLIENT)
                ));

        this.webClient.get().
                uri("http://localhost:8081/users/gshmalyukh/repositories?filter=all").
                exchange().expectStatus()
                .isOk()
                .expectBody(String.class).
                isEqualTo(APP_EXPECTED_RESPONSE_ALL);

        this.webClient.get().
                uri("http://localhost:8081/users/gshmalyukh/repositories?filter=nonforked").
                exchange().expectStatus()
                .isOk()
                .expectBody(String.class).
                isEqualTo(APP_EXPECTED_RESPONSE_NONFORKED);

        this.webClient.get().
                uri("http://localhost:8081/users/gshmalyukh/repositories?filter=forked").
                exchange().expectStatus()
                .isOk()
                .expectBody(String.class).
                isEqualTo(APP_EXPECTED_RESPONSE_FORKED);
    }


    @Test
    void shouldReturnNotFound() {
        WireMock.configureFor("localhost", 8080);
        stubFor(get(urlEqualTo("/users/gshmalyukh/repos?per_page=3"))
                .willReturn(aResponse()
                        .withStatus(404)
                ));

        this.webClient.get().
                uri("http://localhost:8081/users/gshmalyukh/repositories").
                exchange().expectStatus()
                .isNotFound()
                .expectBody(String.class).
                isEqualTo(APP_NOT_FOUND_RESPONSE);
    }

    @Test
    void shouldReturnNotAcceptable() {
        this.webClient.get().
                uri("http://localhost:8081/users/gshmalyukh/repositories")
                .accept(MediaType.APPLICATION_XML)
                .exchange().expectStatus()
                .isEqualTo(406)
                .expectBody(String.class).
                isEqualTo(APP_NOT_ACCEPTABLE_RESPONSE);
    }

    @Test
    void shouldReturnUnauthorized() {
        WireMock.configureFor("localhost", 8080);
        stubFor(get(urlEqualTo("/users/gshmalyukh/repos?per_page=3"))
                .willReturn(aResponse()
                        .withStatus(401)
                ));

        this.webClient.get().
                uri("http://localhost:8081/users/gshmalyukh/repositories")
                .accept(MediaType.APPLICATION_JSON)
                .exchange().expectStatus()
                .isUnauthorized()
                .expectBody(String.class).
                isEqualTo(APP_UNAUTHORIZED_RESPONSE);
    }
}
