/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.noroot;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinServlet;

@Route("")
@PWA(name = "testView", shortName = "tw")
@HtmlImport("frontend://bower_components/polymer/polymer.html")
public class NoRootTestView extends Div {
    static final String TEST_VIEW_ID = "testView";

    public NoRootTestView() {
        setId(TEST_VIEW_ID);
        getElement().executeJavaScript(String.format(
                "document.getElementById('%s').textContent = 'Polymer version: ' + Polymer.version",
                TEST_VIEW_ID));
    }

    @WebServlet(name = "customMappingServlet", urlPatterns = "/custom/*")
    public static class CustomMappingServlet extends VaadinServlet {
    }
}
