/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.testutil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Base class for TestBench tests which use a UI/View matched to the test name
 * according to the convention (remove {@code IT}, add {@code View} or
 * {@code UI}).
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ViewOrUITest extends AbstractTestBenchTest {

    @Override
    protected String getTestPath() {
        Class<? extends Component> viewClass = getViewClass();
        try {
            if (viewClass != null) {
                return "/view/"
                        + URLEncoder.encode(viewClass.getName(), UTF_8.name());
            }

            Class<? extends UI> uiClass = getUIClass();
            if (uiClass != null) {
                return "/run/"
                        + URLEncoder.encode(uiClass.getName(), UTF_8.name());
            }

        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException(
                "Could not find a View or UI class for the test. Ensure "
                        + getClass().getName().replaceFirst("IT$", "")
                        + "View/UI exists "
                        + " or override either getTestPath() or getViewClass()/getUIClass() in your test");

    }

    /**
     * Returns the View class the current test is connected to.
     * <p>
     * Uses name matching and replaces "IT" with "View"
     *
     * @return the View class the current test is connected to or null if no
     *         View class was found
     */
    @SuppressWarnings("unchecked")
    protected Class<? extends Component> getViewClass() {
        return (Class<? extends Component>) findClass(Component.class,
                getClass().getName().replaceFirst("IT$", "View"));
    }

    /**
     * Returns the UI class the current test is connected to.
     * <p>
     * Uses name matching and replaces "IT" with "UI"
     *
     * @return the UI class the current test is connected to or null if no UI
     *         class was found
     */
    @SuppressWarnings("unchecked")
    protected Class<? extends UI> getUIClass() {
        return (Class<? extends UI>) findClass(UI.class,
                getClass().getName().replaceFirst("IT$", "UI"));
    }

    private Class<?> findClass(Class<?> typeToFind, String classNameToFind) {

        try {
            Class<?> cls = Class.forName(classNameToFind);
            if (typeToFind.isAssignableFrom(cls)) {
                return cls;
            }
        } catch (Exception e) {
            // Here only to please Sonar...
            getLogger().debug("{} for {} not found", typeToFind.getSimpleName(),
                    getClass().getName());
        }
        return null;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(ViewOrUITest.class.getName());
    }

}
