/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server.startup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.webjars.WebJarAssetLocator;

import elemental.json.Json;

/**
 * A filter that is used to redirect requests to inside the webjars. This allows
 * Flow users to add and use web components as Maven dependencies.
 */
public class WebJarFilter implements Filter {
    private static final char SEPARATOR = '/';
    private static final String PREFIX = "/bower_components/";
    static final String PATTERN = PREFIX + '*';

    private final WebJarAssetLocator locator = new WebJarAssetLocator();
    private final Map<String, String> nameToWebJarAndVersion = new HashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        locator.getWebJars().forEach((webJar, version) -> {
            String bowerName = getBowerName(webJar);
            if (bowerName != null) {
                String webJarAndVersion = webJar + SEPARATOR + version;
                String oldWebJarAndVersion = nameToWebJarAndVersion
                        .put(bowerName, webJarAndVersion);

                if (oldWebJarAndVersion != null) {
                    if (oldWebJarAndVersion.contains(version)) {
                        Logger.getLogger(getClass().getName())
                                .config(() -> String.format(
                                        "Have found multiple webjars with name '%s' and version '%s' for module '%s', using the one that was found last",
                                        webJar, version, bowerName));
                    } else {
                        throw new IllegalStateException(String.format(
                                "Module `%s` has two webjars in the project with different versions: `%s` and `%s`. It is unclear which to use.",
                                bowerName, oldWebJarAndVersion,
                                webJarAndVersion));
                    }
                }
            }
        });
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        ServletRequest result = request;
        String webJarPath = getWebJarPath(servletRequest.getPathInfo());
        if (webJarPath != null) {
            // Use a request with a pathInfo that points to the webjar
            result = new HttpServletRequestWrapper(servletRequest) {
                @Override
                public String getPathInfo() {
                    return webJarPath;
                }
            };
        }
        chain.doFilter(result, response);
    }

    private String getWebJarPath(String path) {
        if (!path.startsWith(PREFIX)) {
            return null;
        }
        String pathWithoutPrefix = path.substring(PREFIX.length());

        int separatorIndex = pathWithoutPrefix.indexOf(SEPARATOR);
        if (separatorIndex < 0) {
            return null;
        }

        String bowerModule = pathWithoutPrefix.substring(0, separatorIndex);
        String webJarAndVersion = nameToWebJarAndVersion.get(bowerModule);

        if (webJarAndVersion == null) {
            return null;
        }

        String fileName = pathWithoutPrefix.substring(separatorIndex);
        return String.join("", "/webjars/", webJarAndVersion, fileName);
    }

    private String getBowerName(String webJar) {
        String bowerJsonPath = locator.getFullPathExact(webJar, "bower.json");
        if (bowerJsonPath == null) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(bowerJsonPath),
                StandardCharsets.UTF_8))) {
            // Ignoring linebreaks
            String jsonString = reader.lines().collect(Collectors.joining());
            return Json.parse(jsonString).getString("name");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read resource located at: " + bowerJsonPath, e);
        }
    }

    @Override
    public void destroy() {
        // A nested comment for Sonar: this method is empty because we don't
        // need to free any resources
    }
}
