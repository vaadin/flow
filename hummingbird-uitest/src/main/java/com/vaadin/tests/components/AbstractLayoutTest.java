package com.vaadin.tests.components;

import java.util.LinkedHashMap;

import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentContainer;

public abstract class AbstractLayoutTest<T extends AbstractComponentContainer>
        extends AbstractComponentContainerTest<T> {

    protected static final String CATEGORY_LAYOUT_FEATURES = "Layout features";
    private Command<T, Boolean> marginCommand = new Command<T, Boolean>() {

        @Override
        public void execute(T c, Boolean value, Object data) {
            ((ComponentContainer.MarginHandler) c).setMargin(value);

        }
    };

    protected Command<T, Boolean> spacingCommand = new Command<T, Boolean>() {
        @Override
        public void execute(T c, Boolean value, Object data) {
            ((ComponentContainer.SpacingHandler) c).setSpacing(value);
        }
    };

    private Command<T, Integer> setComponentAlignment = new Command<T, Integer>() {

        @Override
        public void execute(T c, Integer value, Object alignment) {
            Component child = getComponentAtIndex(c, value);
            ((ComponentContainer.AlignmentHandler) c)
                    .setComponentAlignment(child, (Alignment) alignment);
        }
    };

    @Override
    protected void createActions() {
        super.createActions();
        if (ComponentContainer.MarginHandler.class
                .isAssignableFrom(getTestClass())) {
            createMarginsSelect(CATEGORY_LAYOUT_FEATURES);
        }
        if (ComponentContainer.SpacingHandler.class
                .isAssignableFrom(getTestClass())) {
            createSpacingSelect(CATEGORY_LAYOUT_FEATURES);
        }
        if (ComponentContainer.AlignmentHandler.class
                .isAssignableFrom(getTestClass())) {
            createChangeComponentAlignmentAction(CATEGORY_LAYOUT_FEATURES);
        }

    }

    private void createMarginsSelect(String category) {
        LinkedHashMap<String, Boolean> options = new LinkedHashMap<String, Boolean>();
        options.put("off", false);
        options.put("all", true);

        createSelectAction("Margins", category, options, "off", marginCommand);
    }

    private void createSpacingSelect(String category) {
        createBooleanAction("Spacing", category, false, spacingCommand);
    }

    private void createChangeComponentAlignmentAction(String category) {
        String alignmentCategory = "Component alignment";
        createCategory(alignmentCategory, category);

        LinkedHashMap<String, Alignment> options = new LinkedHashMap<String, Alignment>();
        options.put("Top left", Alignment.TOP_LEFT);
        options.put("Top center", Alignment.TOP_CENTER);
        options.put("Top right", Alignment.TOP_RIGHT);

        options.put("Middle left", Alignment.MIDDLE_LEFT);
        options.put("Middle center", Alignment.MIDDLE_CENTER);
        options.put("Middle right", Alignment.MIDDLE_RIGHT);

        options.put("Bottom left", Alignment.BOTTOM_LEFT);
        options.put("Bottom center", Alignment.BOTTOM_CENTER);
        options.put("Bottom right", Alignment.BOTTOM_RIGHT);

        for (int i = 0; i < 20; i++) {
            String componentAlignmentCategory = "Component " + i + " alignment";
            createCategory(componentAlignmentCategory, alignmentCategory);

            for (String option : options.keySet()) {
                createClickAction(option, componentAlignmentCategory,
                        setComponentAlignment, Integer.valueOf(i),
                        options.get(option));
            }

        }

    }
}
