package com.vaadin.base.devserver;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.editor.ComponentType;
import com.vaadin.base.devserver.editor.Editor;
import com.vaadin.base.devserver.editor.Where;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.component.internal.ComponentTracker.Location;

public class EditCode {

    private static Editor editor = new Editor();

    public static void edit(Component component, String editType,
            String value) {
        Location location = ComponentTracker.findCreate(component);
        if (location == null) {
            getLogger().error("Unable to find the location where the component "
                    + component.getClass().getName() + " was created");
            return;
        }

        String createdInClass = location.className();
        // String filename = location.getFileName();

        ComponentType type = ComponentType.getForClass(component.getClass());
        if (type == null) {
            getLogger().error("Don't know how to handle "
                    + component.getClass().getName());
            return;
        }
        editor.setComponentAttribute(createdInClass, location.lineNumber(), 123,
                type, editType, value);

    }

    public static void add(Component reference, Where where,
            ComponentType componentType, String[] constructorArgs) {
        Location createLocation = ComponentTracker.findCreate(reference);
        int referenceComponentCreateLineNumber = createLocation.lineNumber();
        int referenceComponentAttachLineNumber = ComponentTracker
                .findAttach(reference).lineNumber();
        File file = editor.getSourceFile(createLocation.className());
        editor.addComponent(file, referenceComponentCreateLineNumber,
                referenceComponentAttachLineNumber, where, componentType,
                constructorArgs);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(EditCode.class);
    }

}
