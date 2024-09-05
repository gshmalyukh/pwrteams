package com.example.demo.model.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepoDetails {
    @JsonIgnore
    private String id;
    private String repoName;
    private String ownerLogin;
    private List<BranchDetails> branchDetailsList;

    public RepoDetails(RepoDetails other) {
        this.id = other.id;
        this.repoName = other.repoName;
        this.ownerLogin = other.ownerLogin;
        this.branchDetailsList = other.branchDetailsList; // Deep copy not needed
    }
}

