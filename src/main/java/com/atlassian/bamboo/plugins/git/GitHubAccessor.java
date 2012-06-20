package com.atlassian.bamboo.plugins.git;

import org.apache.log4j.Logger;
import com.atlassian.bamboo.rest.util.Get;
import com.atlassian.bamboo.utils.SystemProperty;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.opensymphony.webwork.dispatcher.json.JSONArray;
import com.opensymphony.webwork.dispatcher.json.JSONException;
import com.opensymphony.webwork.dispatcher.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class GitHubAccessor
{
    private static final Logger log = Logger.getLogger(GitHubAccessor.class);

    static final String GITHUB_API_BASE_URL = new SystemProperty(false, "atlassian.bamboo.github.api.base.url",
            "ATLASSIAN_BAMBOO_GITHUB_API_BASE_URL").getValue("https://api.github.com/");
    private static final String JSON_ERROR_MESSAGE_FIELD = "message";

    private final String username;
    private final String password;
    private final boolean onlyPublic;


    public static class GitHubException extends Exception
    {
        public GitHubException(String message)
        {
            super(message);
        }

        public GitHubException(JSONException e)
        {
            super(e);
        }

        public boolean isAuthError()
        {
            return getMessage().equalsIgnoreCase("Bad credentials");
        }
    }

    public GitHubAccessor(String username, String password)
    {
        this.username = username;
        this.password = password;
        this.onlyPublic = StringUtils.isBlank(password);
    }

    public Iterable<String> getAccessibleRepositories() throws IOException, GitHubException
    {
        Iterable<String> userRepos = getUserRepositories();
        Collection<String> orgRepos = Lists.newArrayList();
        for (String org : getUserOrganisations())
        {
            Iterables.addAll(orgRepos, getOrganisationRepositories(org));
        }

        return Iterables.concat(userRepos, orgRepos);
    }

    public Map<String, Iterable<String>> getAccessibleRepositoriesAndBranches() throws IOException, GitHubException
    {
        final Iterable<String> accessibleRepositories = getAccessibleRepositories();
        final Map<String, Iterable<String>> accessibleRepositoriesAndBranches = Maps.newLinkedHashMap();
        for (String accessibleRepository : accessibleRepositories)
        {
            accessibleRepositoriesAndBranches.put(accessibleRepository, getBranches(accessibleRepository));
        }
        return accessibleRepositoriesAndBranches;
    }

    public Iterable<String> getBranches(String userRepo) throws IOException, GitHubException
    {
        JSONArray repoJson = getJSONArrayResponseFromUrl("repos/" + userRepo + "/branches");
        return foldArrayAndSort(repoJson, "name");
    }

    public Iterable<String> getUserOrganisations() throws IOException, GitHubException
    {
        JSONArray repoJson = getJSONArrayResponseFromUrl(getEndPointForCurrentCredentials("orgs"));
        return foldArrayAndSort(repoJson, "login");
    }

    private String getEndPointForCurrentCredentials(final String endPath)
    {
        return onlyPublic ? "users/" + username + "/" + endPath : "user/" + endPath;
    }

    public Iterable<String> getUserRepositories() throws IOException, GitHubException
    {
        return getRepositories(getEndPointForCurrentCredentials("repos"));
    }

    public Iterable<String> getOrganisationRepositories(final String org) throws IOException, GitHubException
    {
        return getRepositories("orgs/" + org + "/repos");
    }

    private Iterable<String> getRepositories(final String endpoint) throws IOException, GitHubException
    {
        final JSONArray json = getJSONArrayResponseFromUrl(endpoint);
        return foldArrayAndSort(json, "full_name");
    }

    private JSONArray getJSONArrayResponseFromUrl(String url) throws IOException, GitHubException
    {
        String stringFromUrl = getStringFromUrl(GITHUB_API_BASE_URL + url);
        try
        {
            return new JSONArray(stringFromUrl);
        }
        catch (JSONException e)
        {
            JSONObject jsonObject = createQuietly(stringFromUrl);
            if (jsonObject!=null && jsonObject.has(JSON_ERROR_MESSAGE_FIELD))
            {
                throw new GitHubException(getStringQuietly(jsonObject));
            }
            throw new GitHubException(e);
        }
    }

    @Nullable
    private String getStringQuietly(JSONObject jsonObject)
    {
        try
        {
            return jsonObject.getString("message");
        }
        catch (JSONException e)
        {
            return null;
        }
    }

    @Nullable
    private JSONObject createQuietly(String stringFromUrl)
    {
        try
        {
            return new JSONObject(stringFromUrl);
        }
        catch (JSONException e)
        {
            return null;
        }
    }


    protected String getStringFromUrl(String url) throws IOException
    {
        Get call = new Get(url);
        if (!onlyPublic)
        {
            call.setBasicCredentials(username, password);
        }
        try
        {
            call.execute();
            String response = IOUtils.toString(call.getResponseAsStream());
            return response;
        }
        finally
        {
            call.release();
        }
    }

    private List<String> foldArrayAndSort(JSONArray repoJson, final String field) throws GitHubException
    {
        return Ordering.natural().sortedCopy(foldArray(repoJson, field));
    }

    private Iterable<String> foldArray(JSONArray jsonArray, String field) throws GitHubException
    {
        try
        {
            List<String> repositories = Lists.newArrayList();
            for (int index = 0; index < jsonArray.length(); index++)
            {
                final JSONObject jsonRepository = jsonArray.getJSONObject(index);

                final String repository = jsonRepository.getString(field);
                repositories.add(repository);
            }
            return repositories;
        }
        catch (JSONException e)
        {
            throw new GitHubException(e);
        }
    }
}
