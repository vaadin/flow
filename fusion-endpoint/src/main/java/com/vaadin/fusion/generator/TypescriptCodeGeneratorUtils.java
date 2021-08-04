package com.vaadin.fusion.generator;

import java.util.List;
import java.util.Map;

import static com.vaadin.fusion.generator.TypescriptCodeGenerator.IMPORT;

class TypescriptCodeGeneratorUtils {
    private static final String JAVA_NAME_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";

    // Method for extracting fully qualified name in a complex type. E.g.
    // 'com.example.mypackage.Bean' will be extracted in the type
    // `Map<String, Map<String, com.example.mypackage.Bean>>`
    static String getSimpleNameFromComplexType(String dataType,
            List<Map<String, String>> imports) {
        TypescriptTypeParser.Node root = TypescriptTypeParser.parse(dataType)
                .traverse().visit((node, _p) -> {
                    String name = node.getName();

                    if (name.contains(".")) {
                        node.setName(getSimpleNameFromImports(name, imports));
                    }

                    return node;
                }).finish();

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
}
