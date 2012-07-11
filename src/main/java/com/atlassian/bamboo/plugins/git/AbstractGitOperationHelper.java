package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.utils.SystemProperty;
import com.atlassian.sal.api.message.I18nResolver;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Constants;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractGitOperationHelper implements GitOperationHelper
{
    private static final Logger log = Logger.getLogger(AbstractGitOperationHelper.class);

    protected static final int DEFAULT_TRANSFER_TIMEOUT = new SystemProperty(false, "atlassian.bamboo.git.timeout", "GIT_TIMEOUT").getValue(10 * 60);
    protected static final int CHANGESET_LIMIT = new SystemProperty(false, "atlassian.bamboo.git.changeset.limit", "GIT_CHANGESET_LIMIT").getValue(100);

    protected static final String[] FQREF_PREFIXES = {Constants.R_HEADS, Constants.R_REFS};

    protected final GitRepository.GitRepositoryAccessData accessData;
    // ------------------------------------------------------------------------------------------------- Type Properties
    // ---------------------------------------------------------------------------------------------------- Dependencies
    protected final BuildLogger buildLogger;
    protected final I18nResolver i18nResolver;

    public AbstractGitOperationHelper(final GitRepository.GitRepositoryAccessData accessData,
                                      final @NotNull BuildLogger buildLogger,
                                      final @NotNull I18nResolver i18nResolver)
    {
        this.accessData = accessData;
        this.buildLogger = buildLogger;
        this.i18nResolver = i18nResolver;
    }
}
