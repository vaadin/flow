/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
