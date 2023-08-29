/*
 * Copyright 2000-2023 Vaadin Ltd.
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
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
    public static final String SERVICE_JAVA_TEMPLATE = "service-java.mustache";

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

        int servicesGeneratedTotal = 0;
        for (int index = 0; index < numberOfRoutes; index++) {
            servicesGeneratedTotal += generateRoute(routePrefix, index)
                    .getServices().size();
        }
        getLog().info(String.format(
                "Generated %s route(s) with %s Spring service(s) in total.",
                numberOfRoutes, servicesGeneratedTotal));
    }

    private List<JavaSpringServiceContext> generateServices(
            JavaClassContext owner, int numberOfServices) throws IOException {
        var result = new ArrayList<JavaSpringServiceContext>();
        for (int index = 0; index < numberOfServices; index++) {
            result.add(generateSpringComponent(owner, "singleton", index));
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

        generateJavaFileByMustacheTemplate(context, ROUTE_JAVA_TEMPLATE);

        return context;
    }

    private JavaSpringServiceContext generateSpringComponent(
            JavaClassContext owner, String scope, int index)
            throws IOException {
        JavaSpringServiceContext context = new JavaSpringServiceContext();
        context.setPackages(apiPackage);
        context.setClassName("Service" + owner.getClassName() + index);
        context.setScope(scope);
        context.setVariableName("serviceVariable" + index);

        generateJavaFileByMustacheTemplate(context, SERVICE_JAVA_TEMPLATE);
        return context;
    }

    private void generateJavaFileByMustacheTemplate(JavaClassContext context,
            String templateFileName) throws IOException {
        File newFile = new File(
                output.toString() + "/" + apiPackage.replace(".", "/") + "/"
                        + context.getClassName() + ".java");
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
