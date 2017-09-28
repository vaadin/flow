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
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.webjars.WebJarAssetLocator;

import com.vaadin.server.Constants;
import com.vaadin.util.ResponseWriter;

import elemental.json.Json;

/**
 * Handles requests that may require webJars contents. In this case, writes the
 * required resource contents from a webJar into the response.
 * <p>
 * Is not applicable to the production mode if user have not enforced the
 * webJars to be enabled with {@link Constants#DISABLE_WEBJARS} param.
 *
 * @author Vaadin Ltd.
 */
public class WebJarServer implements Serializable {
    private static final String PREFIX = "/bower_components/";

    private final transient WebJarAssetLocator locator = new WebJarAssetLocator();
    private final transient Map<String, WebJarBowerDependency> bowerModuleToDependencyName = new HashMap<>();
    private final ResponseWriter responseWriter = new ResponseWriter();

    /**
     * Creates a webJar server that is able to search webJars for files and
     * return them.
     */
    public WebJarServer() {
        locator.getWebJars().forEach((webJarName, version) -> {
            String bowerModuleName = getBowerModuleName(webJarName);
            if (bowerModuleName != null) {
                WebJarBowerDependency newDependency = new WebJarBowerDependency(
                        bowerModuleName, webJarName, version);
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

    /**
     * Searches for file requested in the webJars. If found, the file contents
     * is written into request.
     *
     * @param request
     *            the servlet request
     * @param response
     *            the servlet response
     * @return {@code true} if response was populated with webJar contents,
     *         {@code false} otherwise
     * @throws IOException
     *             if response population fails
     */
    public boolean tryServeWebJarResource(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String webJarPath = getWebJarPath(request.getPathInfo());
        if (webJarPath == null) {
            return false;
        }

        URL resourceUrl = request.getServletContext().getResource(webJarPath);
        if (resourceUrl == null) {
            return false;
        }

        responseWriter.writeResponseContents(webJarPath, resourceUrl, request,
                response);
        return true;
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
        WebJarBowerDependency dependency = bowerModuleToDependencyName
                .get(bowerModuleName);
        if (dependency == null) {
            return null;
        }

        return String.join("", "/webjars/", dependency.toWebPath(),
                pathWithoutPrefix.substring(separatorIndex));
    }
}
