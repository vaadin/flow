package com.vaadin.flow.ui;
/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.LocationChangeEvent;
import com.vaadin.flow.router.NavigationTrigger;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.View;
import com.vaadin.flow.uitest.servlet.ViewClassLocator;
import com.vaadin.ui.UI;

public class SerializationTest {

    @Test
    public void testViewsSerializable() throws Exception {
        UI ui = new UI();
        UI.setCurrent(ui);
        try {
            Collection<Class<? extends View>> viewClasses = new ViewClassLocator(
                    getClass().getClassLoader()).getAllViewClasses();
            for (Class<? extends View> viewClass : viewClasses) {
                View view = viewClass.newInstance();
                view.onLocationChange(new LocationChangeEvent(new Router(), ui,
                        NavigationTrigger.PROGRAMMATIC, new Location(""),
                        Collections.emptyList(), Collections.emptyMap()));
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
