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

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Route;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.event.BeforeNavigationEvent;
import com.vaadin.flow.router.event.BeforeNavigationListener;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("tutorial-routing-lifecycle.asciidoc")
public class NavigationEvents {

    @Route("")
    @Tag("main-layout")
    @HtmlImport("frontend://com/example//MainLayout.html")
    public class MainLayout extends PolymerTemplate<TemplateModel> {

        public MainLayout() {
            SideElement side = new SideElement();
            getElement().appendChild(side.getElement());
        }
    }

    public class SideElement extends Div implements BeforeNavigationListener {
        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            // Handle for instance before navigation clean up
        }
    }


    @Route("no-items")
    public class NoItemsView extends Div {
        public NoItemsView() {
            setText("No items found.");
        }
    }

    @Route("blog")
    public class BlogList extends Div implements BeforeNavigationListener {
        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            // implementation omitted
            Object record = getItem();

            if (record == null) {
                event.rerouteTo(NoItemsView.class);
            }
        }

        private Object getItem() {
            // no-op implementation
            return null;
        }
    }
}
