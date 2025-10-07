package com.vaadin.flow.spring.flowsecuritycontextpath;

import org.junit.Assert;
import org.junit.Test;

public class AppViewIT extends com.vaadin.flow.spring.flowsecurity.AppViewIT {

    @Override
    protected String getRootURL() {
        return super.getRootURL() + "/context";
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
                "Expected :root --fake-aura-theme-loaded custom property to be set by @vaadin/aura/fake-aura.css",
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
                "Expected :root --fake-lumo-theme-loaded custom property to be set by @vaadin/vaadin-lumo-styles/fake-lumo.css",
                "1", lumoLoaded);
    }
}
