package com.vaadin.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.junit.Assert;

public class FluentAPITestHelper {

    public static void testClassAPI(Class<?> clazz, Set<String> ignore) {
        for (Method m : clazz.getDeclaredMethods()) {
        if (!Modifier.isPublic(m.getModifiers())) {
            continue;
        }
        if (Modifier.isStatic(m.getModifiers())) {
            continue;
        }
        if (m.getName().startsWith("get") || m.getName().startsWith("has")
                || m.getName().startsWith("is")
                || ignore.contains(m.getName())) {
            // Ignore
        } else {
            // Setters and such
            Class<?> returnType = m.getReturnType();
            Assert.assertEquals(
                    "Method " + m.getName() + " has invalid return type",
                        clazz, returnType);
        }
        }
    }
}
