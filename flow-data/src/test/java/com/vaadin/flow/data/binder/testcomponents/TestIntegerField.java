package com.vaadin.flow.data.binder.testcomponents;

import com.vaadin.flow.component.Tag;

@Tag("test-integer-field")
public class TestIntegerField
        extends AbstractTestHasValueAndValidation<TestIntegerField, Integer>{

    private int minValue = Integer.MIN_VALUE;

    private int maxValue = Integer.MAX_VALUE;

    private String integerRangeErrorMessage = "Integer value is out of allowed range";

    public TestIntegerField() {
        super(0, Integer::valueOf, String::valueOf);
    }

    public String getIntegerRangeErrorMessage() {
        return integerRangeErrorMessage;
    }

    public void setIntegerRangeErrorMessage(String integerRangeErrorMessage) {
        this.integerRangeErrorMessage = integerRangeErrorMessage;
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }
}
