/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.hummingbird.template;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class RelativeFileResolverTest {

    private RelativeFileResolver resolver = new RelativeFileResolver(
            RelativeFileResolverTest.class, "main.html");

    private RelativeFileResolver resolverWithFolder = new RelativeFileResolver(
            RelativeFileResolverTest.class, "../template/main.html");

    private void resolveAndAssert(String templateName, String expectedContents)
            throws IOException {
        Assert.assertTrue(IOUtils.toString(resolver.resolve(templateName))
                .contains(expectedContents));
        Assert.assertTrue(
                IOUtils.toString(resolverWithFolder.resolve(templateName))
                        .contains(expectedContents));

    }

    @Test
    public void resolveTemplateMain() throws IOException {
        resolveAndAssert("main.html", "Main template");
    }

    @Test
    public void resolveTemplateAbsolute() throws IOException {
        String absoluteTemplateName = "/" + RelativeFileResolverTest.class
                .getPackage().getName().replace(".", "/") + "/main.html";
        resolveAndAssert(absoluteTemplateName, "Main template");
    }

    @Test
    public void resolveTemplateSub() throws IOException {
        resolveAndAssert("sub.html", "Sub template");
    }

    @Test
    public void resolveTemplateSubFolder() throws IOException {
        resolveAndAssert("subfolder/subfoldertemplate.html",
                "Template in sub folder");
    }

    @Test
    public void resolveTemplateThroughParentFolder() throws IOException {
        resolveAndAssert("../template/sub.html", "Sub template");
    }
}
