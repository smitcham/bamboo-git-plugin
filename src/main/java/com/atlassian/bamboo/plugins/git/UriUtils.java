package com.atlassian.bamboo.plugins.git;

import org.eclipse.jgit.transport.URIish;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

public class UriUtils
{
    static final String SSH_SCHEME = "ssh";
    static final String SCHEME_DELIMITER = "://";

    private UriUtils()
    {
    }

    @Nullable
    public static String extractUsername(final String repositoryUrl) throws URISyntaxException
    {
        URIish uri = new URIish(repositoryUrl);

        final String auth = uri.getUser();
        if (auth == null)
        {
            return null;
        }
        return auth;
    }


    public static boolean isSsh(ScpAwareUri repositoryUri)
    {
        return repositoryUri.getScheme().equals(SSH_SCHEME);
    }

    public static boolean isSsh(final String repositoryUrl)
    {
        return repositoryUrl.startsWith(SSH_SCHEME + SCHEME_DELIMITER);
    }

    public static boolean hasScpSyntax(String s)
    {
        int scheme = s.indexOf(SCHEME_DELIMITER);
        if (scheme!=-1)
        {
            return false; //cannot use SCP syntax when a scheme is defined
        }

        int pathDefinitelyStartsHere = s.indexOf("/");
        if (pathDefinitelyStartsHere!=-1)
        {
            s = s.substring(0, pathDefinitelyStartsHere); //don't care about anything after the first /
        }

        return s.contains(":");
    }

    public static URI getUriViaProxy(GitRepository.GitRepositoryAccessData proxyAccessData, ScpAwareUri repositoryUri) throws URISyntaxException
    {
        return new URI(repositoryUri.getScheme(),
                proxyAccessData.proxyRegistrationInfo.getProxyUserName(),
                proxyAccessData.proxyRegistrationInfo.getProxyHost(),
                proxyAccessData.proxyRegistrationInfo.getProxyPort(),
                repositoryUri.getAbsolutePath(),
                repositoryUri.getRawQuery(),
                repositoryUri.getRawFragment());
    }
}
