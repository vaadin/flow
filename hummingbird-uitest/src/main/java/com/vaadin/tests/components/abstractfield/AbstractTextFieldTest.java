package com.vaadin.tests.components.abstractfield;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.vaadin.ui.AbstractTextField;

public abstract class AbstractTextFieldTest<T extends AbstractTextField>
        extends AbstractFieldTest<T> {

    private Command<T, Integer> maxlengthCommand = new Command<T, Integer>() {

        @Override
        public void execute(T c, Integer value, Object data) {
            c.setMaxLength(value);
        }
    };

    private Command<T, String> inputPromptCommand = new Command<T, String>() {
        @Override
        public void execute(T c, String value, Object data) {
            c.setInputPrompt(value);
        }
    };

    private Command<T, Range> selectionRangeCommand = new Command<T, Range>() {
        @Override
        public void execute(T c, Range value, Object data) {
            c.setSelectionRange(value.getStart(),
                    value.getEnd() - value.getStart());

        }
    };
    private Command<T, Object> selectAllCommand = new Command<T, Object>() {
        @Override
        public void execute(T c, Object value, Object data) {
            c.selectAll();
        }
    };

    private Command<T, Integer> setCursorPositionCommand = new Command<T, Integer>() {

        @Override
        public void execute(T c, Integer value, Object data) {
            c.setCursorPosition(value);
        }
    };

    @Override
    protected void createActions() {
        super.createActions();

        createSetTextValueAction(CATEGORY_ACTIONS);

        createMaxLengthAction(CATEGORY_FEATURES);

        createInputPromptAction(CATEGORY_FEATURES);

        createSetTextValueAction(CATEGORY_ACTIONS);
        createCursorPositionAction(CATEGORY_ACTIONS);
        createSelectionRangeAction(CATEGORY_ACTIONS);

    }

    private void createMaxLengthAction(String category) {
        LinkedHashMap<String, Integer> options = createIntegerOptions(100);
        options.put("-", -1);
        createSelectAction("Max length", category, options, "-",
                maxlengthCommand);

    }

    public class Range {
        private int start;
        private int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return start + "-" + end;
        }
    }

    private void createSelectionRangeAction(String category) {
        List<Range> options = new ArrayList<Range>();
        options.add(new Range(0, 10));
        options.add(new Range(0, 1));
        options.add(new Range(0, 2));
        options.add(new Range(1, 2));
        options.add(new Range(2, 5));
        options.add(new Range(5, 10));

        createCategory("Select range", category);

        createClickAction("All", "Select range", selectAllCommand, null);
        for (Range range : options) {
            createClickAction(range.toString(), "Select range",
                    selectionRangeCommand, range);
        }

    }

    private void createCursorPositionAction(String category) {
        String subCategory = "Set cursor position";
        createCategory(subCategory, category);
        for (int i = 0; i < 20; i++) {
            createClickAction(String.valueOf(i), subCategory,
                    setCursorPositionCommand, Integer.valueOf(i));
        }

    }

    private void createInputPromptAction(String category) {
        LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
        options.put("-", null);
        options.put("Enter a value", "Enter a value");
        options.put("- Click here -", "- Click here -");
        createSelectAction("Input prompt", category, options, "-",
                inputPromptCommand);

    }

}
