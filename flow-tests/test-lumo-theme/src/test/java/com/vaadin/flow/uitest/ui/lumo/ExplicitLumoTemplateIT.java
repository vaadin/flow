/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.lumo;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.theme.lumo.Lumo;

public class ExplicitLumoTemplateIT extends AbstractThemedTemplateIT {

    @Test
    public void darkVariantIsUsed_htmlElementHasThemeAttribute() {
        open();

        WebElement html = findElement(By.tagName("html"));
        Assert.assertEquals(Lumo.DARK, html.getAttribute("theme"));
    }

    @Override
    protected String getTagName() {
        return "explicit-lumo-themed-template";
    }

    @Override
    protected String getThemedTemplate() {
        return "theme/lumo/LumoThemedTemplate.html";
    }

}
