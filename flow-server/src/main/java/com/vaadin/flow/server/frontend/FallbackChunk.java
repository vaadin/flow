/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.vaadin.flow.component.dependency.CssImport;

/**
 * Fall back chunk info.
 *
 * @author Vaadin Ltd
 *
 */
public class FallbackChunk implements Serializable {

    private final Set<String> modules;
    private final Set<CssImportData> cssImports;

    /**
     * Creates a new instance using info about modules and css data in fallback
     * chunk.
     *
     * @param modules
     *            fallback modules, not {@code null}
     * @param css
     *            fallback css data, not {@code null}
     */
    public FallbackChunk(Collection<String> modules,
            Collection<CssImportData> css) {
        this.modules = new HashSet<>(Objects.requireNonNull(modules));
        this.cssImports = new HashSet<>(Objects.requireNonNull(css));
    }

    /**
     * Gets a set of all fallback modules.
     *
     * @return a set of fallback modules
     */
    public Set<String> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    /**
     * Gets a set of fallback css data.
     *
     * @return a set of fallback css data
     */
    public Set<CssImportData> getCssImports() {
        return Collections.unmodifiableSet(cssImports);
    }

    /**
     * Css data stored in fallback chunk.
     *
     * @author Vaadin Ltd
     * @see CssImport
     *
     */
    public static class CssImportData implements Serializable {

        private final String value;
        private final String id;
        private final String include;
        private final String themeFor;

        /**
         * Creates a new instance using provided values for css data.
         *
         * @param value
         *            location of the file with the CSS content, not
         *            {@code null}
         * @param id
         *            the 'id' of the new 'dom-module' created, may be
         *            {@code null}
         * @param include
         *            the 'id' of a module to include in the generated
         *            'custom-style', may be {@code null}
         * @param themeFor
         *            the tag name of the themable element that the generated
         *            'dom-module' will target, may be {@code null}
         *
         * @see CssImport
         */
        public CssImportData(String value, String id, String include,
                String themeFor) {
            this.value = Objects.requireNonNull(value);
            this.id = id;
            this.include = include;
            this.themeFor = themeFor;
        }

        public String getValue() {
            return value;
        }

        public String getId() {
            return id;
        }

        public String getInclude() {
            return include;
        }

        public String getThemeFor() {
            return themeFor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, id, include, themeFor);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj.getClass().equals(CssImportData.class)) {
                CssImportData that = (CssImportData) obj;
                return Objects.equals(value, that.value)
                        && Objects.equals(id, that.id)
                        && Objects.equals(include, that.include)
                        && Objects.equals(themeFor, that.themeFor);
            }
            return false;
        }
    }
}
