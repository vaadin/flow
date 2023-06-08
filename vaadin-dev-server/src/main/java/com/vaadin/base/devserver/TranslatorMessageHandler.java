package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.vaadin.base.devserver.editor.Editor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.component.internal.ComponentTracker.Location;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.VaadinSession;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class TranslatorMessageHandler {

    private static final List<String> potentialLocales = List.of("de", "fi_FI",
            "sv_SE", "nn_NO", "es", "zh_CN", "eo", "tlh","la");

    private AbstractConfiguration configuration;
    private static final Set<String> componentWithLabelConstructor = new HashSet<>();
    static {
        addComponentWithLabelConstructor("com.vaadin.flow.component.html.H1");
        addComponentWithLabelConstructor("com.vaadin.flow.component.html.H2");
        addComponentWithLabelConstructor("com.vaadin.flow.component.html.H3");
        addComponentWithLabelConstructor("com.vaadin.flow.component.html.H4");
        addComponentWithLabelConstructor("com.vaadin.flow.component.html.H5");
        addComponentWithLabelConstructor("com.vaadin.flow.component.html.H6");
        addComponentWithLabelConstructor(
                "com.vaadin.flow.component.html.Paragraph");
        addComponentWithLabelConstructor("com.vaadin.flow.component.html.Span");
        addComponentWithLabelConstructor(
                "com.vaadin.flow.component.button.Button");
        addComponentWithLabelConstructor("AppNav");
        addComponentWithLabelConstructor("AppNavItem");
        addComponentWithLabelConstructor("SideNav");
        addComponentWithLabelConstructor("SideNavItem");
        addComponentWithLabelConstructor(
                "com.vaadin.flow.component.textfield.TextField");
        addComponentWithLabelConstructor(
                "com.vaadin.flow.component.select.Select");
        addComponentWithLabelConstructor(
                "com.vaadin.flow.component.combobox.ComboBox");
        addComponentWithLabelConstructor(
                "com.vaadin.flow.component.checkbox.Checkbox");
        addComponentWithLabelConstructor(
                "com.vaadin.flow.component.textarea.TextArea");
        addComponentWithLabelConstructor(
                "com.vaadin.flow.component.datepicker.DatePicker");
        addComponentWithLabelConstructor(
                "com.vaadin.flow.component.datetimepicker.DateTimePicker");
        addComponentWithLabelConstructor(
                "com.vaadin.flow.component.notification.Notification");
    }

    public TranslatorMessageHandler(AbstractConfiguration configuration) {
        this.configuration = configuration;
    }

    private static void addComponentWithLabelConstructor(String cls) {
        componentWithLabelConstructor.add(cls);
        if (cls.contains(".")) {
            componentWithLabelConstructor
                    .add(cls.substring(cls.lastIndexOf('.') + 1));
        }
    }

    public boolean handle(String command, JsonObject data) {
        if (command.equals("translate")) {
            JsonArray localesJson = data.getArray("locales");
            List<String> locales = new ArrayList<>();
            for (int i = 0; i < localesJson.length(); i++) {
                locales.add(localesJson.getString(i));
            }
            translateUI(data, locales);
            return true;
        }
        return false;
    }

    private void translateUI(JsonObject data,
            List<String> selectedLocaleStrings) {
        int uiId = (int) data.getNumber("uiId");
        VaadinSession session = VaadinSession.getCurrent();

        List<String> currentAndNewLocaleStrings = new ArrayList<>(
                selectedLocaleStrings);
        for (String localeString : potentialLocales) {
            if (!currentAndNewLocaleStrings.contains(localeString)
                    && hasTranslation(localeString)) {
                currentAndNewLocaleStrings.add(localeString);
            }
        }
        session.accessSynchronously(() -> {
            UI ui = session.getUIById(uiId);
            List<HasElement> activeRoutes = ui.getInternals()
                    .getActiveRouterTargetsChain();
            String mainPackage = "com.example.application";
            String translatorClass = mainPackage + ".Translator";
            String translatorImport = translatorClass + "._T";

            Map<String, String> originalTexts = new HashMap<>();
            Map<File, String> toWrite = new HashMap<>();

            for (HasElement viewOrLayout : activeRoutes) {
                Location location = ComponentTracker
                        .findAttach((Component) viewOrLayout);

                File javaFile = location.findJavaFile(configuration);
                try {
                    CompilationUnit cu = Editor
                            .parseSource(Editor.readFile(javaFile));
                    if (!Editor.hasImport(cu, translatorImport)) {
                        modifyForTranslations(translatorImport, cu);
                    }
                    originalTexts.putAll(findTranslations(cu));

                    if (isAppLayout(viewOrLayout)) {
                        addLocaleSelect(cu, currentAndNewLocaleStrings);
                    }

                    String newSource = LexicalPreservingPrinter.print(cu);
                    toWrite.put(javaFile, newSource);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            Map<String, Map<String, String>> translations = new HashMap<>();
            for (String localeString : selectedLocaleStrings) {
                Properties existingProperties = new Properties();
                InputStream existingBundle = getClass().getClassLoader()
                        .getResourceAsStream(
                                getTranslationFilename(localeString));

                if (existingBundle != null) {
                    try {
                        existingProperties.load(existingBundle);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            existingBundle.close();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                Map<String, String> needsTranslation = new HashMap<>(
                        originalTexts);
                needsTranslation.entrySet().removeIf(entry -> {
                    String translationKey = getTranslationKey(entry.getKey());
                    return existingProperties.containsKey(translationKey);
                });

                if (!needsTranslation.isEmpty()) {
                    translations.put(localeString, translate(
                            getLocale(localeString), needsTranslation));
                }
            }

            for (String localeString : selectedLocaleStrings) {
                try {
                    if (translations.containsKey(localeString)) {
                        writeTranslationFile(localeString,
                                translations.get(localeString));
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                writeTranslationFile(null, originalTexts);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            try {
                copyTranslator(mainPackage);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            for (File f : toWrite.keySet()) {
                try {
                    FileUtils.write(f, toWrite.get(f), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

    }

    private boolean hasTranslation(String localeString) {
        return getClass().getClassLoader()
                .getResource(getTranslationFilename(localeString)) != null;
    }

    private void copyTranslator(String mainPackage) throws IOException {
        File targetFolder = new File(configuration.getJavaSourceFolder(),
                mainPackage.replace(".", File.separator));
        File target = new File(targetFolder, "Translator.java");
        if (target.exists()) {
            return;
        }

        String source = IOUtils.toString(
                getClass().getResource("Translator.java"),
                StandardCharsets.UTF_8);
        source = source.replace("package com.vaadin.base.devserver",
                "package " + mainPackage);
        FileUtils.write(target, source, StandardCharsets.UTF_8);
    }

    private boolean isAppLayout(HasElement viewOrLayout) {
        try {
            return (Class
                    .forName("com.vaadin.flow.component.applayout.AppLayout")
                    .isAssignableFrom(viewOrLayout.getClass()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addLocaleSelect(CompilationUnit cu,
            List<String> localeStrings) {
        TypeDeclaration<?> type = cu.getType(0);

        String methodName = "addLocaleSelect";
        List<MethodDeclaration> existing = type.getMethodsByName(methodName);
        MethodDeclaration createLocaleSelect;
        if (existing.size() == 1) {
            createLocaleSelect = existing.get(0);
        } else {
            createLocaleSelect = new MethodDeclaration(
                    new NodeList<>(Modifier.privateModifier()), new VoidType(),
                    methodName);
            type.addMember(createLocaleSelect);
        }

        String localeCode = localeStrings.stream().map(localeString -> {
            if (localeString.contains("_")) {
                String[] parts = localeString.split("_", 2);
                return String.format("new Locale(\"%s\",\"%s\")", parts[0],
                        parts[1]);
            } else {
                return String.format("new Locale(\"%s\")", localeString);
            }
        }).collect(Collectors.joining(", "));

        BlockStmt body = createLocaleSelect.getBody().get();
        body.getStatements().clear();
        String code = String.format(
                """
                        Select<Locale> localeSelect = new Select<>();
                        localeSelect.setLabel("Language");
                        localeSelect.setItems(%s);
                        localeSelect.setItemLabelGenerator(locale -> locale.getDisplayLanguage());
                        localeSelect.setValue(UI.getCurrent().getLocale());
                        localeSelect.addValueChangeListener(e -> { VaadinSession.getCurrent().setLocale(e.getValue()); UI.getCurrent().getPage().reload(); });
                        addToDrawer(localeSelect);
                            """,
                localeCode);

        for (String line : code.split("\n")) {
            body.addStatement(line);
        }

        BlockStmt constructor = type.getConstructors().get(0).getBody();
        if (!hasMethodCall(methodName, constructor)) {
            MethodCallExpr addToDrawerCall = new MethodCallExpr(methodName);
            constructor.addStatement(addToDrawerCall);
        }

        addImportIfNeeded(cu, Locale.class.getName(), false);
        addImportIfNeeded(cu, UI.class.getName(), false);
        addImportIfNeeded(cu, "com.vaadin.flow.component.select.Select", false);
        addImportIfNeeded(cu, VaadinSession.class.getName(), false);
    }

    private boolean hasMethodCall(String methodName, BlockStmt constructor) {
        for (Statement s : constructor.getStatements()) {
            if (s.isExpressionStmt()) {
                Expression expr = s.asExpressionStmt().getExpression();
                if (expr.isMethodCallExpr()) {
                    String name = expr.asMethodCallExpr().getNameAsString();
                    if (name.equals(methodName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addImportIfNeeded(CompilationUnit cu, String name,
            boolean isStatic) {
        if (!Editor.hasImport(cu, name)) {
            cu.getImports().add(new ImportDeclaration(name, isStatic, false));
        }
    }

    private Map<String, String> findTranslations(CompilationUnit cu) {
        Set<String> set = getMethodCalls(cu, "_T", null).stream()
                .map(methodCall -> methodCall.getArgument(0)
                        .asStringLiteralExpr().asString())
                .collect(Collectors.toSet());

        return set.stream().collect(Collectors.toMap(key -> key, key -> key));
    }

    private List<MethodCallExpr> findAllSetLabelCalls(CompilationUnit cu) {
        return getMethodCalls(cu, "setLabel", null);
    }

    private List<MethodCallExpr> findAllNotificationShowCalls(
            CompilationUnit cu) {
        return getMethodCalls(cu, "show", "Notification");
    }

    private List<MethodCallExpr> getMethodCalls(CompilationUnit cu,
            String methodName, String classNameForStaticCall) {
        ArrayList<MethodCallExpr> methodCalls = new ArrayList<>();
        cu.accept(new FindMethodCallsVisitor(methodName, classNameForStaticCall,
                methodCalls), null);
        return methodCalls;
    }

    public static class FindMethodCallsVisitor
            extends VoidVisitorAdapter<Object> {
        private String methodName;
        private ArrayList<MethodCallExpr> methodCalls;
        private String classNameForStaticCall;

        public FindMethodCallsVisitor(String methodName,
                String classNameForStaticCall,
                ArrayList<MethodCallExpr> methodCalls) {
            this.methodName = methodName;
            this.classNameForStaticCall = classNameForStaticCall;
            this.methodCalls = methodCalls;
        }

        public void visit(MethodCallExpr e, Object o) {
            if (e.getNameAsString().equals(methodName)) {
                if (classNameForStaticCall != null) {
                    if (!e.getScope().isPresent()) {
                        return;
                    }
                    Expression scope = e.getScope().get();
                    if (scope.isNameExpr() && scope.asNameExpr()
                            .getNameAsString().equals(classNameForStaticCall)) {
                        methodCalls.add(e);
                    }
                } else {
                    methodCalls.add(e);
                }
            } else {
                e.getChildNodes().forEach(node -> node.accept(this, null));
            }
        }
    }

    private List<ObjectCreationExpr> findAllObjectCreationExpr(
            CompilationUnit cu) {
        ArrayList<ObjectCreationExpr> objectCreationExpressions = new ArrayList<>();

        cu.accept(new VoidVisitorAdapter<Object>() {
            public void visit(ObjectCreationExpr e, Object o) {
                objectCreationExpressions.add(e);
            }
        }, null);
        return objectCreationExpressions;
    }

    private void writeTranslationFile(String localeString,
            Map<String, String> keyValues) throws IOException {
        String filename = getTranslationFilename(localeString);
        File file = new File(configuration.getJavaResourceFolder(), filename);
        String data = toProperties(keyValues);
        if (file.exists()) {
            String existingData = FileUtils.readFileToString(file,
                    StandardCharsets.UTF_8);
            data = existingData + "\n" + data;
        }
        FileUtils.write(file, data, StandardCharsets.UTF_8);

    }

    private String getTranslationFilename(String localeString) {
        String filename = "translations";
        if (localeString != null) {
            filename += "_" + localeString;
        }
        filename += ".properties";
        return filename;
    }

    private String toProperties(Map<String, String> keyValues) {
        return keyValues.entrySet().stream().map((entry) -> {
            String lookupKey = getTranslationKey(entry.getKey());
            return lookupKey + "=" + entry.getValue();
        }).collect(Collectors.joining("\n"));
    }

    private String getTranslationKey(String key) {
        return key.replaceAll(" ", "-").replaceAll("[^a-zA-Z0-9-_]", "")
                .replaceAll("(-*)$", "");
    }

    private void modifyForTranslations(String translatorImport,
            CompilationUnit cu) {

        // Add _T static import
        addImportIfNeeded(cu, translatorImport, true);

        // Find string literal component constructors
        findAllObjectCreationExpr(cu).stream()
                .forEach(expr -> replaceStringLiterals(expr));

        // Find setLabel with string literals or string literal + value
        findAllSetLabelCalls(cu).stream()
                .forEach(expr -> replaceSetLabelArg(expr));

        // Find Notification.show with string literals or string literal + value
        findAllNotificationShowCalls(cu).stream()
                .forEach(expr -> replaceSetLabelArg(expr));
    }

    private void replaceSetLabelArg(MethodCallExpr setLabelCall) {
        for (Expression e : setLabelCall.getArguments()) {
            replaceSetLabelArg(e);
        }

    }

    private void replaceSetLabelArg(Expression e) {
        if (e.isStringLiteralExpr()) {
            MethodCallExpr wrappedLiteral = new MethodCallExpr("_T", e.clone());
            e.replace(wrappedLiteral);
        } else if (e.isBinaryExpr()) {
            BinaryExpr bin = e.asBinaryExpr();
            replaceSetLabelArg(bin.getLeft());
            replaceSetLabelArg(bin.getRight());
        }

    }

    private void replaceStringLiterals(ObjectCreationExpr expr) {

        // new Textfield("your name")
        if (!componentWithLabelConstructor
                .contains(expr.getType().getNameAsString())) {
            return;
        }
        for (Expression e : expr.getArguments()) {
            if (e.isStringLiteralExpr()) {
                // Cannot use e here as replacing won't work anymore
                // then
                MethodCallExpr wrappedLiteral = new MethodCallExpr("_T",
                        e.clone());
                e.replace(wrappedLiteral);
            }
        }

    }

    private Map<String, String> translate(Locale locale,
            Map<String, String> originalTexts) {
        String query = "Translate the following messages in json so that the key remains the same but the value is translated into the "
                + locale.getDisplayLanguage() + " locale ";
        query += JsonUtils.mapToJson(originalTexts).toJson();
        try {
            String response = OpenAI.ask(query, System.getenv("OPENAI_KEY"));
            System.out.println("Response: " + response);
            Map<String, String> translations = new HashMap<>();
            JsonObject json = Json.parse(response);
            for (String key : json.keys()) {
                translations.put(key, json.getString(key));
            }
            return translations;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return originalTexts;
    }

    private Locale getLocale(String locale) {
        String[] parts = locale.split("_", 2);
        if (parts.length == 1) {
            return new Locale(parts[0]);
        }
        return new Locale(parts[0], parts[1]);
    }

}
