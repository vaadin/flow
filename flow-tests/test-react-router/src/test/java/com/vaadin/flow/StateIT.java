package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class StateIT extends ChromeBrowserTest {

    @Test
    public void validateReactInUse() {
        open();

        waitForDevServer();

        SpanElement reactEnabled = $(SpanElement.class)
                .id(StateView.ENABLED_SPAN);
        Assert.assertEquals("React not enabled", "React enabled: true",
                reactEnabled.getText());

        SpanElement reactInPackage = $(SpanElement.class)
                .id(StateView.REACT_SPAN);

        Assert.assertEquals("No react found in package.json",
                "React found: true", reactInPackage.getText());
    }

}
