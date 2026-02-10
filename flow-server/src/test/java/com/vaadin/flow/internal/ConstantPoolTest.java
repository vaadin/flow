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
package com.vaadin.flow.internal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ObjectNode;

class ConstantPoolTest {
    private ConstantPool constantPool = new ConstantPool();

    @Test
    public void newConstantPool_noNewItems() {
        Assertions.assertFalse(constantPool.hasNewConstants());
        Assertions.assertEquals(0,
                JacksonUtils.getKeys(constantPool.dumpConstants()).size());
    }

    @Test
    public void valueIsRegistered() {
        ConstantPoolKey reference = new ConstantPoolKey(
                JacksonUtils.createObjectNode());

        String constantId = constantPool.getConstantId(reference);

        Assertions.assertTrue(constantPool.hasNewConstants());

        ObjectNode dump = constantPool.dumpConstants();

        Assertions.assertEquals(1, JacksonUtils.getKeys(dump).size());
        Assertions.assertEquals("{}", dump.get(constantId).toString());
    }

    @Test
    public void sameValue_sameId() {
        ConstantPoolKey reference = new ConstantPoolKey(
                JacksonUtils.createObjectNode());

        String constantId = constantPool.getConstantId(reference);
        constantPool.dumpConstants();

        String otherId = constantPool.getConstantId(
                new ConstantPoolKey(JacksonUtils.createObjectNode()));

        Assertions.assertEquals(constantId, otherId);
        Assertions.assertFalse(constantPool.hasNewConstants());
    }

    @Test
    public void differentValue_differentId() {
        ConstantPoolKey reference = new ConstantPoolKey(
                JacksonUtils.createObjectNode());

        String constantId = constantPool.getConstantId(reference);
        constantPool.dumpConstants();

        String otherId = constantPool.getConstantId(
                new ConstantPoolKey(JacksonUtils.createArrayNode()));

        Assertions.assertNotEquals(constantId, otherId);
        Assertions.assertTrue(constantPool.hasNewConstants());
    }

    @Test
    public void constantPoolKey_exportedDirectly_idCreated() {
        final ConstantPoolKey constantPoolKey = new ConstantPoolKey(
                JacksonUtils.createObjectNode());
        final ObjectNode message = JacksonUtils.createObjectNode();
        constantPoolKey.export(message);
        Assertions.assertTrue(message.has(constantPoolKey.getId()));
    }
}
