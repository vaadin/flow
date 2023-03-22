package com.vaadin.base.devserver.themeeditor.utils;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.vaadin.base.devserver.themeeditor.JavaSourceModifier;

/**
 * Implementation of {@link com.github.javaparser.ast.visitor.GenericVisitor}
 * that searches for local classname by comparing expression type, method call
 * scope and expression comment.
 */
public class LocalClassNameVisitor
        extends GenericVisitorAdapter<ExpressionStmt, String> {

    @Override
    public ExpressionStmt visit(ExpressionStmt n, String arg) {
        if (n.getExpression().isMethodCallExpr()
                && n.getComment().filter(
                        JavaSourceModifier.LOCAL_CLASSNAME_COMMENT::equals)
                        .isPresent()
                && n.getExpression().asMethodCallExpr().getScope()
                        .map(Expression::toString).filter(arg::equals)
                        .isPresent()) {
            return n;
        }
        return super.visit(n, arg);
    }

}