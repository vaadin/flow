package com.vaadin.base.devserver.editor.inputs;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

@Route(value = "empty")
public class EmptyView extends HorizontalLayout {

    public EmptyView() {
        VerticalLayout layout = new VerticalLayout();
        add(layout);
    }
}