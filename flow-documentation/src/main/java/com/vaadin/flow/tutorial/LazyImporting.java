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
package com.vaadin.flow.tutorial;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.LoadMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

@CodeFor("tutorial-ways-of-importing.asciidoc")
public class LazyImporting {

    @Tag("div")
    @HtmlImport("/html/layout.html") // same as @HtmlImport("/html/layout.html", loadMode = LoadMode.EAGER)
    @StyleSheet(value = "/css/big_style_file.css", loadMode = LoadMode.INLINE)
    @JavaScript(value = "/js/animation.js", loadMode = LoadMode.LAZY)
    public class MainLayout extends Component {
        // implementation omitted
    }

    public class MyCustomUI extends UI {

        @Override
        protected void init(VaadinRequest request) {
            getPage().addHtmlImport("/html/layout.html", LoadMode.EAGER);
            getPage().addStyleSheet("/css/big_style_file.css", LoadMode.INLINE);
            getPage().addJavaScript("/js/animation.js", LoadMode.LAZY);
        }
    }
}
