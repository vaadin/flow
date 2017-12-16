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
package com.vaadin.flow.tutorial.polymer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HtmlImport;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.HasChildView;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.flow.router.View;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("polymer-templates/tutorial-template-components-in-slot.asciidoc")
public class PolymerSlotView {
    @Tag("component-container")
    @HtmlImport("/com/example/ComponentContainer.html")
    public class ComponentContainer extends PolymerTemplate<TemplateModel> {

        public ComponentContainer() {
            Element label = ElementFactory.createLabel("Main layout header");
            getElement().appendChild(label);
        }
    }

    @Tag("main-layout")
    @HtmlImport("/com/example/MainLayout.html")
    public class MainLayout extends PolymerTemplate<TemplateModel>
            implements HasChildView {

        private View childView;

        @Override
        public void setChildView(View childView) {
            if (this.childView != null) {
                getElement().removeChild(this.childView.getElement());
            }
            getElement().appendChild(childView.getElement());
            this.childView = childView;
        }
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

    private class HomeView extends Component implements View {
    }

    private class CompanyView extends Component implements View {
    }
}
