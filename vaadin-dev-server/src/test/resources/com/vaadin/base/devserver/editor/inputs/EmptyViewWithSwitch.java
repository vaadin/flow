package com.vaadin.base.devserver.editor.inputs;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

@Route(value = "empty")
public class EmptyView extends HorizontalLayout {

    public EmptyView() {
    }

    private String testSwitch(String value) {
        return switch (value) {
            case "test" -> "test";
            case "prod" -> "Production";
            default -> "N/A";
        };
    }
}