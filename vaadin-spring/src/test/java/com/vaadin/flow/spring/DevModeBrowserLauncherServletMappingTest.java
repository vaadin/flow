package com.vaadin.flow.spring;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;

public class DevModeBrowserLauncherServletMappingTest
        extends DevModeBrowserLauncherNoPropertiesTest {

    @Test
    public void getUrl_withContextPath_givesUrlWithContextPathAndNoUrlMapping() {
        MockServletContext ctx = (MockServletContext) app.getServletContext();
        ctx.setContextPath("/contextpath");
        String url = DevModeBrowserLauncher.getUrl(app);
        Assert.assertEquals("http://localhost:1244/contextpath/", url);
    }

}
