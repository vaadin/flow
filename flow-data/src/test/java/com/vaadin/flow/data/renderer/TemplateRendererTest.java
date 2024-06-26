/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.data.renderer;

import org.junit.Assert;
import org.junit.Test;

public class TemplateRendererTest {
    @Test
    public void missingAttributeBindingDetector() {
        String[] missingDollar = {
                // Basic case
                "<div class='[[item.className]]'>",
                // Spaces around =
                "<div class = '[[item.className]]'>",
                // Double quotes
                "<div class=\"[[item.className]]\">",
                // No quotes
                "<div class=[[item.className]]>",
                // Two-way binding
                "<div class={{item.className}}>",
                // Style property binding
                "<div style='[[item.style]]'>" };

        String[] notMissingDollar = {
                // Basic case with dollar
                "<div class$='[[item.className]]'>",
                // Spaces around =
                "<div class$ = [[item.className]]>",
                // Style binding
                "<div style$='[[item.style]]'>",
                // Attribute name prefix
                "<div my-style='[[item.style]]'>",
                // Classname binding
                "<div className='[[item.className]]'>",
                // Static list, i.e. not a binding at all
                "<div class='static list'>",
                // Binding-like syntax that still isn't binding
                "<div class=((item.not_a_binding))>" };

        for (String template : missingDollar) {
            if (!TemplateRenderer.hasClassOrStyleWithoutDollar(template)) {
                Assert.fail("Missing dollar should be detected for the string: "
                        + template);
            }
        }

        for (String template : notMissingDollar) {
            if (TemplateRenderer.hasClassOrStyleWithoutDollar(template)) {
                Assert.fail(
                        "Missing dollar should not be detected for the string: "
                                + template);
            }
        }
    }
}
