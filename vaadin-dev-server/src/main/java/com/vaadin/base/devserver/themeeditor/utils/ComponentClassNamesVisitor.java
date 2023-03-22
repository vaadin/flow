package com.vaadin.base.devserver.themeeditor.utils;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of {@link com.github.javaparser.ast.visitor.GenericVisitor}
 * that searches for classnames applied to given component.
 */
public class ComponentClassNamesVisitor
        extends GenericVisitorAdapter<Void, String> {

    private List<String> arguments = new ArrayList<>();

    @Override
    public Void visit(MethodCallExpr n, String arg) {
        if (n.getScope().map(Expression::toString).filter(arg::equals)
                .isPresent()) {
            if (Objects.equals("addClassName", n.getName().asString())) {
                n.getArguments().stream().map(Expression::asStringLiteralExpr)
                        .map(StringLiteralExpr::asString)
                        .forEach(arguments::add);
            }
        }
        return super.visit(n, arg);
    }

    public List<String> getArguments() {
        return arguments;
    }

}