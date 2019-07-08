package com.vaadin.flow.server.frontend.scanner;

import net.bytebuddy.jar.asm.AnnotationVisitor;
import net.bytebuddy.jar.asm.Opcodes;

/**
 * An annotation visitor implementation that enables repeated annotations.
 */
abstract class RepeatedAnnotationVisitor extends AnnotationVisitor {
    RepeatedAnnotationVisitor() {
        super(Opcodes.ASM7);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        return this;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name,
            String descriptor) {
        return this;
    }

    @Override
    public abstract void visit(String name, Object value);
}
