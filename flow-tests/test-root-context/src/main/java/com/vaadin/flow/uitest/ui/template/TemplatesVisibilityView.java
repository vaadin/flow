/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
