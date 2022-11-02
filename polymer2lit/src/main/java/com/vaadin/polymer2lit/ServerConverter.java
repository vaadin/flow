package com.vaadin.polymer2lit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.plaf.synth.SynthScrollBarUI;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MemberSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class ServerConverter {

    static public void convertFile(Path filePath) throws IOException {
        String source = read(filePath);
        if (!source.contains("PolymerTemplate")) {
            return;
        }

        String out = transform(source);
        if (source.equals(out)) {
            return;
        }

        try (FileWriter fw = new FileWriter(filePath.toFile())) {
            fw.write(out);
        }
    }

    static String transform(String source) throws IOException {
        JavaClassSource javaClass;
        try {
            javaClass = Roaster.parse(JavaClassSource.class, source);
        } catch (org.jboss.forge.roaster.ParserException e) {
            return source;
        }

        String superType = javaClass.getSuperType();
        if (!superType.startsWith("PolymerTemplate") && !superType.startsWith(
                "com.vaadin.flow.component.polymertemplate.PolymerTemplate")) {
            return source;
        }

        if (superType.contains("<")) {
            String modelType = superType.substring(superType.indexOf("<") + 1,
                    superType.indexOf(">"));
            if (!modelType.equals("TemplateModel")) {
                transformModel(modelType, javaClass);
            }
        }

        javaClass.setSuperType(
                "com.vaadin.flow.component.littemplate.LitTemplate");
        javaClass.removeImport("com.vaadin.flow.component.polymertemplate.Id");
        javaClass.removeImport("com.vaadin.flow.templatemodel.TemplateModel");
        javaClass.removeImport(
                "com.vaadin.flow.component.polymertemplate.PolymerTemplate");
        javaClass.addImport("com.vaadin.flow.component.template.Id");
        String result = javaClass.toUnformattedString();
        return result;
    }

    private static void transformModel(String modelType,
            JavaClassSource javaClass) {
        if (modelType.startsWith(javaClass.getName() + ".")) {
            String internalName = modelType
                    .substring(javaClass.getName().length() + 1);
            // Sub interface
            JavaSource<?> nested = javaClass.getNestedType(internalName);
            // System.out.println("Sub: " + nested.isInterface());
            JavaInterfaceSource model = (JavaInterfaceSource) nested;

            model.removeInterface("TemplateModel");
            LinkedHashSet<String> getters = new LinkedHashSet<>();
            LinkedHashSet<String> setters = new LinkedHashSet<>();
            LinkedHashMap<String, Type<?>> types = new LinkedHashMap<String, Type<?>>();

            for (MemberSource<JavaInterfaceSource, ?> member : model
                    .getMembers()) {
                MethodSource<?> method = (MethodSource<?>) member;
                String name = member.getName();
                String property = getProperty(name);
                if (isSetter(name)) {
                    // System.out.println(method.getParameters());
                    Type<?> type = method.getParameters().get(0).getType();
                    // System.out.println(property + ": " + type);
                    types.put(property, type);
                    // createSetterCode(property, )
                    setters.add(property);
                } else {
                    Type<?> type = method.getReturnType();
                    types.put(property, type);
                    getters.add(property);
                }
            }

            MethodSource<JavaClassSource> getModelMethod = javaClass
                    .addMethod("private " + internalName + " getModel() {}");
            StringBuilder body = new StringBuilder();
            body.append("return new ").append(internalName).append("() {");
            for (String property : types.keySet()) {
                Type<?> type = types.get(property);

                Map<String, String> replacements = new HashMap<>();
                replacements.put("property", property);
                replacements.put("type", type.getName());
                String defaultValue = getDefaultValue(type);
                replacements.put("defaultValue", defaultValue);

                if (setters.contains(property)) {

                    // System.out.println("Setter: " + property + "(" + type +
                    // ")");

                    replacements.put("methodName",
                            "set" + capitalize(property));

                    StringSubstitutor sub = new StringSubstitutor(replacements);

                    body.append(sub.replace("@Override\n"));
                    body.append(sub.replace(
                            "public void ${methodName}(${type} ${property}) {\n"));
                    if (defaultValue == null) {
                        body.append(
                                "/* FIXME Implement this method which could not be automatically generated*/\n");
                        // body.append("// The model method was defined as");
                    } else {
                        body.append(sub.replace(
                                "getElement().setProperty(\"${property}\", ${property});\n"));
                    }
                    body.append(sub.replace("}\n"));

                }
                if (getters.contains(property)) {

                    // System.out.println("Getter: " + property + "(" + type +
                    // ")");

                    if (type.getName().equals("boolean")) {
                        replacements.put("methodName",
                                "is" + capitalize(property));
                    } else {
                        replacements.put("methodName",
                                "get" + capitalize(property));
                    }

                    StringSubstitutor sub = new StringSubstitutor(replacements);

                    body.append(sub.replace("@Override\n"));
                    body.append(
                            sub.replace("public ${type} ${methodName}() {\n"));
                    if (defaultValue == null) {
                        body.append(
                                "/* FIXME Implement this method which could not be automatically generated*/\n");
                        // body.append("// The model method was defined as");
                    } else {
                        body.append(sub.replace(
                                "return getElement().getProperty(\"${property}\", ${defaultValue});\n"));
                    }
                    body.append(sub.replace("}\n"));

                }
            }

            body.append("};");
            // TODO For some reason, this strips comments
            getModelMethod.setBody(body.toString());

        } else {
            System.err.println(
                    "Warning: Do not know how to handle external models. Only models which are internal interfaces: "
                            + modelType);
        }

    }

    private static String getDefaultValue(Type<?> type) {
        if (type.getName().equalsIgnoreCase("boolean")) {
            return "false";
        } else if (type.getQualifiedName().equals(String.class.getName())) {
            return "null";
        }
        return null;

    }

    private static String capitalize(String property) {
        return property.substring(0, 1).toUpperCase(Locale.ENGLISH)
                + property.substring(1);
    }

    private static boolean isSetter(String methodName) {
        return methodName.startsWith("set");
    }

    private static String getProperty(String methodName) {
        String name = methodName.replaceFirst("^(set|is|get)", "");
        return name.substring(0, 1).toLowerCase(Locale.ENGLISH)
                + name.substring(1);
    }

    private static String read(Path filePath) throws IOException {
        try (FileInputStream stream = new FileInputStream(filePath.toFile())) {
            return IOUtils.toString(stream, StandardCharsets.UTF_8);
        }
    }
}
