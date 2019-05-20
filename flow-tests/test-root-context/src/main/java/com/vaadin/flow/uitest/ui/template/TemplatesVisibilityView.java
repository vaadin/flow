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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.VirtualChildrenList;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route(value = "com.vaadin.flow.uitest.ui.template.TemplatesVisibilityView", layout = ViewTestLayout.class)
public class TemplatesVisibilityView extends AbstractDivView {

    public TemplatesVisibilityView() {
        JsGrandParentView grandParent = new JsGrandParentView();
        grandParent.setId("grand-parent");

        grandParent.setVisible(false);

        add(grandParent);

        add(createButton("Change grand parent visibility",
                "grand-parent-visibility",
                event -> grandParent.setVisible(!grandParent.isVisible())));

        StateNode subTemplateChild = grandParent.getElement().getNode()
                .getFeature(VirtualChildrenList.class).iterator().next();

        JsSubTemplate subTemplate = (JsSubTemplate) Element
                .get(subTemplateChild).getComponent().get();

        add(createButton("Change sub template visibility",
                "sub-template-visibility",
                event -> subTemplate.setVisible(!subTemplate.isVisible())));

        StateNode grandChildNode = subTemplate.getElement().getNode()
                .getFeature(VirtualChildrenList.class).iterator().next();

        JsInjectedGrandChild grandChild = (JsInjectedGrandChild) Element
                .get(grandChildNode).getComponent().get();

        add(createButton("Change grand child visibility",
                "grand-child-visibility",
                event -> grandChild.setVisible(!grandChild.isVisible())));

        add(createButton("Update sub template property via client side",
                "client-side-update-property",
                event -> grandParent.updateChildViaClientSide()));
    }

}
