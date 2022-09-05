package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ResyncListenerIT extends ChromeBrowserTest {

    @Test
    public void listenerWorksAfterResync() {
        open();
        $(ButtonElement.class).id("resync").click();
        open();
        $(ButtonElement.class).id("button").click();
        NotificationElement notification = $(NotificationElement.class).first();
        Assert.assertEquals("Works", notification.getText());
    }

}
