package com.atlassian.bamboo.plugins.git.rest.resource;

import com.atlassian.bamboo.plugins.git.GitHubAccessor;
import com.atlassian.bamboo.plugins.git.GitHubRepository;
import com.atlassian.bamboo.plugins.git.github.api.rest.entity.GitHubBranchEntity;
import com.atlassian.bamboo.plugins.git.github.api.rest.entity.GitHubRepositoryEntity;
import com.atlassian.bamboo.plugins.git.rest.commons.RestConstants;
import com.atlassian.bamboo.plugins.git.rest.entity.ListBranchesResponse;
import com.atlassian.bamboo.plugins.git.rest.entity.ListRepositoriesResponse;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.repository.RepositoryData;
import com.atlassian.bamboo.repository.RepositoryDataEntity;
import com.atlassian.bamboo.repository.RepositoryDataImpl;
import com.atlassian.bamboo.repository.RepositoryDefinitionManager;
import com.atlassian.bamboo.rest.entity.RestResponse;
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.util.Narrow;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sun.jersey.spi.resource.Singleton;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

@Path(RestConstants.GITHUB)
@Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
@Singleton
public class GitHubResource
{
    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(GitHubResource.class);
    // ------------------------------------------------------------------------------------------------------- Constants
    // ------------------------------------------------------------------------------------------------- Type Properties
    // ---------------------------------------------------------------------------------------------------- Dependencies
    private final EncryptionService encryptionService;
    private final I18nResolver i18nResolver;
    private final RepositoryDefinitionManager repositoryDefinitionManager;
    // ---------------------------------------------------------------------------------------------------- Constructors

    public GitHubResource(EncryptionService encryptionService, I18nResolver i18nResolver, RepositoryDefinitionManager repositoryDefinitionManager)
    {
        this.encryptionService = encryptionService;
        this.i18nResolver = i18nResolver;
        this.repositoryDefinitionManager = repositoryDefinitionManager;
    }

    // ----------------------------------------------------------------------------------------------- Interface Methods
    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     *
     * @param uriInfo
     * @param username
     * @param password
     * @param repositoryId
     * @param query
     * @return
     */
    @POST
    @Path(RestConstants.REPOSITORIES + "/{" + RestConstants.USERNAME + "}")
    public Response getAvailableRepositories(@Context UriInfo uriInfo,
                                             @PathParam(RestConstants.USERNAME) String username,
                                             @FormParam(RestConstants.PASSWORD) String password,
                                             @FormParam(RestConstants.REPOSITORY_ID) long repositoryId,
                                             @FormParam(RestConstants.QUERY) String query)
    {
        if (repositoryId > 0 && StringUtils.isBlank(password))
        {
            password = getRepositoryPassword(repositoryId);
        }

        RestResponse.Builder builder = RestResponse.builder();

        try
        {
            GitHubAccessor gitHubAccessor = new GitHubAccessor(username, password);

            ImmutableList<GitHubRepositoryEntity> repositories = GitHubRepositoryEntity.orderingByFullName().immutableSortedCopy(
                    Iterables.transform(gitHubAccessor.getAccessibleRepositories(),
                                        new Function<String, GitHubRepositoryEntity>()
                                        {
                                            @Override
                                            public GitHubRepositoryEntity apply(@Nullable String input)
                                            {
                                                return GitHubRepositoryEntity.builder().fullName(input).build();
                                            }
                                        })
            );

            ListRepositoriesResponse response = builder.build(ListRepositoriesResponse.class);
            response.setRepositories(repositories);

            return Response.ok(response).build();
        }
        catch (IOException e)
        {
            log.warn(i18nResolver.getText("repository.github.ajaxError"), e);
            builder.error(i18nResolver.getText("repository.github.ajaxError") + e.toString());
        }
        catch (GitHubAccessor.GitHubException e)
        {
            log.warn(i18nResolver.getText("repository.github.ajaxError"), e);
            builder.error(i18nResolver.getText("repository.github.ajaxError") + e.toString());
        }

        return Response.ok(builder.build(RestResponse.class)).build();
    }

    /**
     *
     * @param uriInfo
     * @param owner
     * @param name
     * @param username
     * @param password
     * @param repositoryId
     * @return
     */
    @POST
    @Path(RestConstants.REPOSITORIES + "/{" + RestConstants.OWNER + "}/{" + RestConstants.NAME + "}/" + RestConstants.BRANCHES)
    public Response getBranches(@Context UriInfo uriInfo,
                                @PathParam(RestConstants.OWNER) String owner,
                                @PathParam(RestConstants.NAME) String name,
                                @FormParam(RestConstants.USERNAME) String username,
                                @FormParam(RestConstants.PASSWORD) String password,
                                @FormParam(RestConstants.REPOSITORY_ID) long repositoryId)
    {
        if (repositoryId > 0 && StringUtils.isBlank(password))
        {
            password = getRepositoryPassword(repositoryId);
        }

        RestResponse.Builder builder = RestResponse.builder();

        try
        {
            GitHubAccessor gitHubAccessor = new GitHubAccessor(username, password);

            ImmutableList<GitHubBranchEntity> branches = GitHubBranchEntity.orderingByName().immutableSortedCopy(
                    Iterables.transform(gitHubAccessor.getBranches(String.format("%s/%s", owner, name)),
                                        new Function<String, GitHubBranchEntity>()
                                        {
                                            @Override
                                            public GitHubBranchEntity apply(@Nullable String input)
                                            {
                                                return GitHubBranchEntity.builder().name(input).build();
                                            }
                                        })
            );

            ListBranchesResponse response = builder.build(ListBranchesResponse.class);
            response.setBranches(branches);

            return Response.ok(response).build();
        }
        catch (IOException e)
        {
            log.warn(i18nResolver.getText("repository.github.ajaxError"), e);
            builder.error(i18nResolver.getText("repository.github.ajaxError") + e.toString());
        }
        catch (GitHubAccessor.GitHubException e)
        {
            log.warn(i18nResolver.getText("repository.github.ajaxError"), e);
            builder.error(i18nResolver.getText("repository.github.ajaxError") + e.toString());
        }

        return Response.ok(builder.build(RestResponse.class)).build();
    }

    // -------------------------------------------------------------------------------------- Basic Accessors / Mutators

    @Nullable
    private String getRepositoryPassword(long repositoryId)
    {
        RepositoryDataEntity repositoryDataEntity = repositoryDefinitionManager.getRepositoryDataEntity(repositoryId);
        if (repositoryDataEntity != null)
        {
            RepositoryData repositoryData = new RepositoryDataImpl(repositoryDataEntity);
            Repository repository = repositoryData.getRepository();
            GitHubRepository ghRepository = Narrow.to(repository, GitHubRepository.class);
            if (ghRepository != null)
            {
                return encryptionService.decrypt(ghRepository.getEncryptedPassword());
            }
        }

        return null;
    }
}
