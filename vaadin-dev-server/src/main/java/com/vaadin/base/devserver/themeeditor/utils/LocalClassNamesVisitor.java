package com.vaadin.base.devserver.themeeditor.utils;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.vaadin.base.devserver.themeeditor.JavaSourceModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of {@link com.github.javaparser.ast.visitor.GenericVisitor}
 * that searches for classnames applied to given component.
 */
public class LocalClassNamesVisitor
        extends GenericVisitorAdapter<Void, Void> {

    private List<String> arguments = new ArrayList<>();

    @Override
    public Void visit(MethodCallExpr n, Void arg) {
        if (Objects.equals("addClassName", n.getName().asString()) &&
                n.getComment().filter(JavaSourceModifier.LOCAL_CLASSNAME_COMMENT::equals).isPresent()) {
            n.getArguments().stream().map(Expression::asStringLiteralExpr)
                    .map(StringLiteralExpr::asString)
                    .forEach(arguments::add);
        }
        return super.visit(n, arg);
    }

    public List<String> getArguments() {
        return arguments;
    }

}