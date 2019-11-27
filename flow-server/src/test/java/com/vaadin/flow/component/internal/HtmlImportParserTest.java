/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.component.internal;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

public class HtmlImportParserTest {

    @Test
    public void emptyImport_noDependencies() {
        String root = "/frontend/foo.html";

        HtmlImportParser.parseImports(root, streamFactory(root),
                Function.identity(), dependency -> {
                    Assert.fail("There should be no dependencies");
                });
    }

    @Test
    public void variousURIs_URIsAreCollectedCorrectly() {
        String root = "frontend://baz/foo.html";

        Set<String> dependencies = new HashSet<>();

        HtmlImportParser.parseImports(root,
                streamFactory(root, "relative1.html", "foo/../relative1.html",
                        "../relative2.html", "sub/relative3.html",
                        "/absolute.html"),
                Function.identity(), dependencies::add);

        Assert.assertEquals(4, dependencies.size());

        Assert.assertTrue(
                "Dependencies parser doesn't return the simple relative URI (same parent)",
                dependencies.contains("frontend://baz/relative1.html"));
        Assert.assertTrue(
                "Dependencies parser doesn't return the relative URI which is located in the parent folder",
                dependencies.contains("frontend://relative2.html"));
        Assert.assertTrue(
                "Dependencies parser doesn't return the relative URI which is located sub folder",
                dependencies.contains("frontend://baz/sub/relative3.html"));
        Assert.assertTrue("Dependencies parser doesn't return the absolute URI",
                dependencies.contains("/absolute.html"));
    }

    @Test
    public void normalizeURI() throws Exception {
        assertEquals("http://foo/bar", normalize("http://foo/bar"));
        assertEquals("http://foo/../bar", normalize("http://foo/../bar"));
        assertEquals("http://foo/bar", normalize("http://foo/./bar"));
        assertEquals("http://foo/bar", normalize("http://foo/baz/../bar"));

        for (String protocol : new String[] { "frontend", "context", "base" }) {
            assertEquals(protocol + "://foo/bar",
                    normalize(protocol + "://foo/bar"));
            assertEquals(protocol + "://bar",
                    normalize(protocol + "://foo/../bar"));
            assertEquals(protocol + "://foo", normalize(protocol + "://./foo"));
            assertEquals(protocol + "://foo",
                    normalize(protocol + "://././foo"));
            assertEquals(protocol + "://foo/bar",
                    normalize(protocol + "://foo/./bar"));
            assertEquals(protocol + "://foo/bar",
                    normalize(protocol + "://foo/baz/../bar"));
            assertEquals(
                    protocol + "://bower_components/vaadin-button/src/vaadin-button.html",
                    normalize(protocol
                            + "://src/views/login/../../../bower_components/vaadin-button/src/vaadin-button.html"));
            assertEquals(protocol + "://components/js/hash-actions.html",
                    normalize(protocol
                            + "://components/../components/js/hash-actions.html"));
        }

    }

    private String normalize(String string) throws Exception {
        return HtmlImportParser.toNormalizedURI(new URI(string));
    }

    private static Function<String, InputStream> streamFactory(
            String expectedPath, String... imports) {
        String contents = Stream.of(imports)
                .map(imprt -> "<link rel='import' href='" + imprt + "'>")
                .collect(Collectors.joining());
        return path -> {
            Assert.assertEquals(expectedPath, path);
            return new ByteArrayInputStream(
                    contents.getBytes(StandardCharsets.UTF_8));
        };
    }

}
