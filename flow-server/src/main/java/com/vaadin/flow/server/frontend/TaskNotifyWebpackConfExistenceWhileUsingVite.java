/*
 * Copyright 2000-2022 Vaadin Ltd.
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

package com.vaadin.flow.server.frontend;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;

/**
 * Notifies the user about the existence of webpack.config.js while the project
 * is running Vite as the frontend build tool. This can be helpful especially
 * when migrating to 23.2 and later since it prevent any confusion or any
 * accidential misconfiguation in webpack related config files while using vite.
 * <p>
 * This task is only added to the list of executables for the application
 * startup if Vite is the active frontend build tool.
 */
public class TaskNotifyWebpackConfExistenceWhileUsingVite
        implements FallibleCommand, Serializable {

    private final File configFolder;

    TaskNotifyWebpackConfExistenceWhileUsingVite(File configFolder) {
        this.configFolder = configFolder;
    }

    @Override
    public void execute() {
        try {
            validate();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void validate() throws IOException {
        Path webpackConfigFile = Paths.get(configFolder.getPath(),
                WEBPACK_CONFIG);
        if (!Files.exists(webpackConfigFile)) {
            return;
        }
        //@formatter:off
        throw new IllegalStateException("\n"
              + "**************************************************************************\n"
              + "*  webpack related config file 'webpack.config.js' is detected in your   *\n"
              + "*  project while Vite is the default frontend build tool as of V23.2.0.  *\n"
              + "*  This is to announce that any existing webpack custom configuration    *\n"
              + "*  may not work out of the box in Vite.                                  *\n"
              + "*  If you don't have any custom webpack configuration, you can just      *\n"
              + "*  delete/move the 'webpack.config.js' and restart the application.      *\n"
              + "*  If you are migrating from an earlier version and you have custom      *\n"
              + "*  configurations in your 'webpack.config.js' file, you should either    *\n"
              + "*  consider migrating your custom configurations to a Vite alternative   *\n"
              + "*  solution and remove 'webpack.config.js' before running the            *\n"
              + "*  application again, or just reactivate webpack via setting the         *\n"
              + "*  'com.vaadin.experimental.webpackForFrontendBuild=true' feature flag   *\n"
              + "*  in [project-root]/src/main/resources/vaadin-featureflags.properties   *\n"
              + "*  (you can create the file if not exists) and restart the application.  *\n"
              + "*  Using webpack with Vaadin applications is deprecated and the support  *\n"
              + "*  for reactivating it would be removed in the next major release.       *\n"
              + "**************************************************************************\n");
        //@formatter:on
    }
}
