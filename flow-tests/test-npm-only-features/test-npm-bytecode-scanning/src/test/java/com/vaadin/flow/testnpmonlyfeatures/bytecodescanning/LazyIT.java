/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class LazyIT extends ChromeBrowserTest {

    @Test
    public void lazyLoadedWhenEnteringLazyView() {
        open();

        // The component should not be loaded yet
        TestBenchElement component = $("lazy-component").first();
        Assert.assertEquals("", component.getText());

        String lazyView = getTestURL(getRootURL(),
                "/view/com.vaadin.flow.testnpmonlyfeatures.bytecodescanning.LazyView",
                null);

        getDriver().get(lazyView);
        // The component should now be loaded
        component = $("lazy-component").first();
        Assert.assertEquals("Lazy component", component.getText());
    }

    @Override
    protected Class<? extends Component> getViewClass() {
        return EagerViewWithLazyComponent.class;
    }

}
