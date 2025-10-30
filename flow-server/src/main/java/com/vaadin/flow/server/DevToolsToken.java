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
package com.vaadin.flow.server;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import org.slf4j.LoggerFactory;

/**
 * Representation of the security token exchanged with the Dev Tools client to
 * validate websocket connections.
 *
 * The token is temporarily stored locally to prevent websocket connection to be
 * refused after a server restart.
 *
 * For internal yse only.
 */
public class DevToolsToken implements Serializable {

    /**
     * Random token to verify dev-tools websocket connections.
     */
    private static String randomDevToolsToken = UUID.randomUUID().toString();

    /**
     * Initialize the dev-tools token, potentially loading a value generated
     * previously, before a server shutdown.
     *
     * @param vaadinService
     *            Vaadin service instance
     */
    static synchronized void init(VaadinService vaadinService) {
        File projectFolder = vaadinService.getDeploymentConfiguration()
                .getProjectFolder();
        if (projectFolder != null) {
            String uniqueUid = UUID
                    .nameUUIDFromBytes((projectFolder.getAbsolutePath())
                            .getBytes(StandardCharsets.UTF_8))
                    .toString();
            File tokenFile = new File(System.getProperty("java.io.tmpdir"),
                    uniqueUid);
            if (tokenFile.exists()) {
                try {
                    randomDevToolsToken = UUID
                            .fromString(Files.readString(tokenFile.toPath()))
                            .toString();
                } catch (Exception e) {
                    LoggerFactory.getLogger(DevToolsToken.class).debug(
                            "Cannot read dev-tools token file, using a random new token. "
                                    + "Browser page might need a reload to make dev-tools websocket establish a connection.",
                            e);
                }
            } else {
                try {
                    Files.writeString(tokenFile.toPath(), randomDevToolsToken);
                } catch (IOException e) {
                    LoggerFactory.getLogger(DevToolsToken.class).debug(
                            "Cannot write dev-tools token file. A new token will be generated on server restart.",
                            e);
                }
            }
        }
    }

    /**
     * Gets the token exchanged between Dev Tools client and the server.
     *
     * @return dev-tools token, never {@literal null}.
     */
    public static String getToken() {
        return randomDevToolsToken;
    }

}
