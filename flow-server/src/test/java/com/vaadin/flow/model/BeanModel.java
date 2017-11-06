package com.vaadin.flow.model;

import com.vaadin.flow.model.TemplateModel;

public interface BeanModel extends TemplateModel {
    boolean isBooleanValue();

    void setBooleanValue(boolean b);

    int getIntValue();

    void setIntValue(int i);

    double getDoubleValue();

    void setDoubleValue(double d);

    String getString();

    void setString(String s);

    Boolean getBooleanObject();

    void setBooleanObject(Boolean bObject);

    Integer getIntObject();

    void setIntObject(Integer iObject);

    Double getDoubleObject();

    void setDoubleObject(Double dObject);
}
