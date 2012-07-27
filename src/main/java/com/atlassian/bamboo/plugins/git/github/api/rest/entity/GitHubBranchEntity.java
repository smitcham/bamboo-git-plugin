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

@XmlRootElement(name = RestConstants.BRANCH)
@XmlAccessorType(FIELD)
public class GitHubBranchEntity
{
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = Logger.getLogger(GitHubBranchEntity.class);
    // ------------------------------------------------------------------------------------------------------- Constants
    // ------------------------------------------------------------------------------------------------- Type Properties

    @XmlElement(name = "name")
    private String name;

/*
  {
    "name": "master",
    "commit": {
      "sha": "6dcb09b5b57875f334f61aebed695e2e4193db5e",
      "url": "https://api.github.com/repos/octocat/Hello-World/commits/c5b97d5ae6c19d5c5df71a34c7fbeeda2479ccbc"
    }
  }
*/
    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    // ----------------------------------------------------------------------------------------------- Interface Methods
    // -------------------------------------------------------------------------------------------------- Action Methods
    // -------------------------------------------------------------------------------------------------- Public Methods

    private static enum OrderingByName implements Comparator<GitHubBranchEntity>
    {
        INSTANCE;

        private static final Ordering<GitHubBranchEntity> ORDERING = Ordering.from(INSTANCE);

        @Override
        public int compare(GitHubBranchEntity o1, GitHubBranchEntity o2)
        {
            if (o1 == null || o2 == null)
            {
                return (o1 == o2) ? 0 : o1 == null ? -1 : 1;
            }

            return new CompareToBuilder()
                    .append(o1.getName(), o2.getName(), String.CASE_INSENSITIVE_ORDER)
                    .toComparison();
        }
    }

    public static Ordering<GitHubBranchEntity> orderingByName()
    {
        return OrderingByName.ORDERING;
    }

    public static class Builder
    {
        private String name;

        public Builder name(String name)
        {
            this.name = name;
            return this;
        }

        public GitHubBranchEntity build()
        {
            GitHubBranchEntity entity = new GitHubBranchEntity();
            entity.name = name;
            return entity;
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators

    public String getName()
    {
        return name;
    }
}
