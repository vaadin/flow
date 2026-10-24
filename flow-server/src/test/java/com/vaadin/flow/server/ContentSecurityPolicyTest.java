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
package com.vaadin.flow.server;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContentSecurityPolicyTest {

    @Test
    void toHeaderValue_directivesInFirstSetOrder() {
        ContentSecurityPolicy policy = new ContentSecurityPolicy()
                .setDirective("Default-Src", "'self'")
                .setDirective("img-src", "'self'")
                .setDirective("upgrade-insecure-requests")
                .addSources("IMG-SRC", "data:", "'self'")
                .setDirective("default-src", "'none'")
                .addSources("frame-src", "https://maps.example.com")
                .removeDirective("upgrade-insecure-requests");

        assertEquals(
                "default-src 'none'; img-src 'self' data:; frame-src https://maps.example.com",
                policy.toHeaderValue());
        assertEquals(List.of("'self'", "data:"), policy.getSources("img-src"));
        assertEquals(List.of(), policy.getSources("script-src"));
    }

    @Test
    void toHeaderValue_directiveWithoutSources_writtenAsName() {
        assertEquals("upgrade-insecure-requests; base-uri 'self'",
                new ContentSecurityPolicy()
                        .setDirective("upgrade-insecure-requests")
                        .setDirective("base-uri", "'self'").toHeaderValue());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "'self' data:", "'self';", "a,b" })
    void addSources_invalidSource_throws(String source) {
        assertThrows(IllegalArgumentException.class,
                () -> new ContentSecurityPolicy().addSources("img-src",
                        source));
    }

    @Test
    void setDirective_invalidName_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ContentSecurityPolicy().setDirective("img-src; a"));
    }
}
