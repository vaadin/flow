/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.util.Collections;
import java.util.List;
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

    @Test
    public void of_toString_returnsKeys() {
        Key key = Key.of("foo");

        Assert.assertEquals("foo", key.toString());

        key = Key.of("foo", "bar");

        Assert.assertEquals("foo,  additional keys : [bar]", key.toString());
    }

    @Test
    public void of_equals_stringRepresentationsEqual_toKeysAreEqual() {
        Key key1 = Key.of("foo");
        Key key2 = Key.of("foo");

        Assert.assertEquals(key1, key2);
    }

    @Test
    public void of_equals_stringRepresentationsNotEqual_toKeysAreNotEqual() {
        Key key1 = Key.of("foo");
        Key key2 = Key.of("bar");

        Assert.assertNotEquals(key1, key2);
    }

    @Test
    public void of_equals_secondKeyHasAdditionalKeys_toKeysAreNotEqual() {
        Key key1 = Key.of("foo");
        Key key2 = Key.of("foo", "bar");

        Assert.assertNotEquals(key1, key2);
    }

    @Test
    public void of_equals_differentClasses_toKeysAreNotEqual() {
        Key key1 = Key.of("foo");
        Key key2 = new Key() {

            @Override
            public List<String> getKeys() {
                return Collections.singletonList("foo");
            }
        };

        Assert.assertNotEquals(key1, key2);
    }

    @Test
    public void of_equalKeys_hasSameHashCode() {
        Key key1 = Key.of("foo", "bar");
        Key key2 = Key.of("foo", "bar");
        Assert.assertEquals(key1.hashCode(), key2.hashCode());
    }

}
