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
import java.util.Set;

/**
 * Fall back chunk info.
 *
 * @author Vaadin Ltd
 *
 */
public class FallbackChunk implements Serializable {

    private final Set<String> modules;
    private final Set<CssImportData> cssImports;

    public FallbackChunk(Collection<String> modules,
            Collection<CssImportData> css) {
        this.modules = new HashSet<>(modules);
        this.cssImports = new HashSet<>(css);
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

    public static class CssImportData implements Serializable {

        private final String value;
        private final String id;
        private final String include;
        private final String themeFor;

        public CssImportData(String value, String id, String include,
                String themeFor) {
            this.value = value;
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
    }
}
