/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
