/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.io.InputStream;
import java.io.OutputStream;
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
import java.net.http.HttpResponse.BodyHandler;
import java.nio.file.Files;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default file downloader implementation.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public final class DefaultFileDownloader implements FileDownloader {

    private static final int DEFAULT_BUFFER_SIZE = 8192;
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
            String password, ProgressListener progressListener)
            throws DownloadException {
        this.userName = userName;
        this.password = password;

        // force tls to 1.2 since github removed weak cryptographic standards
        // https://blog.github.com/2018-02-02-weak-cryptographic-standards-removal-notice/
        String oldProtocols = System.setProperty(HTTPS_PROTOCOLS, "TLSv1.2");
        try {
            if ("file".equalsIgnoreCase(downloadURI.getScheme())) {
                Files.copy(new File(downloadURI).toPath(),
                        destination.toPath());
            } else {
                downloadFile(destination, downloadURI, progressListener);
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

    private void downloadFile(File destination, URI downloadUri,
            ProgressListener progressListener)
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
            BodyHandler<InputStream> bodyHandler = HttpResponse.BodyHandlers
                    .ofInputStream();

            HttpResponse<InputStream> response = client.send(request,
                    bodyHandler);
            if (response.statusCode() != 200) {
                throw new DownloadException("Got error code "
                        + response.statusCode() + " from the server.");
            }
            long contentLength = response.headers()
                    .firstValueAsLong("Content-Length").orElse(-1L);

            try (FileOutputStream out = openOutputStream(destination)) {
                copy(response.body(), out, contentLength, progressListener);
            }

            if (contentLength != -1 && destination.length() != contentLength) {
                throw new DownloadException("Error downloading from "
                        + downloadUri + ". Expected " + contentLength
                        + " bytes but got " + destination.length());
            }

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }

    }

    private static FileOutputStream openOutputStream(final File file)
            throws IOException {
        file.getParentFile().mkdirs();
        return new FileOutputStream(file, false);
    }

    /**
     * From {@link IOUtils#copyLarge(InputStream, OutputStream, byte[])} and
     * {@link IOUtils#copyLarge(InputStream, OutputStream)}
     *
     * @param inputStream
     *            the input stream
     * @param outputStream
     *            the output stream
     * @param total
     *            the total number of bytes to copy or -1 if unknown
     * @param progressListener
     *            the progress listener or null
     * @return the number of bytes copied
     * @throws IOException
     */
    long copy(InputStream inputStream, OutputStream outputStream, long total,
            ProgressListener progressListener) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream");
        Objects.requireNonNull(outputStream, "outputStream");
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        long count = 0;
        int n;
        double lastReportedProgress = 0.0;
        while (-1 != (n = inputStream.read(buffer))) {
            outputStream.write(buffer, 0, n);
            count += n;

            lastReportedProgress = reportProgress(progressListener, total,
                    count, lastReportedProgress);
        }
        if (lastReportedProgress != 1.0 && lastReportedProgress != total) {
            lastReportedProgress = reportProgress(progressListener, total,
                    count, 0.0);
        }
        return count;
    }

    private double reportProgress(ProgressListener progressListener, long total,
            long count, double lastReportedProgress) {
        // Progress reporting
        if (progressListener == null) {
            return lastReportedProgress;
        }

        if (total == -1) {
            // We don't know the total size, so send an event every 1MB
            if (count >= (lastReportedProgress + 1024 * 1024)) {
                progressListener.onProgress(count, total, -1);
                return count;
            }
        } else {
            // We know the total size, so send and event every 1%
            double progress = (double) count / total;
            if ((progress - lastReportedProgress) > 0.01) {
                progressListener.onProgress(count, total, progress);
                return progress;
            }
        }
        return lastReportedProgress;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger("FileDownloader");
    }
}
