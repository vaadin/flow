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
package com.vaadin.flow.dom.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;

/**
 * @author Muammer Yucel
 * @since 2.2.
 * @see https://github.com/vaadin/flow/issues/7190
 */
public class ElementStateProviderDeserializationTest {

    @Test
    public void shouldRemoveChildComponentFromDeserializedParent()
            throws Exception {

        TestParentComponent parent = (TestParentComponent) deserialize(
                serialize(new TestParentComponent(new TestChildComponent())));

        Component child = parent.getChildren().findFirst()
                .orElseThrow(IllegalStateException::new);

        parent.remove(child);

        Assert.assertEquals("Child component should have been removed.", 0,
                parent.getChildren().count());
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

    private static class TestParentComponent extends Component
            implements HasComponents {

        private static final long serialVersionUID = 1L;

        public TestParentComponent(Component... components) {
            super(new Element(Tag.DIV));
            add(components);
        }
    }

    private static class TestChildComponent extends Component {

        private static final long serialVersionUID = 1L;

        public TestChildComponent() {
            super(new Element(Tag.DIV));
        }
    }
}
