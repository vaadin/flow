package com.vaadin.flow.templatemodel;

public class Bean {
    private boolean booleanValue;
    private Boolean booleanObject;
    private int intValue;
    private Integer intObject;
    private double doubleValue;
    private Double doubleObject;
    private String string;

    public Bean() {
    }

    public Bean(int value) {
        intValue = value;
        intObject = intValue;
        doubleValue = value;
        doubleObject = doubleValue;
        booleanValue = value != 0;
        booleanObject = booleanValue;
        string = String.valueOf(value);
    }

    @AllowClientUpdates
    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public boolean isBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Boolean getBooleanObject() {
        return booleanObject;
    }

    public void setBooleanObject(Boolean booleanObject) {
        this.booleanObject = booleanObject;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public Integer getIntObject() {
        return intObject;
    }

    public void setIntObject(Integer intObject) {
        this.intObject = intObject;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Double getDoubleObject() {
        return doubleObject;
    }

    @AllowClientUpdates(ClientUpdateMode.ALLOW)
    public void setDoubleObject(Double doubleObject) {
        this.doubleObject = doubleObject;
    }

}
