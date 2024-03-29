package com.atlassian.bamboo.plugins.git;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GitCommandBuilder
{
    private static final Logger log = Logger.getLogger(GitCommandBuilder.class);

    private final List<String> commands = new ArrayList<String>();
    private final Map<String, String> env = Maps.newHashMap();
    private String executable;
    private String branch;
    private String revision;
    private String source;
    private String destination;
    private String sshKeyFile;
    private String sshCommand;
    private boolean sshCompression;
    private boolean verbose;
    private boolean maxVerboseOutput;
    private boolean shallowClone;

    protected GitCommandBuilder(String... commands)
    {
        this.commands.addAll(Arrays.asList(commands));
    }

    public GitCommandBuilder executable(String executable)
    {
        this.executable = executable;
        return this;
    }

    public GitCommandBuilder branch(String branch)
    {
        this.branch = branch;
        return this;
    }

    public GitCommandBuilder revision(String revision)
    {
        this.revision = revision;
        return this;
    }

    public GitCommandBuilder destination(String destination)
    {
        this.destination = destination;
        return this;
    }

    public GitCommandBuilder source(String source)
    {
        this.source = source;
        return this;
    }

    public GitCommandBuilder verbose(Boolean verbose)
    {
        this.verbose = verbose;
        return this;
    }

    public GitCommandBuilder sshCommand(String sshCommand)
    {
        this.sshCommand = sshCommand;
        return this;
    }

    public GitCommandBuilder sshKeyFile(String sshKeyFile)
    {
        this.sshKeyFile = sshKeyFile;
        return this;
    }

    public GitCommandBuilder sshCompression(boolean sshCompression)
    {
        this.sshCompression = sshCompression;
        return this;
    }

    public GitCommandBuilder maxVerboseOutput(boolean maxVerboseOutput)
    {
        this.maxVerboseOutput = maxVerboseOutput;
        return this;
    }

    public GitCommandBuilder shallowClone()
    {
        this.shallowClone = true;
        return this;
    }

    public GitCommandBuilder append(String argument)
    {
        commands.add(argument);
        return this;
    }
    
    public GitCommandBuilder env(@Nullable Map<String, String> env)
    {
        this.env.putAll(env);
        return this;
    }

    public List<String> build()
    {
        List<String> commandArgs = new ArrayList<String>();

        if (executable != null)
        {
            commandArgs.add(executable);
        }
        else
        {
            commandArgs.add("git");
        }

        commandArgs.addAll(commands);

        if (verbose || maxVerboseOutput)
        {
            commandArgs.add("--verbose");
        }

        if (StringUtils.isNotBlank(branch))
        {
            commandArgs.add("--branch");
            commandArgs.add(branch);
        }

        if (revision != null)
        {
            commandArgs.add("--rev");
            commandArgs.add(revision);
        }

        if (source != null)
        {
            commandArgs.add(source);
        }
        if (destination != null)
        {
            commandArgs.add(destination);
        }

        if (shallowClone)
        {
            commandArgs.add("--depth");
            commandArgs.add("1");
        }

        return commandArgs;
    }

    @NotNull
    public Map<String, String> getEnv()
    {
        if (StringUtils.isNotBlank(sshCommand))
        {
            log.debug("GIT_SSH="+sshCommand);
            env.put("GIT_SSH", sshCommand);
        }

        return env;
    }

    public String toString() {
        return build().toString();
    }
}
