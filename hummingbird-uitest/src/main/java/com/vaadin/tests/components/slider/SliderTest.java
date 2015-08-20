package com.vaadin.tests.components.slider;

import java.util.LinkedHashMap;

import com.vaadin.tests.components.abstractfield.AbstractFieldTest;
import com.vaadin.ui.Slider;

public class SliderTest extends AbstractFieldTest<Slider> {

    private Command<Slider, Double> minCommand = new Command<Slider, Double>() {
        @Override
        public void execute(Slider c, Double value, Object data) {
            c.setMin(value);
        }
    };

    private Command<Slider, Double> maxCommand = new Command<Slider, Double>() {
        @Override
        public void execute(Slider c, Double value, Object data) {
            c.setMax(value);
        }
    };

    private Command<Slider, Double> stepCommand = new Command<Slider, Double>() {
        @Override
        public void execute(Slider c, Double value, Object data) {
            c.setStep(value);
        }
    };

    @Override
    protected Class<Slider> getTestClass() {
        return Slider.class;
    }

    @Override
    protected void createActions() {
        super.createActions();

        createMinSelect(CATEGORY_FEATURES);
        createMaxSelect(CATEGORY_FEATURES);
        createStepSelect(CATEGORY_FEATURES);
    }

    private void createStepSelect(String category) {
        LinkedHashMap<String, Double> options = new LinkedHashMap<>();
        options.put("0.01", 0.01);
        options.put("0.1", 0.1);
        options.put("1", 1.0);
        options.put("10", 10.0);
        createSelectAction("Step", category, options, "1", stepCommand);

    }

    private void createMaxSelect(String category) {
        createSelectAction("Max", category, createDoubleOptions(100), "0",
                maxCommand);
    }

    private void createMinSelect(String category) {
        createSelectAction("Min", category, createDoubleOptions(100), "0",
                minCommand);

    }

}
