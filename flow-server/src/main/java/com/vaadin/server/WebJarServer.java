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

package com.vaadin.server;

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

import com.vaadin.util.ResponseWriter;

import elemental.json.Json;

/**
 * Handles requests that may require webjars contents. In this case, writes the
 * required resource contents from a webjar into the response.
 *
 * Does not applicable to the production mode if user have not enforced the
 * webjars to be enabled with {@link Constants#DISABLE_WEBJARS} param.
 *
 * @author Vaadin Ltd.
 */
class WebJarServer implements Serializable {
    private static final char SEPARATOR = '/';
    private static final String PREFIX = "/bower_components/";

    private final WebJarAssetLocator locator = new WebJarAssetLocator();
    private final ResponseWriter responseWriter = new ResponseWriter();
    private final Map<String, String> nameToWebJarAndVersion = new HashMap<>();
    private final boolean webJarsDisabled;

    WebJarServer(VaadinService service) {
        webJarsDisabled = areWebJarsDisabled(service);

        if (!webJarsDisabled) {
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
    }

    private boolean areWebJarsDisabled(VaadinService service) {
        String userConfiguredProperty = service.getDeploymentConfiguration()
                .getStringProperty(Constants.DISABLE_WEBJARS, null);
        if (userConfiguredProperty != null) {
            return Boolean.parseBoolean(userConfiguredProperty);
        } else {
            return service.getDeploymentConfiguration().isProductionMode();
        }
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

    /**
     * Searches for file requested in the webjars. If found, the file contents
     * is written into request.
     *
     * @param request
     *            the servlet request
     * @param response
     *            the servlet response
     * @return {@code true} if response was populated with webjar contents,
     *         {@code false} otherwise
     * @throws IOException
     *             if response population fails
     */
    boolean tryServeWebJarResource(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        if (webJarsDisabled) {
            return false;
        }

        String webJarPath = getWebJarPath(request.getPathInfo());
        if (webJarPath == null) {
            return false;
        }

        URL resourceUrl = request.getServletContext().getResource(webJarPath);
        responseWriter.writeResponseContents(webJarPath, resourceUrl, request,
                response);
        return true;
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
}
