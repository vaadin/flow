package com.vaadin.flow.templatemodel;

public class CircularDependencyBeanB {
    private String name;

    private CircularDependencyBean otherRelative;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CircularDependencyBean getOherRelativeBean() {
        return otherRelative;
    }

    public void setOtherRelativeBean(CircularDependencyBean otherRelative) {
        this.otherRelative = otherRelative;
    }
}
