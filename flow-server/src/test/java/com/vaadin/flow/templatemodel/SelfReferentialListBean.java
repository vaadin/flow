package com.vaadin.flow.templatemodel;

import java.util.List;

public class SelfReferentialListBean {
    private String name;
    private List<SelfReferentialListBean> children;
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public void setChildren(List<SelfReferentialListBean> children) {
        this.children = children;
    }
    
    public List<SelfReferentialListBean> getChildren() {
        return children;
    }
    
}
