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
package com.vaadin.flow.server.frontend;

import jakarta.servlet.ServletContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendTools.INSTALL_NODE_LOCALLY;
import static java.lang.String.format;

/**
 * A class for static methods and definitions that might be used in different
 * locations.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class FrontendUtils {

    public static final String PROJECT_BASEDIR = "project.basedir";

    /**
     * Default folder for the node related content. It's the base directory for
     * {@link Constants#PACKAGE_JSON} and {@link FrontendUtils#NODE_MODULES}.
     *
     * By default it's the project root folder.
     */
    public static final String DEFAULT_NODE_DIR = "./";

    /**
     * Location for the installed node packages. This folder is always
     * considered by node, even though we define extra folders with the
     * <code>NODE_PATH</code>.
     */
    public static final String NODE_MODULES = "node_modules/";

    /**
     * Default folder used for source and generated folders.
     */
    public static final String FRONTEND = "frontend/";

    /**
     * Default folder for client-side generated files inside the project root
     * frontend folder.
     */
    public static final String GENERATED = "generated/";

    /**
     * Path of the folder containing application frontend source files, it needs
     * to be relative to the {@link FrontendUtils#DEFAULT_NODE_DIR}
     *
     * By default it is <code>/frontend</code> in the project folder.
     */
    public static final String DEFAULT_FRONTEND_DIR = DEFAULT_NODE_DIR
            + FRONTEND;

    /**
     * The name of the vite configuration file.
     */
    public static final String VITE_CONFIG = "vite.config.ts";

    /**
     * The name of the generated vite configuration file.
     */
    public static final String VITE_GENERATED_CONFIG = "vite.generated.ts";

    /**
     * The name of the service worker source file for InjectManifest method of
     * the workbox plugin.
     */
    public static final String SERVICE_WORKER_SRC = "sw.ts";

    /**
     * The JavaScript version of the service worker file, for checking if a user
     * has a JavaScript version of a custom service worker file already.
     */
    public static final String SERVICE_WORKER_SRC_JS = "sw.js";

    /**
     * The folder inside the 'generated' folder where frontend resources from
     * jars are copied.
     */
    public static final String JAR_RESOURCES_FOLDER = "jar-resources";

    /**
     * The location where javascript files present in jar resources are copied
     * and can be imported from.
     */
    public static final String JAR_RESOURCES_IMPORT = "Frontend/generated/"
            + JAR_RESOURCES_FOLDER + "/";
    /**
     * The location where javascript files present in jar resources are copied
     * and can be imported from, relative to the frontend folder.
     */
    public static final String JAR_RESOURCES_IMPORT_FRONTEND_RELATIVE = JAR_RESOURCES_IMPORT
            .replace("Frontend/", "./");

    /**
     * Name of the file that contains application imports, javascript, theme and
     * style annotations.
     */
    public static final String IMPORTS_NAME = "generated-flow-imports.js";

    /**
     * The TypeScript definitions for the {@link FrontendUtils#IMPORTS_NAME}
     * file.
     */
    public static final String IMPORTS_D_TS_NAME = "generated-flow-imports.d.ts";

    public static final String THEME_IMPORTS_D_TS_NAME = "theme.d.ts";
    public static final String THEME_IMPORTS_NAME = "theme.js";

    /**
     * File name of the bootstrap file that is generated in frontend
     * {@link #GENERATED} folder. The bootstrap file is always executed in a
     * Vaadin app.
     */
    public static final String BOOTSTRAP_FILE_NAME = "vaadin.ts";

    /**
     * File name of the web component bootstrap file that is generated in
     * frontend {@link #GENERATED} folder. The bootstrap file is always executed
     * in an exported web component.
     */
    public static final String WEB_COMPONENT_BOOTSTRAP_FILE_NAME = "vaadin-web-component.ts";

    /**
     * File name of the feature flags file that is generated in frontend
     * {@link #GENERATED} folder. The feature flags file contains code to define
     * feature flags as globals that might be used by Vaadin web components or
     * application code.
     */
    public static final String FEATURE_FLAGS_FILE_NAME = "vaadin-featureflags.js";

    /**
     * File name of the index.html in client side.
     */
    public static final String INDEX_HTML = "index.html";

    /**
     * File name of the web-component.html in client side.
     */
    public static final String WEB_COMPONENT_HTML = "web-component.html";

    /**
     * File name of the index.ts in client side.
     */
    public static final String INDEX_TS = "index.ts";

    /**
     * File name of the index.js in client side.
     */
    public static final String INDEX_JS = "index.js";

    /**
     * File name of the index.tsx in client side.
     */
    public static final String INDEX_TSX = "index.tsx";

    /**
     * File name of Vite helper used in development mode.
     */
    public static final String VITE_DEVMODE_TS = "vite-devmode.ts";

    /**
     * Default generated path for generated frontend files.
     */
    public static final String DEFAULT_PROJECT_FRONTEND_GENERATED_DIR = DEFAULT_FRONTEND_DIR
            + GENERATED;

    /**
     * A parameter for overriding the {@link FrontendUtils#DEFAULT_FRONTEND_DIR}
     * folder.
     */
    public static final String PARAM_FRONTEND_DIR = "vaadin.frontend.frontend.folder";

    /**
     * Set to {@code true} to ignore node/npm tool version checks.
     */
    public static final String PARAM_IGNORE_VERSION_CHECKS = "vaadin.ignoreVersionChecks";

    /**
     * A special prefix used to map imports placed in the
     * {@link FrontendUtils#DEFAULT_FRONTEND_DIR}. e.g.
     * <code>import 'Frontend/foo.js';</code> references the
     * file<code>frontend/foo.js</code>.
     */
    public static final String FRONTEND_FOLDER_ALIAS = "Frontend/";

    /**
     * The prefix used to import files generated by Flow.
     */
    public static final String FRONTEND_GENERATED_FLOW_IMPORT_PATH = FRONTEND_FOLDER_ALIAS
            + "generated/flow/";

    /**
     * File used to enable npm mode.
     */
    public static final String TOKEN_FILE = Constants.VAADIN_CONFIGURATION
            + "flow-build-info.json";

    /**
     * A key in a Json object for chunks list.
     */
    public static final String CHUNKS = "chunks";

    /**
     * The entry-point key used for the exported bundle.
     */
    public static final String EXPORT_CHUNK = "export";

    /**
     * A key in a Json object for css imports data.
     */
    public static final String CSS_IMPORTS = "cssImports";

    /**
     * A key in a Json object for js modules data.
     */
    public static final String JS_MODULES = "jsModules";

    /**
     * A parameter informing about the location of the
     * {@link FrontendUtils#TOKEN_FILE}.
     */
    public static final String PARAM_TOKEN_FILE = "vaadin.frontend.token.file";

    public static final String DISABLE_CHECK = "%nYou can disable the version check using -D%s=true";

    private static final String TOO_OLD = "%n%n======================================================================================================"
            + "%nYour installed '%s' version (%s) is too old. Supported versions are %d.%d+" //
            + "%nPlease install a new one either:"
            + "%n  - by following the https://nodejs.org/en/download/ guide to install it globally"
            + "%n  - or by running the frontend-maven-plugin goal to install it in this project:"
            + INSTALL_NODE_LOCALLY + "%n" //
            + DISABLE_CHECK //
            + "%n======================================================================================================%n";

    // Proxy config properties keys (for both system properties and environment
    // variables) can be either fully upper case or fully lower case
    static final String SYSTEM_NOPROXY_PROPERTY_KEY = "NOPROXY";
    static final String SYSTEM_HTTPS_PROXY_PROPERTY_KEY = "HTTPS_PROXY";
    static final String SYSTEM_HTTP_PROXY_PROPERTY_KEY = "HTTP_PROXY";

    public static final String YELLOW = "\u001b[38;5;111m%s\u001b[0m";

    public static final String RED = "\u001b[38;5;196m%s\u001b[0m";

    public static final String GREEN = "\u001b[38;5;35m%s\u001b[0m";

    public static final String BRIGHT_BLUE = "\u001b[94m%s\u001b[0m";

    /**
     * Only static stuff here.
     */
    private FrontendUtils() {
    }

    /**
     * Get the Operating System name from the {@code os.name} system property.
     *
     * @return operating system name
     */
    public static String getOsName() {
        return System.getProperty("os.name");
    }

    /**
     * Check if the current os is Windows.
     *
     * @return true if windows
     */
    public static boolean isWindows() {
        return getOsName().startsWith("Windows");
    }

    /**
     * Read a stream and copy the content into a String using system line
     * separators for all 'carriage return' characters.
     *
     * @param inputStream
     *            the input stream
     * @return the string
     */
    public static String streamToString(InputStream inputStream) {
        String ret = "";
        try (InputStream handledStream = inputStream) {
            return IOUtils.toString(handledStream, StandardCharsets.UTF_8)
                    .replaceAll("\\R", System.lineSeparator());
        } catch (IOException exception) {
            // ignore exception on close()
            getLogger().warn("Couldn't close template input stream", exception);
        }
        return ret;
    }

    /**
     * Creates a process builder for the given list of program and arguments. If
     * the program is defined as an absolute path, then the directory that
     * contains the program is also appended to PATH so that the it can locate
     * related tools.
     *
     * @param command
     *            a list with the program and arguments
     * @return a configured process builder
     */
    public static ProcessBuilder createProcessBuilder(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        /*
         * Ensure the location of the command to run is in PATH. This is in some
         * cases needed by npm to locate a node binary.
         */
        File commandFile = new File(command.get(0));
        if (commandFile.isAbsolute()) {
            String commandPath = commandFile.getParent();
            Map<String, String> environment = processBuilder.environment();

            String pathEnvVar;
            if (isWindows()) {
                /*
                 * Determine the name of the PATH environment variable on
                 * Windows, as variables names are not case-sensitive (the
                 * common name is "Path").
                 */
                pathEnvVar = environment.keySet().stream()
                        .filter("PATH"::equalsIgnoreCase).findFirst()
                        .orElse("Path");
            } else {
                pathEnvVar = "PATH";
            }

            String path = environment.get(pathEnvVar);
            if (path == null || path.isEmpty()) {
                path = commandPath;
            } else {
                // Ensure that a custom node is first in the path so it is used
                // e.g. for postinstall scripts that run "node something"
                path = commandPath + File.pathSeparatorChar + path;
            }
            environment.put(pathEnvVar, path);
        }

        return processBuilder;
    }

    /**
     * Gets the content of the <code>frontend/index.html</code> file which is
     * served by vite in dev-mode and read from classpath in production mode.
     * <p>
     * NOTE: In dev mode, the file content is fetched using an http request so
     * that we don't need to have a separate index.html's content watcher.
     * Auto-reloading will work automatically, like other files in the
     * `frontend/` folder.
     *
     * @param service
     *            the vaadin service
     * @return the content of the index html file as a string, null if not
     *         found.
     * @throws IOException
     *             on error when reading file
     *
     */
    public static String getIndexHtmlContent(VaadinService service)
            throws IOException {
        return getFileContent(service, INDEX_HTML);
    }

    /**
     * Gets the content of the <code>frontend/web-component.html</code> file
     * which is served by vite in dev-mode and read from classpath in production
     * mode.
     * <p>
     * NOTE: In dev mode, the file content is fetched using an http request so
     * that we don't need to have a separate web-component.html's content
     * watcher. Auto-reloading will work automatically, like other files in the
     * `frontend/` folder.
     *
     * @param service
     *            the vaadin service
     * @return the content of the web-component.html file as a string, null if
     *         not found.
     * @throws IOException
     *             on error when reading file
     *
     */
    public static String getWebComponentHtmlContent(VaadinService service)
            throws IOException {
        return getFileContent(service, WEB_COMPONENT_HTML);
    }

    private static String getFileContent(VaadinService service, String path)
            throws IOException {
        DeploymentConfiguration config = service.getDeploymentConfiguration();
        InputStream content = null;

        try {
            Optional<DevModeHandler> devModeHandler = DevModeHandlerManager
                    .getDevModeHandler(service);
            if (config.isProductionMode()) {
                // In production mode, this is on the class path
                content = getFileFromClassPath(service, path);
            } else if (devModeHandler.isPresent()) {
                content = getFileFromDevModeHandler(devModeHandler.get(), path);
            } else {
                // Get directly from the frontend folder in the project
                content = getFileFromFrontendDir(config, path);
            }

            return content != null ? streamToString(content) : null;
        } finally {
            IOUtils.closeQuietly(content);
        }
    }

    private static InputStream getFileFromFrontendDir(
            AbstractConfiguration config, String path) {
        File file = new File(new File(config.getProjectFolder(), "frontend"),
                path);
        if (file.exists()) {
            try {
                return Files.newInputStream(file.toPath());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return null;
    }

    private static InputStream getFileFromClassPath(VaadinService service,
            String filePath) {
        final URL resource = service.getContext().getAttribute(Lookup.class)
                .lookup(ResourceProvider.class)
                .getApplicationResource(VAADIN_WEBAPP_RESOURCES + filePath);
        if (resource == null) {
            getLogger().error("Cannot get the '{}' from the classpath",
                    filePath);
            return null;
        }
        try {
            return resource.openStream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream getFileFromDevModeHandler(
            DevModeHandler devModeHandler, String filePath) throws IOException {
        return devModeHandler.prepareConnection("/" + filePath, "GET")
                .getInputStream();
    }

    /**
     * Get the contents of a frontend file from the running dev server.
     *
     * @param service
     *            the Vaadin service.
     * @param path
     *            the file path.
     * @return an input stream for reading the file contents; null if there is
     *         no such file or the dev server is not running.
     */
    public static InputStream getFrontendFileFromDevModeHandler(
            VaadinService service, String path) {
        Optional<DevModeHandler> devModeHandler = DevModeHandlerManager
                .getDevModeHandler(service);
        if (devModeHandler.isPresent()) {
            try {
                File frontendFile = resolveFrontendPath(
                        devModeHandler.get().getProjectRoot(), path);
                return frontendFile == null ? null
                        : new FileInputStream(frontendFile);
            } catch (IOException e) {
                throw new UncheckedIOException("Error reading file " + path, e);
            }
        }
        return null;
    }

    /**
     * Looks up the frontend resource at the given path. If the path starts with
     * {@code ./}, first look in {@code frontend}, then in
     * {@value FrontendUtils#JAR_RESOURCES_FOLDER}. If the path does not start
     * with {@code ./}, look in {@code node_modules} instead.
     *
     * @param projectRoot
     *            the project root folder.
     * @param path
     *            the file path.
     * @return an existing {@link File} , or null if the file doesn't exist.
     */
    public static File resolveFrontendPath(File projectRoot, String path) {
        return resolveFrontendPath(projectRoot, path,
                new File(projectRoot, FrontendUtils.FRONTEND));
    }

    /**
     * Looks up the fronted resource at the given path. If the path starts with
     * {@code ./}, first look in {@code frontend}, then in
     * {@value FrontendUtils#JAR_RESOURCES_FOLDER}. If the path does not start
     * with {@code ./}, look in {@code node_modules} instead.
     *
     * @param projectRoot
     *            the project root folder.
     * @param path
     *            the file path.
     * @param frontendDirectory
     *            the frontend directory.
     * @return an existing {@link File} , or null if the file doesn't exist.
     */
    public static File resolveFrontendPath(File projectRoot, String path,
            File frontendDirectory) {
        File nodeModulesFolder = new File(projectRoot, NODE_MODULES);
        File addonsFolder = getJarResourcesFolder(frontendDirectory);
        List<File> candidateParents = path.startsWith("./")
                ? Arrays.asList(frontendDirectory, addonsFolder)
                : Arrays.asList(nodeModulesFolder, frontendDirectory,
                        addonsFolder);
        return candidateParents.stream().map(parent -> new File(parent, path))
                .filter(File::exists).findFirst().orElse(null);
    }

    /**
     * Get resource from JAR package.
     *
     * @param jarImport
     *            jar file to get (no resource folder should be added)
     * @return resource as String or {@code null} if not found
     */
    public static String getJarResourceString(String jarImport,
            ClassFinder finder) {
        URL resource = finder
                .getResource(RESOURCES_FRONTEND_DEFAULT + "/" + jarImport);
        if (resource == null) {
            resource = finder.getResource(
                    COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT + "/" + jarImport);
        }

        if (resource == null) {
            return null;
        }
        try (InputStream frontendContent = resource.openStream()) {
            return FrontendUtils.streamToString(frontendContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the front-end resources folder. This is where the contents of JAR
     * dependencies are copied to.
     *
     * @param frontendDirectory
     *            project's frontend directory
     * @return a {@link File} representing a folder with copied resources
     */
    public static File getJarResourcesFolder(File frontendDirectory) {
        return new File(getFrontendGeneratedFolder(frontendDirectory),
                JAR_RESOURCES_FOLDER);
    }

    public static File getFrontendGeneratedFolder(File frontendDirectory) {
        return new File(frontendDirectory, GENERATED);
    }

    private static String buildTooOldString(String tool, String version,
            int supportedMajor, int supportedMinor) {
        return String.format(TOO_OLD, tool, version, supportedMajor,
                supportedMinor, PARAM_IGNORE_VERSION_CHECKS);
    }

    /**
     * Get directory where project's frontend files are located.
     *
     * @param configuration
     *            the current deployment configuration
     *
     * @return {@link #DEFAULT_FRONTEND_DIR} or value of
     *         {@link #PARAM_FRONTEND_DIR} if it is set.
     */
    public static File getProjectFrontendDir(
            AbstractConfiguration configuration) {
        String propertyValue = configuration
                .getStringProperty(PARAM_FRONTEND_DIR, DEFAULT_FRONTEND_DIR);
        File f = new File(propertyValue);
        if (f.isAbsolute()) {
            return f;
        }
        return new File(configuration.getProjectFolder(), propertyValue);
    }

    /**
     * Get relative path from a source path to a target path in Unix form. All
     * the Windows' path separator will be replaced.
     *
     * @param source
     *            the source path
     * @param target
     *            the target path
     * @return unix relative path from source to target
     */
    public static String getUnixRelativePath(Path source, Path target) {
        return getUnixPath(source.relativize(target));
    }

    /**
     * Get path as a String in Unix form.
     *
     * @param source
     *            path to get
     * @return path as a String in Unix form.
     */
    public static String getUnixPath(Path source) {
        return source.toString().replaceAll("\\\\", "/");
    }

    static void validateToolVersion(String tool, FrontendVersion toolVersion,
            FrontendVersion supported) {
        if (isVersionAtLeast(toolVersion, supported)) {
            return;
        }

        throw new IllegalStateException(buildTooOldString(tool,
                toolVersion.getFullVersion(), supported.getMajorVersion(),
                supported.getMinorVersion()));
    }

    static boolean isVersionAtLeast(FrontendVersion toolVersion,
            FrontendVersion required) {
        int major = toolVersion.getMajorVersion();
        int minor = toolVersion.getMinorVersion();
        return (major > required.getMajorVersion()
                || (major == required.getMajorVersion()
                        && minor >= required.getMinorVersion()));
    }

    /**
     * Thrown when detecting the version of a tool fails.
     */
    public static class UnknownVersionException extends Exception {

        /**
         * Constructs an exception telling which tool was being detected and
         * using what command.
         *
         * @param tool
         *            the tool being detected
         * @param extraInfo
         *            extra information which might be helpful to the end user
         */
        public UnknownVersionException(String tool, String extraInfo) {
            super("Unable to detect version of " + tool + ". " + extraInfo);
        }

        /**
         * Constructs an exception telling which tool was being detected and
         * using what command, and the exception causing the failure.
         *
         * @param tool
         *            the tool being detected
         * @param extraInfo
         *            extra information which might be helpful to the end user
         * @param cause
         *            the exception causing the failure
         */
        public UnknownVersionException(String tool, String extraInfo,
                Exception cause) {
            super("Unable to detect version of " + tool + ". " + extraInfo,
                    cause);
        }
    }

    /**
     * Thrown when the command execution fails.
     */
    public static class CommandExecutionException extends Exception {
        /**
         * Constructs an exception telling what code the command execution
         * process was exited with.
         *
         * @param processExitCode
         *            process exit code
         */
        public CommandExecutionException(int processExitCode) {
            super("Process execution failed with exit code " + processExitCode);
        }

        /**
         * Constructs an exception telling what code the command execution
         * process was exited with and the output that it produced.
         *
         * @param processExitCode
         *            process exit code
         * @param output
         *            the output from the command
         * @param errorOutput
         *            the error output from the command
         */
        public CommandExecutionException(int processExitCode, String output,
                String errorOutput) {
            super("Process execution failed with exit code " + processExitCode
                    + "\nOutput: " + output + "\nError output: " + errorOutput);
        }

        /**
         * Constructs an exception telling what was the original exception the
         * command execution process failed with.
         *
         * @param cause
         *            the cause exception of process failure.
         */
        public CommandExecutionException(Throwable cause) {
            super("Process execution failed", cause);
        }
    }

    protected static FrontendVersion getVersion(String tool,
            List<String> versionCommand) throws UnknownVersionException {
        String output;
        try {
            output = executeCommand(versionCommand);
        } catch (CommandExecutionException e) {
            throw new UnknownVersionException(tool,
                    "Using command " + String.join(" ", versionCommand), e);
        }

        try {
            return new FrontendVersion(parseVersionString(output));
        } catch (IOException e) {
            throw new UnknownVersionException(tool,
                    "Expected a version number as output but got '" + output
                            + "'" + " when using command "
                            + String.join(" ", versionCommand),
                    e);
        }
    }

    /**
     * Executes a given command as a native process.
     *
     * @param command
     *            the command to be executed and it's arguments.
     * @return process output string.
     * @throws CommandExecutionException
     *             if the process completes exceptionally.
     */
    public static String executeCommand(List<String> command)
            throws CommandExecutionException {
        try {
            Process process = FrontendUtils.createProcessBuilder(command)
                    .start();

            CompletableFuture<Pair<String, String>> streamConsumer = consumeProcessStreams(
                    process);
            int exitCode = process.waitFor();
            Pair<String, String> outputs = streamConsumer.get();
            if (exitCode != 0) {
                throw new CommandExecutionException(exitCode,
                        outputs.getFirst(), outputs.getSecond());
            }
            return outputs.getFirst();
        } catch (ExecutionException e) {
            throw new CommandExecutionException(e.getCause());
        } catch (IOException | InterruptedException e) {
            throw new CommandExecutionException(e);
        }
    }

    /**
     * Reads input and error stream from the give process asynchronously.
     *
     * The method returns a {@link CompletableFuture} that is completed when
     * both the streams are consumed.
     *
     * Streams are converted into strings and wrapped into a {@link Pair},
     * mapping input stream into {@link Pair#getFirst()} and error stream into
     * {@link Pair#getSecond()}.
     *
     * This method should be mainly used to avoid that {@link Process#waitFor()}
     * hangs indefinitely on some operating systems because process streams are
     * not consumed. See https://github.com/vaadin/flow/issues/15339 for an
     * example case.
     *
     * @param process
     *            the process whose streams should be read
     * @return a {@link CompletableFuture} that return the string contents of
     *         the process input and error streams when both are consumed,
     *         wrapped into a {@link Pair}.
     */
    public static CompletableFuture<Pair<String, String>> consumeProcessStreams(
            Process process) {
        CompletableFuture<String> stdOut = CompletableFuture
                .supplyAsync(() -> streamToString(process.getInputStream()));
        CompletableFuture<String> stdErr = CompletableFuture
                .supplyAsync(() -> streamToString(process.getErrorStream()));
        return CompletableFuture.allOf(stdOut, stdErr).thenApply(
                unused -> new Pair<>(stdOut.getNow(""), stdErr.getNow("")));
    }

    /**
     * Parse the version number of node/npm from version output string.
     *
     * @param versionString
     *            string containing version output, typically produced by
     *            <code>tool --version</code>
     * @return FrontendVersion of versionString
     * @throws IOException
     *             if parsing fails
     */
    public static FrontendVersion parseFrontendVersion(String versionString)
            throws IOException {
        return new FrontendVersion((parseVersionString(versionString)));
    }

    /**
     * Gets vaadin home directory ({@code ".vaadin"} folder in the user home
     * dir).
     * <p>
     * The directory is created if it's doesn't exist.
     *
     * @return a vaadin home directory
     */
    public static File getVaadinHomeDirectory() {
        File home = FileUtils.getUserDirectory();
        if (!home.exists()) {
            throw new IllegalStateException("The user directory '"
                    + home.getAbsolutePath() + "' doesn't exist");
        }
        if (!home.isDirectory()) {
            throw new IllegalStateException("The path '"
                    + home.getAbsolutePath() + "' is not a directory");
        }
        File vaadinFolder = new File(home, ".vaadin");
        if (vaadinFolder.exists()) {
            if (vaadinFolder.isDirectory()) {
                return vaadinFolder;
            } else {
                throw new IllegalStateException("The path '"
                        + vaadinFolder.getAbsolutePath()
                        + "' is not a directory. "
                        + "This path is used to store vaadin related data. "
                        + "Please either remove the file or create a directory");
            }
        }
        try {
            FileUtils.forceMkdir(vaadinFolder);
            return vaadinFolder;
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Couldn't create '.vaadin' folder inside home directory '"
                            + home.getAbsolutePath() + "'",
                    exception);
        }
    }

    /**
     * Parse the version number of node/npm from the given output.
     *
     * @param output
     *            The output, typically produced by <code>tool --version</code>
     * @return the parsed version as an array with 3-4 elements
     * @throws IOException
     *             if parsing fails
     */
    static String parseVersionString(String output) throws IOException {
        Optional<String> lastOuput = Stream.of(output.split("\n"))
                .filter(line -> !line.matches("^[ ]*$"))
                .reduce((first, second) -> second);
        return lastOuput.map(line -> line.replaceFirst("^v", ""))
                .orElseThrow(() -> new IOException("No output"));
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(FrontendUtils.class);
    }

    /**
     * Pretty prints a command line order. It split in lines adapting to 80
     * columns, and allowing copy and paste in console. It also removes the
     * current directory to avoid security issues in log files.
     *
     * @param baseDir
     *            the current directory
     * @param command
     *            the command and it's arguments
     * @return the string for printing in logs
     */
    public static String commandToString(String baseDir, List<String> command) {
        StringBuilder retval = new StringBuilder("\n");
        StringBuilder curLine = new StringBuilder();
        for (String fragment : command) {
            if (curLine.length() + fragment.length() > 55) {
                retval.append(curLine.toString());
                retval.append("\\ \n");
                curLine = new StringBuilder("    ");
            }
            curLine.append(fragment.replace(baseDir, "."));
            curLine.append(" ");
        }
        retval.append(curLine.toString());
        retval.append("\n");
        return retval.toString();
    }

    /**
     * Tries to parse the given package's frontend version or if it doesn't
     * exist, returns {@code null}. In case the value cannot be parsed, logs an
     * error and returns {@code null}.
     *
     * @param sourceJson
     *            json object that has the package
     * @param pkg
     *            the package name
     * @param versionOrigin
     *            origin of the version (like a file), used in error message
     * @return the frontend version the package or {@code null}
     */
    public static FrontendVersion getPackageVersionFromJson(
            JsonObject sourceJson, String pkg, String versionOrigin) {
        if (!sourceJson.hasKey(pkg)) {
            return null;
        }
        try {
            final String versionString = sourceJson.getString(pkg);
            return new FrontendVersion(pkg, versionString);
        } catch (ClassCastException classCastException) { // NOSONAR
            LoggerFactory.getLogger(FrontendVersion.class).warn(
                    "Ignoring error while parsing frontend dependency version for package '{}' in '{}'",
                    pkg, versionOrigin);
        } catch (NumberFormatException nfe) {
            // intentionally not failing the build at this point
            LoggerFactory.getLogger(FrontendVersion.class).warn(
                    "Ignoring error while parsing frontend dependency version in {}: {}",
                    versionOrigin, nfe.getMessage());
        }
        return null;
    }

    /**
     * Intentionally send to console instead to log, useful when executing
     * external processes.
     *
     * @param format
     *            Format of the line to send to console, it must contain a `%s`
     *            outlet for the message
     * @param message
     *            the string to show
     */
    @SuppressWarnings("squid:S106")
    public static void console(String format, Object message) {
        System.out.print(format(format, message));
    }

    /**
     * Try to remove the {@code node_modules} directory, if it exists inside the
     * given base directory. Note that pnpm uses symlinks internally, so delete
     * utilities that follow symlinks when deleting and/or modifying permissions
     * may not work as intended.
     *
     * @param nodeModules
     *            the {@code node_modules} directory
     * @throws IOException
     *             on failure to delete any one file, or if the directory name
     *             is not {@code node_modules}
     */
    public static void deleteNodeModules(File nodeModules) throws IOException {
        if (!nodeModules.exists()) {
            return;
        }

        if (!nodeModules.isDirectory()
                || !nodeModules.getName().equals("node_modules")) {
            throw new IOException(nodeModules.getAbsolutePath()
                    + " does not look like a node_modules directory");
        }

        deleteDirectory(nodeModules);
    }

    /**
     * Recursively delete given directory and contents.
     * <p>
     * Will not delete contents of symlink or junction directories, only the
     * link file.
     *
     * @param directory
     *            directory to delete
     * @throws IOException
     *             on failure to delete or read any one file
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        if (!(Files.isSymbolicLink(directory.toPath())
                || isJunction(directory.toPath()))) {
            cleanDirectory(directory);
        }

        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Check that directory is not a windows junction which is basically a
     * symlink.
     *
     * @param directory
     *            directory path to check
     * @return true if directory is a windows junction
     * @throws IOException
     *             if an I/O error occurs
     */
    private static boolean isJunction(Path directory) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase()
                .contains("windows");
        BasicFileAttributes attrs = Files.readAttributes(directory,
                BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return isWindows && attrs.isDirectory() && attrs.isOther();
    }

    private static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) { // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (File file : files) {
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (exception != null) {
            throw exception;
        }
    }

    private static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException(
                            "File does not exist: " + file);
                }
                String message = "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    /**
     * Gets the servlet path (excluding the context path) for the servlet used
     * for serving the VAADIN frontend bundle.
     *
     * @return the path to the servlet used for the frontend bundle. Empty for a
     *         /* mapping, otherwise always starts with a slash but never ends
     *         with a slash
     */
    public static String getFrontendServletPath(ServletContext servletContext) {
        String mapping = VaadinServlet.getFrontendMapping();
        if (mapping.endsWith("/*")) {
            mapping = mapping.replace("/*", "");
        }

        return mapping;
    }

    /**
     * Gets the folder where Flow generated frontend files are placed.
     *
     * @param frontendFolder
     *            the project frontend folder
     * @return the folder for Flow generated files
     */
    public static File getFlowGeneratedFolder(File frontendFolder) {
        return new File(getFrontendGeneratedFolder(frontendFolder), "flow");

    }

    /**
     * Gets the location of the generated import file for Flow.
     *
     * @param frontendFolder
     *            the project frontend folder
     * @return the location of the generated import JS file
     */
    public static File getFlowGeneratedImports(File frontendFolder) {
        return new File(getFlowGeneratedFolder(frontendFolder), IMPORTS_NAME);
    }

    /**
     * Gets the folder where exported web components are generated.
     *
     * @param frontendFolder
     *            the project frontend folder
     * @return the exported web components folder
     */
    public static File getFlowGeneratedWebComponentsFolder(
            File frontendFolder) {
        return new File(getFlowGeneratedFolder(frontendFolder),
                "web-components");
    }

}
