package com.vaadin.fusion.generator.typescript;

import java.util.List;
import java.util.Map;

import com.vaadin.fusion.generator.GeneratorUtils;

import static com.vaadin.fusion.generator.typescript.CodeGenerator.IMPORT;

class CodeGeneratorUtils {
    // Method for extracting fully qualified name in a complex type. E.g.
    // 'com.example.mypackage.Bean' will be extracted in the type
    // `Map<String, Map<String, com.example.mypackage.Bean>>`
    static String getSimpleNameFromComplexType(String dataType,
            List<Map<String, String>> imports) {
        TypeParser.Node root = TypeParser.parse(dataType).traverse()
                .visit(new SimpleNameVisitor(imports)).finish();

        return root.toString();
    }

    static String getSimpleNameFromImports(String dataType,
            List<Map<String, String>> imports) {
        for (Map<String, String> anImport : imports) {
            if (GeneratorUtils.equals(dataType, anImport.get(IMPORT))) {
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

    static class SimpleNameVisitor extends TypeParser.Visitor {
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
