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
