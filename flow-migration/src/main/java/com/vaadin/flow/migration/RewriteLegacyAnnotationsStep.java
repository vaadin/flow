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
package com.vaadin.flow.migration;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.utils.FlowFileUtils;

/**
 * Rewrites {@link HtmlImport}/{@link StyleSheet} annotation to corresponding
 * {@link JsModule} annotation.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class RewriteLegacyAnnotationsStep extends ClassPathIntrospector {

    private static final String HTML_EXTENSION = ".html";
    private static final String CSS_EXTENSION = ".css";
    private final URL compiledClassesURL;
    private final Collection<File> sourceRoots;

    private static final String BOWER_COMPONENT_PREFIX = AbstractCopyResourcesStep.BOWER_COMPONENTS
            + "/";

    private static final String CLASS_DECLARATION_PATTERN = "(\\s|public|final|abstract|private|static|protected)*\\s+class\\s+%s(|<|>|\\?|\\w|\\s|,|\\&)*\\s+((extends\\s+(\\w|<|>|\\?|,)+)|(implements\\s+(|<|>|\\?|\\w|\\s|,)+( ,(\\w|<|>|\\?|\\s|,))*))?\\s*\\{";

    private final Map<Class<?>, Pattern> compiledClassPatterns = new HashMap<>();

    private final Map<String, Pattern> compiledReplacePatterns = new HashMap<>();

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
    public RewriteLegacyAnnotationsStep(File compiledClassesDir,
            ClassFinder finder, Collection<File> sourceRoots) {
        super(finder);
        compiledClassesURL = FlowFileUtils.convertToUrl(compiledClassesDir);
        this.sourceRoots = sourceRoots;
    }

    /**
     * Search for java files in the project and replace {@link HtmlImport} to
     * {@link JsModule} annotation with updated value.
     */
    public void rewrite() {
        Map<Class<?>, Map<Class<? extends Annotation>, Collection<String>>> annotationPerClass = new HashMap<>();

        collectAnnotatedClasses(
                loadClassInProjectClassLoader(HtmlImport.class.getName()),
                annotationPerClass);
        collectAnnotatedClasses(
                loadClassInProjectClassLoader(StyleSheet.class.getName()),
                annotationPerClass);

        annotationPerClass.forEach(this::rewriteAnnotations);
    }

    private void collectAnnotatedClasses(Class<? extends Annotation> annotation,
            Map<Class<?>, Map<Class<? extends Annotation>, Collection<String>>> annotationPerClass) {
        Stream<Class<?>> classes = getAnnotatedClasses(annotation);

        classes.forEach(
                clazz -> handleClass(clazz, annotation, annotationPerClass));
    }

    private void handleClass(Class<?> clazz,
            Class<? extends Annotation> annotation,
            Map<Class<?>, Map<Class<? extends Annotation>, Collection<String>>> annotationPerClass) {
        URL location = clazz.getProtectionDomain().getCodeSource()
                .getLocation();
        if (!compiledClassesURL.toExternalForm()
                .equals(location.toExternalForm())) {
            return;
        }
        Collection<String> paths = collectAnnotationValues(clazz, annotation);
        if (!paths.isEmpty()) {
            Map<Class<? extends Annotation>, Collection<String>> annotationPaths = annotationPerClass
                    .computeIfAbsent(clazz, cl -> new HashMap<>());
            annotationPaths.put(annotation, paths);
        }
    }

    private Collection<String> collectAnnotationValues(Class<?> clazz,
            Class<? extends Annotation> annotationType) {
        Annotation[] annotationsByType = clazz
                .getAnnotationsByType(annotationType);
        Collection<String> result = new ArrayList<>();
        for (Annotation annotation : annotationsByType) {
            String path = invokeAnnotationMethod(annotation, "value")
                    .toString();
            result.add(path);

        }
        return result;
    }

    private void rewriteAnnotations(Class<?> clazz,
            Map<Class<? extends Annotation>, Collection<String>> annotations) {
        Collection<File> javaFiles = findJavaSourceFiles(clazz);
        if (javaFiles.isEmpty()) {
            LoggerFactory.getLogger(RewriteLegacyAnnotationsStep.class).debug(
                    "Could not find Java source code for class '{}'", clazz);
        } else {
            javaFiles
                    .forEach(javaFile -> rewrite(javaFile, clazz, annotations));
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
            compiledClassPatterns.put(clazz, classDeclarationPattern);
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
            Map<Class<? extends Annotation>, Collection<String>> annotations) {
        String content = readFile(javaFile);
        Pattern classDeclarationPattern = compiledClassPatterns.get(clazz);
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

        for (Entry<Class<? extends Annotation>, Collection<String>> entry : annotations
                .entrySet()) {

            beforeClassDeclaration = rewrite(javaFile, beforeClassDeclaration,
                    entry.getKey(), entry.getValue());
        }

        try {
            FileUtils.write(javaFile,
                    beforeClassDeclaration
                            + content.substring(classDeclarationStart),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLogger().warn("Could not write source code back to file '{}'",
                    javaFile);
        }
    }

    private String rewrite(File javaFile, String content,
            Class<? extends Annotation> annotation, Collection<String> paths) {
        String result = content;
        // replace FQN first
        result = replace(annotation.getName(), result,
                "\\b" + annotation.getName().replace(".", "\\.") + "\\b",
                JsModule.class.getName());

        // replace annotation attached to the class with @ sign
        result = replace(annotation.getSimpleName(), result,
                "(\\s*)@" + annotation.getSimpleName() + "\\b",
                "$1@" + JsModule.class.getSimpleName());
        for (String path : paths) {
            result = result.replaceAll(
                    String.format("\"%s\"", Pattern.quote(path)),
                    String.format("\"%s\"", rewritePath(path,
                            externalComponent -> handleBowerComponentImport(
                                    javaFile, externalComponent),
                            nonVaadinComponentPath -> handleNonVaadinComponent(
                                    javaFile, nonVaadinComponentPath))));
        }
        return result;
    }

    /**
     * Does the same as {@link String#replaceAll(String, String)} but caches the
     * compiled pattern
     */
    private String replace(String patternKey, String content, String regexp,
            String replacement) {
        Pattern pattern = compiledReplacePatterns.computeIfAbsent(patternKey,
                key -> Pattern.compile(regexp));
        return pattern.matcher(content).replaceAll(replacement);
    }

    private void handleBowerComponentImport(File javaFile,
            String bowerComponentPath) {
        getLogger().warn(
                "External bower component {} is imported in the {} file, "
                        + "a converted '@JsModule' "
                        + "annotation requires also a `@NpmPackage` annotation with "
                        + "a module name and a version. The migrated project won't "
                        + "be built without this information.",
                bowerComponentPath, javaFile.getPath());
    }

    private void handleNonVaadinComponent(File javaFile,
            String nonVaadinComponentPath) {
        getLogger().error(
                "In {} file, added a JS module import '@JsModule(\"{}\")' "
                        + "that you need to manually map to the correct package vendor from npm",
                javaFile.getPath(), nonVaadinComponentPath);
    }

    private String rewritePath(String path,
            Consumer<String> externalComponentHandler,
            Consumer<String> nonVaadinComponentHandler) {
        String result = path;
        result = rewriteExtension(result, HTML_EXTENSION);
        result = rewriteExtension(result, CSS_EXTENSION);

        result = removePrefix(result,
                ApplicationConstants.BASE_PROTOCOL_PREFIX);
        result = removePrefix(result,
                ApplicationConstants.FRONTEND_PROTOCOL_PREFIX);
        result = removePrefix(result,
                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX);

        if (result.startsWith(BOWER_COMPONENT_PREFIX)) {
            result = result.substring(
                    AbstractCopyResourcesStep.BOWER_COMPONENTS.length());
            externalComponentHandler.accept(result);
            if (result.startsWith("/vaadin")) {
                result = "@vaadin" + result;
            } else {
                result = "NPM_VENDOR" + result;
                getLogger().warn("Don't know how to resolve Html import '{}'",
                        path);
                nonVaadinComponentHandler.accept(result);
            }
        } else if (result.startsWith("/")) {
            result = "." + result;
        } else if (!result.startsWith("./")) {
            result = "./" + result;
        }
        return result;
    }

    private String rewriteExtension(String path, String extension) {
        if (path.endsWith(extension)) {
            return path.substring(0, path.length() - extension.length())
                    + ".js";
        }
        return path;
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
        return LoggerFactory.getLogger(RewriteLegacyAnnotationsStep.class);
    }
}
