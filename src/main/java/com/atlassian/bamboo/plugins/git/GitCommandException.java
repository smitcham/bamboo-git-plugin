package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.repository.RepositoryException;

/**
 * This class types git repository errors in running commands.
 *
 */
class GitCommandException extends RepositoryException {

    private final String stdout;
    private final String stderr;

    /**
     * Create a command exception containing the message and the stderr
     *
     * @param message The error message
     * @param stderr Command standard error output
     */
    public GitCommandException(String message, String stdout, String stderr) {
        super(message);
        this.stdout = stdout;
        this.stderr = stderr;
    }

    /**
     * Create a command exception containing the message and root cause and the stderr
     *
     * @param message The error message
     * @param cause   The root cause
     * @param stderr Command standard error output
     */
    public GitCommandException(String message, Throwable cause, String stdout, String stderr) {
        super(message, cause);
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public String getStdout()
    {
        return stdout;
    }

    public String getStderr()
    {
        return stderr;
    }

    @Override
    public String getMessage()
    {
        final StringBuilder sb = new StringBuilder(super.getMessage());
        sb
                .append(", stderr:\n")
                .append(stderr);
        if (!stdout.equals(stderr))
        {
            sb
                    .append("\nstdout:\n")
                    .append(stdout);
        }

        return sb.toString();
    }
}
