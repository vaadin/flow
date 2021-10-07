/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.server.Version;

import org.slf4j.LoggerFactory;

/**
 * Data for a info message to the debug window.
 */
public class ServerInfo {

    private String flowVersion;
    private String vaadinVersion = "unknown";

    /**
     * Creates a new instance.
     */
    public ServerInfo() {
        this.flowVersion = Version.getFullVersion();
        try (InputStream vaadinVersionsStream = getClass().getClassLoader()
                .getResourceAsStream("vaadin_versions.json")) {
            if (vaadinVersionsStream != null) {
                ObjectMapper m = new ObjectMapper();
                JsonNode vaadinVersions = m.readTree(vaadinVersionsStream);
                this.vaadinVersion = vaadinVersions.get("platform").asText();
            } else {
                LoggerFactory.getLogger(getClass()).info(
                        "Unable to determine version information. No vaadin_versions.json found");
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(getClass())
                    .error("Unable to determine version information", e);
        }
    }

    public String getFlowVersion() {
        return flowVersion;
    }

    public String getVaadinVersion() {
        return vaadinVersion;
    }
}
