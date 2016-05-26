/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template.parser;

import java.util.Optional;

import org.jsoup.nodes.TextNode;
import org.junit.Assert;
import org.junit.Test;

public class TemplateIncludeBuilderFactoryTest {

    @Test
    public void parseInclude() {
        assertIncludePath("foo.html", "@include foo.html@");
        assertIncludePath("foo.html", "@include foo.html @");
        assertIncludePath("foo.html", "@include foo.html @ ");
        assertIncludePath("foo.html", " @include foo.html @ ");

        assertIncludePath("foo/bar.html", "@include foo/bar.html@");
        assertIncludePath("foo bar.html", "@include foo bar.html@");
        assertIncludePath("fooäbar.html", "@include fooäbar.html@");
    }

    private void assertIncludePath(String expected, String includeText) {
        Optional<String> parsed = TemplateIncludeBuilderFactory
                .getIncludePath(new TextNode(includeText, ""));
        Assert.assertTrue(parsed.isPresent());
        Assert.assertEquals(expected, parsed.get());
    }
}
