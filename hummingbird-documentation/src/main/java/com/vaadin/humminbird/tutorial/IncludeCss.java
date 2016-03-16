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
package com.vaadin.humminbird.tutorial;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

/**
 * Tutorial code related to tutorial-include-css.asciidoc.
 */
public class IncludeCss extends UI {

    @Override
    protected void init(VaadinRequest request) {
        // Loaded from "styles.css" in our context root
        getPage().addStyleSheet("styles.css");

        // Loaded from "/root.css" regardless of how our application is deployed
        getPage().addStyleSheet("/root.css");

        // Loaded from "http://example.com/example.css" regardless of where our
        // application is deployed
        getPage().addStyleSheet("http://example.com/example.css");
    }

}
