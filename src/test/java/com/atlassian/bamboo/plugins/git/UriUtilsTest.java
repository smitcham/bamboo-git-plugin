package com.atlassian.bamboo.plugins.git;

import com.atlassian.bamboo.plugins.ssh.ProxyRegistrationInfoImpl;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class UriUtilsTest
{
    GitRepository.GitRepositoryAccessData proxyAccessData = new GitRepository.GitRepositoryAccessData();

    @BeforeClass
    public void setup()
    {
        proxyAccessData.proxyRegistrationInfo = new ProxyRegistrationInfoImpl("proxyHost", 22, null, "proxyUserName");
    }

    @Test
    public void uriCreation() throws URISyntaxException
    {
        String uri = "ssh://[user@]host.xz[:port]/path/to/repo.git/";
        createUris(uri, false);
        uri = "git://host.xz[:port]/path/to/repo.git/";
        createUris(uri, false);
        uri = "http[s]://host.xz[:port]/path/to/repo.git/";
        createUris(uri, false);
        uri = "ftp[s]://host.xz[:port]/path/to/repo.git/";
        createUris(uri, false);
        uri = "rsync://host.xz/path/to/repo.git/";
        createUris(uri, false);
        uri = "ssh://[user@]host.xz[:port]/~[user]/path/to/repo.git/";
        createUris(uri, false);
        uri = "git://host.xz[:port]/~[user]/path/to/repo.git/";
        createUris(uri, false);
        uri = "/path/to/repo.git/";
        createUris(uri, false);
        uri = "file:///path/to/repo.git/";
        createUris(uri, false);


        uri = "[user@]host.xz:path/to/repo.git/";
        createUris(uri, true);

        uri = "[user@]host.xz:/~[user]/path/to/repo.git/";
        createUris(uri, false);
    }

    private void createUris(String uriStr, boolean isRelative) throws URISyntaxException
    {
        String minimalUriStr = uriStr.replaceAll("\\[[:\\w@]*\\]", "");
        testUri(minimalUriStr, isRelative);

        String fullUriStr = uriStr.replaceAll("[\\[\\]]", "");
        testUri(fullUriStr, isRelative);
    }

    private void testUri(String minimalUriStr, boolean isRelative) throws URISyntaxException
    {
        ScpAwareUri uri = ScpAwareUri.create(minimalUriStr);
        Assert.assertTrue(uri.getRawPath().contains("path"), uri.getRawPath() +  " should contain path");
        Assert.assertEquals(uri.isRelativePath(), isRelative);
        URI minimalUriViaProxy = UriUtils.getUriViaProxy(proxyAccessData, uri);
    }

    @Test
    public void recognisesScpLikeUris()
    {
        Assert.assertFalse(UriUtils.hasScpSyntax("host"));
        Assert.assertFalse(UriUtils.hasScpSyntax("host/path"));
        Assert.assertFalse(UriUtils.hasScpSyntax("user@host/path"));
        Assert.assertFalse(UriUtils.hasScpSyntax("ssh://user:password@host/path"));
        Assert.assertFalse(UriUtils.hasScpSyntax("ssh://user:password@host:22/path"));

        Assert.assertTrue(UriUtils.hasScpSyntax("host:"));
        Assert.assertTrue(UriUtils.hasScpSyntax("host:22"));
        Assert.assertTrue(UriUtils.hasScpSyntax("host:22/path"));
        Assert.assertTrue(UriUtils.hasScpSyntax("host:/path"));
        Assert.assertTrue(UriUtils.hasScpSyntax("user@host:22/path"));
        Assert.assertTrue(UriUtils.hasScpSyntax("host:path"));
        Assert.assertTrue(UriUtils.hasScpSyntax("host:path/path"));
        Assert.assertTrue(UriUtils.hasScpSyntax("user@host:path"));
        Assert.assertTrue(UriUtils.hasScpSyntax("user:password@host/path"));
        Assert.assertTrue(UriUtils.hasScpSyntax("user:password@host:22/path"));
    }

}
