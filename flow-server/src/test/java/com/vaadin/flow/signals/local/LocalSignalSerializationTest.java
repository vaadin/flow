/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.signals.local;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

public class LocalSignalSerializationTest {

    private void assertSerializeAndDeserialize(Object obj) {
        try {
            serializeAndDeserialize(obj);
        } catch (Throwable e) {
            fail("Not Serializable: " + e.getClass().getName() + ": "
                    + e.getMessage());
        }
    }

    /**
     * Performs actual serialization/deserialization
     *
     * @param <T>
     *            the type of the instance
     * @param instance
     *            the instance
     * @return the copy of the source object
     * @throws Throwable
     *             if something goes wrong.
     */
    @SuppressWarnings({ "UnusedReturnValue", "WeakerAccess" })
    public <T> T serializeAndDeserialize(T instance) throws Throwable {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bs);
        out.writeObject(instance);
        byte[] data = bs.toByteArray();
        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(data));

        @SuppressWarnings("unchecked")
        T readObject = (T) in.readObject();

        return readObject;
    }

    @Test
    public void testValueSignal_withNullValue_notSerializable() {
        ValueSignal<String> signal = new ValueSignal<>();
        assertSerializeAndDeserialize(signal);

        signal.value("Test");
        assertSerializeAndDeserialize(signal);
    }

    @Test
    void testListSignal_empty_notSerializable() {
        ListSignal<String> signal = new ListSignal<>();
        assertSerializeAndDeserialize(signal);

        signal.insertFirst("Test");
        assertSerializeAndDeserialize(signal);
    }
}
