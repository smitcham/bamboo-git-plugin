package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.author.AuthorImpl;
import com.atlassian.bamboo.commit.CommitContext;
import com.atlassian.bamboo.commit.CommitFileImpl;
import com.atlassian.bamboo.commit.CommitImpl;
import com.atlassian.utils.process.LineOutputHandler;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class CommitOutputHandler extends LineOutputHandler implements GitCommandProcessor.GitOutputHandler
{
    private static final Logger log = Logger.getLogger(CommitOutputHandler.class);

    // ------------------------------------------------------------------------------------------------------- Constants
    private static final String HASH = "[hash]";
    private static final String COMMITER_NAME = "[commiter_name]";
    private static final String COMMITER_EMAIL = "[commiter_email]";
    private static final String TIMESTAMP = "[timestamp]";
    private static final String SUMMARY = "[summary]";

    public static final String LOG_COMMAND_FORMAT_STRING = HASH+"%H%n"+COMMITER_NAME+"%cN%n"+COMMITER_EMAIL+"%ce%n"+TIMESTAMP+"%ct%n"+SUMMARY+"%s%n";

    // ------------------------------------------------------------------------------------------------- Type Properties
    List<CommitContext> extractedCommits = Lists.newArrayList();
    Set<String> shallows;
    CommitImpl currentCommit = null;
    String commiterName = null;
    int skippedCommitCount;
    int maxCommitNumber;

    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    public CommitOutputHandler(@NotNull Set<String> shallows)
    {
        this.shallows = shallows;
        maxCommitNumber = Integer.MAX_VALUE;
    }

    public CommitOutputHandler(@NotNull Set<String> shallows, int maxCommitNumber)
    {
        this.shallows = shallows;
        this.maxCommitNumber = maxCommitNumber;
    }

    // ----------------------------------------------------------------------------------------------- Interface Methods
    @Override
    public String getStdout()
    {
        return "";
    }

    @Override
    protected void processLine(final int lineNum, final String line)
    {
        if (line.startsWith(HASH))
        {
            if (extractedCommits.size() < maxCommitNumber)
            {
                currentCommit = new CommitImpl();
                currentCommit.setAuthor(new AuthorImpl(AuthorImpl.UNKNOWN_AUTHOR));
                currentCommit.setChangeSetId(getLineContent(HASH,line));
                extractedCommits.add(currentCommit);
            }
            else
            {
                currentCommit = null;
                commiterName = null;
                skippedCommitCount++;
            }
        }
        else if (line.startsWith(COMMITER_NAME))
        {
            if (currentCommit != null)
            {
                commiterName = getLineContent(COMMITER_NAME, line);
            }
        }
        else if (line.startsWith(COMMITER_EMAIL))
        {
            if (currentCommit != null && !StringUtils.isBlank(commiterName))
            {
                String email = getLineContent(COMMITER_EMAIL, line);
                currentCommit.setAuthor(new AuthorImpl(String.format("%s <%s>", commiterName, email), null, email));
            }
        }
        else if (line.startsWith(TIMESTAMP))
        {
            if (currentCommit != null)
            {
                String timestampString = getLineContent(TIMESTAMP, line);
                currentCommit.setDate(new Date(Long.parseLong(timestampString)*1000));
            }
        }
        else if (line.startsWith(SUMMARY))
        {
            if (currentCommit != null)
            {
                currentCommit.setComment(getLineContent(SUMMARY, line));
            }
        }
        else if (!StringUtils.isBlank(line) && currentCommit != null && !shallows.contains(currentCommit.getChangeSetId()))
        {
            currentCommit.addFile(new CommitFileImpl(currentCommit.getChangeSetId(), line.trim()));
        }
    }

    private String getLineContent(final String keyword, final String line)
    {
        return line.substring(keyword.length()).trim();
    }

    // -------------------------------------------------------------------------------------------------- Action Methods
    // -------------------------------------------------------------------------------------------------- Public Methods
    public List<CommitContext> getExtractedCommits()
    {
        return extractedCommits;
    }

    public int getSkippedCommitCount()
    {
        return skippedCommitCount;
    }
    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators
}
