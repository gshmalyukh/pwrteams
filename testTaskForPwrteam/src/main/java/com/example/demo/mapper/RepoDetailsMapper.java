package com.example.demo.mapper;

import com.example.demo.model.api.RepoDetails;
import com.example.demo.model.client.GitHubRepo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RepoDetailsMapper {
    @Mapping( target = "repoName", source = "name" )
    @Mapping( target = "ownerLogin", source = "owner.login" )
    @Mapping(target = "branchDetailsList", ignore = true)
    RepoDetails toRepoDetails(GitHubRepo repo);
}
