package com.vaadin.flow.templatemodel;

public class BeanContainingBeans {
    private Bean bean1;
    private Bean bean2;

    public BeanContainingBeans() {
    }

    public BeanContainingBeans(Bean bean1, Bean bean2) {
        this.bean1 = bean1;
        this.bean2 = bean2;
    }

    public Bean getBean1() {
        return bean1;
    }

    public void setBean1(Bean bean1) {
        this.bean1 = bean1;
    }

    public Bean getBean2() {
        return bean2;
    }

    public void setBean2(Bean bean2) {
        this.bean2 = bean2;
    }

}
