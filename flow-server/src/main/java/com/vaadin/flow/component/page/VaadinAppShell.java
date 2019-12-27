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
package com.vaadin.flow.component.page;

import java.io.Serializable;

import com.vaadin.flow.server.AppShellSettings;

/**
 * A marker class to configure the index.hml page when using client-side
 * bootstrapping. This class supports the following annotations that affect the
 * generated index.html page:
 *
 * <ul>
 * <li>{@link Meta}: appends an HTML {@code <meta>} tag to the bottom of the
 * {@code <head>} element</li>
 * </ul>
 *
 * <p>
 * There should be at max one class extending {@link VaadinAppShell} in the
 * application.
 * </p>
 *
 * <p>
 * NOTE: the application shell class is the only valid target for the page
 * configuration annotations listed above. The application would fail to start
 * if any of these annotations is wrongly placed on a class other than the
 * application shell class.
 * <p>
 *
 * <code>
 * &#64;Meta(name = "Author", content = "Donald Duck")
 * public class AppShell implements VaadinAppShell {
 * }
 * </code>
 *
 * @since 3.0
 */
public interface VaadinAppShell extends Serializable {

    /**
     * Configure the initial page settings when called.
     *
     * @param settings
     *            initial page settings
     */
    default void configurePage(AppShellSettings settings) {
    };
}
