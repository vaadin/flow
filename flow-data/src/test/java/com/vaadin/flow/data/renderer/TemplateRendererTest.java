/*
 * Copyright 2000-2020 Vaadin Ltd.
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
