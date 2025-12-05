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
package com.vaadin.flow.spring.flowthemessecuritycontextpath;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.spring.test.AbstractSpringTest;

public class AppViewIT extends AbstractSpringTest {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/ui";
    }

    @Override
    protected String getTestPath() {
        return "";
    }

    @Test
    public void stylesheet_auraThemeImport_importedWithNoExtraSecurityConfig() {
        open("");

        // Verify that the Aura theme stylesheet from @StyleSheet has been
        // applied
        // by checking that the CSS custom property is present on :root
        String auraLoaded = (String) executeScript(
                "return getComputedStyle(document.documentElement).getPropertyValue('--fake-aura-theme-loaded').trim() ");
        Assert.assertEquals(
                "Expected :root --fake-aura-theme-loaded custom property to be set by aura/fake-aura.css",
                "1", auraLoaded);
    }

    @Test
    public void stylesheet_lumoThemeImport_importedWithNoExtraSecurityConfig() {
        open("");

        // Verify that the Lumo theme stylesheet from @StyleSheet has been
        // applied
        // by checking that the CSS custom property is present on :root
        String lumoLoaded = (String) executeScript(
                "return getComputedStyle(document.documentElement).getPropertyValue('--fake-lumo-theme-loaded').trim() ");
        Assert.assertEquals(
                "Expected :root --fake-lumo-theme-loaded custom property to be set by lumo/fake-lumo.css",
                "1", lumoLoaded);
    }
}
