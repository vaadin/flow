/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.flow.tutorial;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HtmlImport;
import com.vaadin.flow.component.JavaScript;
import com.vaadin.flow.component.StyleSheet;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("application-structure/tutorial-importing.asciidoc")
public class Importing {

    //@formatter:off - custom line wrapping
    @Tag("div")
    @JavaScript("/js/script.js")
    @HtmlImport("/html/htmlimport.html")
    static class HtmlComponent extends Component implements HasText {
        // implementation omitted
    }
    //@formatter:on

    public class MyCustomUI extends UI {

        @Override
        protected void init(VaadinRequest request) {
            //@formatter:off - custom line wrapping

            // Loaded regardless of how your application is deployed
            getPage().addHtmlImport("/html/htmlimport.html");
            getPage().addJavaScript("/js/script.js");

            //@formatter:on
        }
    }

    @JavaScript("1.js")
    @StyleSheet("1.css")
    @HtmlImport("1.html")
    @JavaScript("2.js")
    @StyleSheet("2.css")
    @HtmlImport("2.html")
    static class OrderedDependencies extends UI {
    }
}
