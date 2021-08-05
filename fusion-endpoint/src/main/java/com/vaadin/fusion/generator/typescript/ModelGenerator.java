package com.vaadin.fusion.generator.typescript;

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

import com.vaadin.fusion.generator.GeneratorUtils;
import com.vaadin.fusion.generator.MainGenerator;
import com.vaadin.fusion.generator.OpenAPIObjectGenerator;

import static com.vaadin.fusion.generator.typescript.CodeGeneratorUtils.getSimpleNameFromImports;

public class ModelGenerator {
    private static final Pattern ARRAY_TYPE_NAME_PATTERN = Pattern
            .compile("ReadonlyArray<(.*)>");
    private static final Pattern MAPPED_TYPE_NAME_PATTERN = Pattern
            .compile("Readonly<Record<string, (.*)>>");
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
        TypeParser.Node root = TypeParser.parse(name).traverse()
                .visit(new ModelTypeVisitor()).finish();

        return root.toString();
    }

    private static String getModelVariableArguments(String name,
            boolean optional, List<String> constrainArguments) {
        ModelArgumentsVisitor visitor = new ModelArgumentsVisitor(optional,
                constrainArguments);
        TypeParser.Node root = TypeParser.parse(name);

        root.traverse().visit(visitor).finish();

        return visitor.getResult();
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

    private static class ModelArgumentsVisitor extends Visitor {
        private final StringBuilder builder = new StringBuilder();
        private final List<String> constrainArguments;
        private final boolean isRootOptional;

        ModelArgumentsVisitor(boolean isRootOptional,
                List<String> constrainArguments) {
            this.isRootOptional = isRootOptional;
            this.constrainArguments = constrainArguments;
        }

        @Override
        public TypeParser.Node enter(TypeParser.Node node,
                TypeParser.Node parent) {
            if (parent == null || isArray(parent)) {
                if (parent != null) {
                    builder.append(", ");
                }

                builder.append(prepareModelName(node));
                builder.append(", [");
                builder.append(
                        parent == null ? isRootOptional : node.isUndefined());

                return node;
            }

            // If array chain ended, let's just remove all children nodes
            return null;
        }

        @Override
        public TypeParser.Node exit(TypeParser.Node node,
                TypeParser.Node parent) {
            if (parent == null && constrainArguments.size() > 0) {
                builder.append(", ");
                builder.append(String.join(", ", constrainArguments));
            }

            if (parent == null || isArray(parent)) {
                builder.append("]");
            }

            return node;
        }

        String getResult() {
            return builder.toString();
        }

        private String prepareModelName(TypeParser.Node node) {
            if (isArray(node)) {
                return ARRAY_MODEL_NAME;
            } else if (isObject(node)) {
                return OBJECT_MODEL_NAME;
            } else if (isPrimitive(node)) {
                return getPrimitiveModelName(node);
            }

            return getOtherModelName(node);
        }
    }

    private static class ModelTypeVisitor extends Visitor {
        private final Set<TypeParser.Node> visitedNodes = new HashSet<>();

        @Override
        public TypeParser.Node enter(TypeParser.Node node,
                TypeParser.Node parent) {
            node.setUndefined(false);

            if (isArray(node)) {
                // ReadonlyArray<Type, Type>
                TypeParser.Node newNode = new TypeParser.Node(ARRAY_MODEL_NAME);

                TypeParser.Node arrayItem = node.getNested().get(0);

                if (isPrimitive(arrayItem)) {
                    newNode.addNested(arrayItem);
                    visitedNodes.add(arrayItem);
                } else {
                    newNode.addNested(getModelValueType(arrayItem));
                }

                newNode.addNested(arrayItem.clone());

                visitedNodes.add(newNode);

                return newNode;
            } else if (isObject(node)
                    && (parent == null || !isObjectModel(parent))) {
                // Readonly<Record<Type, Type>>
                TypeParser.Node wrapper = new TypeParser.Node(
                        OBJECT_MODEL_NAME);
                wrapper.addNested(node);

                // Record<Type, Type>
                TypeParser.Node record = node.getNested().get(0);
                TypeParser.Node key = record.getNested().get(0);
                TypeParser.Node value = record.getNested().get(1);

                if (isPrimitive(value)) {
                    record.getNested().set(1, value);
                    visitedNodes.add(value);
                } else {
                    record.getNested().set(1, getModelValueType(value));
                }

                visitedNodes.add(wrapper);
                visitedNodes.add(node);
                visitedNodes.add(record);
                visitedNodes.add(key);

                return wrapper;
            } else if (isPrimitive(node) && !visitedNodes.contains(node)) {
                node.setName(getPrimitiveModelName(node));
                visitedNodes.add(node);
                return node;
            }

            if (!visitedNodes.contains(node)) {
                node.setName(getOtherModelName(node));
            }

            return node;
        }

        private TypeParser.Node getModelValueType(TypeParser.Node node) {
            TypeParser.Node modelValueNode = new TypeParser.Node(
                    MainGenerator.MODEL + "Value");
            modelValueNode.addNested(node);

            visitedNodes.add(modelValueNode);

            return modelValueNode;
        }
    }

    private abstract static class Visitor extends TypeParser.Visitor {
        protected static final String ARRAY_MODEL_NAME = "Array"
                + MainGenerator.MODEL;
        protected static final String OBJECT_MODEL_NAME = "Object"
                + MainGenerator.MODEL;
        private static final Set<String> PRIMITIVES = Collections
                .unmodifiableSet(new HashSet<>(
                        Arrays.asList("string", "number", "boolean")));

        protected String getOtherModelName(TypeParser.Node node) {
            return node.getName() + MainGenerator.MODEL;
        }

        protected String getPrimitiveModelName(TypeParser.Node node) {
            return GeneratorUtils.capitalize(node.getName())
                    + MainGenerator.MODEL;
        }

        protected boolean isArray(TypeParser.Node node) {
            return node.getName().contains("Array");
        }

        protected boolean isObject(TypeParser.Node node) {
            return node.getName().equals("Readonly")
                    && node.getNested().get(0).getName().equals("Record");
        }

        protected boolean isObjectModel(TypeParser.Node node) {
            return OBJECT_MODEL_NAME.equals(node.getName());
        }

        protected boolean isPrimitive(TypeParser.Node node) {
            return PRIMITIVES.contains(node.getName());
        }
    }
}
