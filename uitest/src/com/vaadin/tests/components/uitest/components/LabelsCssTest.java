package com.vaadin.tests.components.uitest.components;

import com.vaadin.server.ThemeResource;
import com.vaadin.tests.components.uitest.TestSampler;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

public class LabelsCssTest extends GridLayout {

    private TestSampler parent;
    private int debugIdCounter = 0;

    public LabelsCssTest(TestSampler parent) {
        this.parent = parent;
        setSpacing(true);
        setWidth("100%");
        setColumns(5);

        createLabelWith(null, "Default empty label", null, null);
        createLabelWith(null, "Label with icon", null, parent.ICON_URL);
        Label l = createLabelWith("The caption", "With caption and tooltip", null, null);
        l.setDescription("The tooltip");

        createLabelWith("H1", ValoTheme.LABEL_H1);
        createLabelWith("H2", ValoTheme.LABEL_H2);
        createLabelWith("H3", ValoTheme.LABEL_H3);
        createLabelWith("H4", ValoTheme.LABEL_H4);
        createLabelWith("Big", ValoTheme.LABEL_LARGE);
        createLabelWith("Small", ValoTheme.LABEL_SMALL);
        createLabelWith("Tiny", ValoTheme.LABEL_TINY);
        createLabelWith("Color", ValoTheme.LABEL_COLORED);
        createLabelWith("Failure", ValoTheme.LABEL_FAILURE);
        // Will break test bench as the spinner spins and it's not identical in
        // all screen shots
        // createLabelWith("Loading", ValoTheme.LABEL_LOADING);
        createLabelWith("Big", ValoTheme.LABEL_LARGE);
        createLabelWith("Big", ValoTheme.LABEL_LARGE);

    }

    private Label createLabelWith(String content, String primaryStyleName) {
        return createLabelWith(null, content, primaryStyleName, null);
    }

    private Label createLabelWith(String caption, String content, String primaryStyleName, String iconUrl) {

        Label l = new Label();
        l.setId("label" + debugIdCounter++);
        if (caption != null) {
            l.setCaption(caption);
        }

        if (content != null) {
            l.setValue(content);
        }

        if (primaryStyleName != null) {
            l.addStyleName(primaryStyleName);
        }

        if (iconUrl != null) {
            l.setIcon(new ThemeResource(iconUrl));
        }

        addComponent(l);
        return l;

    }

    @Override
    public void addComponent(Component component) {
        parent.registerComponent(component);
        super.addComponent(component);
    }

}
