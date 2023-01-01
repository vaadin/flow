package com.vaadin.base.devserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.editor.ComponentType;
import com.vaadin.base.devserver.editor.Editor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ComponentTracker;

public class EditCode {

    public static void edit(
            Component component, String editType,
            String value) {
        StackTraceElement location = ComponentTracker.findCreate(component);
        if (location == null) {
            getLogger().error("Unable to find the location where the component "
                    + component.getClass().getName() + " was created");
            return;
        }

        String createdInClass = location.getClassName();
        // String filename = location.getFileName();

        ComponentType type = ComponentType.getForClass(component.getClass());
        if (type == null) {
            getLogger().error("Don't know how to handle "
                    + component.getClass().getName());
            return;
        }
        new Editor().setComponentAttribute(createdInClass,
                location.getLineNumber(), 123, type, editType, value);

    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(EditCode.class);
    }

}
