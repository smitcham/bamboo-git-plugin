package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.FeatureManager;
import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.fileserver.BuildDirectoryManager;
import com.atlassian.bamboo.build.logger.NullBuildLogger;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.core.TransportProtocol;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.branch.BranchIntegrationHelper;
import com.atlassian.bamboo.plugins.git.GitRepository.GitRepositoryAccessData;
import com.atlassian.bamboo.project.Project;
import com.atlassian.bamboo.repository.RepositoryException;
import com.atlassian.bamboo.security.StringEncrypter;
import com.atlassian.bamboo.ssh.SshProxyService;
import com.atlassian.bamboo.util.BambooFileUtils;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.i18n.DefaultI18nBean;
import com.atlassian.bamboo.utils.i18n.TextProviderAdapter;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.BuildContextImpl;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.CustomVariableContextImpl;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.collect.Maps;
import com.opensymphony.xwork.TextProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.eclipse.jgit.lib.Repository;
import org.jetbrains.annotations.Nullable;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.Returns;
import org.testng.annotations.AfterClass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

public class GitAbstractTest
{
    public static final PlanKey PLAN_KEY = PlanKeys.getPlanKey("PLAN-KEY");
    private final Collection<File> filesToCleanUp = Collections.synchronizedCollection(new ArrayList<File>());
    private final Collection<Repository> repositoriesToCleanUp = Collections.synchronizedCollection(new ArrayList<Repository>());
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("com.atlassian.bamboo.plugins.git.i18n", Locale.US);

    protected static final String COMITTER_NAME = "Committer";
    protected static final String COMITTER_EMAIL = "committer@example.com";

    public static void setRepositoryProperties(GitRepository gitRepository, File repositorySourceDir, String branch) throws Exception
    {
        setRepositoryProperties(gitRepository, repositorySourceDir.getAbsolutePath(), branch);
    }

    public static void setRepositoryProperties(GitRepository gitRepository, String repositoryUrl, String branch) throws Exception
    {
        setRepositoryProperties(gitRepository, repositoryUrl, branch, null, null);
    }

    public static void setRepositoryProperties(GitRepository gitRepository, String repositoryUrl, String branch, @Nullable String sshKey, @Nullable String sshPassphrase) throws Exception
    {
        setRepositoryProperties(gitRepository, repositoryUrl, branch, sshKey, sshPassphrase, Collections.<String, String>emptyMap());
    }

    public static void setRepositoryProperties(GitRepository gitRepository, String repositoryUrl, String branch, String sshKey, String sshPassphrase, Map<String, String> paramMap) throws Exception
    {
        StringEncrypter encrypter = new StringEncrypter();

        Map<String, Object> params = new HashMap<String, Object>(paramMap);

        params.put("repository.git.branch", branch);
        if (sshKey != null)
        {
            params.put("repository.git.ssh.key", encrypter.encrypt(sshKey));
            params.put("repository.git.authenticationType", GitAuthenticationType.SSH_KEYPAIR.name());
        }
        if (sshPassphrase != null)
        {
            params.put("repository.git.ssh.passphrase", encrypter.encrypt(sshPassphrase));
        }
        setRepositoryProperties(gitRepository, repositoryUrl, params);
    }

    public static void setRepositoryProperties(GitRepository gitRepository, File repositorySourceDir) throws Exception
    {
        setRepositoryProperties(gitRepository, repositorySourceDir.getAbsolutePath(), Collections.<String, Object>emptyMap());
    }

    public static void setRepositoryProperties(GitRepository gitRepository, String repositoryUrl) throws Exception
    {
        setRepositoryProperties(gitRepository, repositoryUrl, Collections.<String, Object>emptyMap());
    }

    public static void setRepositoryProperties(GitRepository gitRepository, String repositoryUrl, Map<String, ?> paramMap) throws Exception
    {
        BuildConfiguration buildConfiguration = new BuildConfiguration();
        buildConfiguration.setProperty("repository.git.repositoryUrl", repositoryUrl);

        for (Map.Entry<String, ?> entry : paramMap.entrySet())
        {
            buildConfiguration.setProperty(entry.getKey(), entry.getValue());
        }

        final ErrorCollection errorCollection = gitRepository.validate(buildConfiguration);
        if (errorCollection.hasAnyErrors())
        {
            throw new Exception("validation failed: " + errorCollection.toString());
        }
        gitRepository.populateFromConfig(buildConfiguration);
    }

    public GitOperationHelper createJGitOperationHelper(final GitRepositoryAccessData accessData)
    {
        TextProvider textProvider = Mockito.mock(TextProvider.class);
        return new JGitOperationHelper(accessData, new NullBuildLogger(), textProvider);
    }

    public NativeGitOperationHelper createNativeGitOperationHelper(final GitRepositoryAccessData accessData) throws RepositoryException
    {
        TextProvider textProvider = Mockito.mock(TextProvider.class);
        final GitRepository repository = Mockito.mock(GitRepository.class);
        Mockito.when(repository.getWorkingDirectory()).thenReturn(new File("/"));
        Mockito.when(repository.getGitCapability()).thenReturn("/usr/bin/git");
        final SshProxyService sshProxyService = Mockito.mock(SshProxyService.class);
        return new NativeGitOperationHelper(repository, accessData, sshProxyService, new NullBuildLogger(), textProvider);
    }

    public GitRepository createNativeGitRepository() throws Exception
    {
        return _createGitRepository(new NativeGitRepositoryFixture());
    }

    public GitRepository createGitRepository() throws Exception
    {
        return _createGitRepository(new GitRepositoryFixture());
    }

    public GitRepository _createGitRepository(GitRepository fixture) throws Exception
    {
        File workingDirectory = createTempDirectory();

        BuildLoggerManager buildLoggerManager = Mockito.mock(BuildLoggerManager.class, new Returns(new NullBuildLogger()));
        fixture.setBuildLoggerManager(buildLoggerManager);

        BuildDirectoryManager buildDirectoryManager = Mockito.mock(BuildDirectoryManager.class, new Returns(workingDirectory));
        fixture.setBuildDirectoryManager(buildDirectoryManager);
        fixture.setTextProvider(getTextProvider());

        CustomVariableContext customVariableContext = new CustomVariableContextImpl();
        customVariableContext.setVariables(Maps.<String, VariableDefinitionContext>newHashMap());
        fixture.setCustomVariableContext(customVariableContext);
        
        fixture.setCapabilityContext(mock(CapabilityContext.class));

        SshProxyService sshProxyService = mock(SshProxyService.class);
        fixture.setSshProxyService(sshProxyService);

        final BranchIntegrationHelper branchIntegrationHelper = mock(BranchIntegrationHelper.class);
        Mockito.when(branchIntegrationHelper.getCommitterName(fixture)).thenReturn(COMITTER_NAME);
        Mockito.when(branchIntegrationHelper.getCommitterEmail(fixture)).thenReturn(COMITTER_EMAIL);
        fixture.setBranchIntegrationHelper(branchIntegrationHelper);

        FeatureManager featureManager = mock(FeatureManager.class);
        when(featureManager.isTransportSupported(Matchers.<TransportProtocol>any())).thenReturn(true);

        fixture.setFeatureManager(featureManager);

        return fixture;
    }

    public File createTempDirectory() throws IOException
    {
        File tmp = BambooFileUtils.createTempDirectory("bamboo-git-plugin-test");
        FileUtils.forceDeleteOnExit(tmp);
        filesToCleanUp.add(tmp);
        return tmp;
    }

    protected <T extends Repository> T register(T repository)
    {
        repositoriesToCleanUp.add(repository);
        return repository;
    }

    public static void verifyContents(File directory, final String expectedZip) throws IOException
    {
        final Enumeration<? extends ZipEntry> entries = new ZipFile(GitAbstractTest.class.getResource("/" + expectedZip).getFile()).entries();
        int fileCount = 0;
        while (entries.hasMoreElements())
        {
            ZipEntry zipEntry = entries.nextElement();
            if (!zipEntry.isDirectory())
            {
                fileCount++;
                String fileName = zipEntry.getName();
                final File file = new File(directory, fileName);
                assertEquals(FileUtils.checksumCRC32(file), zipEntry.getCrc(), "CRC for " + file);
            }
        }

        final Collection files = listFiles(directory, FileFilterUtils.trueFileFilter(), FileFilterUtils.notFileFilter(FileFilterUtils.nameFileFilter(".git")));
        assertEquals(files.size(), fileCount, "Number of files");
    }

    public static TextProvider getTextProvider()
    {
        DefaultI18nBean bean = new DefaultI18nBean(Locale.US, Mockito.mock(PluginAccessor.class));
        bean.getI18nBundles().add(resourceBundle);
        return new TextProviderAdapter(bean);
    }

    protected static GitRepositoryAccessData createAccessData(String repositoryUrl)
    {
        return createAccessData(repositoryUrl, null);
    }

    protected static GitRepositoryAccessData createAccessData(File repositoryFile, String branch)
    {
        return createAccessData(repositoryFile.getAbsolutePath(), branch);
    }

    protected static GitRepositoryAccessData createAccessData(String repositoryUrl, @Nullable String branch)
    {
        return createAccessData(repositoryUrl, branch, null, null, null, null);
    }

    protected static GitRepositoryAccessData createAccessData(String repositoryUrl, String branch, @Nullable String username, @Nullable String password, @Nullable String sshKey, @Nullable String sshPassphrase)
    {
        GitRepositoryAccessData accessData = new GitRepositoryAccessData();
        accessData.repositoryUrl = repositoryUrl;
        accessData.branch = branch;
        accessData.username = username;
        accessData.password = password;
        accessData.sshKey = sshKey;
        accessData.sshPassphrase = sshPassphrase;
        accessData.commandTimeout = 1;
        return accessData;
    }

    protected static BuildContext mockBuildContext()
    {
        Chain chain = Mockito.mock(Chain.class);
        Mockito.when(chain.getKey()).thenReturn(PLAN_KEY.toString());

        Project project = Mockito.mock(Project.class);
        Mockito.when(chain.getProject()).thenReturn(project);
        return new BuildContextImpl(chain, 1, null, null, null);
    }

    protected static File getCheckoutDir(GitRepository gitRepository)
    {
        return new File(gitRepository.getWorkingDirectory(), "checkoutDir");
    }

    @AfterClass
    void cleanUpFiles()
    {
        for (File file : filesToCleanUp)
        {
            FileUtils.deleteQuietly(file);
        }
        for (Repository repository : repositoriesToCleanUp)
        {
            repository.close();
        }
    }

    static class GitRepositoryFixture extends GitRepository
    {
        @Override
        public String getGitCapability()
        {
            return "";
        }

        @Override
        public File getCacheDirectory()
        {
            return GitCacheDirectory.getCacheDirectory(getWorkingDirectory(), getSubstitutedAccessData());
        }
    }

    static class NativeGitRepositoryFixture extends GitRepositoryFixture
    {
        @Override
        public String getGitCapability()
        {
            return "/usr/bin/git";
        }
    }
}
