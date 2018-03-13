package com.vaadin.flow.data.value;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;

@Tag("value-change-mode-component")
public class ValueChangeModeComponent extends Component
        implements HasValueChangeMode<ValueChangeModeComponent, String> {

    private String value;
    private ValueChangeMode valueChangeMode;

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public ValueChangeMode getValueChangeMode() {
        return valueChangeMode;
    }

    @Override
    public void setValueChangeMode(ValueChangeMode valueChangeMode) {
        this.valueChangeMode = valueChangeMode;
        HasValueChangeMode.super.setValueChangeMode(valueChangeMode);
    }

}
