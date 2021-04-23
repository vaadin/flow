package com.vaadin.flow.server.connect;

import java.lang.reflect.AnnotatedElement;

import javax.annotation.Nonnull;

import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

public final class Annotation {
    public static boolean isElementRequired(AnnotatedElement element) {
        return element.isAnnotationPresent(Nonnull.class);
    }

    public static boolean isNodeRequired(NodeWithAnnotations<?> node) {
        return node.isAnnotationPresent(Nonnull.class);
    }
}