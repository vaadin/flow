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
