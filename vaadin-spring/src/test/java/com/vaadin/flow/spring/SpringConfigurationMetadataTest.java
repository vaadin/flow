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
package com.vaadin.flow.spring;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.vaadin.flow.server.InitParameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Checks that the Spring Boot configuration metadata, which IDEs use to
 * autocomplete properties in {@code application.properties}, matches the Vaadin
 * parameters that are read from the Spring environment.
 */
class SpringConfigurationMetadataTest {

    // Parameters that are not read from the Spring environment at runtime, so
    // they must not be suggested. See
    // https://github.com/vaadin/flow/issues/26161
    private static final Set<String> NOT_RUNTIME_PARAMETERS = Set.of(
            InitParameters.UI_PARAMETER, InitParameters.CI_BUILD,
            InitParameters.SKIP_DEV_BUNDLE_REBUILD,
            InitParameters.FORCE_PRODUCTION_BUILD,
            InitParameters.COMPRESS_BUNDLE,
            InitParameters.COMMERCIAL_WITH_BANNER,
            InitParameters.CLEAN_BUILD_FRONTEND_FILES,
            InitParameters.SERVLET_PARAMETER_JSBUNDLE,
            InitParameters.SERVLET_PARAMETER_STATISTICS_JSON);

    @Test
    void additionalMetadata_eachPropertyIsReadAsOneRuntimeParameter()
            throws IOException {
        for (String property : readPropertyNames(
                "META-INF/additional-spring-configuration-metadata.json")) {
            Set<String> parameters = findParametersReading(property);
            assertEquals(1, parameters.size(), "'" + property
                    + "' must be read as exactly one Vaadin parameter, but is read as "
                    + parameters);
            String parameter = parameters.iterator().next();
            assertFalse(NOT_RUNTIME_PARAMETERS.contains(parameter),
                    "'" + property + "' is read as '" + parameter
                            + "', which is not a runtime parameter");
        }
    }

    @Test
    void metadata_documentsEveryRuntimeParameter() throws IOException {
        Set<String> documented = new HashSet<>();
        for (String property : readPropertyNames(
                "META-INF/spring-configuration-metadata.json")) {
            documented.addAll(findParametersReading(property));
        }

        Set<String> undocumented = new TreeSet<>(SpringServlet.PROPERTY_NAMES);
        undocumented.removeAll(NOT_RUNTIME_PARAMETERS);
        undocumented.removeAll(documented);
        assertEquals(Set.of(), undocumented,
                "Add the parameters to additional-spring-configuration-metadata.json");
    }

    private static Set<String> findParametersReading(String property) {
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(
                new MapPropertySource("test", Map.of(property, "value")));
        Set<String> parameters = new TreeSet<>();
        for (String parameter : SpringServlet.PROPERTY_NAMES) {
            if (SpringUtil.getVaadinProperty(env, parameter) != null) {
                parameters.add(parameter);
            }
        }
        return parameters;
    }

    private static List<String> readPropertyNames(String resource)
            throws IOException {
        try (InputStream stream = SpringConfigurationMetadataTest.class
                .getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, resource + " not found");
            JsonNode metadata = JsonMapper.builder().build().readTree(stream);
            return metadata.get("properties").valueStream()
                    .map(property -> property.get("name").asString()).toList();
        }
    }
}
