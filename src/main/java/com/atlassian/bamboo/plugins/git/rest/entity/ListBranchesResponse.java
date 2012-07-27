package com.atlassian.bamboo.plugins.git.rest.entity;

import com.atlassian.bamboo.plugins.git.github.api.rest.entity.GitHubBranchEntity;
import com.atlassian.bamboo.plugins.git.rest.commons.RestConstants;
import com.atlassian.bamboo.rest.entity.RestResponse;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import static javax.xml.bind.annotation.XmlAccessType.FIELD;

@XmlRootElement(name = RestConstants.RESPONSE)
@XmlAccessorType(FIELD)
public class ListBranchesResponse extends RestResponse
{
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = Logger.getLogger(ListBranchesResponse.class);
    // ------------------------------------------------------------------------------------------------------- Constants
    // ------------------------------------------------------------------------------------------------- Type Properties
    @XmlElementWrapper(name = RestConstants.BRANCHES)
    @XmlElement(name = RestConstants.BRANCH)
    private List<GitHubBranchEntity> branches;
    // ---------------------------------------------------------------------------------------------------- Dependencies
    // ---------------------------------------------------------------------------------------------------- Constructors
    // ----------------------------------------------------------------------------------------------- Interface Methods
    // -------------------------------------------------------------------------------------------------- Action Methods
    // -------------------------------------------------------------------------------------------------- Public Methods
    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators

    public void setBranches(List<GitHubBranchEntity> branches)
    {
        this.branches = branches;
    }
}
