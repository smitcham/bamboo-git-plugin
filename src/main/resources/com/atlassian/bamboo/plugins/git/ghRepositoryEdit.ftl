[#-- @ftlvariable name="repository" type="com.atlassian.bamboo.plugins.git.GitHubRepository" --]

[@ww.textfield labelKey='repository.github.username' name='repository.github.username' required='true' /]
[#if buildConfiguration.getString('repository.github.password')?has_content]
    [@ww.checkbox labelKey='repository.password.change' toggle='true' name='temporary.github.password.change' /]
    [@ui.bambooSection dependsOn='temporary.github.password.change']
        [@ww.password labelKey='repository.github.password' name='repository.github.temporary.password' /]
    [/@ui.bambooSection]
[#else]
    [@ww.hidden name='temporary.github.password.change' value='true' /]
    [@ww.password labelKey='repository.github.password' name='repository.github.temporary.password' /]
[/#if]

[@ww.select labelKey='repository.github.repository' name='repository.github.repository' descriptionKey='repository.github.repository.description' fieldClass='github-repository']
    [@ww.param name='disabled' value=!(buildConfiguration.getString('repository.github.repository')?has_content) /]
    [@ww.param name='extraUtility'][@ui.displayButton id='repository-github-load-repositories' valueKey='repository.github.loadRepositories'/][/@ww.param]
    [#if buildConfiguration.getString('repository.github.repository')?has_content]
        [@ww.param name='headerKey2' value=buildConfiguration.getString('repository.github.repository') /]
        [@ww.param name='headerValue2' value=buildConfiguration.getString('repository.github.repository') /]
    [/#if]
[/@ww.select]

[@ww.select labelKey='repository.github.branch' name='repository.github.branch' descriptionKey='repository.github.branch.description' fieldClass='github-branch']
    [@ww.param name='hidden' value=!(buildConfiguration.getString('repository.github.branch')?has_content) /]
    [#if buildConfiguration.getString('repository.github.branch')?has_content]
        [@ww.param name='headerKey2' value=buildConfiguration.getString('repository.github.branch') /]
        [@ww.param name='headerValue2' value=buildConfiguration.getString('repository.github.branch') /]
    [/#if]
[/@ww.select]

[@ww.checkbox labelKey='repository.github.useShallowClones' toggle='true' name='repository.github.useShallowClones' /]
[#if (plan.buildDefinition.branchIntegrationConfiguration.enabled)!false ]
    [@ui.bambooSection dependsOn='repository.github.useShallowClones']
        [@ui.messageBox type='info' titleKey='repository.git.messages.branchIntegration.shallowClonesWillBeDisabled' /]
    [/@ui.bambooSection]
[/#if]

<script type="text/javascript">
(function () {
    var rf = new BAMBOO.GITHUB.RepositoryForm({
        repositoryKey: '${repository.key?js_string}',
        repositoryId: ${(repositoryId)!0},
        selectors: {
            repositoryType: '#selectedRepository',
            username: 'input[name="repository.github.username"]',
            password: 'input[name="repository.github.temporary.password"]',
            loadRepositoriesButton: '#repository-github-load-repositories',
            repository: 'select[name="repository.github.repository"]',
            branch: 'select[name="repository.github.branch"]'
        }
    });
    rf.init();
}());
</script>
