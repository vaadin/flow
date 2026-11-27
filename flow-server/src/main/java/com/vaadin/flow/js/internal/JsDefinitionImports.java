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
package com.vaadin.flow.js.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.js.JsDefinitionImport;

/**
 * Reads the {@link JsDefinitionImport} annotations of an interface into the
 * modules its JavaScript imports from, which both the build, generating the
 * functions, and the server, computing their identifiers, go by.
 * <p>
 * For internal use only. May be renamed or removed in any release.
 */
public final class JsDefinitionImports implements Serializable {

    /**
     * What the generated function of a method annotated with
     * {@link JsWithExecutionContext} names the execution context, which an
     * imported name would hide.
     */
    public static final String EXECUTION_CONTEXT = "$executionContext";

    /**
     * A name an expression is given from a module.
     *
     * @param name
     *            the name the expression sees
     * @param export
     *            the export of the module the name is given:
     *            <code>default</code> for the default export, the name of a
     *            named export, or <code>null</code> for the whole module
     */
    public record Binding(String name,
            @Nullable String export) implements Serializable {
    }

    /**
     * A module that an interface imports from, with every name it is given from
     * it, merged from all the annotations that name the module.
     *
     * @param from
     *            the module as it is declared
     * @param bindings
     *            the names given from the module, in the order they are
     *            declared
     */
    public record ModuleImport(String from,
            List<Binding> bindings) implements Serializable {
    }

    private JsDefinitionImports() {
        // Only static members
    }

    /**
     * Gets the modules the given interface declares imports from, ordered by
     * module, so that the same declarations always give the same function. Only
     * the annotations of the type itself count, as annotations on an interface
     * are not inherited.
     *
     * @param type
     *            the interface, not <code>null</code>
     * @return the modules, empty when the interface imports nothing
     */
    public static List<ModuleImport> collect(Class<?> type) {
        Map<String, List<Binding>> byModule = new TreeMap<>();
        for (JsDefinitionImport annotation : type
                .getDeclaredAnnotationsByType(JsDefinitionImport.class)) {
            byModule.computeIfAbsent(annotation.from(),
                    from -> new ArrayList<>()).addAll(bindings(annotation));
        }
        return byModule.entrySet().stream()
                .map(entry -> new ModuleImport(entry.getKey(),
                        List.copyOf(entry.getValue())))
                .toList();
    }

    /**
     * Gets the names the given annotation declares, in the order of an
     * <code>import</code> statement: the default export, the named exports and
     * then the whole module.
     *
     * @param annotation
     *            the annotation, not <code>null</code>
     * @return the names, empty when the annotation declares none
     */
    public static List<Binding> bindings(JsDefinitionImport annotation) {
        List<Binding> bindings = new ArrayList<>();
        if (!annotation.defaultExport().isEmpty()) {
            bindings.add(new Binding(annotation.defaultExport(), "default"));
        }
        for (String export : annotation.exports()) {
            bindings.add(new Binding(export, export));
        }
        if (!annotation.namespace().isEmpty()) {
            bindings.add(new Binding(annotation.namespace(), null));
        }
        return bindings;
    }

    /**
     * Describes the given imports as text that is hashed into the identifier of
     * a function, so that two methods declaring the same JavaScript with
     * different imports are different functions.
     *
     * @param imports
     *            the imports, as {@link #collect(Class)} gets them, not
     *            <code>null</code>
     * @return the description, empty when there are no imports
     */
    public static String describe(List<ModuleImport> imports) {
        return imports.stream()
                .map(module -> module.from().length() + ":" + module.from()
                        + "{"
                        + module.bindings().stream()
                                .map(binding -> binding.name() + "="
                                        + (binding.export() == null ? "*"
                                                : "." + binding.export()))
                                .collect(Collectors.joining(","))
                        + "}")
                .collect(Collectors.joining());
    }
}
