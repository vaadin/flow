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

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinAppShellSettings;

/**
 * A marker interface to configure the index.hml page when using client-side
 * bootstrapping. This class supports the following annotations that affect the
 * generated index.html page (also known as 'application shell'):
 *
 * <ul>
 * <li>{@link Meta}: appends an HTML {@code <meta>} tag to the bottom of the
 * {@code <head>} element</li>
 * <li>{@link Inline}: inlines a static content in any point of the
 * document</li>
 * <li>{@link Viewport}: defines the viewport tag of the page</li>
 * <li>{@link BodySize}: configures the size of the body</li>
 * <li>{@link PageTitle}: establishes the page title</li>
 * <li>{@link PWA}: Defines application PWA properties</li>
 * </ul>
 * <p>
 * There is a single application shell for the entire Vaadin application, and
 * therefore there can be at max one class implementing {@link VaadinAppShell}.
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
 * &#64;PWA(name = "My Fun Application", shortName = "fun-app")
 * &#64;Inline("my-custom-javascript.js")
 * &#64;Viewport("width=device-width, initial-scale=1")
 * &#64;BodySize(height = "100vh", width = "100vw")
 * &#64;PageTitle("my-title")
 * public class AppShell implements VaadinAppShell {
 * }
 * </code>
 *
 * @since 3.0
 */
public interface VaadinAppShell extends Serializable {

    /**
     * Configure the initial application shell settings when called.
     *
     * @param settings
     *            initial application shell settings
     */
    default void configurePage(VaadinAppShellSettings settings) {
    }
}
