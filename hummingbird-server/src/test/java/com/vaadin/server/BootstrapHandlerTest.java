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
    public void testInitialPageTitle_noExplicitTitleSet_uiTitleUsed() {
        TestUI ui = new TestUI();
        BootstrapContext context = new BootstrapContext(null, null, null, ui);

        Assert.assertEquals(UI_TITLE,
                BootstrapHandler.getInitialPageTitle(context).get());

        Assert.assertEquals(0, ui.getFrameworkData()
                .dumpPendingJavaScriptInvocations().size());
    }

    @Test
    public void testInitialPageTitle_explicitTitleSet_uiTitleIgnored() {
        TestUI ui = new TestUI();
        BootstrapContext context = new BootstrapContext(null, null, null, ui);

        String overriddenPageTitle = "overridden";
        ui.getPage().setTitle(overriddenPageTitle);

        Assert.assertEquals(overriddenPageTitle,
                BootstrapHandler.getInitialPageTitle(context).get());

        Assert.assertEquals(0, ui.getFrameworkData()
                .dumpPendingJavaScriptInvocations().size());
    }

}
