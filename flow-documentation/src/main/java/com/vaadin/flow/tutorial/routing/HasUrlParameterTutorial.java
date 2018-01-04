/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tutorial.routing;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("routing/tutorial-router-url-parameters.asciidoc")
public class HasUrlParameterTutorial {

    @Route(value = "greet")
    public class GreetingComponent extends Div
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            setText(String.format("Hello, %s!", parameter));
        }
    }

    @Route("greet")
    public class OptionalGreeting extends Div
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event,
                @OptionalParameter String parameter) {
            if (parameter == null) {
                setText("Welcome anonymous.");
            } else {
                setText(String.format("Welcome %s.", parameter));
            }
        }
    }

    @Route("greet")
    public class WildcardGreeting extends Div
            implements HasUrlParameter<String> {

        @Override
        public void setParameter(BeforeEvent event,
                @WildcardParameter String parameter) {
            if (parameter.isEmpty()) {
                setText("Welcome anonymous.");
            } else {
                setText(String.format("Handling parameter %s.", parameter));
            }
        }
    }
}
