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
package org.vaadin.jandex;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvokers;

@Tag("hello-world-jandex")
@NpmPackage(value = "@axa-ch/input-text", version = "6.1.0")
@JsModule("./src/hello-world-jandex.ts")
public class HelloWorldJandex extends Component {

    /**
     * A definition of the add-on, which the application finds in the Jandex
     * index that the add-on ships.
     */
    @JsDefinition
    public interface GreetingJs extends JsInvokers {

        @JsExpression("return 'Hello from ' + this.localName + ', ' + $0")
        PendingJavaScriptResult greet(String name);
    }

    /**
     * Creates the hello world template.
     */
    public HelloWorldJandex() {
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
