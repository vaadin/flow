/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator.typescript;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.vaadin.fusion.generator.GeneratorUtils;

import static com.vaadin.fusion.generator.typescript.CodeGenerator.IMPORT;

class CodeGeneratorUtils {
    private CodeGeneratorUtils() {
    }

    // Method for extracting fully qualified name in a complex type. E.g.
    // 'com.example.mypackage.Bean' will be extracted in the type
    // `Map<String, Map<String, com.example.mypackage.Bean>>`
    static String getSimpleNameFromComplexType(String dataType,
            List<Map<String, String>> imports) {
        return TypeParser.parse(dataType).traverse()
                .visit(new SimpleNameVisitor(imports)).finish().toString();
    }

    static String getSimpleNameFromImports(String dataType,
            List<Map<String, String>> imports) {
        for (Map<String, String> anImport : imports) {
            if (Objects.equals(dataType, anImport.get(IMPORT))) {
                return GeneratorUtils.firstNonBlank(anImport.get("importAs"),
                        anImport.get("className"));
            }
        }
        if (GeneratorUtils.contains(dataType, "<")
                || GeneratorUtils.contains(dataType, "{")
                || GeneratorUtils.contains(dataType, "|")) {
            return getSimpleNameFromComplexType(dataType, imports);
        }
        return getSimpleNameFromQualifiedName(dataType);
    }

    static String getSimpleNameFromQualifiedName(String qualifiedName) {
        if (GeneratorUtils.contains(qualifiedName, ".")) {
            return GeneratorUtils.substringAfterLast(qualifiedName, ".");
        }
        return qualifiedName;
    }

    static class SimpleNameVisitor implements TypeParser.Visitor {
        private final List<Map<String, String>> imports;

        SimpleNameVisitor(List<Map<String, String>> imports) {
            this.imports = imports;
        }

        @Override
        public TypeParser.Node enter(TypeParser.Node node,
                TypeParser.Node parent) {
            String name = node.getName();

            if (name.contains(".")) {
                node.setName(getSimpleNameFromImports(name, imports));
            }

            return node;
        }
    }
}
