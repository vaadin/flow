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
package com.vaadin.flow.component.dependency;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.hotswap.HotswapResourceEvent;
import com.vaadin.flow.hotswap.VaadinHotswapper;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Handles the automatic hotswapping of CSS resources in a Vaadin-based
 * application. This class listens for changes in application resources and
 * takes appropriate actions to update stylesheets in the browser.
 * <p>
 * When a resource with {@code .css} extension is changed, this class determines
 * if the related file exists in a known public folder (`META-INF/resource`,
 * `resource`, `static`, `public`) in the output resources folder (e.g.
 * `target/classes` for Maven) and triggers a resource update by the Vaadin Dev
 * Server.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.0
 */
public class StyleSheetHotswapper implements VaadinHotswapper {

    public static final Logger LOGGER = LoggerFactory
            .getLogger(StyleSheetHotswapper.class);

    @Override
    public void onResourcesChange(HotswapResourceEvent event) {
        if (event.anyMatches(".*\\.css")) {
            LOGGER.debug(
                    "Triggering browser live reload because of CSS resources changes");

            VaadinService vaadinService = event.getVaadinService();
            File buildResourcesFolder = vaadinService
                    .getDeploymentConfiguration().getOutputResourceFolder();

            List<String> publicStaticResourcesPaths = Stream
                    .of("META-INF/resources", "resources", "static", "public")
                    .map(path -> new File(buildResourcesFolder, path))
                    .filter(File::exists)
                    .map(staticResourceFolder -> FrontendUtils
                            .getUnixPath(staticResourceFolder.toPath()))
                    .toList();

            event.getChangedResources().stream()
                    .filter(uri -> !new File(uri.getPath()).isDirectory())
                    .forEach(resource -> {
                        String resourcePath = resource.getPath();
                        for (String staticResourcesPath : publicStaticResourcesPaths) {
                            if (resourcePath.startsWith(staticResourcesPath)) {
                                String path = resourcePath
                                        .replace(staticResourcesPath, "");
                                if (path.startsWith("/")) {
                                    path = path.substring(1);
                                }
                                event.updateClientResource(
                                        ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
                                                + path,
                                        null);
                            }
                        }
                    });
        }
    }
}
