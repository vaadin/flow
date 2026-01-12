/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.client;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

/**
 * Tests the java code.
 */
public class CodeTest {

    /**
     * Verifies that all type declaration are of the implementation, not super
     * interface, so that the GWT compiler inlines implementations.
     */
    @Test
    public void gwtGenerics() throws IOException {
        gwtGenericsDir(new File("src/main/java"));
    }

    private static void gwtGenericsDir(File dir) throws IOException {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                gwtGenericsDir(file);
            } else {
                gwtGenerics(file);
            }
        }
    }

    private static void gwtGenerics(File file) throws IOException {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        String value = FileUtils.readFileToString(file, UTF_8);
        parser.setSource(value.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        cu.accept(new ASTVisitor() {

            Set<String> imports = new HashSet<>();
            String packageName;

            @Override
            public boolean visit(PackageDeclaration node) {
                packageName = node.getName().toString();
                return false;
            }

            @Override
            public boolean visit(ImportDeclaration node) {
                imports.add(node.getName().toString());
                return false;
            }

            @Override
            public boolean visit(VariableDeclarationStatement node) {
                for (Object frament : node.fragments()) {
                    if (frament instanceof VariableDeclarationFragment) {
                        VariableDeclarationFragment variableDeclaration = (VariableDeclarationFragment) frament;
                        Expression expression = variableDeclaration
                                .getInitializer();
                        if (expression instanceof ClassInstanceCreation) {
                            ClassInstanceCreation classInstanceCreation = (ClassInstanceCreation) expression;
                            Class<?> typeClass = getClass(node.getType());
                            Class<?> instanceClass = getClass(
                                    classInstanceCreation.getType());
                            if (typeClass != instanceClass && typeClass
                                    .isAssignableFrom(instanceClass)) {
                                fail("Variable type must be the specific implementation in "
                                        + node + " in " + file.getName());
                            }
                        }
                    }
                }
                return false;
            }

            private Class<?> getClass(Type type) {
                if (type instanceof ArrayType) {
                    type = ((ArrayType) type).getElementType();
                }
                if (type instanceof ParameterizedType) {
                    type = ((ParameterizedType) type).getType();
                }
                String className = type.toString();
                if (className.indexOf('.') == -1) {
                    String dotPrefix = '.' + className;
                    for (String i : imports) {
                        if (i.endsWith(dotPrefix)) {
                            className = i;
                            break;
                        }
                    }
                }

                Class<?> clas = getClass(className);
                if (clas != null) {
                    return clas;
                }

                clas = getClass("java.lang." + className);
                if (clas != null) {
                    return clas;
                }

                try {
                    String fileName = file.getName();
                    fileName = fileName.substring(0, fileName.lastIndexOf('.'));

                    if (fileName.equals(className)) {
                        return Class.forName(packageName + '.' + fileName);
                    }

                    clas = getClass(packageName + '.' + className);
                    if (clas != null) {
                        return clas;
                    }

                    return Class.forName(
                            packageName + '.' + fileName + '$' + className);
                } catch (ClassNotFoundException e) {
                    fail("Could not load class " + e);
                    return null;
                }
            }

            private Class<?> getClass(String className) {
                try {
                    return ClassUtils.getClass(className);
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }
        });
    }
}
