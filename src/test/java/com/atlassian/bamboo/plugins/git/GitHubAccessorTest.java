package com.atlassian.bamboo.plugins.git;

import com.google.inject.internal.ImmutableMap;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

public class GitHubAccessorTest
{
    private static final String FURRY_OCTO_NEMESIS = "furry-octo-nemesis";
    private static final String NORTH_AMERICAN_WIGHT = "north-american-wight";

    private static final String JOHNSMITH = "johnsmith";
    private static final String EXPECTED_USER_REPOSITORIES = JOHNSMITH + "/" + FURRY_OCTO_NEMESIS + ", " + JOHNSMITH + "/" + NORTH_AMERICAN_WIGHT;

    private static final String ORG_REPOS_JSON = "  [{\n" +
            "    \"has_wiki\": false,\n" +
            "    \"has_issues\": false,\n" +
            "    \"forks\": 0,\n" +
            "    \"open_issues\": 0,\n" +
            "    \"mirror_url\": null,\n" +
            "    \"language\": \"Java\",\n" +
            "    \"description\": \"Jenkins clover plugin\",\n" +
            "    \"svn_url\": \"https://github.com/atlassian/clover-jenkins-plugin\",\n" +
            "    \"pushed_at\": \"2012-05-10T07:10:00Z\",\n" +
            "    \"full_name\": \"atlassian/clover-jenkins-plugin\",\n" +
            "    \"fork\": true,\n" +
            "    \"created_at\": \"2012-04-24T09:58:47Z\",\n" +
            "    \"url\": \"https://api.github.com/repos/atlassian/clover-jenkins-plugin\",\n" +
            "    \"has_downloads\": true,\n" +
            "    \"watchers\": 2,\n" +
            "    \"size\": 120,\n" +
            "    \"homepage\": \"http://jenkins-ci.org/\",\n" +
            "    \"clone_url\": \"https://github.com/atlassian/clover-jenkins-plugin.git\",\n" +
            "    \"ssh_url\": \"git@github.com:atlassian/clover-jenkins-plugin.git\",\n" +
            "    \"private\": false,\n" +
            "    \"html_url\": \"https://github.com/atlassian/clover-jenkins-plugin\",\n" +
            "    \"updated_at\": \"2012-05-10T07:10:00Z\",\n" +
            "    \"owner\": {\n" +
            "      \"login\": \"atlassian\",\n" +
            "      \"gravatar_id\": \"45dba3e8ec3cfdfe00e8c9b43330fc7f\",\n" +
            "      \"url\": \"https://api.github.com/users/atlassian\",\n" +
            "      \"id\": 168166,\n" +
            "      \"avatar_url\": \"https://secure.gravatar.com/avatar/45dba3e8ec3cfdfe00e8c9b43330fc7f?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-orgs.png\"\n" +
            "    },\n" +
            "    \"name\": \"clover-jenkins-plugin\",\n" +
            "    \"id\": 4123258,\n" +
            "    \"git_url\": \"git://github.com/atlassian/clover-jenkins-plugin.git\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"has_wiki\": true,\n" +
            "    \"has_issues\": false,\n" +
            "    \"forks\": 0,\n" +
            "    \"open_issues\": 0,\n" +
            "    \"mirror_url\": null,\n" +
            "    \"language\": \"Java\",\n" +
            "    \"description\": \"Clover Plugin\",\n" +
            "    \"svn_url\": \"https://github.com/atlassian/clover-hudson-plugin\",\n" +
            "    \"pushed_at\": \"2012-05-14T08:55:47Z\",\n" +
            "    \"full_name\": \"atlassian/clover-hudson-plugin\",\n" +
            "    \"fork\": true,\n" +
            "    \"created_at\": \"2012-04-24T10:05:36Z\",\n" +
            "    \"url\": \"https://api.github.com/repos/atlassian/clover-hudson-plugin\",\n" +
            "    \"has_downloads\": true,\n" +
            "    \"watchers\": 2,\n" +
            "    \"size\": 116,\n" +
            "    \"homepage\": \"http://wiki.hudson-ci.org/display/HUDSON/Clover+Plugin\",\n" +
            "    \"clone_url\": \"https://github.com/atlassian/clover-hudson-plugin.git\",\n" +
            "    \"ssh_url\": \"git@github.com:atlassian/clover-hudson-plugin.git\",\n" +
            "    \"private\": false,\n" +
            "    \"html_url\": \"https://github.com/atlassian/clover-hudson-plugin\",\n" +
            "    \"updated_at\": \"2012-05-14T08:55:48Z\",\n" +
            "    \"owner\": {\n" +
            "      \"login\": \"atlassian\",\n" +
            "      \"gravatar_id\": \"45dba3e8ec3cfdfe00e8c9b43330fc7f\",\n" +
            "      \"url\": \"https://api.github.com/users/atlassian\",\n" +
            "      \"id\": 168166,\n" +
            "      \"avatar_url\": \"https://secure.gravatar.com/avatar/45dba3e8ec3cfdfe00e8c9b43330fc7f?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-orgs.png\"\n" +
            "    },\n" +
            "    \"name\": \"clover-hudson-plugin\",\n" +
            "    \"id\": 4123321,\n" +
            "    \"git_url\": \"git://github.com/atlassian/clover-hudson-plugin.git\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"has_wiki\": false,\n" +
            "    \"has_issues\": false,\n" +
            "    \"forks\": 0,\n" +
            "    \"open_issues\": 0,\n" +
            "    \"mirror_url\": null,\n" +
            "    \"language\": \"Java\",\n" +
            "    \"description\": \"Mirror of Apache Xalan Java\",\n" +
            "    \"svn_url\": \"https://github.com/atlassian/xalan-j\",\n" +
            "    \"pushed_at\": \"2012-04-26T05:40:47Z\",\n" +
            "    \"full_name\": \"atlassian/xalan-j\",\n" +
            "    \"fork\": true,\n" +
            "    \"created_at\": \"2012-04-26T05:39:48Z\",\n" +
            "    \"url\": \"https://api.github.com/repos/atlassian/xalan-j\",\n" +
            "    \"has_downloads\": true,\n" +
            "    \"watchers\": 1,\n" +
            "    \"size\": 132,\n" +
            "    \"homepage\": null,\n" +
            "    \"clone_url\": \"https://github.com/atlassian/xalan-j.git\",\n" +
            "    \"ssh_url\": \"git@github.com:atlassian/xalan-j.git\",\n" +
            "    \"private\": false,\n" +
            "    \"master_branch\": \"trunk\",\n" +
            "    \"html_url\": \"https://github.com/atlassian/xalan-j\",\n" +
            "    \"updated_at\": \"2012-04-26T05:40:49Z\",\n" +
            "    \"owner\": {\n" +
            "      \"login\": \"atlassian\",\n" +
            "      \"gravatar_id\": \"45dba3e8ec3cfdfe00e8c9b43330fc7f\",\n" +
            "      \"url\": \"https://api.github.com/users/atlassian\",\n" +
            "      \"id\": 168166,\n" +
            "      \"avatar_url\": \"https://secure.gravatar.com/avatar/45dba3e8ec3cfdfe00e8c9b43330fc7f?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-orgs.png\"\n" +
            "    },\n" +
            "    \"name\": \"xalan-j\",\n" +
            "    \"id\": 4144696,\n" +
            "    \"git_url\": \"git://github.com/atlassian/xalan-j.git\"\n" +
            "  }\n" +
            "]\n";

    private static final String USER_REPOS_JSON = "[\n" +
            "  {\n" +
            "    \"open_issues\": 0,\n" +
            "    \"svn_url\": \"https://github.com/" + JOHNSMITH + "/" + NORTH_AMERICAN_WIGHT + "\",\n" +
            "    \"git_url\": \"git://github.com/" + JOHNSMITH + "/" + NORTH_AMERICAN_WIGHT + ".git\",\n" +
            "    \"html_url\": \"https://github.com/" + JOHNSMITH + "/" + NORTH_AMERICAN_WIGHT + "\",\n" +
            "    \"pushed_at\": \"2012-06-14T14:59:34Z\",\n" +
            "    \"language\": null,\n" +
            "    \"description\": \"\",\n" +
            "    \"full_name\": \"" + JOHNSMITH + "/" + NORTH_AMERICAN_WIGHT + "\",\n" +
            "    \"has_downloads\": true,\n" +
            "    \"watchers\": 1,\n" +
            "    \"fork\": false,\n" +
            "    \"clone_url\": \"https://github.com/" + JOHNSMITH + "/" + NORTH_AMERICAN_WIGHT + ".git\",\n" +
            "    \"ssh_url\": \"git@github.com:" + JOHNSMITH + "/" + NORTH_AMERICAN_WIGHT + ".git\",\n" +
            "    \"created_at\": \"2012-06-14T14:59:34Z\",\n" +
            "    \"url\": \"https://api.github.com/repos/" + JOHNSMITH + "/" + NORTH_AMERICAN_WIGHT + "\",\n" +
            "    \"size\": 0,\n" +
            "    \"homepage\": null,\n" +
            "    \"private\": false,\n" +
            "    \"mirror_url\": null,\n" +
            "    \"updated_at\": \"2012-06-14T14:59:34Z\",\n" +
            "    \"owner\": {\n" +
            "      \"login\": \"" + JOHNSMITH + "\",\n" +
            "      \"gravatar_id\": \"b364da54f64b0e84125f4b1c8628c454\",\n" +
            "      \"url\": \"https://api.github.com/users/" + JOHNSMITH + "\",\n" +
            "      \"avatar_url\": \"https://secure.gravatar.com/avatar/b364da54f64b0e84125f4b1c8628c454?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-140.png\",\n" +
            "      \"id\": 737454\n" +
            "    },\n" +
            "    \"name\": \"" + NORTH_AMERICAN_WIGHT + "\",\n" +
            "    \"permissions\": {\n" +
            "      \"pull\": true,\n" +
            "      \"admin\": true,\n" +
            "      \"push\": true\n" +
            "    },\n" +
            "    \"has_wiki\": true,\n" +
            "    \"has_issues\": true,\n" +
            "    \"id\": 4664640,\n" +
            "    \"forks\": 1\n" +
            "  },\n" +
            "  {\n" +
            "    \"open_issues\": 0,\n" +
            "    \"svn_url\": \"https://github.com/" + JOHNSMITH + "/" + FURRY_OCTO_NEMESIS + "\",\n" +
            "    \"git_url\": \"git://github.com/" + JOHNSMITH + "/" + FURRY_OCTO_NEMESIS + ".git\",\n" +
            "    \"html_url\": \"https://github.com/" + JOHNSMITH + "/" + FURRY_OCTO_NEMESIS + "\",\n" +
            "    \"pushed_at\": \"2012-06-14T14:41:00Z\",\n" +
            "    \"language\": null,\n" +
            "    \"description\": \"MyOnlyRepo\",\n" +
            "    \"full_name\": \"" + JOHNSMITH + "/" + FURRY_OCTO_NEMESIS + "\",\n" +
            "    \"has_downloads\": true,\n" +
            "    \"watchers\": 1,\n" +
            "    \"fork\": false,\n" +
            "    \"clone_url\": \"https://github.com/" + JOHNSMITH + "/" + FURRY_OCTO_NEMESIS + ".git\",\n" +
            "    \"ssh_url\": \"git@github.com:" + JOHNSMITH + "/" + FURRY_OCTO_NEMESIS + ".git\",\n" +
            "    \"created_at\": \"2012-06-14T14:40:59Z\",\n" +
            "    \"url\": \"https://api.github.com/repos/" + JOHNSMITH + "/" + FURRY_OCTO_NEMESIS + "\",\n" +
            "    \"size\": 0,\n" +
            "    \"homepage\": null,\n" +
            "    \"private\": false,\n" +
            "    \"mirror_url\": null,\n" +
            "    \"updated_at\": \"2012-06-14T14:41:00Z\",\n" +
            "    \"owner\": {\n" +
            "      \"login\": \"" + JOHNSMITH + "\",\n" +
            "      \"gravatar_id\": \"b364da54f64b0e84125f4b1c8628c454\",\n" +
            "      \"url\": \"https://api.github.com/users/" + JOHNSMITH + "\",\n" +
            "      \"avatar_url\": \"https://secure.gravatar.com/avatar/b364da54f64b0e84125f4b1c8628c454?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-140.png\",\n" +
            "      \"id\": 737454\n" +
            "    },\n" +
            "    \"name\": \"" + FURRY_OCTO_NEMESIS + "\",\n" +
            "    \"permissions\": {\n" +
            "      \"pull\": true,\n" +
            "      \"admin\": true,\n" +
            "      \"push\": true\n" +
            "    },\n" +
            "    \"has_wiki\": true,\n" +
            "    \"has_issues\": true,\n" +
            "    \"id\": 4664414,\n" +
            "    \"forks\": 1\n" +
            "  }\n" +
            "]";

    private static final String USER_ORGS = "[\n" +
            "  {\n" +
            "    \"url\": \"https://api.github.com/orgs/atlassian\",\n" +
            "    \"avatar_url\": \"https://secure.gravatar.com/avatar/45dba3e8ec0ecb33fc7f?d=https://a248.e.akamai.net/assets.github.com%2Fimages%2Fgravatars%2Fgravatar-orgs.png\",\n" +
            "    \"login\": \"atlassian\",\n" +
            "    \"id\": 168166\n" +
            "  }\n" +
            "]\n";

    private static final String MASTER_BRANCH = "[\n" +
            "  {\n" +
            "    \"commit\": {\n" +
            "      \"sha\": \"d620bb5a95366113786092055c25fac927f18798\",\n" +
            "      \"url\": \"https://api.github.com/repos/johnsmith/furry-octo-nemesis/commits/d620bb5a95366113786092055c25fac927f18798\"\n" +
            "    },\n" +
            "    \"name\": \"master\"\n" +
            "  }\n" +
            "]";

    private static final String SOME_BRANCHES = "[\n" +
            "  {\n" +
            "    \"commit\": {\n" +
            "      \"url\": \"https://api.github.com/repos/atlassian/xalan-j/commits/5451282d8cbd922a5f738c99afb8277db01d1b3a\",\n" +
            "      \"sha\": \"5451282d8cbd922a5f738c99afb8277db01d1b3a\"\n" +
            "    },\n" +
            "    \"name\": \"xalan-j_2_1_0_maint\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"commit\": {\n" +
            "      \"url\": \"https://api.github.com/repos/atlassian/xalan-j/commits/512e3961d82b34902d012c47b65654516cf904e1\",\n" +
            "      \"sha\": \"512e3961d82b34902d012c47b65654516cf904e1\"\n" +
            "    },\n" +
            "    \"name\": \"jdk-1_4_2\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"commit\": {\n" +
            "      \"url\": \"https://api.github.com/repos/atlassian/xalan-j/commits/b9c10aa8da4b76c6e267dd66aed26e5579e30c70\",\n" +
            "      \"sha\": \"b9c10aa8da4b76c6e267dd66aed26e5579e30c70\"\n" +
            "    },\n" +
            "    \"name\": \"trunk\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"commit\": {\n" +
            "      \"url\": \"https://api.github.com/repos/atlassian/xalan-j/commits/a77a8c4480cc097fcf0f562a0f4555eb924370bd\",\n" +
            "      \"sha\": \"a77a8c4480cc097fcf0f562a0f4555eb924370bd\"\n" +
            "    },\n" +
            "    \"name\": \"jaxp-ri-1_2_0-fcs-branch\"\n" +
            "  }]";

    private static final String FAULT = "{\n" +
            "  \"message\": \"Bad credentials\"\n" +
            "}\n";
    private final static Map<String, String> REQUEST_TO_RESPONSE =
            ImmutableMap.<String, String>builder()
                    .put("users/johnsmith/repos", USER_REPOS_JSON)
                    .put("orgs/atlassian/repos", ORG_REPOS_JSON)
                    .put("users/johnsmith/orgs", USER_ORGS)
                    .put("repos/johnsmith/furry-octo-nemesis/branches", MASTER_BRANCH)
                    .put("repos/johnsmith/north-american-wight/branches", MASTER_BRANCH)
                    .put("repos/atlassian/clover-hudson-plugin/branches", MASTER_BRANCH)
                    .put("repos/atlassian/clover-jenkins-plugin/branches", MASTER_BRANCH)
                    .put("repos/atlassian/xalan-j/branches", SOME_BRANCHES)
                    .put("orgs/faulty/repos", FAULT)
                    .build();

    private static final String EXPECTED_ATLASSIAN_REPOSITORIES = "atlassian/clover-hudson-plugin, atlassian/clover-jenkins-plugin, atlassian/xalan-j";

    @Test
    public void retrievesSortedRepositoryList() throws IOException, GitHubAccessor.GitHubException
    {
        final GitHubAccessor accessor = getGitHubAccessor(JOHNSMITH, "");
        final String expected = "[" + EXPECTED_USER_REPOSITORIES + "]";
        Assert.assertEquals(accessor.getUserRepositories().toString(), expected);
    }

    @Test
    public void retrievesOrganisationRepositories() throws IOException, GitHubAccessor.GitHubException
    {
        final GitHubAccessor accessor = getGitHubAccessor("johnsmith", "");

        final String expected = "[" + EXPECTED_ATLASSIAN_REPOSITORIES + "]";
        Assert.assertEquals(accessor.getOrganisationRepositories("atlassian").toString(), expected);
    }

    @Test
    public void retrievesOrganisations() throws IOException, GitHubAccessor.GitHubException
    {
        final GitHubAccessor accessor = getGitHubAccessor("johnsmith", "");

        final String expected = "[atlassian]";
        Assert.assertEquals(accessor.getUserOrganisations().toString(), expected);
    }

    @Test
    public void retrievesAccessibleRepositories() throws IOException, GitHubAccessor.GitHubException
    {
        final GitHubAccessor accessor = getGitHubAccessor("johnsmith", "");

        final String expected = "[" + EXPECTED_USER_REPOSITORIES + ", " + EXPECTED_ATLASSIAN_REPOSITORIES + "]";
        Assert.assertEquals(accessor.getAccessibleRepositories().toString(), expected);
    }

    @Test
    public void retrievesAccessibleRepositoriesAndBranches() throws IOException, GitHubAccessor.GitHubException
    {
        final GitHubAccessor accessor = getGitHubAccessor("johnsmith", "");

        final String expected = "{johnsmith/furry-octo-nemesis=[master], johnsmith/north-american-wight=[master], atlassian/clover-hudson-plugin=[master], atlassian/clover-jenkins-plugin=[master], atlassian/xalan-j=[jaxp-ri-1_2_0-fcs-branch, jdk-1_4_2, trunk, xalan-j_2_1_0_maint]}";
        Assert.assertEquals(accessor.getAccessibleRepositoriesAndBranches().toString(), expected);
    }

   
    @Test(expectedExceptions = GitHubAccessor.GitHubException.class)
    void handlesErrors() throws IOException, GitHubAccessor.GitHubException
    {
        final GitHubAccessor accessor = getGitHubAccessor("a", "a");
        Assert.assertEquals(accessor.getOrganisationRepositories("faulty").toString(), "");
    }

    private GitHubAccessor getGitHubAccessor(final String username, final String password)
    {
        return
                new GitHubAccessor(username, password)
                {
                    @Override
                    protected String getStringFromUrl(String url) throws IOException
                    {
                        String endpoint = url.replace(GitHubAccessor.GITHUB_API_BASE_URL, "");
                        String response = REQUEST_TO_RESPONSE.get(endpoint);
                        if (response == null)
                        {
                            throw new NullPointerException(endpoint + " is an unknown URL");
                        }
                        return response;
                    }
                };
    }
}
