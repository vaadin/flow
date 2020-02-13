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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.StringTokenizer;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfig.class);

    private final List<Proxy> proxies;

    /**
     * Create a new proxy configuration with given proxies.
     * @param proxies list of available proxies
     */
    public ProxyConfig(List<Proxy> proxies) {
        this.proxies = proxies;
    }

    /**
     * Check if no proxies have been defined.
     * @return true if we have no proxies
     */
    public boolean isEmpty() {
        return proxies.isEmpty();
    }

    /**
     * Get a proxy for url.
     * @param requestUrl url to get proxy for
     * @return Proxy if one found, else null.
     */
    public Proxy getProxyForUrl(String requestUrl) {
        if (proxies.isEmpty()) {
            LOGGER.info("No proxies configured");
            return null;
        }
        final URI uri = URI.create(requestUrl);
        for (Proxy proxy : proxies) {
            if (!proxy.isNonProxyHost(uri.getHost())) {
                return proxy;
            }
        }
        LOGGER.info("Could not find matching proxy for host: {}", uri.getHost());
        return null;
    }

    /**
     * Get a defined secure proxy.
     * @return first secure proxy from the proxy list, or null if no secure proxies.
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
     * @return first proxy that is not secure from the proxy list, or null if no secure proxies.
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
    public static class Proxy {
        public final String id;
        public final String protocol;
        public final String host;
        public final int port;
        public final String username;
        public final String password;
        public final String nonProxyHosts;

        public Proxy(
                String id, String protocol, String host, int port, String username, String password, String nonProxyHosts) {
            this.host = host;
            this.id = id;
            this.protocol = protocol;
            this.port = port;
            this.username = username;
            this.password = password;
            this.nonProxyHosts = nonProxyHosts;
        }

        public boolean useAuthentication(){
            return username != null && !username.isEmpty();
        }

        public URI getUri() {
            String authentication = useAuthentication() ? username + ":" + password : null;
            try {
                // Proxies should be schemed with http, even if the protocol is https
                return new URI("http", authentication, host, port, null, null, null);
            } catch (URISyntaxException e) {
                throw new ProxyConfigException("Invalid proxy settings", e);
            }
        }

        public boolean isSecure(){
            return "https".equals(protocol);
        }

        public boolean isNonProxyHost(String host) {
            if (host != null && nonProxyHosts != null && nonProxyHosts.length() > 0) {
                for (StringTokenizer tokenizer = new StringTokenizer(nonProxyHosts, "|"); tokenizer.hasMoreTokens(); ) {
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
            return id + "{" +
                    "protocol='" + protocol + '\'' +
                    ", host='" + host + '\'' +
                    ", port=" + port +
                    (useAuthentication()? ", with username/passport authentication" : "") +
                    '}';
        }
    }

    static class ProxyConfigException extends RuntimeException {

        private ProxyConfigException(String message, Exception cause) {
            super(message, cause);
        }

    }
}
