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
package com.vaadin.flow.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.todo.DecoratorElement;

/**
 * The TypeScript sources of an add-on are copied into the jar-resources folder,
 * which the generated project {@code tsconfig.json} excludes so that the type
 * checking rules of the project are not enforced on them. A bundler applies
 * {@code compilerOptions} only from a configuration that owns the file, so
 * those sources need a configuration of their own, see
 * {@code TaskGenerateJarResourcesTsConfig}. Without it they are transformed
 * with no compiler options at all, which breaks them in two ways this test
 * covers against the bundle the build actually produces. The behaviour they
 * break is covered from the browser by the add-on component of
 * {@code TemplateIT}, this test pins down the transformation itself so that a
 * regression names its cause instead of failing as an application that does not
 * start.
 *
 * @see <a href=
 *      "https://github.com/vaadin/flow/issues/24982">vaadin/flow#24982</a>
 */
public class AddonDecoratorIT extends ChromeBrowserTest {

    // A decorator left untransformed, as in
    // "(@customElement("decorator-element") class extends LitElement {"
    private static final Pattern RAW_DECORATOR = Pattern
            .compile("@\\w+\\([^)]*\\)\\s*class\\b");

    @Test
    public void addonDecoratorSource_isTranspiledInTheBundle()
            throws IOException {
        // The dev bundle is built when the application is first opened
        open();
        waitForElementPresent(By.tagName(DecoratorElement.TAG));

        String chunk = getBundleChunkOf(DecoratorElement.TAG);

        Assert.assertFalse(
                "The decorators of the add-on source should be transpiled, "
                        + "browsers cannot parse a raw decorator. Bundle "
                        + "chunk:\n" + chunk,
                RAW_DECORATOR.matcher(chunk).find());
        Assert.assertTrue(
                "The decorators of the add-on source should be transpiled to "
                        + "the __decorate helper",
                chunk.contains("__decorate"));
        Assert.assertTrue(
                "The initializer of the reactive property should be assigned "
                        + "in the constructor, a native class field shadows "
                        + "the accessor Lit installs for it",
                chunk.contains("this.label = \"Default\"")
                        || chunk.contains("this.label = 'Default'"));
    }

    /**
     * Reads the built chunk that contains the given custom element tag.
     */
    private String getBundleChunkOf(String tag) throws IOException {
        File baseDir = new File(System.getProperty("user.dir", "."));
        File buildFolder = new File(baseDir, "target/"
                + Constants.DEV_BUNDLE_LOCATION + "/webapp/VAADIN/build");
        Assert.assertTrue("The dev bundle should have been built into "
                + buildFolder.getPath(), buildFolder.isDirectory());

        List<File> chunks = List
                .of(buildFolder.listFiles((dir, name) -> name.endsWith(".js")));
        for (File chunk : chunks) {
            String content = FileUtils.readFileToString(chunk,
                    StandardCharsets.UTF_8);
            if (content.contains(tag)) {
                return content;
            }
        }
        throw new AssertionError("No built chunk of the dev bundle contains '"
                + tag + "', looked at " + chunks);
    }
}
