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
package com.vaadin.server.webjar;

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
    private static final String PREFIX = "/bower_components/";
    public static final String PATTERN = PREFIX + '*';

    private final WebJarAssetLocator locator = new WebJarAssetLocator();
    private final Map<String, WebJarBowerDependency> bowerModuleToDependencyName = new HashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        locator.getWebJars().forEach((webJarName, version) -> {
            String bowerModuleName = getBowerModuleName(webJarName);
            if (bowerModuleName != null) {
                WebJarBowerDependency newDependency = new WebJarBowerDependency(
                        webJarName, version);
                bowerModuleToDependencyName.merge(bowerModuleName,
                        newDependency, this::mergeDependencies);
            }
        });
    }

    private WebJarBowerDependency mergeDependencies(
            WebJarBowerDependency oldDependency,
            WebJarBowerDependency newDependency) {
        int comparison = oldDependency.compareVersions(newDependency);
        if (comparison == 0) {
            Logger.getLogger(getClass().getName()).config(() -> String.format(
                    "Have found multiple webJars with name and version: '%s'",
                    oldDependency));
            return oldDependency;
        } else if (comparison > 0) {
            return oldDependency;
        } else {
            return newDependency;
        }
    }

    private String getBowerModuleName(String webJarName) {
        String bowerJsonPath = locator.getFullPathExact(webJarName,
                "bower.json");
        if (bowerJsonPath == null) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(bowerJsonPath),
                StandardCharsets.UTF_8))) {
            return Json.parse(reader.lines().collect(Collectors.joining()))
                    .getString("name");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to read resource located at: " + bowerJsonPath, e);
        }
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
        if (bowerModuleToDependencyName.isEmpty() || !path.startsWith(PREFIX)) {
            return null;
        }
        String pathWithoutPrefix = path.substring(PREFIX.length());

        int separatorIndex = pathWithoutPrefix.indexOf('/');
        if (separatorIndex < 0) {
            return null;
        }

        String bowerModuleName = pathWithoutPrefix.substring(0, separatorIndex);
        String webJarAndVersion = bowerModuleToDependencyName
                .get(bowerModuleName).toWebPath();

        if (webJarAndVersion == null) {
            return null;
        }

        String fileName = pathWithoutPrefix.substring(separatorIndex);
        return String.join("", "/webjars/", webJarAndVersion, fileName);
    }

    @Override
    public void destroy() {
        // A nested comment for Sonar: this method is empty because we don't
        // need to free any resources
    }
}
