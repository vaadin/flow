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
package com.vaadin.quarkus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceProviderTest {

    @Test
    public void quarkusResourceProvider_returnsExpectedResources()
            throws IOException {
        QuarkusResourceProvider resourceProvider = new QuarkusResourceProvider();
        // ======== resourceProvider.getApplicationResource(s)(String)
        URL applicationResource = resourceProvider
                .getApplicationResource("resource-provider/some-resource.json");

        assertNotNull(applicationResource);

        List<URL> resources = resourceProvider.getApplicationResources(
                "resource-provider/some-resource.json");

        assertEquals(1, resources.size());

        assertNotNull(resources.get(0));

        URL nonExistent = resourceProvider
                .getApplicationResource("resource-provider/non-existent.txt");

        assertNull(nonExistent);

        // =========== resourceProvider.getClientResource

        URL clientResource = resourceProvider
                .getClientResource("resource-provider/some-resource.json");

        assertNotNull(clientResource);

        InputStream stream = resourceProvider.getClientResourceAsStream(
                "resource-provider/some-resource.json");

        String content = IOUtils.readLines(stream, StandardCharsets.UTF_8)
                .stream().collect(Collectors.joining("\n"));
        ObjectNode object = JacksonUtils.readTree(content);
        assertTrue(object.get("client-resource").asBoolean());
    }
}
