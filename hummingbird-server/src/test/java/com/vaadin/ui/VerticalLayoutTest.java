package com.vaadin.ui;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.hummingbird.kernel.ElementTest;

public class VerticalLayoutTest {

    VerticalLayout vl;

    @Before
    public void setup() {
        vl = new VerticalLayout();
    }

    @Test
    public void initialClass() {
        ElementTest.assertElementEquals(
                ElementTest
                        .parse("<div class='layout flex-children vertical' style='width:100.0%'>"),
                vl.getElement());
    }

}
