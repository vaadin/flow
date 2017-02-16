package com.vaadin.hummingbird;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.vaadin.external.jsoup.Jsoup;
import com.vaadin.external.jsoup.nodes.Attribute;
import com.vaadin.external.jsoup.nodes.Document;
import com.vaadin.external.jsoup.nodes.Element;
import com.vaadin.external.jsoup.select.Elements;
import com.vaadin.hummingbird.demo.expensemanager.views.EditView;
import com.vaadin.hummingbird.template.RelativeFileResolver;

// Parse a HB template and print equivalent java code
public class HBTemplateToJava {

    public static void main(String[] args) throws IOException {

        // Change this class to process a certain template
        Class<?> cls = EditView.class;

        String name = cls.getSimpleName() + ".html";
        RelativeFileResolver templateResolver = new RelativeFileResolver(cls, name);
        String templateFileName = new File(name).getName();
        InputStream templateContentStream = templateResolver.resolve(templateFileName);

        Document document = Jsoup.parse(templateContentStream, null, "");

        printImports(document.body());
        printElements(document.body(), "body");
    }

    private static HashMap<String, Integer> counters = new HashMap<>();
    private static Set<String> imports = new HashSet<>();

    private static String camelize(String input) {
        return String.format(input.replaceAll("\\-(.)", "%S"),
                (Object[]) input.replaceAll("[^-]*-(.)[^-]*", "$1-").split("-"));
    }

    private static String capitalize(String input) {
        return String.format(input.replaceFirst(".", "%S"), input.charAt(0));
    }

    private static String computeImport(String input, String clazz) {
        String tmp[] = input.split("-");
        if (tmp.length == 1) {
            return "com.vaadin.hummingbird.html." + clazz;
        } else {
            return "com.vaadin.hummingbird.components." + tmp[0] + "." + clazz;
        }
    }

    private static String computeVarName(String name) {
        Integer i = counters.get(name);
        if (i != null) {
            counters.put(name, ++i);
            name += i;
        } else {
            counters.put(name, 0);
        }
        return name;
    }

    private static void printImports(Element e) {
        String camel = camelize(e.tagName());
        String clazz = capitalize(camel);
        String imp = computeImport(e.tagName(), clazz);
        if (!imports.contains(imp)) {
            System.out.println("import " + imp + ";");
        }
        imports.add(imp);
        Elements children = e.children();
        for (Element ele : children) {
            printImports(ele);
        }
    }

    private static void printElements(Element e, String parent) {
        String camel = camelize(e.tagName());
        String clazz = capitalize(camel);
        String name = computeVarName(camel);
        System.out.println("");
        System.out.println(clazz + " " + name + " = new " + clazz + "();");
        System.out.println(parent + ".add(" + name + ");");
        if (e.children().isEmpty() && e.text() != null) {
            System.out.println(name + ".setText(\"" + e.text() + "\");");
        }
        for (Attribute attr : e.attributes()) {
            printMethodCall(name, attr.getKey(), attr.getValue());
        }
        Elements children = e.children();
        for (Element ele : children) {
            printElements(ele, name);
        }
    }

    private static void printMethodCall(String var, String input, String val) {
        String method = "";
        if (input.contains("(")) {
            method = var + ".add" + capitalize(camelize(input.replaceFirst("\\((.*)\\)", "$1")) + "Listener") +  "(e -> " + val.replace("$server", "this") + ");";
        } else if ("class".equals(input)) {
            for (String s : val.split(" +")) {
                method += var + ".addClassName(\"" + s + "\")s;\n";
            }
        } else {
            method = var + ".set" + capitalize(camelize(input)) + "(" + (val == null || val.isEmpty() ? "true" : ("\"" + val + "\"")) + ")";
        }
        System.out.println(method);
    }
}
