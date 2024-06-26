/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

public class KeyTest {

    @Tag("input")
    public static class InputComponent extends Component
            implements KeyNotifier {

    }

    @Test
    public void listenerWithMultipleKeyValues() {
        InputComponent input = new InputComponent();
        AtomicBoolean fired = new AtomicBoolean(false);
        input.addKeyPressListener(Key.of("foo", "bar"),
                event -> fired.set(true));

        input.fireEvent(new KeyPressEvent(input, "foo"));
        Assert.assertTrue(fired.get());

        fired.set(false);
        input.fireEvent(new KeyPressEvent(input, "bar"));
        Assert.assertTrue(fired.get());

        fired.set(false);
        input.fireEvent(new KeyPressEvent(input, "baz"));
        Assert.assertFalse(fired.get());
    }

}
