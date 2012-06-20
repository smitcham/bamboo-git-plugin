package com.atlassian.bamboo.plugins.git;

import org.apache.commons.lang.StringUtils;

import java.net.URI;

public class ScpAwareUri
{
    private static final String SSH_PREFIX = UriUtils.SSH_SCHEME + UriUtils.SCHEME_DELIMITER;
    URI delegate;
    private final boolean relativePath;

    private ScpAwareUri(URI uri, boolean relativePath)
    {
        delegate = uri;
        this.relativePath = relativePath;
    }

    public static ScpAwareUri create(String url)
    {
        boolean isRelativePath = false;
        if (UriUtils.hasScpSyntax(url))
        {
            int slashIndex = url.indexOf("/");
            int colonIndex = url.indexOf(":");

            boolean noSlash = slashIndex == -1;
            boolean noSlashAfterColon = colonIndex < slashIndex && colonIndex+1!=slashIndex;
            isRelativePath = noSlash || noSlashAfterColon;
            url = SSH_PREFIX + (isRelativePath ? url.replace(":", "/") : url);
        }
        return new ScpAwareUri(URI.create(url), isRelativePath);
    }

    public String getRawPath()
    {
        if (relativePath)
        {
            return StringUtils.removeStart(delegate.getRawPath(), "/");
        }
        return delegate.getRawPath();
    }

    public String getScheme()
    {
        return delegate.getScheme();
    }


    public String getUserInfo()
    {
        return delegate.getUserInfo();
    }

    public int getPort()
    {
        return delegate.getPort();
    }

    public String getHost()
    {
        return delegate.getHost();
    }

    public String getRawQuery()
    {
        return delegate.getRawQuery();
    }

    public String getRawFragment()
    {
        return delegate.getRawFragment();
    }

    public boolean isRelativePath()
    {
        return relativePath;
    }

    public String getAbsolutePath()
    {
        return delegate.getRawPath(); //paths with scheme are always absolute
    }
}
