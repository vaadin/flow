package com.vaadin.base.devserver.editor;

public enum ComponentType {

    BUTTON("com.vaadin.flow.component.button.Button"), TEXTFIELD(
            "com.vaadin.flow.component.textfield.TextField");

    private String className;

    public static ComponentType getForClass(Class<?> name) {
        for (ComponentType type : values()) {
            if (type.getClassName().equals(name.getName())) {
                return type;
            }
        }
        return null;
    }

    private ComponentType(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public static ComponentType from(String componentType) {
        if (componentType.equalsIgnoreCase("button")) {
            return ComponentType.BUTTON;
        } else if (componentType.equalsIgnoreCase("textfield")) {
            return ComponentType.TEXTFIELD;
        }
        return null;
    }
}