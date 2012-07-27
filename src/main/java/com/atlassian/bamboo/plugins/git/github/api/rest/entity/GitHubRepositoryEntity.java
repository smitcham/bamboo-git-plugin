package com.atlassian.bamboo.plugins.git.github.api.rest.entity;

import com.atlassian.bamboo.plugins.git.rest.commons.RestConstants;
import com.google.common.collect.Ordering;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Comparator;

import static javax.xml.bind.annotation.XmlAccessType.FIELD;

@XmlRootElement(name = RestConstants.REPOSITORY)
@XmlAccessorType(FIELD)
public class GitHubRepositoryEntity
{
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = Logger.getLogger(GitHubRepositoryEntity.class);
    // ------------------------------------------------------------------------------------------------------- Constants
    // ------------------------------------------------------------------------------------------------- Type Properties

    @XmlElement(name = "full_name")
    private String fullName;

/*
  {
    "url": "https://api.github.com/repos/octocat/Hello-World",
    "html_url": "https://github.com/octocat/Hello-World",
    "clone_url": "https://github.com/octocat/Hello-World.git",
    "git_url": "git://github.com/octocat/Hello-World.git",
    "ssh_url": "git@github.com:octocat/Hello-World.git",
    "svn_url": "https://svn.github.com/octocat/Hello-World",
    "mirror_url": "git://git.example.com/octocat/Hello-World",
    "id": 1296269,
    "owner": {
      "login": "octocat",
      "id": 1,
      "avatar_url": "https://github.com/images/error/octocat_happy.gif",
      "gravatar_id": "somehexcode",
      "url": "https://api.github.com/users/octocat"
    },
    "name": "Hello-World",
    "full_name": "octocat/Hello-World",
    "description": "This your first repo!",
    "homepage": "https://github.com",
    "language": null,
    "private": false,
    "fork": false,
    "forks": 9,
    "watchers": 80,
    "size": 108,
    "master_branch": "master",
    "open_issues": 0,
    "pushed_at": "2011-01-26T19:06:43Z",
    "created_at": "2011-01-26T19:01:12Z",
    "updated_at": "2011-01-26T19:14:43Z"
  }
*/
    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    // ----------------------------------------------------------------------------------------------- Interface Methods
    // -------------------------------------------------------------------------------------------------- Action Methods
    // -------------------------------------------------------------------------------------------------- Public Methods

    private static enum OrderingByFullName implements Comparator<GitHubRepositoryEntity>
    {
        INSTANCE;

        private static final Ordering<GitHubRepositoryEntity> ORDERING = Ordering.from(INSTANCE);

        @Override
        public int compare(GitHubRepositoryEntity o1, GitHubRepositoryEntity o2)
        {
            if (o1 == null || o2 == null)
            {
                return (o1 == o2) ? 0 : o1 == null ? -1 : 1;
            }

            return new CompareToBuilder()
                    .append(o1.getFullName(), o2.getFullName(), String.CASE_INSENSITIVE_ORDER)
                    .toComparison();
        }
    }

    public static Ordering<GitHubRepositoryEntity> orderingByFullName()
    {
        return OrderingByFullName.ORDERING;
    }

    public static class Builder
    {
        private String fullName;

        public Builder fullName(String fullName)
        {
            this.fullName = fullName;
            return this;
        }

        public GitHubRepositoryEntity build()
        {
            GitHubRepositoryEntity entity = new GitHubRepositoryEntity();
            entity.fullName = fullName;
            return entity;
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators

    public String getFullName()
    {
        return fullName;
    }
}
