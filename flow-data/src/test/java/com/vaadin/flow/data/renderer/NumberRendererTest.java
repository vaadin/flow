/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.data.renderer;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.function.ValueProvider;

public class NumberRendererTest {

    @Test
    public void getFormattedValue_numberIsFormattedUsingLocale() {
        NumberRenderer<Number> renderer = new NumberRenderer<>(
                ValueProvider.identity(), Locale.GERMANY);

        String formatted = renderer.getFormattedValue(1.2);
        Assert.assertEquals("1,2", formatted);

        renderer = new NumberRenderer<>(ValueProvider.identity(),
                Locale.ENGLISH);

        formatted = renderer.getFormattedValue(1.2);
        Assert.assertEquals("1.2", formatted);
    }

}
