package com.vaadin.flow.templatemodel;

public class BeanContainingBeans {
    private Bean bean1;
    private Bean bean2;

    public static interface NestedBean {
        @AllowClientUpdates(ClientUpdateMode.DENY)
        void setDenyInt(int value);

        Double getDoubleObject();

        @AllowClientUpdates(ClientUpdateMode.IF_TWO_WAY_BINDING)
        void setDoubleObject(Double doubleObject);
    }

    public BeanContainingBeans() {
    }

    public BeanContainingBeans(Bean bean1, Bean bean2) {
        this.bean1 = bean1;
        this.bean2 = bean2;
    }

    @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "booleanObject")
    @AllowClientUpdates(ClientUpdateMode.ALLOW)
    public Bean getBean1() {
        return bean1;
    }

    public void setBean1(Bean bean1) {
        this.bean1 = bean1;
    }

    @AllowClientUpdates(value = ClientUpdateMode.ALLOW, path = "intValue")
    @AllowClientUpdates(ClientUpdateMode.ALLOW)
    public Bean getBean2() {
        return bean2;
    }

    public void setBean2(Bean bean2) {
        this.bean2 = bean2;
    }

    @AllowClientUpdates(ClientUpdateMode.ALLOW)
    public NestedBean getBean3() {
        return null;
    }

}
