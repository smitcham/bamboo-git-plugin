package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryData;
import com.atlassian.bamboo.repository.RepositoryDataEntity;
import com.atlassian.bamboo.repository.RepositoryDataImpl;
import com.atlassian.bamboo.repository.RepositoryDefinitionManager;
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.util.Narrow;
import com.atlassian.bamboo.ww2.actions.PlanActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.PlanEditSecurityAware;
import com.google.common.collect.Lists;
import com.opensymphony.webwork.dispatcher.json.JSONException;
import com.opensymphony.webwork.dispatcher.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LoadGitHubRepositories extends PlanActionSupport implements PlanEditSecurityAware
{
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = Logger.getLogger(LoadGitHubRepositories.class);

    // ------------------------------------------------------------------------------------------------------- Constants
    // ------------------------------------------------------------------------------------------------- Type Properties
    private String username;
    private String password;
    private long repositoryId;
    // ---------------------------------------------------------------------------------------------------- Dependencies
    private RepositoryDefinitionManager repositoryDefinitionManager;
    private EncryptionService encryptionService;
    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    // -------------------------------------------------------------------------------------------------- Action Methods

    public String doLoad() throws Exception
    {
        return SUCCESS;
    }

    // ----------------------------------------------------------------------------------------------- Interface Methods

    @NotNull
    @Override
    public JSONObject getJsonObject() throws JSONException
    {
        Map<String, Iterable<String>> gitHubRepositories = null;

        if (repositoryId > 0 && StringUtils.isBlank(password))
        {
            RepositoryDataEntity repositoryDataEntity = repositoryDefinitionManager.getRepositoryDataEntity(repositoryId);
            if (repositoryDataEntity != null)
            {
                RepositoryData repositoryDefinition = new RepositoryDataImpl(repositoryDataEntity);
                Repository repository = repositoryDefinition.getRepository();
                GitHubRepository ghRepository = Narrow.to(repository, GitHubRepository.class);
                if (ghRepository != null)
                {
                    password = encryptionService.decrypt(ghRepository.getPassword());
                }
            }
        }

        if (StringUtils.isBlank(username))
        {
            addFieldError("username", getText("repository.github.error.emptyUsername"));
        }
        checkFieldXssSafety("username", username);

        if (hasErrors())
        {
            return super.getJsonObject();
        }

        try
        {
            gitHubRepositories = getGitHubRepositoresAndBranches();
        }
        catch (GitHubAccessor.GitHubException e)
        {
            if (e.isAuthError())
            {
                if (getPlan() != null)
                {
                    addFieldError("username", getText("repository.github.error.notAuthorized"));
                }
                else
                {
                    addFieldError("temporary.password", getText("repository.github.error.notAuthorized"));
                }
            }
            else
            {
                addActionError(getText("repository.github.ajaxError") + e.toString());
                log.error("Could not load bitbucket repositories for " + username + ".", e);
            }
        }
        catch (Exception e)
        {
            addActionError(getText("repository.github.ajaxError") + e.toString());
            log.error("Could not load bitbucket repositories for " + username + ".", e);
        }

        JSONObject jsonObject = super.getJsonObject();

        if (hasErrors())
        {
            return jsonObject;
        }

        assert gitHubRepositories!=null;
        List<JSONObject> data = Lists.newArrayList();
        for (Map.Entry<String, Iterable<String>> entry : gitHubRepositories.entrySet())
        {
            String repository = entry.getKey();
            for (String branch : entry.getValue())
            {
                data.add(new JSONObject()
                        .put("value", branch)
                        .put("text", branch)
                        .put("supportedValues", new String[]{repository}));
            }
        }
        jsonObject.put("repositoryBranchFilter", new JSONObject().put("data", data));
        jsonObject.put("gitHubRepositories", newJsonLinkedHashMap(gitHubRepositories));

        return jsonObject;
    }

    private JSONObject newJsonLinkedHashMap(Map<String, Iterable<String>> gitHubRepositories) throws JSONException
    {
        final JSONObject map = new JSONObject();
        for (Map.Entry<String, Iterable<String>> entry : gitHubRepositories.entrySet())
        {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods
    // -------------------------------------------------------------------------------------------------- Private Helper

    @NotNull
    private Map<String, Iterable<String>> getGitHubRepositoresAndBranches() throws IOException, GitHubAccessor.GitHubException
    {
        final GitHubAccessor gitHubAccessor = new GitHubAccessor(username, password);

        final Map<String, Iterable<String>> accessibleRepositoriesAndBranches = gitHubAccessor.getAccessibleRepositoriesAndBranches();

        if (accessibleRepositoriesAndBranches.isEmpty())
        {
            addFieldError("username", getText("repository.bitbucket.error.noRepositories", Arrays.asList(username)));
        }
        return accessibleRepositoriesAndBranches;
    }

    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators

    public void setUsername(String username)
    {
        this.username = username;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public void setRepositoryId(final long repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public void setRepositoryDefinitionManager(final RepositoryDefinitionManager repositoryDefinitionManager)
    {
        this.repositoryDefinitionManager = repositoryDefinitionManager;
    }
}
