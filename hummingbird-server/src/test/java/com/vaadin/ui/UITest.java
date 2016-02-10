package com.vaadin.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.server.VaadinRequest;

public class UITest {

    @Test
    public void elementIsBody() {
        UI ui = new UI() {

            @Override
            protected void init(VaadinRequest request) {
            }
        };
        Assert.assertEquals("body", ui.getElement().getTag());
    }
}
