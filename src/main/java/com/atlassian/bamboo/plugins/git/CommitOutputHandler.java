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

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Date;
import java.util.List;
import java.util.Set;

@NotThreadSafe
public class CommitOutputHandler extends LineOutputHandler implements GitCommandProcessor.GitOutputHandler
{
    private static final Logger log = Logger.getLogger(CommitOutputHandler.class);

    // ------------------------------------------------------------------------------------------------------- Constants
    private static final String SALT = "[d31bfa5_BAM_";
    private static final String HASH = SALT + "hash]";
    private static final String COMMITER_NAME = SALT + "commiter_name]";
    private static final String COMMITER_EMAIL = SALT + "commiter_email]";
    private static final String TIMESTAMP = SALT + "timestamp]";
    private static final String COMMIT_MESSAGE = SALT + "commit_message]";
    private static final String FILE_LIST = SALT + "file_list]";

    public static final String LOG_COMMAND_FORMAT_STRING = HASH+"%H%n"+COMMITER_NAME+"%cN%n"+COMMITER_EMAIL+"%ce%n"+TIMESTAMP+"%ct%n"+COMMIT_MESSAGE+"%B%n"+FILE_LIST;

    private enum CommitParserState
    {
        INFO,
        COMMIT_MESSAGE,
        FILE_LIST
    }
    // ------------------------------------------------------------------------------------------------- Type Properties
    private List<CommitContext> extractedCommits = Lists.newArrayList();
    private Set<String> shallows;
    private CommitImpl currentCommit = null;
    private String commiterName = null;
    private int skippedCommitCount;
    private int maxCommitNumber;
    private int commitCount = 0;
    private StringBuilder commitMessage = null;

    CommitParserState parserState = CommitParserState.INFO;

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
        if (extractedCommits.size() <= maxCommitNumber)
        {
            if (parserState != CommitParserState.COMMIT_MESSAGE && line.startsWith(HASH))
            {
                if (extractedCommits.size() < maxCommitNumber)
                {
                    parserState = CommitParserState.INFO;
                    currentCommit = new CommitImpl();
                    commiterName  = null;
                    currentCommit.setAuthor(new AuthorImpl(AuthorImpl.UNKNOWN_AUTHOR));
                    currentCommit.setChangeSetId(getLineContent(HASH,line));
                    extractedCommits.add(currentCommit);
                }
            }
            if (parserState == CommitParserState.COMMIT_MESSAGE)
            {
                if (line.startsWith(FILE_LIST))
                {
                    if (currentCommit != null && commitMessage != null)
                    {
                        currentCommit.setComment(commitMessage.toString());
                        commitMessage = null;
                    }
                    parserState = CommitParserState.FILE_LIST;
                }
                else
                {
                    commitMessage.append('\n');
                    commitMessage.append(line);
                }
            }
            else if (parserState == CommitParserState.FILE_LIST && currentCommit != null && !StringUtils.isBlank(line) && !shallows.contains(currentCommit.getChangeSetId()))
            {
                currentCommit.addFile(new CommitFileImpl(currentCommit.getChangeSetId(), line.trim()));
            }
            else if (parserState == CommitParserState.INFO)
            {
                if (line.startsWith(COMMITER_NAME))
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
                else if (line.startsWith(COMMIT_MESSAGE))
                {
                    commitMessage = new StringBuilder(getLineContent(COMMIT_MESSAGE, line));
                    parserState = CommitParserState.COMMIT_MESSAGE;
                }
            }
        }
        else if (line.startsWith(HASH)) //too many commits
        {
            currentCommit = null;
            commiterName = null;
            skippedCommitCount++;
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
