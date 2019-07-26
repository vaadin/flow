/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.plugin.migration;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.plugin.common.ClassPathIntrospector;
import com.vaadin.flow.plugin.common.FlowPluginFileUtils;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Rewrites {@link HtmlImport} annotation to corresponding {@link JsModule}
 * annotation.
 *
 * @author Vaadin Ltd
 *
 */
public class RewriteHtmlImportsStep extends ClassPathIntrospector {

    private static final String HTML_EXTENSION = ".html";
    private final URL compiledClassesURL;
    private final Collection<File> sourceRoots;

    private static final String CLASS_DECLARATION_PATTERN = "(\\s|public|final|abstract|private|static|protected)*\\s+class\\s+%s(|<|>|\\?|\\w|\\s|,|\\&)*\\s+((extends\\s+(\\w|<|>|\\?|,)+)|(implements\\s+(|<|>|\\?|\\w|\\s|,)+( ,(\\w|<|>|\\?|\\s|,))*))?\\s*\\{";

    private final Map<Class<?>, Pattern> compiledPatterns = new HashMap<>();

    /**
     * Creates a new instance using the {@code compiledClassesDir} directory (to
     * filter out classes which belongs to project only, not dependencies),
     * class {@code finder} instance and collection of project source roots to
     * search Java source files.
     *
     * @param compiledClassesDir
     *            directory with compiled classes
     * @param finder
     *            a class finder
     * @param sourceRoots
     *            project source root directories
     */
    public RewriteHtmlImportsStep(File compiledClassesDir, ClassFinder finder,
            Collection<File> sourceRoots) {
        super(finder);
        compiledClassesURL = FlowPluginFileUtils
                .convertToUrl(compiledClassesDir);
        this.sourceRoots = sourceRoots;
    }

    /**
     * Search for java files in the project and replace {@link HtmlImport} to
     * {@link JsModule} annotation with updated value.
     */
    public void rewrite() {
        Class<? extends Annotation> annotationInProjectContext = loadClassInProjectClassLoader(
                HtmlImport.class.getName());
        Stream<Class<?>> classes = getAnnotatedClasses(
                annotationInProjectContext);
        Map<Class<?>, Collection<String>> imports = new HashMap<>();
        classes.forEach(clazz -> handleClass(clazz, imports));

        imports.forEach(this::rewriteImports);
    }

    private void handleClass(Class<?> clazz,
            Map<Class<?>, Collection<String>> imports) {
        Class<? extends Annotation> annotationInProjectContext = loadClassInProjectClassLoader(
                HtmlImport.class.getName());
        URL location = clazz.getProtectionDomain().getCodeSource()
                .getLocation();
        if (!compiledClassesURL.toExternalForm()
                .equals(location.toExternalForm())) {
            return;
        }
        Annotation[] annotationsByType = clazz
                .getAnnotationsByType(annotationInProjectContext);
        for (Annotation annotation : annotationsByType) {
            String path = invokeAnnotationMethod(annotation, "value")
                    .toString();
            Collection<String> paths = imports.computeIfAbsent(clazz,
                    cls -> new ArrayList<>());
            paths.add(path);
        }
    }

    private void rewriteImports(Class<?> clazz, Collection<String> paths) {
        Collection<File> javaFiles = findJavaSourceFiles(clazz);
        if (javaFiles.isEmpty()) {
            LoggerFactory.getLogger(RewriteHtmlImportsStep.class).debug(
                    "Could not find Java source code for class '{}'", clazz);
        } else {
            javaFiles.forEach(javaFile -> rewrite(javaFile, clazz, paths));
        }
    }

    private Collection<File> findJavaSourceFiles(Class<?> clazz) {
        String packageName = clazz.getPackage().getName();
        String pckgPath = packageName.replace(".", "/");
        Collection<File> pkgDirs = new ArrayList<>();
        Collection<File> result = new ArrayList<>();
        for (File sourceRoot : sourceRoots) {
            File pkgFolder = new File(sourceRoot, pckgPath);
            if (!pkgFolder.exists()) {
                continue;
            }
            pkgDirs.add(pkgFolder);
            Class<?> topLevelClass = getTopLevelEnclosingClass(clazz);
            File mayBeJavaFile = new File(pkgFolder,
                    topLevelClass.getSimpleName() + ".java");
            if (mayBeJavaFile.exists()) {
                result.add(mayBeJavaFile);
                break;
            }
        }
        if (result.isEmpty()) {
            // the class doesn't have to be in its own java file, we need to
            // scan all files for its declaration...
            for (File pkg : pkgDirs) {
                result.addAll(findClassFiles(pkg, clazz));
            }

        }
        return result;
    }

    /**
     * Find {@code clazz} declaration in files inside the {@code pkgDir}.
     * <p>
     * The way which is used to find the class is not exact, so there may be
     * several matching files.
     */
    private Collection<File> findClassFiles(File pkgDir, Class<?> clazz) {
        Collection<File> result = new ArrayList<>();
        for (File file : pkgDir.listFiles()) {
            if (!file.isFile() || !file.getName().endsWith(".java")) {
                continue;
            }
            String content = readFile(file);
            if (content == null) {
                continue;
            }
            Pattern classDeclarationPattern = getClassDeclarationPattern(clazz);
            compiledPatterns.put(clazz, classDeclarationPattern);
            if (classDeclarationPattern.matcher(content).find()) {
                result.add(file);
            }
        }
        return result;
    }

    private String readFile(File file) {
        try {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLogger().warn("Could not read source code from file '{}'", file);
            return null;
        }
    }

    private void rewrite(File javaFile, Class<?> clazz,
            Collection<String> paths) {
        String content = readFile(javaFile);
        Pattern classDeclarationPattern = compiledPatterns.get(clazz);
        if (classDeclarationPattern == null) {
            classDeclarationPattern = getClassDeclarationPattern(clazz);
        }
        Matcher matcher = classDeclarationPattern.matcher(content);
        int classDeclarationStart = content.length();
        if (matcher.find()) {
            classDeclarationStart = matcher.start();
        } else {
            getLogger().debug(
                    "Implementation issue: unable to find class declaration inside {} java source file",
                    javaFile);
        }

        String beforeClassDeclaration = content.substring(0,
                classDeclarationStart);

        String rewritten = beforeClassDeclaration.replaceAll("\\bHtmlImport\\b",
                "JsModule");
        for (String path : paths) {
            rewritten = rewritten.replaceAll(
                    String.format("\"%s\"", Pattern.quote(path)),
                    String.format("\"%s\"", rewritePath(path)));
        }
        rewritten = rewritten + content.substring(classDeclarationStart);
        try {
            FileUtils.write(javaFile, rewritten, StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLogger().warn("Could not write source code back to file '{}'",
                    javaFile);
        }
    }

    private String rewritePath(String path) {
        String rewritten = path;
        if (rewritten.endsWith(HTML_EXTENSION)) {
            rewritten = rewritten.substring(0,
                    rewritten.length() - HTML_EXTENSION.length()) + ".js";
        }
        rewritten = removePrefix(rewritten,
                ApplicationConstants.BASE_PROTOCOL_PREFIX);
        rewritten = removePrefix(rewritten,
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);
        rewritten = removePrefix(rewritten,
                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX);

        if (rewritten.startsWith(AbstractCopyResourcesStep.BOWER_COMPONENTS)) {
            rewritten = rewritten.substring(
                    AbstractCopyResourcesStep.BOWER_COMPONENTS.length());
            if (rewritten.startsWith("/vaadin")) {
                rewritten = "@vaadin" + rewritten;
            } else {
                getLogger().warn("Don't know how to resolve Html import '{}'",
                        path);
            }
        } else if (rewritten.startsWith("/")) {
            rewritten = "." + rewritten;
        } else if (!rewritten.startsWith("./")) {
            rewritten = "./" + rewritten;
        }
        return rewritten;
    }

    private String removePrefix(String path, String prefix) {
        if (path.startsWith(prefix)) {
            return path.substring(prefix.length());
        }
        return path;
    }

    private Pattern getClassDeclarationPattern(Class<?> clazz) {
        return Pattern.compile(String.format(CLASS_DECLARATION_PATTERN,
                clazz.getSimpleName()));
    }

    private Class<?> getTopLevelEnclosingClass(Class<?> clazz) {
        Class<?> enclosingClass = clazz.getEnclosingClass();
        if (enclosingClass == null) {
            return clazz;
        }
        return getTopLevelEnclosingClass(enclosingClass);
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(RewriteHtmlImportsStep.class);
    }
}
