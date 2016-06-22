package com.vaadin.server;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.vaadin.annotations.Title;
import com.vaadin.server.BootstrapHandler.BootstrapContext;
import com.vaadin.server.BootstrapHandler.PreRenderMode;
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

    @Test
    public void prerenderMode() {
        Map<String, PreRenderMode> parametertoMode = new HashMap<>();
        parametertoMode.put("only", PreRenderMode.PRE_ONLY);
        parametertoMode.put("no", PreRenderMode.LIVE_ONLY);

        parametertoMode.put("", PreRenderMode.PRE_AND_LIVE);
        parametertoMode.put(null, PreRenderMode.PRE_AND_LIVE);
        parametertoMode.put("foobar", PreRenderMode.PRE_AND_LIVE);

        for (String parameter : parametertoMode.keySet()) {
            VaadinRequest request = Mockito.mock(VaadinRequest.class);
            Mockito.doAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation)
                        throws Throwable {
                    return parameter;
                }
            }).when(request).getParameter("prerender");
            BootstrapContext context = new BootstrapContext(request, null, null,
                    null);
            Assert.assertEquals(parametertoMode.get(parameter),
                    context.getPreRenderMode());
        }
    }
}
