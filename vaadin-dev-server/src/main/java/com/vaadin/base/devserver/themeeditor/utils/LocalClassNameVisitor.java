package com.vaadin.base.devserver.themeeditor.utils;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.vaadin.base.devserver.themeeditor.JavaSourceModifier;

/**
 * Implementation of {@link com.github.javaparser.ast.visitor.GenericVisitor}
 * that searches for local classname expression statement by comparing
 * expression type, method call scope and expression comment.
 *
 * Scope may be null in case of own instance method calls.
 */
public class LocalClassNameVisitor
        extends GenericVisitorAdapter<ExpressionStmt, String> {

    private final SimpleName methodName;

    public LocalClassNameVisitor(boolean overlay) {
        methodName = overlay ? new SimpleName("setOverlayClassName")
                : new SimpleName("addClassName");
    }

    @Override
    public ExpressionStmt visit(ExpressionStmt n, String scope) {
        // filter anything other than method calls
        if (!n.getExpression().isMethodCallExpr()) {
            return super.visit(n, scope);
        }

        // and anything without matching comment
        if (n.getComment()
                .filter(JavaSourceModifier.LOCAL_CLASSNAME_COMMENT::equals)
                .isEmpty()) {
            return super.visit(n, scope);
        }

        // and not required method calls
        if (!n.getExpression().asMethodCallExpr().getName()
                .equals(methodName)) {
            return super.visit(n, scope);
        }

        // and with not matching scope (if defined)
        if (scope != null && n.getExpression().asMethodCallExpr().getScope()
                .map(Expression::toString).filter(scope::equals).isEmpty()) {
            return super.visit(n, scope);
        }

        // voila!
        return n;
    }

}