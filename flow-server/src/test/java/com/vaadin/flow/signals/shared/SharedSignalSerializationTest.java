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
package com.vaadin.flow.signals.shared;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.signals.TestUtil;

import static org.junit.jupiter.api.Assertions.fail;

public class SharedSignalSerializationTest {

    private <T> T assertSerializeAndDeserialize(T obj) {
        try {
            return serializeAndDeserialize(obj);
        } catch (Throwable e) {
            fail("Not Serializable: " + e.getClass().getName() + ": "
                    + e.getMessage());
            return null;
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
    public void sharedValueSignal_serializable() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
        assertSerializeAndDeserialize(signal);

        TestUtil.assertSuccess(signal.set("Test"));
        assertSerializeAndDeserialize(signal);
    }

    @Test
    void sharedListSignal_serializable() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        assertSerializeAndDeserialize(signal);

        TestUtil.assertSuccess(signal.insertFirst("Test"));
        assertSerializeAndDeserialize(signal);
    }

    @Test
    void sharedMapSignal_serializable() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        assertSerializeAndDeserialize(signal);

        TestUtil.assertSuccess(signal.put("key", "Test"));
        assertSerializeAndDeserialize(signal);
    }

    @Test
    void sharedNodeSignal_serializable() {
        SharedNodeSignal signal = new SharedNodeSignal();
        assertSerializeAndDeserialize(signal);

        TestUtil.assertSuccess(signal.putChildWithValue("key", "Test"));
        signal = assertSerializeAndDeserialize(signal);

        Assert.assertEquals("Test", signal.get().mapChildren().get("key").get()
                .value(String.class));
    }

    @Test
    void sharedNumberSignal_serializable() {
        SharedNumberSignal signal = new SharedNumberSignal(0.0);
        assertSerializeAndDeserialize(signal);

        TestUtil.assertSuccess(signal.set(123.45));
        assertSerializeAndDeserialize(signal);
    }
}
