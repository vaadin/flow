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

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Tag;
import com.vaadin.components.paper.button.PaperButton;
import com.vaadin.components.paper.dialog.PaperDialog;
import com.vaadin.flow.demo.MainLayout.MainLayoutModel;
import com.vaadin.flow.demo.model.DemoObject;
import com.vaadin.flow.demo.views.DemoView;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.HasChildView;
import com.vaadin.flow.router.View;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.ui.UI;

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

    private PaperDialog sourceDialog;
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
         * Sets the options on the menu.
         * 
         * @param selectors
         *            The selectable demos in the page menu.
         */
        void setSelectors(List<DemoObject> selectors);
    }

    /**
     * Default constructor of the layout.
     */
    public MainLayout() {
        List<DemoObject> selectors = new ArrayList<>();
        for (Class<? extends DemoView> view : ComponentDemoRegister
                .getAvailableViews()) {
            selectors.add(
                    new DemoObject(view.getAnnotation(ComponentDemo.class)));
        }
        getModel().setSelectors(selectors);
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

    /**
     * Opens a dialog with a source code relevant to the current view.
     */
    @EventHandler
    public void showSource() {
        prepareDialog();

        SourceContent content = new SourceContent();
        if (selectedView instanceof DemoView) {
            ((DemoView) selectedView).populateSources(content);
        } else {
            content.setText("No source content available for non demo view");
        }

        Element element = sourceDialog.getElement();
        element.appendChild(ElementFactory.createHeading2(
                selectedView.getClass().getAnnotation(ComponentDemo.class)
                        .name() + " sources"));
        element.appendChild(content.getElement());

        sourceDialog.setOpened(true);

        PaperButton closeButton = new PaperButton();
        closeButton.getElement().setText("Close");
        closeButton.getElement().getStyle().set("float", "right");
        closeButton.setRaised(true);
        closeButton.getElement().addEventListener("click",
                event -> sourceDialog.setOpened(false));

        sourceDialog.getElement().appendChild(closeButton.getElement());

        UI.getCurrent().getPage().executeJavaScript("Prism.highlightAll();");
    }

    private void prepareDialog() {
        if (sourceDialog == null) {
            sourceDialog = new PaperDialog();
            sourceDialog.setModal(true);

            getUI().get().getElement().appendChild(sourceDialog.getElement());
        }

        sourceDialog.getElement().removeAllChildren();
    }
}
