package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.core.RepositoryUrlObfuscator;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.ssh.ProxyErrorReceiver;
import com.atlassian.utils.process.ExternalProcess;
import com.atlassian.utils.process.ExternalProcessBuilder;
import com.atlassian.utils.process.LineOutputHandler;
import com.atlassian.utils.process.OutputHandler;
import com.atlassian.utils.process.PluggableProcessHandler;
import com.atlassian.utils.process.StringOutputHandler;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RefSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class GitCommandProcessor implements Serializable, ProxyErrorReceiver
{
    private static final Logger log = Logger.getLogger(GitRepository.class);

    // ------------------------------------------------------------------------------------------------------- Constants

    static final Pattern gitVersionPattern = Pattern.compile("^git version (.*)");

    // ------------------------------------------------------------------------------------------------- Type Properties

    private final String gitExecutable;
    private final BuildLogger buildLogger;
    private final int commandTimeoutInMinutes;
    private final boolean maxVerboseOutput;
    private String proxyErrorMessage;
    private Throwable proxyException;
    private String sshCommand;

    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    public GitCommandProcessor(@Nullable final String gitExecutable, @NotNull final BuildLogger buildLogger, final int commandTimeoutInMinutes, boolean maxVerboseOutput)
    {
        this.gitExecutable = gitExecutable;
        this.buildLogger = buildLogger;
        Preconditions.checkArgument(commandTimeoutInMinutes>0, "Command timeout must be greater than 0");
        this.commandTimeoutInMinutes = commandTimeoutInMinutes;
        this.maxVerboseOutput = maxVerboseOutput;
    }

    // ----------------------------------------------------------------------------------------------- Interface Methods
    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Checks whether git exist in current system.
     *
     * @param workingDirectory specifies arbitrary directory.
     *
     * @throws RepositoryException when git wasn't found in current system.
     */
    public void checkGitExistenceInSystem(@NotNull final File workingDirectory) throws RepositoryException
    {
        GitCommandBuilder commandBuilder = createCommandBuilder("version");

        GitStringOutputHandler outputHandler = new GitStringOutputHandler();

        try
        {
            final int exitCode = runCommand(commandBuilder, workingDirectory, outputHandler);
            String output = outputHandler.getOutput();
            Matcher matcher = gitVersionPattern.matcher(output);
            if (!matcher.find())
            {
                String errorMessage = "Git Executable capability `" + gitExecutable + "' does not seem to be a git client. Is it properly set?";
                log.error(errorMessage + " Exit code: " + exitCode + " Output:\n" + output);
                throw new RepositoryException(errorMessage);
            }
        }
        catch (GitCommandException e)
        {
            throw new RepositoryException("Git not found. Is Git Executable capability properly set in configuration?", e);
        }
    }

    /**
     * Creates .git repository in a given directory.
     *
     * @param workingDirectory - directory in which we want to create empty repository.
     *
     * @throws RepositoryException when init command fails
     */
    public void runInitCommand(@NotNull final File workingDirectory) throws RepositoryException
    {
        GitCommandBuilder commandBuilder = createCommandBuilder("init");
        runCommand(commandBuilder, workingDirectory, new LoggingOutputHandler(buildLogger));
    }

    public List<String> runStatusCommand(@NotNull final File workingDirectory) throws RepositoryException
    {
        GitCommandBuilder commandBuilder = createCommandBuilder("status", "--porcelain", "--untracked-files=no");
        final LineOutputHandlerImpl gitOutputHandler = new LineOutputHandlerImpl();
        runCommand(commandBuilder, workingDirectory, gitOutputHandler);
        log.debug("git status output: " + gitOutputHandler.getStdout());
        return gitOutputHandler.getLines();
    }

    public void runFetchCommand(@NotNull final File workingDirectory, @NotNull final GitRepository.GitRepositoryAccessData accessData, RefSpec refSpec, boolean useShallow) throws RepositoryException
    {
        GitCommandBuilder commandBuilder = createCommandBuilder("fetch", accessData.repositoryUrl, refSpec.toString(), "--update-head-ok");
        if (useShallow)
        {
            commandBuilder.shallowClone();
        }
        if (accessData.verboseLogs)
        {
            commandBuilder.verbose(true);
            commandBuilder.append("--progress");
        }
        runCommand(commandBuilder, workingDirectory, new LoggingOutputHandler(buildLogger));
    }

    public void runCheckoutCommand(@NotNull final File workingDirectory, String revision) throws RepositoryException
    {
        /**
         * this call to git log checks if requested revision is considered as HEAD of resolved branch. If so, instead of calling explicit revision,
         * checkout to branch is called to avoid DETACHED HEAD
         */
        String possibleBranch = getPossibleBranchNameForCheckout(workingDirectory, revision);

        String destination = revision;
        if (StringUtils.isNotBlank(possibleBranch))
        {
           destination = possibleBranch;
        }
        GitCommandBuilder commandBuilder = createCommandBuilder("checkout", "-f", destination);
        runCommand(commandBuilder, workingDirectory, new LoggingOutputHandler(buildLogger));
    }

    public void runSubmoduleUpdateCommand(@NotNull final File workingDirectory) throws RepositoryException
    {
        GitCommandBuilder commandBuilder = createCommandBuilder("submodule", "update", "--init", "--recursive");

        runCommand(commandBuilder, workingDirectory, new LoggingOutputHandler(buildLogger));
    }

    // -------------------------------------------------------------------------------------------------- Helper Methods

    public String getPossibleBranchNameForCheckout(File workingDirectory, String revision) throws RepositoryException
    {
        GitCommandBuilder commandBuilder = createCommandBuilder("log", "-1", "--format=%d", "--decorate=full");
        commandBuilder.append(revision);
        final GitStringOutputHandler outputHandler = new GitStringOutputHandler();
        runCommand(commandBuilder, workingDirectory, outputHandler);

        String revisionDescription = outputHandler.getOutput();
        if (StringUtils.isNotBlank(revisionDescription))
        {
            Set<String> possibleBranches = Sets.newHashSet(
                    Splitter.on(',').trimResults().split(
                            CharMatcher.anyOf("()").removeFrom(StringUtils.trim(revisionDescription))));
            for (String possibleBranch : possibleBranches)
            {
                if (possibleBranch.startsWith(Constants.R_HEADS))
                {
                    return StringUtils.removeStart(possibleBranch, Constants.R_HEADS);
                }
            }
        }
        return "";
    }

    public GitCommandBuilder createCommandBuilder(String... commands)
    {
        return new GitCommandBuilder(commands)
                .executable(gitExecutable)
                .sshCommand(sshCommand);
    }

    public void reportProxyError(String message, Throwable exception)
    {
        proxyErrorMessage = message;
        proxyException = exception;
    }

    public void runCommand(@NotNull final GitCommandBuilder commandBuilder, @NotNull final File workingDirectory) throws RepositoryException
    {
        runCommand(commandBuilder, workingDirectory, new LoggingOutputHandler(buildLogger));
    }

    public int runCommand(@NotNull final GitCommandBuilder commandBuilder, @NotNull final File workingDirectory,
                          @NotNull final GitOutputHandler outputHandler) throws RepositoryException
    {
        //noinspection ResultOfMethodCallIgnored
        workingDirectory.mkdirs();

        PluggableProcessHandler handler = new PluggableProcessHandler();
        handler.setOutputHandler(outputHandler);
        handler.setErrorHandler(outputHandler);

        final List<String> commandArgs = commandBuilder.build();
        if (maxVerboseOutput || log.isDebugEnabled())
        {
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : RepositoryUrlObfuscator.obfuscatePasswordsInUrls(commandArgs))
            {
                stringBuilder.append(s).append(" ");
            }
            if (maxVerboseOutput)
            {
                buildLogger.addBuildLogEntry(stringBuilder.toString());
            }
            log.debug(stringBuilder.toString());
        }
        //log.info("Running in " + workingDirectory + ": '" + StringUtils.join(commandArgs, "' '") + "'");

        final ExternalProcessBuilder externalProcessBuilder = new ExternalProcessBuilder()
                .command(commandArgs, workingDirectory)
                .handler(handler)
                .env(commandBuilder.getEnv());

        ExternalProcess process = externalProcessBuilder.build();

        process.setTimeout(TimeUnit.MINUTES.toMillis(commandTimeoutInMinutes));
        process.execute();

        if (!handler.succeeded())
        {
            // command may contain user password (url) in plaintext -> hide it from bamboo plan/build logs. see BAM-5781
            throw new GitCommandException(
                    "command " + RepositoryUrlObfuscator.obfuscatePasswordsInUrls(commandArgs) + " failed with code " + handler.getExitCode() + "." +
                    " Working directory was ["+ workingDirectory + "].", proxyException != null ? proxyException : handler.getException(),
                    outputHandler.getStdout(),
                    proxyErrorMessage != null ? "SSH Proxy error: " + proxyErrorMessage : outputHandler.getStdout());
        }

        return handler.getExitCode();
    }

    /**
     * Returns true if there are modified files in the working directory or repository index after the merge
     */
    public void runMergeCommand(@NotNull final GitCommandBuilder commandBuilder, @NotNull final File workspaceDir) throws RepositoryException
    {
        final LoggingOutputHandler mergeOutputHandler = new LoggingOutputHandler(buildLogger);
        runCommand(commandBuilder, workspaceDir, mergeOutputHandler);
        log.debug(mergeOutputHandler.getStdout());
    }

    interface GitOutputHandler extends OutputHandler
    {
        String getStdout();
    }

    static class GitStringOutputHandler extends StringOutputHandler implements GitOutputHandler
    {
        @Override
        public String getStdout()
        {
            return getOutput();
        }
    }

    static class LineOutputHandlerImpl extends LineOutputHandler implements GitOutputHandler
    {
        private final List<String> lines = Lists.newLinkedList();

        @Override
        protected void processLine(int i, String s)
        {
            lines.add(s);
        }

        @NotNull
        public List<String> getLines()
        {
            return lines;
        }

        @Override
        public String getStdout()
        {
            return lines.toString();
        }
    }

    static class LoggingOutputHandler extends LineOutputHandler implements GitCommandProcessor.GitOutputHandler
    {
        final BuildLogger buildLogger;
        final StringBuilder stringBuilder;

        public LoggingOutputHandler(@NotNull final BuildLogger buildLogger)
        {
            this.buildLogger = buildLogger;
            stringBuilder = new StringBuilder();
        }

        @Override
        protected void processLine(int i, String s)
        {
            buildLogger.addBuildLogEntry(s);
            if (stringBuilder.length()!=0)
            {
                stringBuilder.append("\n");
            }
            stringBuilder.append(s);
        }

        @Override
        public String getStdout()
        {
            return stringBuilder.toString();
        }
    }

    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators


    public void setSshCommand(String sshCommand)
    {
        this.sshCommand = sshCommand;
    }
}