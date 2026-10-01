/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.quarkus.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.server.VaadinService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quarkus reads the Jandex index of each Flow module with the Jandex that the
 * Quarkus BOM manages, which is the Jandex on this test classpath. Jandex
 * rejects an index in a format newer than it knows, and then no Quarkus
 * application with Vaadin builds. So every Flow index has to be readable here,
 * in the format that the jandex.format.version build property sets.
 */
class FlowJandexIndexTest {

    @Test
    void read_flowIndexes_readableInConfiguredFormat() throws IOException {
        int configuredVersion = Integer
                .parseInt(System.getProperty("jandex.format.version"));

        Map<URL, Integer> flowIndexVersions = new LinkedHashMap<>();
        boolean flowServerIndexed = false;
        for (URL url : Collections.list(getClass().getClassLoader()
                .getResources("META-INF/jandex.idx"))) {
            try (InputStream in = url.openStream()) {
                IndexReader reader = new IndexReader(in);
                Index index = assertDoesNotThrow(reader::read,
                        "Quarkus cannot read the Jandex index " + url);
                if (index.getKnownClasses().stream().anyMatch(
                        c -> c.name().toString().startsWith("com.vaadin."))) {
                    flowIndexVersions.put(url, reader.getIndexVersion());
                    flowServerIndexed |= index.getClassByName(
                            DotName.createSimple(VaadinService.class)) != null;
                }
            }
        }

        assertTrue(flowServerIndexed,
                "No Jandex index of flow-server on the classpath, found "
                        + flowIndexVersions.keySet());
        flowIndexVersions.forEach((url, version) -> assertEquals(
                configuredVersion, version,
                "Index format of " + url + " is not jandex.format.version"));
    }
}
