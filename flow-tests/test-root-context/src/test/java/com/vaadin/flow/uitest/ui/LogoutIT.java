package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class LogoutIT extends ChromeBrowserTest {

    @Test
    public void setLocation_noErrorMessages() {
        open();

        $(NativeButtonElement.class).first().click();

        waitUntil(driver -> $("a").exists());

        Assert.assertTrue($("a").attribute("href", "link").exists());
    }
}
