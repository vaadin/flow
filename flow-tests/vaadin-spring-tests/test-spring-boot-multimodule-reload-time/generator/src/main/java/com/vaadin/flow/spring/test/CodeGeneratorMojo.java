/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.spring.test;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.samskivert.mustache.Mustache;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class CodeGeneratorMojo extends AbstractMojo {

    public static final String ROUTE_JAVA_TEMPLATE = "route-java.mustache";
    public static final String ADDONS_ROUTE_JAVA_TEMPLATE = "addons-route-java.mustache";
    public static final String SERVICE_JAVA_TEMPLATE = "service-java.mustache";
    public static final String CSS_IMPORT_TEMPLATE = "css-import.mustache";
    public static final String JS_MODULE_TEMPLATE = "js-module.mustache";

    /**
     * Output directory.
     */
    @Parameter(name = "output", property = "vaadin.test.codegen.maven.plugin.output", defaultValue = "${project.build.directory}/generated-sources/codegen")
    private File output;

    /**
     * The package for generated classes.
     */
    @Parameter(name = "apiPackage", defaultValue = "com.vaadin.flow.spring.test")
    private String apiPackage;

    @Parameter(name = "numberOfGeneratedRoutes", property = "vaadin.test.codegen.maven.plugin.routes", defaultValue = "500")
    private int numberOfGeneratedRoutes;

    @Parameter(name = "numberOfGeneratedServicesPerRoute", property = "vaadin.test.codegen.maven.plugin.services.per.route", defaultValue = "1")
    private int numberOfGeneratedServicesPerRoute;

    @Parameter(name = "numberOfGeneratedCssImportsPerRoute", property = "vaadin.test.codegen.maven.plugin.cssimports.per.route", defaultValue = "0")
    private int numberOfGeneratedCssImportsPerRoute;

    @Parameter(name = "numberOfGeneratedJsModulesPerRoute", property = "vaadin.test.codegen.maven.plugin.jsmodules.per.route", defaultValue = "0")
    private int numberOfGeneratedJsModulesPerRoute;

    @Parameter(name = "includeAddons", property = "vaadin.test.codegen.maven.plugin.include.addons", defaultValue = "false")
    private boolean includeAddons;

    /**
     * Skip the execution.
     */
    @Parameter(name = "skip", property = "vaadin.test.codegen.skip", required = false, defaultValue = "false")
    private Boolean skip;

    /**
     * The project being built.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project}")
    private MavenProject project;

    @Override
    public synchronized void execute()
            throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Code generation is skipped.");
            return;
        }

        try {
            getLog().info("Code generation started.");

            generateRoutes("test-route", numberOfGeneratedRoutes);

            addCompileSourceRootIfConfigured();
            getLog().info("Code generation done.");
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException(
                    "Code generation failed. See above for the full exception.");
        }
    }

    private void generateRoutes(String routePrefix, int numberOfRoutes)
            throws IOException {
        // start by deleting existing package
        FileUtils.deleteDirectory(new File(
                output.toString() + "/" + apiPackage.replace(".", "/")));
        // ... and generated frontend css and js
        FileUtils.deleteDirectory(new File(project.getBasedir().toString()
                + "/src/main/frontend/generated-css"));
        FileUtils.deleteDirectory(new File(project.getBasedir().toString()
                + "/src/main/frontend/generated-js"));

        int servicesGeneratedTotal = 0;
        int cssImportsGeneratedTotal = 0;
        int jsModulesGeneratedTotal = 0;
        for (int index = 0; index < numberOfRoutes; index++) {
            var context = generateRoute(routePrefix, index);
            servicesGeneratedTotal += context.getServices().size();
            cssImportsGeneratedTotal += context.getCssImports().size();
            jsModulesGeneratedTotal += context.getJsModules().size();
        }
        if (includeAddons) {
            generateAddonsRoute();
        }

        getLog().info(String.format(
                "Generated %s route(s) / %s Spring service(s) / %s CssImport(s) / %s JsModule(s) in total.%s",
                numberOfRoutes, servicesGeneratedTotal,
                cssImportsGeneratedTotal, jsModulesGeneratedTotal,
                (includeAddons) ? " Generated 'addons' route." : ""));
        if (numberOfRoutes > 0 && (cssImportsGeneratedTotal > 0
                || jsModulesGeneratedTotal > 0)) {
            getLog().info("Frontend files generated in "
                    + project.getBasedir().toString() + "/src/main/frontend");
        }
    }

    private List<JavaSpringServiceContext> generateServices(
            JavaClassContext owner, int numberOfServices) throws IOException {
        var result = new ArrayList<JavaSpringServiceContext>();
        for (int index = 0; index < numberOfServices; index++) {
            result.add(generateSpringComponent(owner, "singleton", index));
        }
        return result;
    }

    private List<CssImportContext> generateCssImports(JavaClassContext owner,
            int numberOfCssImports) throws IOException {
        var result = new ArrayList<CssImportContext>();
        for (int index = 0; index < numberOfCssImports; index++) {
            result.add(generateCssImport(owner, index));
        }
        return result;
    }

    private List<JsModuleContext> generateJsModules(JavaClassContext owner,
            int numberOfJsModules) throws IOException {
        var result = new ArrayList<JsModuleContext>();
        for (int index = 0; index < numberOfJsModules; index++) {
            result.add(generateJsModule(owner, index));
        }
        return result;
    }

    private JavaRouteContext generateRoute(String routePrefix, int index)
            throws IOException {

        JavaRouteContext context = new JavaRouteContext();
        context.setPackages(apiPackage);
        context.setClassName("MyRoute" + index);
        context.setRoute(routePrefix + "-" + index);

        var services = generateServices(context,
                numberOfGeneratedServicesPerRoute);
        context.setServices(services);

        var cssImports = generateCssImports(context,
                numberOfGeneratedCssImportsPerRoute);
        context.setCssImports(cssImports);

        var jsModules = generateJsModules(context,
                numberOfGeneratedJsModulesPerRoute);
        context.setJsModules(jsModules);

        generateJavaFileByMustacheTemplate(context, ROUTE_JAVA_TEMPLATE);

        return context;
    }

    private void generateAddonsRoute() throws IOException {

        JavaRouteContext context = new JavaRouteContext();
        context.setPackages(apiPackage);
        context.setClassName("AddonsRoute");

        generateJavaFileByMustacheTemplate(context, ADDONS_ROUTE_JAVA_TEMPLATE);
    }

    private JavaSpringServiceContext generateSpringComponent(
            JavaClassContext owner, String scope, int index)
            throws IOException {
        JavaSpringServiceContext context = new JavaSpringServiceContext();
        context.setPackages(apiPackage);
        context.setClassName("Service" + owner.getClassName() + "_" + index);
        context.setScope(scope);
        context.setVariableName("serviceVariable" + index);

        generateJavaFileByMustacheTemplate(context, SERVICE_JAVA_TEMPLATE);
        return context;
    }

    private CssImportContext generateCssImport(JavaClassContext owner,
            int index) throws IOException {
        CssImportContext context = new CssImportContext();
        context.setTargetStyleName("css-import-"
                + owner.getClassName().toLowerCase(Locale.ROOT) + "-" + index);
        context.setValue(
                "./generated-css/" + context.getTargetStyleName() + ".css");

        generateFrontendFileByMustacheTemplate(context, CSS_IMPORT_TEMPLATE,
                getFrontendPath(context.getValue()));
        return context;
    }

    private JsModuleContext generateJsModule(JavaClassContext owner, int index)
            throws IOException {
        JsModuleContext context = new JsModuleContext();
        context.setTag("js-module-"
                + owner.getClassName().toLowerCase(Locale.ROOT) + "-" + index);
        context.setValue("./generated-js/" + context.getTag() + ".js");
        context.setName(
                Stream.of(context.getTag().split("-"))
                        .map(str -> str.substring(0, 1).toUpperCase(Locale.ROOT)
                                + str.substring(1))
                        .collect(Collectors.joining()));

        generateFrontendFileByMustacheTemplate(context, JS_MODULE_TEMPLATE,
                getFrontendPath(context.getValue()));
        return context;
    }

    private void generateJavaFileByMustacheTemplate(JavaClassContext context,
            String templateFileName) throws IOException {
        File newFile = new File(
                output.toString() + "/" + apiPackage.replace(".", "/") + "/"
                        + context.getClassName() + ".java");
        generateFileByMustacheTemplate(context, templateFileName, newFile);
    }

    private static String getFrontendPath(String value) {
        return value.startsWith("./") ? value.substring(2) : value;
    }

    private void generateFrontendFileByMustacheTemplate(Object context,
            String templateFileName, String frontEndFilePath)
            throws IOException {
        File newFile = new File(project.getBasedir().toString()
                + "/src/main/frontend/" + frontEndFilePath);
        generateFileByMustacheTemplate(context, templateFileName, newFile);
    }

    private void generateFileByMustacheTemplate(Object context,
            String templateFileName, File newFile) throws IOException {
        newFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(newFile,
                StandardCharsets.UTF_8)) {
            Mustache.compiler()
                    .compile(new FileReader(project.getBasedir().toString()
                            + "/../generator/src/main/resources/"
                            + templateFileName, StandardCharsets.UTF_8))
                    .execute(context, writer);
        }
    }

    private void addCompileSourceRootIfConfigured() {
        project.addCompileSourceRoot(output.toString());
    }
}
