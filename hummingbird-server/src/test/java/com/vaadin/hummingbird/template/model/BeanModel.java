package com.vaadin.hummingbird.template.model;

public interface BeanModel extends TemplateModel {
    public boolean isBooleanValue();

    public void setBooleanValue(boolean b);

    public int getIntValue();

    public void setIntValue(int i);

    public double getDoubleValue();

    public void setDoubleValue(double d);

    public String getString();

    public void setString(String s);

    public Boolean getBooleanObject();

    public void setBooleanObject(Boolean bObject);

    public Integer getIntObject();

    public void setIntObject(Integer iObject);

    public Double getDoubleObject();

    public void setDoubleObject(Double dObject);
}
