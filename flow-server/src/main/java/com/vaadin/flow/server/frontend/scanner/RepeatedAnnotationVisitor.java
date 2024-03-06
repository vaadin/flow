/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.Opcodes;

/**
 * An annotation visitor implementation that enables repeated annotations.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
abstract class RepeatedAnnotationVisitor extends AnnotationVisitor {
    RepeatedAnnotationVisitor() {
        super(Opcodes.ASM9);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        return this;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        return this;
    }

    @Override
    public abstract void visit(String name, Object value);
}
