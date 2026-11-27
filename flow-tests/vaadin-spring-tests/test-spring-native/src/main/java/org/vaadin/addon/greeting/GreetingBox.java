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
package org.vaadin.addon.greeting;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

/**
 * Stands for a component of an add-on. Its package is outside the packages of
 * the application and outside {@code com.vaadin}, as the package of an add-on
 * from another vendor is.
 */
public class GreetingBox extends Span {

    @JsDefinition
    public interface GreetingJs extends JsInvokers {

        @JsExpression("return 'Hello from ' + this.localName + ', ' + $0")
        PendingJavaScriptResult greet(String name);
    }

    /**
     * Greets the given name in the browser.
     *
     * @param name
     *            the name to greet
     * @return the greeting
     */
    public PendingJavaScriptResult greet(String name) {
        return getElement().executeJs(GreetingJs.class).greet(name);
    }
}
