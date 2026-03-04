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
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SharedSignalSerializationTest {

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
    <T> T serializeAndDeserialize(T instance) throws Throwable {
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
    void sharedValueSignal_notSerializable() {
        SharedValueSignal<String> signal = new SharedValueSignal<>("");
        assertThrows(NotSerializableException.class,
                () -> serializeAndDeserialize(signal));
    }

    @Test
    void sharedListSignal_notSerializable() {
        SharedListSignal<String> signal = new SharedListSignal<>(String.class);
        assertThrows(NotSerializableException.class,
                () -> serializeAndDeserialize(signal));

    }

    @Test
    void sharedMapSignal_notSerializable() {
        SharedMapSignal<String> signal = new SharedMapSignal<>(String.class);
        assertThrows(NotSerializableException.class,
                () -> serializeAndDeserialize(signal));
    }

    @Test
    void sharedNodeSignal_notSerializable() {
        SharedNodeSignal signal = new SharedNodeSignal();
        assertThrows(NotSerializableException.class,
                () -> serializeAndDeserialize(signal));
    }

    @Test
    void sharedNumberSignal_notSerializable() {
        SharedNumberSignal signal = new SharedNumberSignal(0.0);
        assertThrows(NotSerializableException.class,
                () -> serializeAndDeserialize(signal));
    }
}
