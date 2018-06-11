package com.vaadin.flow.templatemodel;

public class SelfReferentialBean {
    private String name;
    private SelfReferentialBean parent;
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public void setParent(SelfReferentialBean parent) {
        this.parent = parent;
    }
    
    public SelfReferentialBean getParent() {
        return parent;
    }
}
