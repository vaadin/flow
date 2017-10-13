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
package com.vaadin.flow.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.demo.ComponentDemo.DemoCategory;
import com.vaadin.flow.demo.MainLayout.MainLayoutModel;
import com.vaadin.flow.demo.model.DemoObject;
import com.vaadin.flow.demo.views.DemoView;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.OptionalParameter;
import com.vaadin.router.Route;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.common.StyleSheet;
import com.vaadin.ui.polymertemplate.PolymerTemplate;
import com.vaadin.util.ReflectTools;

/**
 * Main layout of the application. It contains the menu, header and the main
 * section of the page.
 */
@Route("")
@Tag("main-layout")
@HtmlImport("src/main-layout.html")
@JavaScript("src/script/prism.js")
@StyleSheet("src/css/prism.css")
public class MainLayout extends PolymerTemplate<MainLayoutModel>
        implements HasUrlParameter<String> {

    private Component selectedView;

    /**
     * The model of the layout, allowing to set the properties needed for the
     * layout to function.
     */
    public interface MainLayoutModel extends TemplateModel {
        /**
         * Sets the selected page, making the selection on the menu to appear.
         *
         * @param page
         *            The name selected page in the selection menu.
         */
        void setPage(String page);

        /**
         * Sets the options on the menu under Vaadin Components.
         *
         * @param selectors
         *            The selectable Vaadin component demos in the page menu.
         */
        void setVaadinComponentSelectors(List<DemoObject> selectors);

        /**
         * Sets the options on the menu under Paper Components.
         *
         * @param selectors
         *            The selectable Paper component demos in the page menu.
         */
        void setPaperComponentSelectors(List<DemoObject> selectors);
    }

    /**
     * Default constructor of the layout.
     */
    public MainLayout() {
        List<DemoObject> vaadinComponentSelectors = new ArrayList<>();
        List<DemoObject> paperComponentSelectors = new ArrayList<>();

        for (Class<? extends DemoView> view : ComponentDemoRegister
                .getAvailableViews()) {
            ComponentDemo annotation = view.getAnnotation(ComponentDemo.class);
            if (annotation.category() == DemoCategory.VAADIN) {
                vaadinComponentSelectors.add(new DemoObject(annotation));
            } else if (annotation.category() == DemoCategory.PAPER) {
                paperComponentSelectors.add(new DemoObject(annotation));
            }
        }

        Collections.sort(vaadinComponentSelectors, this::compareDemos);
        Collections.sort(paperComponentSelectors, this::compareDemos);

        getModel().setVaadinComponentSelectors(vaadinComponentSelectors);
        getModel().setPaperComponentSelectors(paperComponentSelectors);
    }

    private int compareDemos(DemoObject demo1, DemoObject demo2) {
        if (demo1.getSubcategory().equals(demo2.getSubcategory())) {
            return demo1.getName().compareToIgnoreCase(demo2.getName());
        }
        return demo1.getSubcategory()
                .compareToIgnoreCase(demo2.getSubcategory());
    }

    @Override
    public void setParameter(BeforeNavigationEvent event,
            @OptionalParameter String parameter) {

        if (parameter == null) {
            return;
        } else {

            if (selectedView != null) {
                if (Objects.equals(
                        selectedView.getClass()
                                .getAnnotation(ComponentDemo.class).href(),
                        parameter)) {
                    return;
                }
                selectedView.getElement().removeFromParent();
            }

            ComponentDemoRegister.getViewFor(parameter)
                    .ifPresent(this::addComponentToView);
        }
    }

    private void addComponentToView(Class<? extends DemoView> view) {
        selectedView = ReflectTools.createInstance(view);
        getElement().appendChild(selectedView.getElement());

        if (DemoView.class.isAssignableFrom(view)) {
            getModel().setPage(view.getAnnotation(ComponentDemo.class).name());
        }
    }

}
