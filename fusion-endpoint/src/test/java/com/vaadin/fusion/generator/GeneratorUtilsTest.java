/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator;

import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.vaadin.fusion.Endpoint;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class GeneratorUtilsTest {

    @Test
    public void should_Not_BeConsideredAsHavingAnAnnotation_When_GivenClassDoesNotHaveAnnotationDeclarationNorImport() {
        NodeWithAnnotations<?> declarationWithoutEndpointAnnotation = Mockito
                .mock(NodeWithAnnotations.class);
        CompilationUnit compilationUnitWithoutEndpointImport = Mockito
                .mock(CompilationUnit.class);

        Mockito.doReturn(Optional.empty())
                .when(declarationWithoutEndpointAnnotation)
                .getAnnotationByClass(Endpoint.class);
        Mockito.doReturn(new NodeList<>())
                .when(compilationUnitWithoutEndpointImport).getImports();

        Assert.assertFalse(
                "A class without Endpoint annotation nor import should not be considered as an Endpoint",
                GeneratorUtils.hasAnnotation(
                        declarationWithoutEndpointAnnotation,
                        compilationUnitWithoutEndpointImport, Endpoint.class));
    }

    @Test
    public void should_Not_BeConsideredAsHavingAnAnnotation_When_GivenClassHavsAnnotationDeclarationButWithDifferentImport() {
        NodeWithAnnotations<?> declarationWithEndpointAnnotation = Mockito
                .mock(NodeWithAnnotations.class);
        CompilationUnit compilationUnitWithNonVaadinEndpointImport = Mockito
                .mock(CompilationUnit.class);

        AnnotationExpr endpointAnnotation = Mockito.mock(AnnotationExpr.class);
        Mockito.doReturn(Optional.of(endpointAnnotation))
                .when(declarationWithEndpointAnnotation)
                .getAnnotationByClass(Endpoint.class);

        NodeList<ImportDeclaration> imports = new NodeList<>();
        ImportDeclaration importDeclaration = Mockito
                .mock(ImportDeclaration.class);
        Mockito.doReturn("some.non.vaadin.Endpoint").when(importDeclaration)
                .getNameAsString();
        imports.add(importDeclaration);
        Mockito.doReturn(imports)
                .when(compilationUnitWithNonVaadinEndpointImport).getImports();

        Assert.assertFalse(
                "A class with a non Vaadin Endpoint should not be considered as an Endpoint",
                GeneratorUtils.hasAnnotation(declarationWithEndpointAnnotation,
                        compilationUnitWithNonVaadinEndpointImport,
                        Endpoint.class));
    }

    @Test
    public void should_BeConsideredAsHavingAnAnnotation_When_GivenClassHavsAnnotationDeclarationAndTheImport() {
        NodeWithAnnotations<?> declarationWithEndpointAnnotation = Mockito
                .mock(NodeWithAnnotations.class);
        CompilationUnit compilationUnitWithVaadinEndpointImport = Mockito
                .mock(CompilationUnit.class);

        AnnotationExpr endpointAnnotation = Mockito.mock(AnnotationExpr.class);
        Mockito.doReturn(Optional.of(endpointAnnotation))
                .when(declarationWithEndpointAnnotation)
                .getAnnotationByClass(Endpoint.class);

        NodeList<ImportDeclaration> imports = new NodeList<>();
        ImportDeclaration importDeclaration = Mockito
                .mock(ImportDeclaration.class);
        Mockito.doReturn(Endpoint.class.getName()).when(importDeclaration)
                .getNameAsString();
        imports.add(importDeclaration);
        Mockito.doReturn(imports).when(compilationUnitWithVaadinEndpointImport)
                .getImports();

        Assert.assertTrue(
                "A class with a Vaadin Endpoint should be considered as an Endpoint",
                GeneratorUtils.hasAnnotation(declarationWithEndpointAnnotation,
                        compilationUnitWithVaadinEndpointImport,
                        Endpoint.class));
    }
}