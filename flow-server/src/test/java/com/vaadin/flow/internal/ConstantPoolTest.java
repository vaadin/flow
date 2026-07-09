/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

public class ConstantPoolTest {
    private ConstantPool constantPool = new ConstantPool();

    @Test
    public void newConstantPool_noNewItems() {
        Assert.assertFalse(constantPool.hasNewConstants());
        Assert.assertEquals(0,
                JacksonUtils.getKeys(constantPool.dumpConstants()).size());
    }

    @Test
    public void valueIsRegistered() {
        ConstantPoolKey reference = new ConstantPoolKey(
                JacksonUtils.createObjectNode());

        String constantId = constantPool.getConstantId(reference);

        Assert.assertTrue(constantPool.hasNewConstants());

        ObjectNode dump = constantPool.dumpConstants();

        Assert.assertEquals(1, JacksonUtils.getKeys(dump).size());
        Assert.assertEquals("{}", dump.get(constantId).toString());
    }

    @Test
    public void sameValue_sameId() {
        ConstantPoolKey reference = new ConstantPoolKey(
                JacksonUtils.createObjectNode());

        String constantId = constantPool.getConstantId(reference);
        constantPool.dumpConstants();

        String otherId = constantPool.getConstantId(
                new ConstantPoolKey(JacksonUtils.createObjectNode()));

        Assert.assertEquals(constantId, otherId);
        Assert.assertFalse(constantPool.hasNewConstants());
    }

    @Test
    public void differentValue_differentId() {
        ConstantPoolKey reference = new ConstantPoolKey(
                JacksonUtils.createObjectNode());

        String constantId = constantPool.getConstantId(reference);
        constantPool.dumpConstants();

        String otherId = constantPool.getConstantId(
                new ConstantPoolKey(JacksonUtils.createArrayNode()));

        Assert.assertNotEquals(constantId, otherId);
        Assert.assertTrue(constantPool.hasNewConstants());
    }

    @Test
    public void constantPoolKey_exportedDirectly_idCreated() {
        final ConstantPoolKey constantPoolKey = new ConstantPoolKey(
                JacksonUtils.createObjectNode());
        final ObjectNode message = JacksonUtils.createObjectNode();
        constantPoolKey.export(message);
        Assert.assertTrue(message.has(constantPoolKey.getId()));
    }
}
