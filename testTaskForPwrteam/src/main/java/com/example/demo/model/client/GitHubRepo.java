package com.example.demo.model.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GitHubRepo {
    private String id;
    private String name;
    private Owner owner;
    private boolean fork;
    @JsonProperty("branches_url")
    private String branchesUrl;

    @Data
    public static class Owner {
        private String login;
    }
}