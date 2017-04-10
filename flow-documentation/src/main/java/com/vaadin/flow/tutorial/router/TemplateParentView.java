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
package com.vaadin.flow.tutorial.router;

import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.flow.tutorial.routing.Routing.CompanyView;
import com.vaadin.flow.tutorial.routing.Routing.HomeView;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.ui.AngularTemplate;

@Deprecated
@CodeFor("deprecated/tutorial-routing-template-parent.asciidoc")
public class TemplateParentView {
    public class MainLayout extends AngularTemplate {
        // Nothing needed here
    }

    public class MyRouterConfigurator implements RouterConfigurator {
        @Override
        public void configure(RouterConfiguration configuration) {
            //@formatter:off - custom line wrapping
            configuration.setRoute("", HomeView.class, MainLayout.class);
            configuration.setRoute("company", CompanyView.class, MainLayout.class);
            //@formatter:on
        }
    }
}
