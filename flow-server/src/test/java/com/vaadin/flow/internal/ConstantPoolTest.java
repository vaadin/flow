/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import org.junit.Assert;
import org.junit.Test;

import elemental.json.Json;
import elemental.json.JsonObject;

public class ConstantPoolTest {
    private ConstantPool constantPool = new ConstantPool();

    @Test
    public void newConstantPool_noNewItems() {
        Assert.assertFalse(constantPool.hasNewConstants());
        Assert.assertEquals(0, constantPool.dumpConstants().keys().length);
    }

    @Test
    public void valueIsRegistered() {
        ConstantPoolKey reference = new ConstantPoolKey(Json.createObject());

        String constantId = constantPool.getConstantId(reference);

        Assert.assertTrue(constantPool.hasNewConstants());

        JsonObject dump = constantPool.dumpConstants();

        Assert.assertEquals(1, dump.keys().length);
        Assert.assertEquals("{}", dump.get(constantId).toJson());
    }

    @Test
    public void sameValue_sameId() {
        ConstantPoolKey reference = new ConstantPoolKey(Json.createObject());

        String constantId = constantPool.getConstantId(reference);
        constantPool.dumpConstants();

        String otherId = constantPool
                .getConstantId(new ConstantPoolKey(Json.createObject()));

        Assert.assertEquals(constantId, otherId);
        Assert.assertFalse(constantPool.hasNewConstants());
    }

    @Test
    public void differentValue_differentId() {
        ConstantPoolKey reference = new ConstantPoolKey(Json.createObject());

        String constantId = constantPool.getConstantId(reference);
        constantPool.dumpConstants();

        String otherId = constantPool
                .getConstantId(new ConstantPoolKey(Json.createArray()));

        Assert.assertNotEquals(constantId, otherId);
        Assert.assertTrue(constantPool.hasNewConstants());
    }

    @Test
    public void constantPoolKey_exportedDirectly_idCreated() {
        final ConstantPoolKey constantPoolKey = new ConstantPoolKey(
                Json.createObject());
        final JsonObject message = Json.createObject();
        constantPoolKey.export(message);
        Assert.assertTrue(message.hasKey(constantPoolKey.getId()));
    }
}
