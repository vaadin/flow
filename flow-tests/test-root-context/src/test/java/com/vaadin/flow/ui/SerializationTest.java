package com.vaadin.flow.ui;
/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.uitest.servlet.ViewClassLocator;

public class SerializationTest {

    @Test
    public void testViewsSerializable() throws Exception {
        UI ui = new UI();
        UI.setCurrent(ui);
        try {
            Collection<Class<? extends Component>> viewClasses = new ViewClassLocator(
                    getClass().getClassLoader()).getAllViewClasses();
            for (Class<? extends Component> viewClass : viewClasses) {
                Component view = viewClass.newInstance();
                // view.onLocationChange(new LocationChangeEvent(new Router(),
                // ui,
                // NavigationTrigger.PROGRAMMATIC, new Location(""),
                // Collections.emptyList(), Collections.emptyMap()));
                try {
                    Assert.assertNotNull(serializeDeserialize(view));
                } catch (Exception e) {
                    throw new AssertionError(
                            "Can't serialize view " + viewClass.getName(), e);
                }
            }
        } finally {
            UI.setCurrent(null);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T serializeDeserialize(T t)
            throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bs);
        out.writeObject(t);
        byte[] data = bs.toByteArray();
        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(data));

        return (T) in.readObject();
    }
}
