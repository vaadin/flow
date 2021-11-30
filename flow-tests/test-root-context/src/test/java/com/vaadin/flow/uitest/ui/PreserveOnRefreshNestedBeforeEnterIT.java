package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

// IT for https://github.com/vaadin/flow/issues/12356
public class PreserveOnRefreshNestedBeforeEnterIT extends ChromeBrowserTest {

    @Test
    public void refreshViewWithNestedLayouts_eachBeforeEnterIsCalledOnlyOnce() {
        open();

        Assert.assertEquals("1", $(SpanElement.class)
                .id("RootLayout-before-enter-count").getText());
        Assert.assertEquals("1", $(SpanElement.class)
                .id("NestedLayout-before-enter-count").getText());
        Assert.assertEquals("1", $(SpanElement.class)
                .id("PreserveOnRefreshNestedBeforeEnterView-before-enter-count")
                .getText());

        open();

        Assert.assertEquals("2", $(SpanElement.class)
                .id("RootLayout-before-enter-count").getText());
        Assert.assertEquals("2", $(SpanElement.class)
                .id("NestedLayout-before-enter-count").getText());
        Assert.assertEquals("2", $(SpanElement.class)
                .id("PreserveOnRefreshNestedBeforeEnterView-before-enter-count")
                .getText());
    }
}
