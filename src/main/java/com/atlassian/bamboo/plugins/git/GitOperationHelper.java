package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.commit.CommitContext;
import com.atlassian.bamboo.plan.branch.VcsBranch;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.v2.build.BuildRepositoryChanges;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface GitOperationHelper
{
    void pushRevision(@NotNull File sourceDirectory, @NotNull String revision) throws RepositoryException;

    String commit(@NotNull File sourceDirectory, @NotNull String message, @NotNull String comitterName, @NotNull String comitterEmail) throws RepositoryException;

    /*
    * returns revision found after checkout in sourceDirectory
    */
    @NotNull
    String checkout(@Nullable File cacheDirectory,
                    @NotNull File sourceDirectory,
                    @NotNull String targetRevision,
                    @Nullable String previousRevision) throws RepositoryException;

    void fetch(@NotNull File sourceDirectory, boolean useShallow) throws RepositoryException;

    @NotNull
    String getCurrentRevision(@NotNull File sourceDirectory) throws RepositoryException;

    @Nullable
    String getRevisionIfExists(@NotNull File sourceDirectory, @NotNull String revision);

    @NotNull
    String obtainLatestRevision() throws RepositoryException;

    @NotNull
    List<VcsBranch> getOpenBranches(@NotNull GitRepository.GitRepositoryAccessData repositoryData) throws RepositoryException;

    boolean checkRevisionExistsInCacheRepository(@NotNull File repositoryDirectory, @NotNull String targetRevision) throws IOException;

    @Nullable
    CommitContext getCommit(File directory, String targetRevision) throws RepositoryException;

    boolean merge(@NotNull File workspaceDir, @NotNull String targetRevision, @NotNull String committerName, @NotNull String committerEmail) throws RepositoryException;

    BuildRepositoryChanges extractCommits(File cacheDirectory, String lastVcsRevisionKey, String targetRevision) throws RepositoryException;
}
