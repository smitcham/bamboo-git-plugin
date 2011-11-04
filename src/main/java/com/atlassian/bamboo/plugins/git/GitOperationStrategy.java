package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.repository.RepositoryException;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * TODO: Document this class / interface here
 *
 * @since v5.0
 */
public interface GitOperationStrategy
{
    void fetch(@NotNull final File sourceDirectory, @NotNull final GitRepository.GitRepositoryAccessData accessData, boolean useShallow) throws RepositoryException;
}