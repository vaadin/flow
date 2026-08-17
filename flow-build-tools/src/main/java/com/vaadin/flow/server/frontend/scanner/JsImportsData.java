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
package com.vaadin.flow.server.frontend.scanner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A single {@code @JsModule} declaration that names values to import from the
 * module, i.e. one that sets {@code imports} or {@code importAll}.
 * <p>
 * Unlike a plain {@code @JsModule}, such a declaration is not emitted as a
 * side-effect import. Instead the named values are imported into a dedicated
 * chunk that publishes them under the declaring class in the client-side
 * imports registry, from where a JavaScript expression sent by the server can
 * receive them as a parameter.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.3
 */
public class JsImportsData implements Serializable {

    private final String className;
    private final String module;
    private final List<String> names;
    private final boolean importAll;
    private final boolean developmentOnly;

    /**
     * Creates a new declaration.
     *
     * @param className
     *            the fully qualified name of the class carrying the annotation,
     *            not {@code null}
     * @param module
     *            the module to import from, not {@code null}
     * @param names
     *            the names to import from the module, may be empty but not
     *            {@code null}
     * @param importAll
     *            {@code true} to import the whole module namespace
     * @param developmentOnly
     *            {@code true} to import the values only in development mode
     */
    public JsImportsData(String className, String module, List<String> names,
            boolean importAll, boolean developmentOnly) {
        this.className = Objects.requireNonNull(className);
        this.module = Objects.requireNonNull(module);
        this.names = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(names)));
        this.importAll = importAll;
        this.developmentOnly = developmentOnly;
    }

    /**
     * Gets the fully qualified name of the class carrying the annotation. This
     * is the key under which the imported values are published.
     *
     * @return the declaring class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the module to import from.
     *
     * @return the module
     */
    public String getModule() {
        return module;
    }

    /**
     * Gets the names to import from the module. Empty when
     * {@link #isImportAll()} is {@code true}.
     *
     * @return the names to import, never {@code null}
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * Checks whether the whole module namespace should be imported instead of
     * individual names.
     *
     * @return {@code true} to import the whole namespace
     */
    public boolean isImportAll() {
        return importAll;
    }

    /**
     * Checks whether the values should only be imported when running in
     * development mode.
     *
     * @return {@code true} to import the values only in development mode
     */
    public boolean isDevelopmentOnly() {
        return developmentOnly;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof JsImportsData other)) {
            return false;
        }
        return importAll == other.importAll
                && developmentOnly == other.developmentOnly
                && className.equals(other.className)
                && module.equals(other.module) && names.equals(other.names);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, module, names, importAll,
                developmentOnly);
    }

    @Override
    public String toString() {
        return "JsImportsData{" + className + " -> " + module + " "
                + (importAll ? "*" : names)
                + (developmentOnly ? " (development only)" : "") + "}";
    }
}
