/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.vaadin.base.devserver.themeeditor.ThemeModifier;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.testutil.ChromeDeviceTest;
import com.vaadin.flow.testutil.TestUtils;

public abstract class AbstractThemeEditorIT extends ChromeDeviceTest {
    private static VaadinContext mockContext = new VaadinContext() {

        Map<String, Object> attributes = new HashMap<>();

        @Override
        public <T> T getAttribute(Class<T> type,
                Supplier<T> defaultValueSupplier) {
            Object result = attributes.get(type.getName());
            if (result == null && defaultValueSupplier != null) {
                result = defaultValueSupplier.get();
                attributes.put(type.getName(), result);
            }
            return type.cast(result);
        }

        @Override
        public <T> void setAttribute(Class<T> clazz, T value) {
            attributes.put(clazz.getName(), value);
        }

        @Override
        public void removeAttribute(Class<?> clazz) {
            attributes.remove(clazz.getName());
        }

        @Override
        public Enumeration<String> getContextParameterNames() {
            return Collections.enumeration(Collections.emptyList());
        }

        @Override
        public String getContextParameter(String name) {
            return null;
        }
    };

    @Override
    protected String getTestPath() {
        return "/context" + super.getTestPath();
    }

    protected void open() {
        open((String[]) null);
    }

    private static File getProjectFolder() {
        File currentFolder = TestUtils.getTestFolder("com");
        while (!new File(currentFolder, FrontendUtils.FRONTEND).exists()) {
            currentFolder = currentFolder.getParentFile();
        }
        return currentFolder;
    }

    protected static class TestThemeModifier extends ThemeModifier {

        public TestThemeModifier() {
            super(mockContext);
        }

        @Override
        protected File getFrontendFolder() {
            return new File(getProjectFolder(), FrontendUtils.FRONTEND);
        }

        public File getStyleSheetFileWithoutSideEffects() {
            return new File(getThemeFile(), getCssFileName());
        }
    }

    protected static class TestAbstractConfiguration
            implements AbstractConfiguration {

        @Override
        public boolean isProductionMode() {
            return false;
        }

        @Override
        public String getStringProperty(String name, String defaultValue) {
            return null;
        }

        @Override
        public boolean getBooleanProperty(String name, boolean defaultValue) {
            return false;
        }

        @Override
        public File getJavaSourceFolder() {
            File projectFolder = getProjectFolder();
            Path pathToJavaSourceFolder = projectFolder.toPath()
                    .resolve(Paths.get("src", "main", "java")).normalize();
            return pathToJavaSourceFolder.toAbsolutePath().toFile();
        }

    }
}
