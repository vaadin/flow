/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
