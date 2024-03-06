/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.installer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default file downloader implementation.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since
 */
public final class DefaultFileDownloader implements FileDownloader {
    public static final String HTTPS_PROTOCOLS = "https.protocols";

    private final ProxyConfig proxyConfig;

    private String userName;
    private String password;

    /**
     * Construct file downloader with given proxy configuration.
     *
     * @param proxyConfig
     *            proxy configuration to use for file download
     */
    public DefaultFileDownloader(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public void download(URI downloadURI, File destination, String userName,
            String password) throws DownloadException {
        this.userName = userName;
        this.password = password;

        // force tls to 1.2 since github removed weak cryptographic standards
        // https://blog.github.com/2018-02-02-weak-cryptographic-standards-removal-notice/
        String oldProtocols = System.setProperty(HTTPS_PROTOCOLS, "TLSv1.2");
        try {
            if ("file".equalsIgnoreCase(downloadURI.getScheme())) {
                FileUtils.copyFile(new File(downloadURI), destination);
            } else {
                downloadFile(destination, downloadURI);
            }
        } catch (IOException e) {
            throw new DownloadException("Could not download " + downloadURI, e);
        } finally {
            // Return original protocol property
            if (oldProtocols == null) {
                System.clearProperty(HTTPS_PROTOCOLS);
            } else {
                System.setProperty(HTTPS_PROTOCOLS, oldProtocols);
            }
        }
    }

    private void downloadFile(File destination, URI downloadUri)
            throws IOException, DownloadException {
        CloseableHttpResponse response = execute(downloadUri);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw new DownloadException(
                    "Got error code " + statusCode + " from the server.");
        }
        new File(
                FilenameUtils.getFullPathNoEndSeparator(destination.toString()))
                .mkdirs();

        HttpEntity responseEntity = response.getEntity();
        long expected = responseEntity.getContentLength();
        try (ReadableByteChannel rbc = Channels
                .newChannel(responseEntity.getContent());
                FileOutputStream fos = new FileOutputStream(destination)) {
            long transferred = fos.getChannel().transferFrom(rbc, 0,
                    Long.MAX_VALUE);
            if (expected > 0 && transferred != expected) {
                // Download failed and channel.transferFrom does not rethrow the
                // exception
                throw new DownloadException(
                        "Error downloading from " + downloadUri + ". Expected "
                                + expected + " bytes but got " + transferred);
            }
        }

    }

    private CloseableHttpResponse execute(URI requestUri) throws IOException {
        CloseableHttpResponse response;
        ProxyConfig.Proxy proxy = proxyConfig
                .getProxyForUrl(requestUri.toString());
        if (proxy != null) {
            getLogger().info("Downloading via proxy {}", proxy.toString());
            return executeViaProxy(proxy, requestUri);
        } else {
            getLogger().info("No proxy was configured, downloading directly");
            if (userName != null && !userName.isEmpty() && password != null
                    && !password.isEmpty()) {
                getLogger().info("Using credentials ({})", userName);
                // Auth target host
                URL aURL = requestUri.toURL();
                HttpClientContext localContext = makeLocalContext(aURL);
                CredentialsProvider credentialsProvider = makeCredentialsProvider(
                        aURL.getHost(), aURL.getPort(), userName, password);
                response = buildHttpClient(credentialsProvider)
                        .execute(new HttpGet(requestUri), localContext);
            } else {
                response = buildHttpClient(null)
                        .execute(new HttpGet(requestUri));
            }
        }
        return response;
    }

    private CloseableHttpResponse executeViaProxy(ProxyConfig.Proxy proxy,
            URI requestUri) throws IOException {
        final CloseableHttpClient proxyClient;
        if (proxy.useAuthentication()) {
            proxyClient = buildHttpClient(makeCredentialsProvider(proxy.host,
                    proxy.port, proxy.username, proxy.password));
        } else {
            proxyClient = buildHttpClient(null);
        }

        final HttpHost proxyHttpHost = new HttpHost(proxy.host, proxy.port);

        final RequestConfig requestConfig = RequestConfig.custom()
                .setProxy(proxyHttpHost).build();

        final HttpGet request = new HttpGet(requestUri);
        request.setConfig(requestConfig);

        return proxyClient.execute(request);
    }

    private CloseableHttpClient buildHttpClient(
            CredentialsProvider credentialsProvider) {
        return HttpClients.custom().disableContentCompression()
                .useSystemProperties()
                .setDefaultCredentialsProvider(credentialsProvider).build();
    }

    private CredentialsProvider makeCredentialsProvider(String host, int port,
            String username, String password) {
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(host, port),
                new UsernamePasswordCredentials(username, password));
        return credentialsProvider;
    }

    private HttpClientContext makeLocalContext(URL requestUrl) {
        // Auth target host
        HttpHost target = new HttpHost(requestUrl.getHost(),
                requestUrl.getPort(), requestUrl.getProtocol());
        // Create AuthCache instance
        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(target, basicAuth);
        // Add AuthCache to the execution context
        HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
        return localContext;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger("FileDownloader");
    }
}
