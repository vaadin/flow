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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.StyleSheet;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("application-structure/tutorial-include-css.asciidoc")
public class IncludeCss {

    //@formatter:off - custom line wrapping
    @StyleSheet("styles.css") // Relative to Servlet URL
    @StyleSheet("http://www.example.com/example.css") // Loaded from external location
    public class MainLayout extends Component {
        // implementation omitted
    }
    //@formatter:on

    public class MyCustomUI extends UI {

        @Override
        protected void init(VaadinRequest request) {
            //@formatter:off - custom line wrapping

            // Loaded from "/root.css" regardless of how your application is deployed
            getPage().addStyleSheet("/root.css");

            //@formatter:on
        }
    }
}
