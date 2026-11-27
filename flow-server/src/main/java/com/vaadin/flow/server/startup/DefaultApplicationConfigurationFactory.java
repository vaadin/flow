/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.FileIOUtils;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.AbstractPropertyConfiguration;
import com.vaadin.flow.server.VaadinContext;

import static com.vaadin.flow.internal.FrontendUtils.TOKEN_FILE;
import static com.vaadin.flow.server.Constants.NPM_TOKEN;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.InitParameters.APPLICATION_PARAMETER_DEVMODE_ENABLE_SERIALIZE_SESSION;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;

/**
 * Default implementation of {@link ApplicationConfigurationFactory}.
 *
 * @author Vaadin Ltd
 *
 * @since 6.0
 */
@Component(service = ApplicationConfigurationFactory.class, property = Constants.SERVICE_RANKING
        + ":Integer=" + Integer.MIN_VALUE)
public class DefaultApplicationConfigurationFactory
        extends AbstractConfigurationFactory
        implements ApplicationConfigurationFactory {

    protected static class ApplicationConfigurationImpl extends
            AbstractPropertyConfiguration implements ApplicationConfiguration {

        private final VaadinContext context;

        protected ApplicationConfigurationImpl(VaadinContext context,
                Map<String, String> properties) {
            super(properties);
            this.context = context;
        }

        @Override
        public boolean isProductionMode() {
            return getBooleanProperty(SERVLET_PARAMETER_PRODUCTION_MODE, false);
        }

        @Override
        public Enumeration<String> getPropertyNames() {
            return Collections.enumeration(getProperties().keySet());
        }

        @Override
        public VaadinContext getContext() {
            return context;
        }

        @Override
        public boolean isDevModeSessionSerializationEnabled() {
            return getBooleanProperty(
                    APPLICATION_PARAMETER_DEVMODE_ENABLE_SERIALIZE_SESSION,
                    false);
        }

    }

    @Override
    public ApplicationConfiguration create(VaadinContext context) {
        Objects.requireNonNull(context);
        Map<String, String> props = new HashMap<>();
        for (final Enumeration<String> paramNames = context
                .getContextParameterNames(); paramNames.hasMoreElements();) {
            final String name = paramNames.nextElement();
            props.put(name, context.getContextParameter(name));
        }
        JsonNode buildInfo = null;
        try {
            String content = getTokenFileContent(props::get);
            if (content == null) {
                content = getTokenFileFromClassloader(context);
            }
            buildInfo = content == null ? null : JacksonUtils.readTree(content);
            if (buildInfo != null) {
                props.putAll(getConfigParametersUsingTokenData(buildInfo));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        return doCreate(context, props);
    }

    /**
     * Creates application configuration instance based on provided data.
     *
     * @param context
     *            the Vaadin context, not {@code null}
     * @param properties
     *            the context parameters, not {@code null}
     * @return a new application configuration instance
     * @since 24.1
     */
    protected ApplicationConfigurationImpl doCreate(VaadinContext context,
            Map<String, String> properties) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(properties);
        return new ApplicationConfigurationImpl(context, properties);
    }

    /**
     * Gets token file from the classpath using the provided {@code context}.
     * <p>
     * The {@code contextClass} may be a class which is defined in the Web
     * Application module/bundle and in this case it may be used to get Web
     * Application resources. Also a {@link VaadinContext} {@code context}
     * instance may be used to get a context of the Web Application (since the
     * {@code contextClass} may be a class not from Web Application module). In
     * WAR case it doesn't matter which class is used to get the resources (Web
     * Application classes or e.g. "flow-server" classes) since they are loaded
     * by the same {@link ClassLoader}. But in OSGi "flow-server" module classes
     * can't be used to get Web Application resources since they are in
     * different bundles.
     *
     * @param context
     *            a VaadinContext which may provide information how to get token
     *            file for the web application
     * @return the token file content
     * @throws IOException
     *             if I/O fails during access to the token file
     */
    protected String getTokenFileFromClassloader(VaadinContext context)
            throws IOException {
        String tokenResource = VAADIN_SERVLET_RESOURCES + TOKEN_FILE;

        Lookup lookup = context.getAttribute(Lookup.class);
        ResourceProvider resourceProvider = lookup
                .lookup(ResourceProvider.class);

        List<URL> resources = resourceProvider
                .getApplicationResources(tokenResource);

        // Accept resource that doesn't contain
        // 'jar!/META-INF/Vaadin/config/flow-build-info.json'
        URL resource = resources.stream()
                .filter(url -> !url.getPath().endsWith("jar!/" + tokenResource))
                .findFirst().orElse(null);
        if (resource == null && !resources.isEmpty()) {
            return getPossibleJarResource(context, resources);
        }
        return resource == null ? null
                : FrontendUtils.streamToString(resource.openStream());

    }

    /**
     * Check if the vite.generated.ts resources is inside 2 jars
     * (flow-server.jar and application.jar) if this is the case then we can
     * accept a build info file from inside jar with a single jar in the path.
     * <p>
     * Else we will accept any flow-build-info and log a warning that it may not
     * be the correct file, but it's the best we could find.
     * <p>
     * A candidate that cannot be used for the application that is being run is
     * skipped, see {@link #getReasonToIgnore(String)}.
     *
     * @return the token file content, or {@code null} if no usable file was
     *         found
     */
    private String getPossibleJarResource(VaadinContext context,
            List<URL> resources) throws IOException {
        Objects.requireNonNull(resources);

        Lookup lookup = context.getAttribute(Lookup.class);
        ResourceProvider resourceProvider = lookup
                .lookup(ResourceProvider.class);

        assert !resources.isEmpty()
                : "Possible jar resource requires resources to be available.";

        URL viteGenerated = resourceProvider
                .getApplicationResource(FrontendUtils.VITE_GENERATED_CONFIG);

        // If vite.generated.ts is inside 2 archives then we are running
        // from a jar, as the jar of flow-server is inside the jar of the
        // application
        boolean runningFromJar = viteGenerated != null
                && countArchiveLevels(viteGenerated.getPath()) >= 2;

        // As we now know that we are running from a jar, the file of the
        // application is the one in the outermost archive, so look at the
        // least nested ones first
        List<URL> candidates = runningFromJar
                ? resources.stream()
                        .sorted(Comparator.comparingInt(
                                url -> countArchiveLevels(url.getPath())))
                        .toList()
                : resources;

        for (URL candidate : candidates) {
            String content = FrontendUtils
                    .streamToString(candidate.openStream());
            String reasonToIgnore = getReasonToIgnore(content);
            if (reasonToIgnore != null) {
                getLogger().warn(
                        "Ignoring the file '{}' found inside a jar, as {}.",
                        candidate.getPath(), reasonToIgnore);
                continue;
            }
            // The file is only known to be the right one when it was
            // picked by the rule for a packaged application
            boolean confidentPick = runningFromJar
                    && countArchiveLevels(candidate.getPath()) == 1;
            if (candidates.size() > 1 && !confidentPick) {
                String warningMessage = String.format(
                        "Unable to fully determine correct flow-build-info.%n"
                                + "Accepting file '%s' first match of '%s' possible (%s).%n"
                                + "Please verify flow-build-info file content.",
                        candidate.getPath(), resources.size(), resources);
                getLogger().warn(warningMessage);
            } else {
                String debugMessage = String.format(
                        "Unable to fully determine correct flow-build-info.%n"
                                + "Accepting file '%s'",
                        candidate.getPath());
                getLogger().debug(debugMessage);
            }
            return content;
        }
        return null;
    }

    /**
     * Checks whether a token file found inside a jar can be used for the
     * application that is being run.
     * <p>
     * A file from a production build carries no folders of the machine it was
     * built on and is always used, as it is the file of a packaged application.
     * A file from a development build is used only when it was written for the
     * application that is being run: the project it names has to be on this
     * machine and, when the project folder of the application can be told from
     * the class path or the working directory, has to be that folder. This
     * keeps an application packaged in development mode working, and leaves out
     * a file packaged into a dependency, whether the dependency was built
     * somewhere else or on this machine.
     *
     * @param content
     *            the token file content, not {@code null}
     * @return the reason not to use the file, or {@code null} when it can be
     *         used
     */
    private String getReasonToIgnore(String content) {
        JsonNode buildInfo;
        try {
            buildInfo = JacksonUtils.readTree(content);
        } catch (RuntimeException e) {
            getLogger().debug("Unable to parse a token file from a jar", e);
            return "it cannot be read as JSON";
        }
        if (buildInfo.has(SERVLET_PARAMETER_PRODUCTION_MODE) && buildInfo
                .get(SERVLET_PARAMETER_PRODUCTION_MODE).booleanValue()) {
            return null;
        }
        if (!buildInfo.has(NPM_TOKEN)) {
            return "it is not from a production build and does not name the project it was written for";
        }
        String projectFolder = buildInfo.get(NPM_TOKEN).asString();
        if (!new File(projectFolder).exists()) {
            return String.format(
                    "it is not from a production build and the project it was written for, '%s', is not on this machine, so it is packaged into a dependency by mistake",
                    projectFolder);
        }
        File project = new File(projectFolder);
        File classpathProjectFolder = getClasspathProjectFolder();
        if (classpathProjectFolder != null) {
            // The application runs from the output folder of its project, so
            // the file has to be for exactly that project
            if (!isSameFolder(project, classpathProjectFolder)) {
                return notWrittenForThisApplication(projectFolder,
                        classpathProjectFolder);
            }
        } else {
            // The working directory is only a hint: a multi-module build may
            // be started from its root, so a project inside it is accepted
            File workingDirectory = getWorkingDirectoryProjectFolder();
            if (workingDirectory != null
                    && !isInsideFolder(project, workingDirectory)) {
                return notWrittenForThisApplication(projectFolder,
                        workingDirectory);
            }
        }
        return null;
    }

    private static String notWrittenForThisApplication(String projectFolder,
            File applicationProjectFolder) {
        return String.format(
                "it is not from a production build and was written for the project in '%s', not for the application being run from '%s', so it is packaged into a dependency by mistake",
                projectFolder, applicationProjectFolder);
    }

    /**
     * Gets the project folder of the application that is being run from the
     * class path, which is known when the application runs from the output
     * folder of its project.
     *
     * @return the project folder, or {@code null} if it cannot be told
     */
    // Package-private for testing
    File getClasspathProjectFolder() {
        return FileIOUtils.getProjectFolderFromClasspath();
    }

    /**
     * Gets the working directory, if it is the folder of a Maven or Gradle
     * project.
     *
     * @return the project folder, or {@code null} if the working directory is
     *         not a project folder
     */
    // Package-private for testing
    File getWorkingDirectoryProjectFolder() {
        return FileIOUtils.getProjectFolderFromWorkingDirectory();
    }

    private static boolean isSameFolder(File folder, File other) {
        try {
            return Files.isSameFile(folder.toPath(), other.toPath());
        } catch (IOException e) {
            return folder.getAbsoluteFile().equals(other.getAbsoluteFile());
        }
    }

    private static boolean isInsideFolder(File folder, File parent) {
        try {
            return folder.toPath().toRealPath()
                    .startsWith(parent.toPath().toRealPath());
        } catch (IOException e) {
            return folder.toPath().toAbsolutePath().normalize()
                    .startsWith(parent.toPath().toAbsolutePath().normalize());
        }
    }

    /**
     * Counts inside how many archives the resource at the given path is.
     * <p>
     * Both the {@code app.jar!/} separator used for a jar opened from the file
     * system and the {@code app.jar/!} separator that Spring Boot 3.2 and newer
     * use for an archive nested in the jar of the application are counted, as a
     * path may contain one of each:
     * {@code nested:/app.jar/!BOOT-INF/lib/flow-server.jar!/vite.generated.ts}.
     *
     * @param path
     *            the path of the resource, not {@code null}
     * @return the number of archives the resource is inside of, {@code 0} if it
     *         is not inside one
     */
    private int countArchiveLevels(String path) {
        return countInstances(path, "jar!/") + countInstances(path, "jar/!");
    }

    /**
     * Counts how many times {@code value} occurs as a non-overlapping substring
     * within {@code input}.
     *
     * @param input
     *            the string to search within, not {@code null}
     * @param value
     *            the substring to count occurrences of, not {@code null} and
     *            not empty
     * @return the number of non-overlapping occurrences of {@code value} in
     *         {@code input}, or {@code 0} if none are found
     */
    private int countInstances(String input, String value) {
        int count = 0;
        int index = input.indexOf(value);
        while (index != -1) {
            count++;
            index = input.indexOf(value, index + value.length());
        }
        return count;
    }

    private Logger getLogger() {
        return LoggerFactory
                .getLogger(DefaultApplicationConfigurationFactory.class);
    }

}
