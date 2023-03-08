package com.vaadin.base.devserver.themeeditor.utils;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.modules.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;

/**
 * Implementation of {@link com.github.javaparser.ast.visitor.GenericVisitor}
 * that finds {@link Node} at given line of source code file.
 */
public class LineNumberVisitor extends GenericVisitorAdapter<Node, Integer> {

    @Override
    public Node visit(AnnotationDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(AnnotationMemberDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ArrayAccessExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ArrayCreationExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ArrayInitializerExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(AssertStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(AssignExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(BinaryExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(BlockStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(BooleanLiteralExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(BreakStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(CastExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(CatchClause n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(CharLiteralExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ClassExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ClassOrInterfaceDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ClassOrInterfaceType n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(CompilationUnit n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ConditionalExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ConstructorDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ContinueStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(DoStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(DoubleLiteralExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(EmptyStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(EnclosedExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(EnumConstantDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(EnumDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ExplicitConstructorInvocationStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(FieldAccessExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(FieldDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ForEachStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ForStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(IfStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(InitializerDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(InstanceOfExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(IntegerLiteralExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(JavadocComment n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(LabeledStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(LongLiteralExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(MarkerAnnotationExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(MemberValuePair n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(MethodCallExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(MethodDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(NameExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(NormalAnnotationExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(NullLiteralExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ObjectCreationExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(PackageDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(Parameter n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(PrimitiveType n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(Name n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(SimpleName n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ArrayType n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ArrayCreationLevel n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(IntersectionType n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(UnionType n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ReturnStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(SingleMemberAnnotationExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(StringLiteralExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(SuperExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(SwitchEntry n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(SwitchStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(SynchronizedStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ThisExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ThrowStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(TryStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(LocalClassDeclarationStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(LocalRecordDeclarationStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(TypeParameter n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(UnaryExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(UnknownType n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(VariableDeclarationExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(VariableDeclarator n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(VoidType n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(WhileStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(WildcardType n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(LambdaExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(MethodReferenceExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(TypeExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ImportDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(BlockComment n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(LineComment n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ModuleDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ModuleRequiresDirective n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ModuleExportsDirective n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ModuleProvidesDirective n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ModuleUsesDirective n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ModuleOpensDirective n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(UnparsableStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ReceiverParameter n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(VarType n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(Modifier n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(SwitchExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(YieldStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(TextBlockLiteralExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(PatternExpr n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(RecordDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(CompactConstructorDeclaration n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    @Override
    public Node visit(ExpressionStmt n, Integer arg) {
        return lineCheck(n, arg) ? n : super.visit(n, arg);
    }

    private boolean lineCheck(Node n, Integer arg) {
        return n.getRange().filter(r -> r.begin.line == arg).isPresent();
    }
}