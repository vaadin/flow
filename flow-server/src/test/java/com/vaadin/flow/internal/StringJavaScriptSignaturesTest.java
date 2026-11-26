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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The signatures file of the forbiddenapis check ignores missing classes, so
 * that modules without flow-server can use it. A listed class that is renamed
 * or moved would then match nothing without failing the build. This test
 * resolves every signature against the classes of flow-server instead.
 */
class StringJavaScriptSignaturesTest {

    private static final Path SIGNATURES = Path.of("..", "forbidden-apis",
            "string-javascript.txt");

    private static final Map<String, Class<?>> PRIMITIVES = Map.of("boolean",
            boolean.class, "byte", byte.class, "char", char.class, "short",
            short.class, "int", int.class, "long", long.class, "float",
            float.class, "double", double.class);

    @Test
    void everySignatureResolves() throws IOException {
        List<String> signatures = Files.readAllLines(SIGNATURES).stream()
                .map(String::trim).filter(line -> !line.isEmpty()
                        && !line.startsWith("#") && !line.startsWith("@"))
                .toList();
        assertFalse(signatures.isEmpty(), "No signatures in " + SIGNATURES);

        List<String> unresolved = new ArrayList<>();
        for (String signature : signatures) {
            try {
                resolve(signature);
            } catch (ReflectiveOperationException
                    | IllegalArgumentException e) {
                unresolved.add(signature + " (" + e + ")");
            }
        }
        assertEquals(List.of(), unresolved,
                "Signatures in " + SIGNATURES + " that match nothing");
    }

    private static void resolve(String signature)
            throws ReflectiveOperationException {
        int hash = signature.indexOf('#');
        if (hash < 0) {
            loadClass(signature);
            return;
        }
        Class<?> type = loadClass(signature.substring(0, hash));
        String member = signature.substring(hash + 1);
        int paren = member.indexOf('(');
        if (paren < 0) {
            type.getDeclaredField(member);
            return;
        }
        String name = member.substring(0, paren);
        String params = member.substring(paren + 1, member.length() - 1);
        Class<?>[] parameterTypes = params.isEmpty() ? new Class<?>[0]
                : Arrays.stream(params.split(","))
                        .map(StringJavaScriptSignaturesTest::loadType)
                        .toArray(Class<?>[]::new);
        if ("<init>".equals(name)) {
            type.getDeclaredConstructor(parameterTypes);
        } else {
            type.getDeclaredMethod(name, parameterTypes);
        }
    }

    private static Class<?> loadType(String name) {
        try {
            if (name.endsWith("[]")) {
                return loadType(name.substring(0, name.length() - 2))
                        .arrayType();
            }
            Class<?> primitive = PRIMITIVES.get(name);
            return primitive != null ? primitive : loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown parameter type " + name,
                    e);
        }
    }

    private static Class<?> loadClass(String name)
            throws ClassNotFoundException {
        return Class.forName(name, false,
                StringJavaScriptSignaturesTest.class.getClassLoader());
    }
}
