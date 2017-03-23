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
package com.vaadin.humminbird.tutorial.polymer;

import java.util.Optional;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.humminbird.tutorial.annotations.CodeFor;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.template.PolymerTemplate;
import com.vaadin.hummingbird.template.model.TemplateModel;

@CodeFor("tutorial-template-basic.asciidoc")
public class HelloWorldTutorial {

    /**
     * Model for the template.
     */
    public interface HelloWorldModel extends TemplateModel {
        /**
         * Gets user input from corresponding template page.
         *
         * @return user input string
         */
        String getUserInput();

        /**
         * Sets greeting that is displayed in corresponding template page.
         *
         * @param greeting
         *            greeting string
         */
        void setGreeting(String greeting);
    }

    @Tag("hello-world")
    @HtmlImport("js/bower_components/polymer/polymer.html")
    @HtmlImport("js/bower_components/paper-input/paper-input.html")
    @HtmlImport("components/HelloWorld.html")
    public class HelloWorld extends PolymerTemplate<HelloWorldModel> {
        private static final String EMPTY_NAME_GREETING = "Please enter your name";

        /**
         * Creates the hello world template.
         */
        public HelloWorld() {
            setId("template");
            getModel().setGreeting(EMPTY_NAME_GREETING);
        }

        @EventHandler
        private void sayHello() {
            // Called from the template click handler
            getModel().setGreeting(Optional
                    .ofNullable(getModel().getUserInput())
                    .filter(string -> !string.isEmpty())
                    .map(greeting -> String.format("Hello %s!", greeting))
                    .orElse(EMPTY_NAME_GREETING));
        }
    }

    public void useTemplate() {
        HelloWorld hello = new HelloWorld();

        Div layout = new Div();
        layout.add(hello);
    }
}
