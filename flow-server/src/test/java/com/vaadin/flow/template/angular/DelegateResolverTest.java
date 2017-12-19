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
package com.vaadin.flow.template.angular;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.template.angular.DelegateResolver;
import com.vaadin.flow.template.angular.RelativeFileResolver;

public class DelegateResolverTest {

    private RelativeFileResolver mainResolver = new RelativeFileResolver(
            DelegateResolverTest.class, "main.html");
    private DelegateResolver subResolver = new DelegateResolver(mainResolver,
            "subfolder");

    private void resolveAndAssert(String templateName, String expectedContents)
            throws IOException {
        Assert.assertTrue(IOUtils.toString(subResolver.resolve(templateName))
                .contains(expectedContents));
    }

    @Test
    public void resolveParentFolderTemplate() throws IOException {
        resolveAndAssert("../main.html", "Main template");
    }

    @Test
    public void resolveSubfolderTemplate() throws IOException {
        resolveAndAssert("subfoldertemplate.html", "Template in sub folder");
    }

    @Test
    public void resolveSubfolderTemplateThoughPath() throws IOException {
        resolveAndAssert("../subfolder/subfoldertemplate.html",
                "Template in sub folder");
    }

    @Test
    public void resolveAbsoluteTemplate() throws IOException {
        String absoluteTemplateName = "/" + DelegateResolverTest.class
                .getPackage().getName().replace(".", "/") + "/main.html";
        resolveAndAssert(absoluteTemplateName, "Main template");
    }
}
