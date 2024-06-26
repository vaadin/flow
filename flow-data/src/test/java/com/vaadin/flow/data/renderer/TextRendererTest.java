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

public class TextRendererTest {

    @Test(expected = IllegalStateException.class)
    public void dontAllowNullInLabelGenerator() {
        TextRenderer<Object> renderer = new TextRenderer<>(obj -> null);
        renderer.createComponent(new Object());
    }

}
