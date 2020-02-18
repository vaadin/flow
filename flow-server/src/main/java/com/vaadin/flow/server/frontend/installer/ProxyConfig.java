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

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy configuration used for downloads and executions.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 *
 * @since
 */
public class ProxyConfig {

    private static final Pattern PROXY_URL_REGEX = Pattern
            .compile("(\\w+)://(([^:]+):(.*)@)?([^:]+)(:(\\d*))?");

    private final List<Proxy> proxies;

    /**
     * Create a new proxy configuration with given proxies.
     *
     * @param proxies
     *            list of available proxies
     */
    public ProxyConfig(List<Proxy> proxies) {
        this.proxies = proxies;
    }

    /**
     * Check if no proxies have been defined.
     *
     * @return true if we have no proxies
     */
    public boolean isEmpty() {
        return proxies.isEmpty();
    }

    /**
     * Get a proxy for url.
     *
     * @param requestUrl
     *            url to get proxy for
     * @return Proxy if one found, else null.
     */
    public Proxy getProxyForUrl(String requestUrl) {
        if (proxies.isEmpty()) {
            getLogger().info("No proxies configured");
            return null;
        }
        final URI uri = URI.create(requestUrl);
        for (Proxy proxy : proxies) {
            if (!proxy.isNonProxyHost(uri.getHost())) {
                return proxy;
            }
        }
        getLogger().info("Could not find matching proxy for host: {}",
                uri.getHost());
        return null;
    }

    /**
     * Get a defined secure proxy.
     *
     * @return first secure proxy from the proxy list, or null if no secure
     *         proxies.
     */
    public Proxy getSecureProxy() {
        for (Proxy proxy : proxies) {
            if (proxy.isSecure()) {
                return proxy;
            }
        }
        return null;
    }

    /**
     * Get first proxy that is not secure.
     *
     * @return first proxy that is not secure from the proxy list, or null if no
     *         secure proxies.
     */
    public Proxy getInsecureProxy() {
        for (Proxy proxy : proxies) {
            if (!proxy.isSecure()) {
                return proxy;
            }
        }
        return null;
    }

    /**
     * Class for holding proxy information.
     */
    public static class Proxy implements Serializable {
        /**
         * Id of proxy.
         */
        public final String id;
        /**
         * Protocol used for proxy.
         */
        public final String protocol;
        /**
         * Proxy host.
         */
        public final String host;
        /**
         * Proxy port.
         */
        public final int port;
        /**
         * User name for proxy.
         */
        public final String username;
        /**
         * Password for proxy.
         */
        public final String password;
        /**
         * Excluded hosts string delimited by '|'.
         */
        public final String nonProxyHosts;

        /**
         * Construct a Proxy object.
         *
         * @param id
         *            proxy id
         * @param protocol
         *            proxy protocol
         * @param host
         *            proxy host
         * @param port
         *            proxy port
         * @param username
         *            user name for proxy
         * @param password
         *            password for proxy
         * @param nonProxyHosts
         *            excluded hosts string
         */
        public Proxy(String id, String protocol, String host, int port,
                String username, String password, String nonProxyHosts) {
            this.host = host;
            this.id = id;
            this.protocol = protocol;
            this.port = port;
            this.username = username;
            this.password = password;
            this.nonProxyHosts = nonProxyHosts;
        }

        /**
         * Construct a Proxy object out of a proxy url
         * 
         * @param id
         *            proxy id
         * @param proxyUrl
         *            proxy url with the format of
         *            protocol://user:password@server:port
         */
        public Proxy(String id, String proxyUrl, String nonProxyHosts) {
            final Matcher matcher = PROXY_URL_REGEX.matcher(proxyUrl);
            if (!matcher.matches())
                throw new IllegalArgumentException(
                        "Provided proxyUrl does not match the format protocol://user:password@server:port");

            this.id = id;
            protocol = matcher.group(1);
            username = matcher.group(3);
            password = matcher.group(4);
            host = matcher.group(5);
            port = Integer.parseInt(matcher.group(7));
            this.nonProxyHosts = nonProxyHosts;
        }

        /**
         * Check if proxy uses authentication.
         *
         * @return true if we have a non empty username
         */
        public boolean useAuthentication() {
            return username != null && !username.isEmpty();
        }

        /**
         * Get the proxy uri.
         *
         * @return URI for this proxy
         */
        public URI getUri() {
            String authentication = useAuthentication()
                    ? username + ":" + password
                    : null;
            try {
                // Proxies should be schemed with http, even if the protocol is
                // https
                return new URI("http", authentication, host, port, null, null,
                        null);
            } catch (URISyntaxException e) {
                throw new ProxyConfigException("Invalid proxy settings", e);
            }
        }

        /**
         * Check if the proxy is secure.
         *
         * @return true is protocol is https
         */
        public boolean isSecure() {
            return "https".equals(protocol);
        }

        /**
         * Check if given host is excluded for proxy.
         *
         * @param host
         *            host to check
         * @return true if host matches a nonProxyHosts pattern
         */
        public boolean isNonProxyHost(String host) {
            if (host != null && nonProxyHosts != null
                    && nonProxyHosts.length() > 0) {
                for (StringTokenizer tokenizer = new StringTokenizer(
                        nonProxyHosts, "|"); tokenizer.hasMoreTokens();) {
                    String pattern = tokenizer.nextToken();
                    pattern = pattern.replace(".", "\\.").replace("*", ".*");
                    if (host.matches(pattern)) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public String toString() {
            return id + "{" + "protocol='" + protocol + '\'' + ", host='" + host
                    + '\'' + ", port=" + port
                    + (useAuthentication()
                            ? ", with username/passport authentication"
                            : "")
                    + '}';
        }
    }

    /**
     * Exception thrown fora proxy configuration exception.
     */
    private static class ProxyConfigException extends RuntimeException {

        /**
         * Create exception with message and cause.
         *
         * @param message
         *            exception message
         * @param cause
         *            exception cause
         */
        private ProxyConfigException(String message, Exception cause) {
            super(message, cause);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger("ProxyConfig");
    }
}
