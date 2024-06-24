/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.renderer;

import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

public class IconRendererTest {

    @Tag(Tag.A)
    public static class TestComponent extends Component {
    }

    @Test(expected = IllegalStateException.class)
    public void dontAllowNullInLabelGenerator() {
        IconRenderer<Object> renderer = new IconRenderer<>(
                obj -> new TestComponent(), obj -> null);
        renderer.createComponent(new Object());
    }

    @Test(expected = IllegalStateException.class)
    public void dontAllowNullInIconGenerator() {
        IconRenderer<Object> renderer = new IconRenderer<>(obj -> null,
                obj -> "");
        renderer.createComponent(new Object());
    }

}
