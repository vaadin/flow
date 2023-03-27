package com.vaadin.base.devserver.themeeditor.utils;

import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

/**
 * Implementation of {@link com.github.javaparser.ast.visitor.GenericVisitor}
 * that finds {@link Statement} at given line of source code file.
 */
public class StatementLineNumberVisitor
        extends GenericVisitorAdapter<Statement, Integer> {

    @Override
    public Statement visit(AssertStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(BlockStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(BreakStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(ContinueStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(DoStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(EmptyStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(ExplicitConstructorInvocationStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(ForEachStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(ForStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(IfStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(LabeledStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(ReturnStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(SwitchStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(SynchronizedStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(ThrowStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(TryStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(LocalClassDeclarationStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(LocalRecordDeclarationStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(WhileStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(UnparsableStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(YieldStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Statement visit(ExpressionStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    private boolean lineCheck(Statement n, Integer arg) {
        return n.getRange().filter(r -> r.begin.line == arg).isPresent();
    }
}