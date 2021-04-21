package com.vaadin.flow.server.connect;

import java.lang.reflect.AnnotatedElement;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

public final class ConnectUtils {
    public static boolean isElementRequired(AnnotatedElement element) {
        return element.isAnnotationPresent(NotNull.class)
                || element.isAnnotationPresent(Nonnull.class);
    }
}