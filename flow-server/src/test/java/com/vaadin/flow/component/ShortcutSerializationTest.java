/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

public class ShortcutSerializationTest {

    @Test
    public void addShortcutForLifecycleOwner_serializationWorks()
            throws Exception {
        Component owner = new FakeComponent();
        UI ui = new UI();
        Component[] components = new Component[] { ui };
        ui.add(owner);
        new ShortcutRegistration(owner, () -> components, event -> {
        }, Key.KEY_A);

        UI ui2 = (UI) deserialize(serialize(ui));
        Assert.assertNotNull(ui2);
    }

    @Test
    public void addAndRemoverShortcutForLifecycleOwner_serializationWorks()
            throws Exception {
        Component owner = new FakeComponent();
        UI ui = new UI();
        Component[] components = new Component[] { ui };
        ui.add(owner);
        new ShortcutRegistration(owner, () -> components, event -> {
        }, Key.KEY_A);
        ui.remove(owner);

        UI ui2 = (UI) deserialize(serialize(ui));
        Assert.assertNotNull(ui2);
    }

    private byte[] serialize(Object object) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            oos.flush();
            return baos.toByteArray();
        }
    }

    private Object deserialize(byte[] bytes)
            throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        }
    }

    @Tag("imaginary-tag")
    private static class FakeComponent extends Component
            implements ClickNotifier<FakeComponent>, Focusable<FakeComponent> {
    }

}
