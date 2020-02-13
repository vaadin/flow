/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.frontend.installer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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
 *
 * @since
 */
public final class DefaultFileDownloader implements FileDownloader {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(FileDownloader.class);
    public static final String HTTPS_PROTOCOLS = "https.protocols";

    private final ProxyConfig proxyConfig;

    private String userName;
    private String password;

    /**
     * Construct file downloader with given proxy configuration.
     * @param proxyConfig proxy configuration to use for file download
     */
    public DefaultFileDownloader(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public void download(String downloadUrl, File destination, String userName,
            String password) throws DownloadException {
        this.userName = userName;
        this.password = password;

        String fixedDownloadUrl = downloadUrl;
        String httpsProtocols = System.setProperty(HTTPS_PROTOCOLS, "TLSv1.2");
        try {
            // force tls to 1.2 since github removed weak cryptographic standards
            // https://blog.github.com/2018-02-02-weak-cryptographic-standards-removal-notice/
            fixedDownloadUrl = FilenameUtils.separatorsToUnix(fixedDownloadUrl);
            URI downloadURI = new URI(fixedDownloadUrl);
            if ("file".equalsIgnoreCase(downloadURI.getScheme())) {
                FileUtils.copyFile(new File(downloadURI), destination);
            } else {
                CloseableHttpResponse response = execute(fixedDownloadUrl);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new DownloadException("Got error code " + statusCode
                            + " from the server.");
                }
                new File(FilenameUtils.getFullPathNoEndSeparator(destination.toString())).mkdirs();
                ReadableByteChannel rbc = Channels
                        .newChannel(response.getEntity().getContent());
                FileOutputStream fos = new FileOutputStream(destination);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                fos.close();
            }
        } catch (IOException | URISyntaxException e) {
            throw new DownloadException(
                    "Could not download " + fixedDownloadUrl, e);
        }finally {
            if(httpsProtocols == null)  {
                System.clearProperty(HTTPS_PROTOCOLS);
            } else {
                System.setProperty(HTTPS_PROTOCOLS, httpsProtocols);
            }
        }
    }

    private CloseableHttpResponse execute(String requestUrl)
            throws IOException {
        CloseableHttpResponse response;
        ProxyConfig.Proxy proxy = proxyConfig.getProxyForUrl(requestUrl);
        if (proxy != null) {
            LOGGER.info("Downloading via proxy " + proxy.toString());
            return executeViaProxy(proxy, requestUrl);
        } else {
            LOGGER.info("No proxy was configured, downloading directly");
            if (StringUtils.isNotEmpty(userName) && StringUtils
                    .isNotEmpty(password)) {
                LOGGER.info("Using credentials (" + userName
                        + ") from settings.xml");
                // Auth target host
                URL aURL = new URL(requestUrl);
                HttpClientContext localContext = makeLocalContext(aURL);
                CredentialsProvider credentialsProvider = makeCredentialsProvider(
                        aURL.getHost(), aURL.getPort(), userName, password);
                response = buildHttpClient(credentialsProvider)
                        .execute(new HttpGet(requestUrl), localContext);
            } else {
                response = buildHttpClient(null)
                        .execute(new HttpGet(requestUrl));
            }
        }
        return response;
    }

    private CloseableHttpResponse executeViaProxy(ProxyConfig.Proxy proxy,
            String requestUrl) throws IOException {
        final CloseableHttpClient proxyClient;
        if (proxy.useAuthentication()) {
            proxyClient = buildHttpClient(
                    makeCredentialsProvider(proxy.host, proxy.port,
                            proxy.username, proxy.password));
        } else {
            proxyClient = buildHttpClient(null);
        }

        final HttpHost proxyHttpHost = new HttpHost(proxy.host, proxy.port);

        final RequestConfig requestConfig = RequestConfig.custom()
                .setProxy(proxyHttpHost).build();

        final HttpGet request = new HttpGet(requestUrl);
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
}
