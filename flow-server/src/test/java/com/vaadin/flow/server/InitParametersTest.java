/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;

public class InitParametersTest {
    @Test
    public void publicMembersAreStringConstants() {
        for (Field field : InitParameters.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers)) {
                Assert.assertEquals(
                        String.format("field '%s' expected String",
                                field.getName()),
                        String.class, field.getType());
                Assert.assertTrue(String.format("field '%s' expected static",
                        field.getName()), Modifier.isStatic(modifiers));
                Assert.assertTrue(String.format("field '%s' expected final",
                        field.getName()), Modifier.isFinal(modifiers));
            }
        }
    }
}
