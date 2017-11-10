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

import com.vaadin.flow.demo.ComponentDemo.DemoCategory;
import com.vaadin.flow.demo.MainLayout.MainLayoutModel;
import com.vaadin.flow.demo.model.DemoObject;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.router.Route;
import com.vaadin.router.RouterLayout;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HasElement;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JavaScript;
import com.vaadin.ui.common.StyleSheet;
import com.vaadin.ui.polymertemplate.PolymerTemplate;

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
        implements RouterLayout {

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
                vaadinComponentSelectors.add(new DemoObject(view));
            } else if (annotation.category() == DemoCategory.PAPER) {
                paperComponentSelectors.add(new DemoObject(view));
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
    public void setRouterLayoutContent(HasElement content) {
        RouterLayout.super.setRouterLayoutContent(content);
        Component component = content.getElement().getComponent().get();
        if (DemoView.class.isAssignableFrom(component.getClass())) {
            getModel().setPage(component.getClass()
                    .getAnnotation(ComponentDemo.class).name());
        }
    }

}
