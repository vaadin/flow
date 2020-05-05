/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.plugin.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;
import static elemental.json.impl.JsonUtil.stringify;

/**
 * Internal goal to generate dev dependencies versions.
 *
 * @author Vaadin Ltd
 *
 */
@Mojo(name = "lock-dev-deps", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class LockDevDepVersionsMojo extends FlowModeAbstractMojo {

    private static final String DEPENDENCIES = "dependencies";
    private static final String VERSION = "version";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File npmFolder;

    /**
     * The folder where dependencies file will be generated. Default is project
     * build dir.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    public File generatedDependenciesFolder;

    /**
     * The path to the generated dependencies file inside
     * {@link #generatedDependenciesFolder}.
     */
    @Parameter
    private String generatedDependencies;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File targetFile = new File(generatedDependenciesFolder,
                generatedDependencies);
        try {
            FileUtils.forceMkdirParent(targetFile);
        } catch (IOException exception) {
            throw new MojoExecutionException(
                    "Can't make directories for the generated file", exception);
        }

        String content = listDevDependencies();

        JsonObject result = Json.createObject();
        JsonObject object = Json.parse(content);
        collectDeps(result, object);

        readVersionsFromPackageJson(result);

        try {
            FileUtils.write(targetFile, stringify(result, 2) + "\n",
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoFailureException(
                    "Couldn't write dependencies into the target file", e);
        }
    }

    @Override
    boolean isDefaultCompatibility() {
        return false;
    }

    private void readVersionsFromPackageJson(JsonObject versions) {
        File nodeModules = new File(npmFolder, NODE_MODULES);
        for (String key : versions.keys()) {
            File module = new File(nodeModules, key);
            File pkgJson = new File(module, Constants.PACKAGE_JSON);
            if (pkgJson.exists()) {
                setPackageVersion(versions, key, pkgJson);
            } else {
                getLog().warn("Couldn't find " + Constants.PACKAGE_JSON
                        + " for '" + key + "' module");
            }
        }
    }

    private void setPackageVersion(JsonObject versions, String key,
            File pkgJson) {
        try {
            JsonObject pkg = Json.parse(FileUtils.readFileToString(pkgJson,
                    StandardCharsets.UTF_8));
            if (pkg.hasKey(VERSION)
                    && pkg.get(VERSION).getType().equals(JsonType.STRING)) {
                versions.put(key, pkg.getString(VERSION));
            }
        } catch (IOException exception) {
            getLog().warn("Couldn't read " + Constants.PACKAGE_JSON + " for '"
                    + key + "' module", exception);
        }
    }

    private String listDevDependencies() throws MojoFailureException {
        FrontendTools tools = new FrontendTools(npmFolder.getAbsolutePath(),
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        List<String> command = tools.getNpmExecutable();
        command.add("ls");
        command.add("-json");
        command.add("-dev");

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.environment().put("ADBLOCK", "1");
        builder.directory(npmFolder);

        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        StringBuilder content = new StringBuilder();
        Process process = null;
        try {
            process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String stdoutLine;
                while ((stdoutLine = reader.readLine()) != null) {
                    content.append(stdoutLine);
                    content.append('\n');
                }
            }

            int errorCode = process.waitFor();
            Logger logger = LoggerFactory
                    .getLogger(LockDevDepVersionsMojo.class);
            if (errorCode != 0) {
                logger.error("Couldn't run npm ls");
            } else {
                logger.debug("Dev dependencies are collected");
            }
        } catch (InterruptedException | IOException e) {
            throw new MojoFailureException(
                    "Couldn't collect dev dependencies from npm", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
        return content.toString();
    }

    private void collectDeps(JsonObject target, JsonObject dep) {
        if (!dep.hasKey(DEPENDENCIES)) {
            return;
        }
        JsonObject deps = dep.get(DEPENDENCIES);
        for (String key : deps.keys()) {
            JsonValue value = deps.get(key);
            if (value instanceof JsonObject) {
                addDependency(target, key, (JsonObject) value);
                collectDeps(target, (JsonObject) value);
            }
        }
    }

    private void addDependency(JsonObject target, String name, JsonObject dep) {
        if (dep.hasKey(VERSION)) {
            String version = dep.getString(VERSION);
            target.put(name, version);
        }
    }

}
