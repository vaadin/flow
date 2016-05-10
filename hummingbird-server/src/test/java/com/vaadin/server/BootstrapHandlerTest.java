package com.vaadin.server;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.Title;
import com.vaadin.server.BootstrapHandler.BootstrapContext;
import com.vaadin.ui.UI;

public class BootstrapHandlerTest {

    static final String UI_TITLE = "UI_TITLE";

    @Title(UI_TITLE)
    private class TestUI extends UI {

        @Override
        protected void init(VaadinRequest request) {
        }

    }

    @Test
    public void testInitialPageTitle_pageSetTitle_noExecuteJavascript() {
        TestUI ui = new TestUI();
        BootstrapContext context = new BootstrapContext(null, null, null, ui);

        String overriddenPageTitle = "overridden";
        ui.getPage().setTitle(overriddenPageTitle);

        Assert.assertEquals(overriddenPageTitle,
                BootstrapHandler.resolvePageTitle(context).get());

        Assert.assertEquals(0,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());
    }

    @Test
    public void testInitialPageTitle_nullTitle_noTitle() {
        TestUI ui = new TestUI();
        BootstrapContext context = new BootstrapContext(null, null, null, ui);

        Assert.assertFalse(
                BootstrapHandler.resolvePageTitle(context).isPresent());
    }
}
