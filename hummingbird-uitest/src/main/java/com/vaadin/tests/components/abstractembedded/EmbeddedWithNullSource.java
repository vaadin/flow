package com.vaadin.tests.components.abstractembedded;

import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.AbstractEmbedded;
import com.vaadin.ui.BrowserFrame;
import com.vaadin.ui.Flash;
import com.vaadin.ui.Image;

public class EmbeddedWithNullSource extends TestBase {

    @Override
    protected void setup() {
        AbstractEmbedded e;

        e = new Image("Image w/o alt text");
        add(e);

        e = new Image("Image w/ alt text");
        e.setAlternateText("Image");
        add(e);

        e = new Flash("Flash w/o alt text");
        add(e);

        e = new Flash("Flash w/ alt text");
        e.setAlternateText("Flash");
        add(e);

        e = new BrowserFrame("BrowserFrame w/o alt text");
        add(e);

        e = new BrowserFrame("BrowserFrame w/ alt text");
        e.setAlternateText("BrowserFrame");
        add(e);

    }

    @Override
    protected String getTestDescription() {
        return "Image without a source causes a client-side NPE";
    }

    @Override
    protected Integer getTicketNumber() {
        return 10101;
    }

}
