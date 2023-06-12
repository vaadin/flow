/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.commons.io.FileUtils;
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

        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .version(Version.HTTP_1_1).followRedirects(Redirect.NORMAL);

        ProxyConfig.Proxy proxy = proxyConfig
                .getProxyForUrl(downloadUri.toString());

        if (proxy != null) {
            getLogger().debug("Downloading via proxy {}", proxy.toString());
            clientBuilder = clientBuilder.proxy(ProxySelector
                    .of(new InetSocketAddress(proxy.host, proxy.port)));
            clientBuilder = clientBuilder.authenticator(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    if (getRequestorType() == RequestorType.PROXY) {
                        return new PasswordAuthentication(proxy.username,
                                proxy.password.toCharArray());
                    }
                    return new PasswordAuthentication(userName,
                            password.toCharArray());
                }
            });
        } else {
            getLogger().debug("No proxy was configured, downloading directly");
            if (userName != null && !userName.isEmpty() && password != null
                    && !password.isEmpty()) {
                getLogger().info("Using credentials ({})", userName);
                clientBuilder = clientBuilder
                        .authenticator(new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(userName,
                                        password.toCharArray());
                            }
                        });

            }
        }

        HttpClient client = clientBuilder.build();
        HttpRequest request = HttpRequest.newBuilder().uri(downloadUri).GET()
                .build();

        try {
            HttpResponse<Path> response = client.send(request,
                    BodyHandlers.ofFile(destination.toPath()));
            if (response.statusCode() != 200) {
                throw new DownloadException("Got error code "
                        + response.statusCode() + " from the server.");
            }
            long expected = response.headers()
                    .firstValueAsLong("Content-Length").getAsLong();
            if (destination.length() != expected) {
                throw new DownloadException("Error downloading from "
                        + downloadUri + ". Expected " + expected
                        + " bytes but got " + destination.length());
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }

    }

    private Logger getLogger() {
        return LoggerFactory.getLogger("FileDownloader");
    }
}
