/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TemplateMappingDetectorIT extends ChromeBrowserTest {

    @Test
    public void regularTemplate_mappedComponentsAreMarkedAsSuch() {
        open();

        TestBenchElement container = $("template-mapping-detector").first();
        assertMappedComponentsAreMarkedProperly(container, false);
    }

    @Test
    public void templateInTemplate_mappedComponentsAreMarkedAsSuch() {
        open();

        TestBenchElement parentTemplate = $("template-mapping-detector-parent")
                .first();
        TestBenchElement container = parentTemplate.$(TestBenchElement.class)
                .id("detector");

        assertMappedComponentsAreMarkedProperly(container, true);
    }

    @Test
    public void composite_mappedComponentsAreMarkedAsSuch() {
        open();

        TestBenchElement container = $(TestBenchElement.class).id("composite");
        assertMappedComponentsAreMarkedProperly(container, false);
    }

    private void assertMappedComponentsAreMarkedProperly(
            TestBenchElement container, boolean templateInTemplate) {
        TestBenchElement mappedComponent = container.$(TestBenchElement.class)
                .id("detector1");
        Assert.assertEquals("Template mapped: true", mappedComponent.getText());

        TestBenchElement standaloneComponent = container
                .$(TestBenchElement.class).id("detector2");

        Assert.assertEquals("Template mapped: false",
                standaloneComponent.getText());

        TestBenchElement standaloneComposite = container
                .$(TestBenchElement.class).id("detector3");

        Assert.assertEquals(
                "Composite template mapped: false Template mapped: false",
                standaloneComposite.getText());

        TestBenchElement theTemplateItself = container.$(TestBenchElement.class)
                .id("detector4");

        Assert.assertEquals("The template itself: " + templateInTemplate,
                theTemplateItself.getText());
    }

}
