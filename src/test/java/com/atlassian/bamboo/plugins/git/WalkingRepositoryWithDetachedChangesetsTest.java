package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.plugins.git.testutils.ExtractComments;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.BuildRepositoryChanges;
import com.atlassian.testtools.ZipResourceDirectory;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class WalkingRepositoryWithDetachedChangesetsTest extends GitAbstractTest
{
    private static final String CHG_1 = "2dc6e849cae7ff78e93179cfe04e62671807109d";
    private static final String CHG_2 = "2b6086bc53e14683de3bc7e80e28e2b9ee344316";
    private static final String CHG_3 = "2d870b2f044b3f8883298a4dac5830263ccbf4c0";
    private static final String CHG_4 = "303d0e7fcf7bea81729d050d9716963eb5a25815";
    private static final String CHG_5 = "f22d51f401b7d6ff22f6b1c611b95ebe757e47bf";
    private static final String COMMENT_2 = "Breaking the test";
    private static final String COMMENT_3 = "Fixing test";
    private static final String COMMENT_4 = "Fixing old and adding new test";
    private static final String COMMENT_5 = "merging detached";

    private File sourceRepositoriesBase;

    @BeforeClass
    public void setUpTmpDirWithRepository() throws Exception
    {
        sourceRepositoriesBase = createTempDirectory();
        ZipResourceDirectory.copyZipResourceToDirectory("detached-git-repos.zip", sourceRepositoriesBase);
    }

    @DataProvider
    Object[][] subsequentChangeDetectionsData()
    {
        return new Object[][]{
                {null, "1", CHG_1, Collections.<String>emptyList()}, // initial builds
                {null, "2", CHG_2, Collections.<String>emptyList()},
                {null, "3", CHG_3, Collections.<String>emptyList()},
                {null, "4", CHG_4, Collections.<String>emptyList()},
                {null, "5", CHG_5, Collections.<String>emptyList()},

                {CHG_1, "2", CHG_2, asList(COMMENT_2)}, // subsequent sequential builds
                {CHG_1, "3", CHG_3, asList(COMMENT_3, COMMENT_2)},
                {CHG_1, "4", CHG_4, asList(COMMENT_4, COMMENT_2)}, // no chg#3 - detached changeset!
                {CHG_1, "5", CHG_5, asList(COMMENT_5, COMMENT_4, COMMENT_3, COMMENT_2)}, // order of 3 and 4 arbitrary!

                {CHG_5, "4", CHG_4, asList(getI18nResolver().getText("repository.git.messages.unknownChanges", CHG_5, CHG_4))} // repository going back


        };
    }

    @Test(dataProvider = "subsequentChangeDetectionsData")
    public void testSubsequentChangeDetections(String previousChangeset, String srcRepo, String expectedHead, List<String> expectedComments) throws Exception
    {
        File source = new File(sourceRepositoriesBase, srcRepo);
        GitRepository gitRepository = createGitRepository();
        setRepositoryProperties(gitRepository, source);

        BuildRepositoryChanges changes = gitRepository.collectChangesSinceLastBuild("GIT-PLAN", previousChangeset);
        String vcsRevisionKey = changes.getVcsRevisionKey();
        Assert.assertNotNull(vcsRevisionKey);
        Assert.assertEquals(vcsRevisionKey, expectedHead);

        List<String> comments = Lists.transform(changes.getChanges(), new ExtractComments());
        Assert.assertEquals(comments, expectedComments);
    }

    @Test(dataProvider = "subsequentChangeDetectionsData", singleThreaded = true)
    public void testSubsequentChangeDetectionsWithCache(String previousChangeset, String srcRepo, String expectedHead, List<String> expectedComments) throws Exception
    {
        File source = new File(sourceRepositoriesBase, srcRepo);
        File singleSource = new File(sourceRepositoriesBase, "testSubsequentChangeDetectionsWithCache_Repo");
        FileUtils.deleteQuietly(singleSource);
        FileUtils.forceMkdir(singleSource);
        FileUtils.copyDirectory(source, singleSource);

        File workingDir = new File(sourceRepositoriesBase, "testSubsequentChangeDetectionsWithCache_WorkDir");
        FileUtils.forceMkdir(workingDir);

        GitRepository gitRepository = createGitRepository();
        gitRepository.setWorkingDir(workingDir);
        setRepositoryProperties(gitRepository, singleSource);

        BuildRepositoryChanges changes = gitRepository.collectChangesSinceLastBuild("GIT-PLAN", previousChangeset);
        String vcsRevisionKey = changes.getVcsRevisionKey();
        Assert.assertNotNull(vcsRevisionKey);
        Assert.assertEquals(vcsRevisionKey, expectedHead);

        List<String> comments = Lists.transform(changes.getChanges(), new ExtractComments());
        Assert.assertEquals(comments, expectedComments);

    }

    @Test
    public void testChangeDetectionAfterPreviousChangesetBecameDetached() throws Exception
    {
        File detachedSrc = new File(sourceRepositoriesBase, "3");
        File nextSrc = new File(sourceRepositoriesBase, "4");
        File singleSource = createTempDirectory();

        GitRepository gitRepository = createGitRepository();
        setRepositoryProperties(gitRepository, singleSource);

        // feed the cache or the detached change won't be known
        FileUtils.copyDirectory(detachedSrc, singleSource);
        BuildRepositoryChanges initialChanges = gitRepository.collectChangesSinceLastBuild("GIT-PLAN", CHG_2); // initial build does not fetch to cache - maybe it should?
        Assert.assertEquals(initialChanges.getVcsRevisionKey(), CHG_3);

        FileUtils.cleanDirectory(singleSource);
        FileUtils.copyDirectory(nextSrc, singleSource);

        BuildRepositoryChanges changes = gitRepository.collectChangesSinceLastBuild("GIT-PLAN", CHG_3);

        Assert.assertEquals(changes.getVcsRevisionKey(), CHG_4);
        List<String> comments = Lists.transform(changes.getChanges(), new ExtractComments());
        Assert.assertEquals(comments, Arrays.asList(COMMENT_4));

    }

    @DataProvider
    Object[][] crossSourceCheckoutData()
    {
        List<String[]> data = new ArrayList<String[]>();
        for (int src = 1; src <= 5; src++)
        {
            for (int dst = 1; dst <= 5; dst++)
            {
                data.add(new String[]{String.valueOf(src), String.valueOf(dst)});
            }
        }
        return data.toArray(new Object[data.size()][]);
    }

    @Test(dataProvider = "crossSourceCheckoutData")
    public void testCrossSourceCheckout(String prevRepo, String newRepo) throws Exception
    {
        GitRepository gitRepository = createGitRepository();

        File prev = new File(sourceRepositoriesBase, prevRepo);
        setRepositoryProperties(gitRepository, prev);

        String prevRev = gitRepository.collectChangesSinceLastBuild("GIT-PLAN", null).getVcsRevisionKey();

        BuildContext buildContext = Mockito.mock(BuildContext.class);
        Mockito.when(buildContext.getPlanKey()).thenReturn("GIT-PLAN");

        gitRepository.retrieveSourceCode(buildContext, prevRev, getCheckoutDir(gitRepository));

        File next = new File(sourceRepositoriesBase, newRepo);
        setRepositoryProperties(gitRepository, next);

        String newRev = gitRepository.collectChangesSinceLastBuild("GIT-PLAN", null).getVcsRevisionKey();

        gitRepository.retrieveSourceCode(buildContext, newRev, getCheckoutDir(gitRepository));
        verifyContents(getCheckoutDir(gitRepository), "detached-git-repos-contents/" + newRepo + ".zip");
    }

    @DataProvider
    Object[][] specificSourceCheckoutData()
    {
        return new String[][]{
                {CHG_1, "1"},
                {CHG_2, "2"},
                {CHG_3, "3"},
                {CHG_4, "4"},
                {CHG_5, "5"},
        };
    }

    @Test(dataProvider = "specificSourceCheckoutData")
    public void testSpecificSourceCheckout(String revision, String contents) throws Exception
    {
        GitRepository gitRepository = createGitRepository();

        File prev = new File(sourceRepositoriesBase, "5");
        setRepositoryProperties(gitRepository, prev);

        BuildContext buildContext = Mockito.mock(BuildContext.class);
        Mockito.when(buildContext.getPlanKey()).thenReturn("GIT-PLAN");

        File checkoutDir = new File(gitRepository.getWorkingDirectory(), "checkout");

        gitRepository.retrieveSourceCode(buildContext, revision, checkoutDir);

        verifyContents(checkoutDir, "detached-git-repos-contents/" + contents + ".zip");
    }
}
