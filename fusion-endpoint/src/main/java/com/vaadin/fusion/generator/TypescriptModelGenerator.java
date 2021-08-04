package com.vaadin.fusion.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.jknack.handlebars.Helper;
import io.swagger.codegen.v3.CodegenProperty;

import static com.vaadin.fusion.generator.TypescriptCodeGeneratorUtils.getSimpleNameFromImports;

public class TypescriptModelGenerator {
    private static final Pattern ARRAY_TYPE_NAME_PATTERN = Pattern
            .compile("ReadonlyArray<(.*)>");
    private static final Pattern MAPPED_TYPE_NAME_PATTERN = Pattern
            .compile("Readonly<Record<string, (.*)>>");
    private static final Set<String> PRIMITIVES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("string", "number", "boolean")));
    private static final Pattern PRIMITIVE_TYPE_NAME_PATTERN = Pattern
            .compile("^(string|number|boolean)");

    static Helper<CodegenProperty> getModelArgumentsHelper() {
        return (prop, options) -> getModelArguments(prop, options.param(0));
    }

    static Helper<CodegenProperty> getModelFullTypeHelper() {
        return (prop, options) -> getModelFullType(
                getSimpleNameFromImports(prop.datatype, options.param(0)));
    }

    private static String fixNameForModel(String name) {
        name = removeOptionalSuffix(name);
        if (ARRAY_TYPE_NAME_PATTERN.matcher(name).find()) {
            name = "Array";
        } else if ("any".equals(name)
                || MAPPED_TYPE_NAME_PATTERN.matcher(name).find()) {
            name = "Object";
        } else if (PRIMITIVE_TYPE_NAME_PATTERN.matcher(name).find()) {
            name = GeneratorUtils.capitalize(name);
        }
        return name + MainGenerator.MODEL;
    }

    private static List<String> getConstrainsArguments(
            CodegenProperty property) {
        List<String> annotations = (List) property.getVendorExtensions()
                .get(OpenAPIObjectGenerator.CONSTRAINT_ANNOTATIONS);
        if (annotations != null) {
            return annotations.stream()
                    .map(annotation -> String.format("new " + "%s", annotation))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static String getModelArguments(CodegenProperty property,
            List<Map<String, String>> imports) {
        String dataType = property.datatype;
        boolean optional = !property.required;
        String simpleName = getSimpleNameFromImports(dataType, imports);
        return getModelVariableArguments(simpleName, optional,
                getConstrainsArguments(property));
    }

    private static String getModelFullType(String name) {
        String arrayModelName = "Array" + MainGenerator.MODEL;
        String objectModelName = "Object" + MainGenerator.MODEL;

        Set<TypescriptTypeParser.Node> visitedNodes = new HashSet<>();
        TypescriptTypeParser.Node root = TypescriptTypeParser.parse(name)
                .traverse().visit((node, parent) -> {
                    node.setUndefined(false);

                    if (node.getName().contains("Array")) {
                        TypescriptTypeParser.Node newNode = new TypescriptTypeParser.Node(
                                arrayModelName);

                        TypescriptTypeParser.Node arrayItem = node.getNested()
                                .get(0);

                        if (PRIMITIVES.contains(arrayItem.getName())) {
                            newNode.addNested(arrayItem);
                            visitedNodes.add(arrayItem);
                        } else {
                            newNode.addNested(
                                    getModelValueType(arrayItem, visitedNodes));
                        }

                        newNode.addNested(arrayItem.clone());

                        visitedNodes.add(newNode);

                        return newNode;
                    } else if (node.getName().equals("Readonly")
                            && node.getNested().get(0).getName()
                                    .equals("Record")
                            && parent.map(
                                    p -> !p.getName().equals(objectModelName))
                                    .orElse(true)) {
                        TypescriptTypeParser.Node wrapper = new TypescriptTypeParser.Node(
                                objectModelName);
                        wrapper.addNested(node);

                        TypescriptTypeParser.Node record = node.getNested()
                                .get(0);
                        TypescriptTypeParser.Node key = record.getNested()
                                .get(0);
                        TypescriptTypeParser.Node value = record.getNested()
                                .get(1);

                        if (PRIMITIVES.contains(value.getName())) {
                            record.getNested().set(1, value);
                            visitedNodes.add(value);
                        } else {
                            record.getNested().set(1,
                                    getModelValueType(value, visitedNodes));
                        }

                        visitedNodes.add(wrapper);
                        visitedNodes.add(node);
                        visitedNodes.add(record);
                        visitedNodes.add(key);

                        return wrapper;
                    } else if (PRIMITIVES.contains(node.getName())
                            && !visitedNodes.contains(node)) {
                        node.setName(GeneratorUtils.capitalize(node.getName())
                                + MainGenerator.MODEL);
                        visitedNodes.add(node);
                        return node;
                    }

                    if (!visitedNodes.contains(node)) {
                        node.setName(node.getName() + MainGenerator.MODEL);
                    }

                    return node;
                }).finish();

        return root.toString();
    }

    private static String getModelFullType1(String name) {
        Matcher matcher = ARRAY_TYPE_NAME_PATTERN.matcher(name);
        if (matcher.find()) {
            String arrayItemType = matcher.group(1);

            String variableName = arrayItemType
                    .endsWith(MainGenerator.OPTIONAL_SUFFIX)
                            ? arrayItemType.substring(0,
                                    arrayItemType.lastIndexOf(
                                            MainGenerator.OPTIONAL_SUFFIX))
                            : arrayItemType;
            return "Array" + MainGenerator.MODEL + "<"
                    + getModelVariableType(variableName) + ", "
                    + getModelFullType(variableName) + ">";
        }
        matcher = MAPPED_TYPE_NAME_PATTERN.matcher(name);
        if (matcher.find()) {
            return "Object" + MainGenerator.MODEL + "<Readonly<Record<string, "
                    + getModelVariableType(matcher.group(1)) + ">>>";
        }
        return fixNameForModel(name);
    }

    private static TypescriptTypeParser.Node getModelValueType(
            TypescriptTypeParser.Node node,
            Set<TypescriptTypeParser.Node> visitedNodes) {
        TypescriptTypeParser.Node modelValueNode = new TypescriptTypeParser.Node(
                MainGenerator.MODEL + "Value");
        modelValueNode.addNested(node);

        visitedNodes.add(modelValueNode);

        return modelValueNode;
    }

    private static String getModelVariableArguments(String name,
            boolean optional, List<String> constrainArguments) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(fixNameForModel(name));
        stringBuilder.append(", [");
        List<String> arguments = new ArrayList<>();
        arguments.add(String.valueOf(optional));
        Matcher matcher = ARRAY_TYPE_NAME_PATTERN.matcher(name);
        if (matcher.find()) {
            String arrayTypeName = matcher.group(1);
            boolean arrayTypeOptional = arrayTypeName
                    .endsWith(MainGenerator.OPTIONAL_SUFFIX);
            arrayTypeName = removeOptionalSuffix(arrayTypeName);
            arguments.add(getModelVariableArguments(arrayTypeName,
                    arrayTypeOptional, Collections.emptyList()));
        }
        if (!constrainArguments.isEmpty()) {
            arguments.addAll(constrainArguments);
        }
        stringBuilder
                .append(arguments.stream().collect(Collectors.joining(", ")));
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    private static String getModelVariableType(String variableName) {
        Matcher matcher = PRIMITIVE_TYPE_NAME_PATTERN.matcher(variableName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return MainGenerator.MODEL + "Type<" + getModelFullType(variableName)
                + ">";
    }

    private static String removeOptionalSuffix(String name) {
        if (name.endsWith(MainGenerator.OPTIONAL_SUFFIX)) {
            return name.substring(0,
                    name.length() - MainGenerator.OPTIONAL_SUFFIX.length());
        }
        return name;
    }
}
