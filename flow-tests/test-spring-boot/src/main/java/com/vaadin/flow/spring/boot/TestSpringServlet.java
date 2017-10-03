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
package com.vaadin.flow.spring.boot;

import com.vaadin.router.Route;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletConfiguration;
import com.vaadin.ui.html.Div;

/**
 * @author Vaadin Ltd
 *
 */
@VaadinServletConfiguration(productionMode = false, usingNewRouting = true)
public class TestSpringServlet extends VaadinServlet {

    @Route("")
    public static class RootNavigationTarget extends Div {

        public RootNavigationTarget() {
            setText("root");
        }
    }

}
