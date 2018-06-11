package com.vaadin.flow.templatemodel;

public class CircularDependencyBean {
    private String name;

    private CircularDependencyBeanB relativeBean;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CircularDependencyBeanB getRelativeBean() {
        return relativeBean;
    }

    public void setRelativeBean(CircularDependencyBeanB relativeBean) {
        this.relativeBean = relativeBean;
    }
}
