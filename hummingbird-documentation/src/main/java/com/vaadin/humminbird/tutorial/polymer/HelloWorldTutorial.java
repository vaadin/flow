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

import com.vaadin.annotations.EventData;
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
         * Sets the text to show in the template.
         *
         * @param text
         *            the text to show in the template
         */
        void setText(String text);
    }

    @Tag("hello-world")
    @HtmlImport("/polymer/location/polymer.html")
    @HtmlImport("/com/example/HelloWorld.html")
    public class HelloWorld extends PolymerTemplate<HelloWorldModel> {

        /**
         * Creates the hello world template.
         */
        public HelloWorld() {
            setId("template");
        }

        @EventHandler
        private void sayHello(@EventData("event.hello") String inputValue) {
            // Called from the template click handler
            getModel().setText(inputValue);
        }
    }

    public void useTemplate() {
        HelloWorld hello = new HelloWorld();

        Div layout = new Div();
        layout.add(hello);
    }
}
