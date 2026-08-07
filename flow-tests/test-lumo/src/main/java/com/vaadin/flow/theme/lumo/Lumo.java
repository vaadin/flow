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
package com.vaadin.flow.theme.lumo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.uitest.ui.dependencies.TestVersion;

/**
 * Lumo theme stub used by tests that rely on Flow's classpath detection of
 * {@code com.vaadin.flow.theme.lumo.Lumo} as the default theme (see
 * {@code AbstractDependenciesScanner.LUMO}). Provides the {@code LIGHT} /
 * {@code DARK} variants and the standard
 * {@link com.vaadin.flow.theme.AbstractTheme} contract without pulling in any
 * Vaadin Lumo npm packages — tests that need real Lumo styling must declare
 * those deps themselves.
 * <p>
 * Because {@code flow-test-lumo} is the shared dependency of every theme test
 * module, it also declares {@code @vaadin/vaadin-themable-mixin} here. Flow's
 * per-component theme CSS feature (a {@code theme/components/<tag>.css} file)
 * generates an import of {@code @vaadin/vaadin-themable-mixin/register-styles},
 * so the mixin must be installed even when no real Vaadin web component is
 * present. Declaring it centrally keeps those modules free of real components
 * while still resolving that import.
 */
@NpmPackage(value = "@vaadin/vaadin-themable-mixin", version = TestVersion.VAADIN)
public class Lumo implements AbstractTheme {

    public static final String LIGHT = "light";
    public static final String DARK = "dark";

    @Override
    public String getBaseUrl() {
        return "src/";
    }

    @Override
    public String getThemeUrl() {
        return "theme/lumo/";
    }

    @Override
    public List<String> getHeaderInlineContents() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getHtmlAttributes(String variant) {
        if (variant.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> attributes = new HashMap<>(1);
        switch (variant) {
        case LIGHT:
            attributes.put("theme", LIGHT);
            break;
        case DARK:
            attributes.put("theme", DARK);
            break;
        default:
            LoggerFactory.getLogger(Lumo.class.getName()).warn(
                    "Lumo theme variant not recognized: '{}'. Using no variant.",
                    variant);
        }
        return attributes;
    }
}
