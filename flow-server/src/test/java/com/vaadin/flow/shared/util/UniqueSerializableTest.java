/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.shared.util;

import java.io.Serializable;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

public class UniqueSerializableTest implements Serializable {

    @Test
    public void testUniqueness() {
        UniqueSerializable o1 = new UniqueSerializable() {
        };
        UniqueSerializable o2 = new UniqueSerializable() {
        };
        Assert.assertFalse(o1 == o2);
        Assert.assertFalse(o1.equals(o2));
    }

    @Test
    public void testSerialization() {
        UniqueSerializable o1 = new UniqueSerializable() {
        };
        UniqueSerializable d1 = (UniqueSerializable) SerializationUtils
                .deserialize(SerializationUtils.serialize(o1));
        Assert.assertTrue(d1.equals(o1));
    }

}
