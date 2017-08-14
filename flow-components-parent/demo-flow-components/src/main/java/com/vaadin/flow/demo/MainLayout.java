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
import java.util.List;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.demo.ComponentDemo.DemoCategory;
import com.vaadin.flow.demo.MainLayout.MainLayoutModel;
import com.vaadin.flow.demo.model.DemoObject;
import com.vaadin.flow.demo.views.DemoView;
import com.vaadin.flow.router.HasChildView;
import com.vaadin.flow.router.View;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;

/**
 * Main layout of the application. It contains the menu, header and the main
 * section of the page.
 */
@Tag("main-layout")
@HtmlImport("frontend://src/main-layout.html")
@JavaScript("frontend://src/script/prism.js")
@StyleSheet("frontend://src/css/prism.css")
public class MainLayout extends PolymerTemplate<MainLayoutModel>
        implements HasChildView {

    private View selectedView;

    /**
     * The model of the layout, allowing to set the properties needed for the
     * layout to function.
     */
    public interface MainLayoutModel extends TemplateModel {
        /**
         * Sets the selected page, making the selection on the mnu to appear.
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
        getModel().setVaadinComponentSelectors(vaadinComponentSelectors);
        getModel().setPaperComponentSelectors(paperComponentSelectors);
    }

    @Override
    public void setChildView(View childView) {
        if (selectedView == childView) {
            return;
        }
        if (selectedView != null) {
            selectedView.getElement().removeFromParent();
        }
        selectedView = childView;

        // uses the <slot> at the template
        getElement().appendChild(childView.getElement());
        if (childView instanceof DemoView) {
            getModel().setPage(childView.getClass()
                    .getAnnotation(ComponentDemo.class).name());
        }
    }
}
