package com.example.demo.mapper;

import com.example.demo.model.api.BranchDetails;
import com.example.demo.model.client.GitHubBranch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BranchDetailsMapper {
    @Mapping( target = "branchName", source = "name" )
    @Mapping( target = "branchSha", source = "commit.sha" )
    BranchDetails toBranchDetails(GitHubBranch branch);
    List<BranchDetails> clientListToApiList(List<GitHubBranch> entity);
}
