package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.commit.CommitContext;
import com.atlassian.bamboo.core.RepositoryUrlObfuscator;
import com.atlassian.bamboo.plan.branch.VcsBranch;
import com.atlassian.bamboo.plan.branch.VcsBranchImpl;
import com.atlassian.bamboo.repository.InvalidRepositoryException;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.ssh.ProxyConnectionData;
import com.atlassian.bamboo.ssh.ProxyConnectionDataBuilder;
import com.atlassian.bamboo.ssh.ProxyException;
import com.atlassian.bamboo.ssh.SshProxyService;
import com.atlassian.bamboo.utils.Pair;
import com.atlassian.bamboo.v2.build.BuildRepositoryChanges;
import com.atlassian.bamboo.v2.build.BuildRepositoryChangesImpl;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.storage.file.RefDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NativeGitOperationHelper extends AbstractGitOperationHelper implements GitOperationHelper
{
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = Logger.getLogger(GitRepository.class);
    private static final String GIT_SCHEME = "git";
    // ------------------------------------------------------------------------------------------------------- Constants
    // ------------------------------------------------------------------------------------------------- Type Properties
    protected SshProxyService sshProxyService;
    GitCommandProcessor gitCommandProcessor;
    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors

    public NativeGitOperationHelper(final @NotNull GitRepository repository,
                                    final @NotNull GitRepository.GitRepositoryAccessData accessData,
                                    final @NotNull SshProxyService sshProxyService,
                                    final @NotNull BuildLogger buildLogger,
                                    final @NotNull I18nResolver i18nResolver) throws RepositoryException
    {
        super(accessData, buildLogger, i18nResolver);
        this.sshProxyService = sshProxyService;
        this.gitCommandProcessor = new GitCommandProcessor(repository.getGitCapability(), buildLogger, accessData.commandTimeout, accessData.verboseLogs);
        this.gitCommandProcessor.checkGitExistenceInSystem(repository.getWorkingDirectory());
        this.gitCommandProcessor.setSshCommand(repository.getSshCapability());
    }

    // ----------------------------------------------------------------------------------------------- Interface Methods

    @Override
    public void pushRevision(@NotNull final File sourceDirectory, @NotNull String revision) throws RepositoryException
    {
        String possibleBranch = gitCommandProcessor.getPossibleBranchNameForCheckout(sourceDirectory, revision);
        if (StringUtils.isBlank(possibleBranch))
        {
            throw new RepositoryException("Can't guess branch name for revision " + revision + " when trying to perform push.");
        }
        final GitRepository.GitRepositoryAccessData proxiedAccessData = adjustRepositoryAccess(accessData);
        GitCommandBuilder commandBuilder = gitCommandProcessor.createCommandBuilder("push", proxiedAccessData.repositoryUrl, possibleBranch);
        if (proxiedAccessData.verboseLogs)
        {
            commandBuilder.verbose(true);
        }
        gitCommandProcessor.runCommand(commandBuilder, sourceDirectory);
    }

    @Override
    public String commit(@NotNull File sourceDirectory, @NotNull String message, @NotNull String comitterName, @NotNull String comitterEmail) throws RepositoryException
    {
        if (!containsSomethingToCommit(sourceDirectory))
        {
            log.debug("Nothing to commit");
            return getCurrentRevision(sourceDirectory);
        }

        GitCommandBuilder commandBuilder = gitCommandProcessor
                .createCommandBuilder("commit", "--all", "-m", message)
                .env(identificationVariables(comitterName, comitterEmail));

        if (accessData.verboseLogs)
        {
            commandBuilder.verbose(true);
        }
        gitCommandProcessor.runCommand(commandBuilder, sourceDirectory);
        return getCurrentRevision(sourceDirectory);
    }

    public ImmutableMap<String, String> identificationVariables(@NotNull String name, @NotNull String email)
    {
        return ImmutableMap.of(
                "GIT_COMMITTER_NAME", name, //needed for merge
                "GIT_COMMITTER_EMAIL", email, //otherwise warning on commit
                "GIT_AUTHOR_NAME", name, //needed for commit
                "GIT_AUTHOR_EMAIL", email); //not required
    }

    // -------------------------------------------------------------------------------------------------- Action Methods
    // -------------------------------------------------------------------------------------------------- Public Methods
    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators

    protected GitRepository.GitRepositoryAccessData adjustRepositoryAccess(@NotNull final GitRepository.GitRepositoryAccessData accessData) throws RepositoryException
    {
        final boolean sshKeypair = accessData.authenticationType == GitAuthenticationType.SSH_KEYPAIR;
        final boolean sshWithPassword = UriUtils.isSsh(accessData.repositoryUrl) && accessData.authenticationType == GitAuthenticationType.PASSWORD;
        final boolean needsProxy = sshKeypair || sshWithPassword;
        if (needsProxy)
        {
            final GitRepository.GitRepositoryAccessData proxyAccessData = accessData.cloneAccessData();

            ScpAwareUri repositoryUri = ScpAwareUri.create(proxyAccessData.repositoryUrl);

            if (GIT_SCHEME.equals(repositoryUri.getScheme()) || UriUtils.isSsh(repositoryUri))
            {
                try
                {
                    String username = UriUtils.extractUsername(proxyAccessData.repositoryUrl);
                    if (username != null)
                    {
                        proxyAccessData.username = username;
                    }

                    final ProxyConnectionDataBuilder proxyConnectionDataBuilder =
                            sshProxyService.createProxyConnectionDataBuilder()
                                    .withRemoteAddress(repositoryUri.getHost(), repositoryUri.getPort() == -1 ? 22 : repositoryUri.getPort())
                                    .withRemoteUserName(StringUtils.defaultIfEmpty(proxyAccessData.username, repositoryUri.getUserInfo()))
                                    .withErrorReceiver(gitCommandProcessor);

                    if (repositoryUri.isRelativePath())
                    {
                        proxyConnectionDataBuilder.withRemotePathMapping(repositoryUri.getAbsolutePath(), repositoryUri.getRawPath());
                    }

                    switch (accessData.authenticationType)
                    {
                        case SSH_KEYPAIR:
                            proxyConnectionDataBuilder.withKeyFromString(proxyAccessData.sshKey, proxyAccessData.sshPassphrase);
                            break;
                        case PASSWORD:
                            proxyConnectionDataBuilder.withRemotePassword(StringUtils.defaultString(proxyAccessData.password));
                            break;
                        default:
                            throw new IllegalArgumentException("Proxy does not know how to handle " + accessData.authenticationType);
                    }

                    final ProxyConnectionData connectionData = proxyConnectionDataBuilder.build();

                    proxyAccessData.proxyRegistrationInfo = sshProxyService.register(connectionData);

                    final URI repositoryViaProxy = UriUtils.getUriViaProxy(proxyAccessData, repositoryUri);

                    proxyAccessData.repositoryUrl = repositoryViaProxy.toString();
                }
                catch (IOException e)
                {
                    if (e.getMessage().contains("exception using cipher - please check password and data."))
                    {
                        throw new RepositoryException(buildLogger.addErrorLogEntry("Encryption exception - please check ssh keyfile passphrase."), e);
                    }
                    else
                    {
                        throw new RepositoryException("Cannot decode connection params", e);
                    }
                }
                catch (ProxyException e)
                {
                    throw new RepositoryException("Cannot create SSH proxy", e);
                }
                catch (URISyntaxException e)
                {
                    throw new RepositoryException("Remote repository URL invalid", e);
                }

                return proxyAccessData;
            }
        }
        else
        {
            if (accessData.authenticationType == GitAuthenticationType.PASSWORD)
            {
                GitRepository.GitRepositoryAccessData credentialsAwareAccessData = accessData.cloneAccessData();
                URI repositoryUrl = wrapWithUsernameAndPassword(credentialsAwareAccessData);
                credentialsAwareAccessData.repositoryUrl = repositoryUrl.toString();

                return credentialsAwareAccessData;
            }
        }

        return accessData;
    }


    /**
     * @return true if modified files exist in the directory or current revision in the directory has changed
     */
    @Override
    public boolean merge(@NotNull final File workspaceDir, @NotNull final String targetRevision,
                         @NotNull String committerName, @NotNull String committerEmail) throws RepositoryException
    {
        GitCommandBuilder commandBuilder =
                gitCommandProcessor
                        .createCommandBuilder("merge", "--no-commit", targetRevision)
                        .env(identificationVariables(committerName, committerEmail));

        String headRevisionBeforeMerge = getCurrentRevision(workspaceDir);
        gitCommandProcessor.runMergeCommand(commandBuilder, workspaceDir);

        if (containsSomethingToCommit(workspaceDir))
        {
            return true;
        }
        //fast forward merge check
        String headRevisionAfterMerge = getCurrentRevision(workspaceDir);
        log.debug("Revision before merge: " + headRevisionBeforeMerge + ", after merge: " + headRevisionAfterMerge);
        return !headRevisionAfterMerge.equals(headRevisionBeforeMerge);
    }

    private boolean containsSomethingToCommit(@NotNull File workspaceDir) throws RepositoryException
    {
        //check for merge with no changes to files, but with changes to index
        final String mergeHead = getRevisionIfExists(workspaceDir, Constants.MERGE_HEAD);
        if (mergeHead!=null)
        {
            log.debug("Has modified index");
            return true;
        }

        final List<String> strings = gitCommandProcessor.runStatusCommand(workspaceDir);
        final boolean hasModifiedFiles = !strings.isEmpty();
        if (hasModifiedFiles)
        {
            log.debug("Has modified files");
        }
        return hasModifiedFiles;
    }

    @NotNull
    private URI wrapWithUsernameAndPassword(GitRepository.GitRepositoryAccessData repositoryAccessData)
    {
        try
        {
            URI remoteUri = new URI(repositoryAccessData.repositoryUrl);
            return new URI(remoteUri.getScheme(),
                           getAuthority(repositoryAccessData),
                           remoteUri.getHost(),
                           remoteUri.getPort(),
                           remoteUri.getPath(),
                           remoteUri.getQuery(),
                           remoteUri.getFragment());
        }
        catch (URISyntaxException e)
        {
            // can't really happen
            final String message = "Cannot parse remote URI: " + repositoryAccessData.repositoryUrl;
            NativeGitOperationHelper.log.error(message, e);
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private String getAuthority(final GitRepository.GitRepositoryAccessData repositoryAccessData) {
        final String username = repositoryAccessData.username;
        if (StringUtils.isEmpty(username))
        {
            return null;
        }

        String repositoryUrl = repositoryAccessData.repositoryUrl;
        
        final boolean passwordAuthentication = repositoryAccessData.authenticationType == GitAuthenticationType.PASSWORD;

        if (!passwordAuthentication || UriUtils.isSsh(repositoryUrl))
        {
            return username;
        }

        String password = repositoryAccessData.password;

        final boolean isHttpBased = repositoryUrl.startsWith("http://") || repositoryUrl.startsWith("https://");
        if (isHttpBased && StringUtils.isBlank(password))
        {
            password = "none"; //otherwise we'll get a password prompt
        }

        return StringUtils.isNotBlank(password) ? (username + ":" + password) : username;
    }

    private void createLocalRepository(final File sourceDirectory, final File cacheDirectory) throws RepositoryException, IOException
    {
        if (!sourceDirectory.exists())
        {
            sourceDirectory.mkdirs();
        }
        File gitDirectory = new File(sourceDirectory, Constants.DOT_GIT);
        String headRef = null;
        File cacheGitDir = null;
        File alternateObjectDir = null;
        if (cacheDirectory != null && cacheDirectory.exists())
        {
            cacheGitDir = new File(cacheDirectory, Constants.DOT_GIT);
            File objectsCache = new File(cacheGitDir, "objects");
            if (objectsCache.exists())
            {
                alternateObjectDir = objectsCache;
                headRef = FileUtils.readFileToString(new File(cacheGitDir, Constants.HEAD));
            }
        }

        if (!gitDirectory.exists())
        {
            buildLogger.addBuildLogEntry(i18nResolver.getText("repository.git.messages.creatingGitRepository", gitDirectory));
            gitCommandProcessor.runInitCommand(sourceDirectory);
        }

        // lets update alternatives here for a moment
        if (alternateObjectDir !=null)
        {
            List<String> alternatePaths = new ArrayList<String>(1);
            alternatePaths.add(alternateObjectDir.getAbsolutePath());
            final File alternates = new File(new File(new File(gitDirectory, "objects"), "info"), "alternates");
            FileUtils.writeLines(alternates, alternatePaths, "\n");
        }

        if (cacheGitDir != null && cacheGitDir.isDirectory())
        {
            // copy tags and branches heads from the cache repository
            FileUtils.copyDirectoryToDirectory(new File(cacheGitDir, Constants.R_TAGS), new File(gitDirectory, Constants.R_REFS));
            FileUtils.copyDirectoryToDirectory(new File(cacheGitDir, Constants.R_HEADS), new File(gitDirectory, Constants.R_REFS));

            File shallow = new File(cacheGitDir, "shallow");
            if (shallow.exists())
            {
                FileUtils.copyFileToDirectory(shallow, gitDirectory);
            }
        }

        if (StringUtils.startsWith(headRef, RefDirectory.SYMREF))
        {
            FileUtils.writeStringToFile(new File(gitDirectory, Constants.HEAD), headRef);
        }
    }

    protected void closeProxy(@NotNull final GitRepository.GitRepositoryAccessData accessData)
    {
        sshProxyService.unregister(accessData.proxyRegistrationInfo);
    }

    @NotNull
    @Override
    public String checkout(@Nullable final File cacheDirectory, @NotNull final File sourceDirectory, @NotNull final String targetRevision, @Nullable final String previousRevision) throws RepositoryException
    {
        // would be cool to store lastCheckoutedRevision in the localRepository somehow - so we don't need to specify it
        buildLogger.addBuildLogEntry(i18nResolver.getText("repository.git.messages.checkingOutRevision", targetRevision));

        try
        {
            createLocalRepository(sourceDirectory, cacheDirectory);
            //try to clean .git/index.lock file prior to checkout, otherwise checkout would fail with Exception
            File lck = new File(sourceDirectory, "index.lock");
            FileUtils.deleteQuietly(lck);

            gitCommandProcessor.runCheckoutCommand(sourceDirectory, targetRevision);
            if (accessData.useSubmodules)
            {
                gitCommandProcessor.runSubmoduleUpdateCommand(sourceDirectory);
            }
            return targetRevision;
        }
        catch (Exception e)
        {
            throw new RepositoryException(buildLogger.addErrorLogEntry(i18nResolver.getText("repository.git.messages.checkoutFailed", targetRevision)) + e.getMessage(), e);
        }
    }

    @Override
    public void fetch(@NotNull final File sourceDirectory, final boolean useShallow) throws RepositoryException
    {
        final String[] branchDescription = {"(unresolved) " + accessData.branch};
        try
        {
            createLocalRepository(sourceDirectory, null);
            final GitRepository.GitRepositoryAccessData proxiedAccessData = adjustRepositoryAccess(accessData);

            try
            {
                final String resolvedBranch;
                if (StringUtils.startsWithAny(accessData.branch, FQREF_PREFIXES))
                {
                    resolvedBranch = accessData.branch;
                }
                else
                {
                    resolvedBranch =  resolveBranch(proxiedAccessData, sourceDirectory, accessData.branch);
                }
                branchDescription[0] = resolvedBranch;

                buildLogger.addBuildLogEntry(i18nResolver.getText("repository.git.messages.fetchingBranch", resolvedBranch, accessData.repositoryUrl)
                                             + (useShallow ? " " + i18nResolver.getText("repository.git.messages.doingShallowFetch") : ""));

                gitCommandProcessor.runFetchCommand(sourceDirectory, proxiedAccessData, "+"+resolvedBranch+":"+resolvedBranch, useShallow);

                //if (resolvedBranch.startsWith(Constants.R_HEADS))
                //{
                //    gitCommandProcessor.runCheckoutCommandForBranchOrRevision(sourceDirectory, StringUtils.removeStart(resolvedBranch, Constants.R_HEADS));
                //}
            }
            finally
            {
                closeProxy(proxiedAccessData);
            }
        }
        catch (Exception e)
        {
            String message = i18nResolver.getText("repository.git.messages.fetchingFailed", accessData.repositoryUrl, branchDescription[0], sourceDirectory);
            throw new RepositoryException(buildLogger.addErrorLogEntry(message + " " + e.getMessage()), e);
        }
    }

    private String resolveBranch(GitRepository.GitRepositoryAccessData accessData, final File sourceDirectory, final String branch) throws RepositoryException
    {
        Collection<String> remoteRefs = gitCommandProcessor.getRemoteRefs(sourceDirectory, accessData);
        final Collection<String> candidates;
        if (StringUtils.isBlank(branch))
        {
            candidates = Arrays.asList(Constants.R_HEADS + Constants.MASTER, Constants.HEAD);
        }
        else if (StringUtils.startsWithAny(branch, FQREF_PREFIXES))
        {
            candidates = Collections.singletonList(branch);
        }
        else
        {
            candidates = Arrays.asList(branch, Constants.R_HEADS + branch, Constants.R_TAGS + branch);
        }
        for (String candidate : candidates)
        {
            if (remoteRefs.contains(candidate))
            {
                return candidate;
            }
        }
        throw new InvalidRepositoryException(i18nResolver.getText("repository.git.messages.cannotDetermineHead", RepositoryUrlObfuscator.obfuscatePasswordInUrl(accessData.repositoryUrl), accessData.branch));
    }

    @NotNull
    @Override
    public List<VcsBranch> getOpenBranches(@NotNull final GitRepository.GitRepositoryAccessData repositoryData, final File workingDir) throws RepositoryException
    {
        final GitRepository.GitRepositoryAccessData proxiedAccessData = adjustRepositoryAccess(repositoryData);
        try
        {
            Set<String> refs = gitCommandProcessor.getRemoteRefs(workingDir, proxiedAccessData);
            List<VcsBranch> openBranches = Lists.newArrayList();
            for (String ref : refs)
            {
                if (ref.startsWith(Constants.R_HEADS))
                {
                    openBranches.add(new VcsBranchImpl(ref.substring(Constants.R_HEADS.length())));
                }
            }
            return openBranches;
        }
        finally
        {
            closeProxy(proxiedAccessData);
        }
    }

    @NotNull
    @Override
    public String getCurrentRevision(@NotNull final File sourceDirectory) throws RepositoryException
    {
        return gitCommandProcessor.getRevisionHash(sourceDirectory, Constants.HEAD);
    }

    @Override
    public String getRevisionIfExists(@NotNull final File sourceDirectory, @NotNull final String revision)
    {
        try
        {
            return gitCommandProcessor.getRevisionHash(sourceDirectory, revision);
        }
        catch (RepositoryException e)
        {
            return null;
        }
    }

    @NotNull
    @Override
    public String obtainLatestRevision() throws RepositoryException
    {
        final GitRepository.GitRepositoryAccessData proxiedAccessData = adjustRepositoryAccess(accessData);
        try
        {
            File workingDir = new File(".");
            String result = gitCommandProcessor.getRemoteBranchLatestCommitHash(workingDir, proxiedAccessData, resolveBranch(proxiedAccessData, workingDir, accessData.branch));
            if (result == null)
            {
                throw new InvalidRepositoryException(i18nResolver.getText("repository.git.messages.cannotDetermineHead", RepositoryUrlObfuscator.obfuscatePasswordInUrl(accessData.repositoryUrl), accessData.branch));
            }
            return result;
        }
        finally
        {
            closeProxy(proxiedAccessData);
        }
    }

    @Override
    public boolean checkRevisionExistsInCacheRepository(@NotNull final File repositoryDirectory, @NotNull final String targetRevision) throws RepositoryException
    {
        return targetRevision.equals(gitCommandProcessor.getRevisionHash(repositoryDirectory, targetRevision));
    }

    @Override
    public CommitContext getCommit(final File directory, final String targetRevision) throws RepositoryException
    {
        return gitCommandProcessor.extractCommit(directory, targetRevision);
    }

    @Override
    public BuildRepositoryChanges extractCommits(final File cacheDirectory, final String lastVcsRevisionKey, final String targetRevision) throws RepositoryException
    {
        Pair<List<CommitContext>, Integer> result = gitCommandProcessor.runLogCommand(cacheDirectory, lastVcsRevisionKey, targetRevision, getShallows(cacheDirectory), CHANGESET_LIMIT);
        BuildRepositoryChanges buildChanges = new BuildRepositoryChangesImpl(targetRevision, result.getFirst());
        buildChanges.setSkippedCommitsCount(result.getSecond());
        return buildChanges;
    }

    private Set<String> getShallows(final File cacheDirectory)
    {
        File shallowFile = new File(new File(cacheDirectory, ".git"), "shallow");
        if (shallowFile.exists())
        {
            try
            {
                LineIterator shallowFileContent = FileUtils.lineIterator(shallowFile);
                try
                {
                    Set<String> result = Sets.newHashSet();
                    while (shallowFileContent.hasNext())
                    {
                        String aShallow = shallowFileContent.nextLine();
                        if (!StringUtils.isBlank(aShallow))
                        {
                            result.add(aShallow.trim());
                        }
                    }
                    return result;
                }
                finally
                {
                    LineIterator.closeQuietly(shallowFileContent);
                }
            }
            catch (IOException e)
            {
                log.warn("Cannot read 'shallow' file " + shallowFile.getAbsolutePath());
            }
        }
        return Collections.emptySet();
    }
}
