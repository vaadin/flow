/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplatesVisibilityView", layout = ViewTestLayout.class)
public class TemplatesVisibilityView extends Div {

    public TemplatesVisibilityView() {
        JsGrandParentView grandParent = new JsGrandParentView();
        grandParent.setId("grand-parent");

        grandParent.setVisible(false);

        NativeButton grandParentVisibility = new NativeButton(
                "Change grand parent visibility",
                event -> grandParent.setVisible(!grandParent.isVisible()));
        grandParentVisibility.setId("grand-parent-visibility");
        add(grandParent, grandParentVisibility);

        StateNode subTemplateChild = grandParent.getElement().getNode()
                .getFeature(VirtualChildrenList.class).iterator().next();

        JsSubTemplate subTemplate = (JsSubTemplate) Element
                .get(subTemplateChild).getComponent().get();

        NativeButton subTemplateVisibility = new NativeButton(
                "Change sub template visibility",
                event -> subTemplate.setVisible(!subTemplate.isVisible()));
        subTemplateVisibility.setId("sub-template-visibility");
        add(subTemplateVisibility);

        StateNode grandChildNode = subTemplate.getElement().getNode()
                .getFeature(VirtualChildrenList.class).iterator().next();

        JsInjectedGrandChild grandChild = (JsInjectedGrandChild) Element
                .get(grandChildNode).getComponent().get();

        NativeButton grandChildVisibility = new NativeButton(
                "Change grand child visibility",
                event -> grandChild.setVisible(!grandChild.isVisible()));
        grandChildVisibility.setId("grand-child-visibility");
        add(grandChildVisibility);

        NativeButton updateSubTemplateViaClientSide = new NativeButton(
                "Update sub template property via client side",
                event -> grandParent.updateChildViaClientSide());
        updateSubTemplateViaClientSide.setId("client-side-update-property");
        add(updateSubTemplateViaClientSide);
    }

}
