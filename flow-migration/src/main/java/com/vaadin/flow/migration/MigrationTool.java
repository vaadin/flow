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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.migration.MigrationConfiguration.Builder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;

/**
 * Main class which allow to call migration from the command line using
 * arguments.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class MigrationTool {

    private static final String DEP_URLS = "depUrls";
    private static final String SOURCE_DIRS = "sourceDirs";
    private static final String TARGET_DIR = "targetDir";
    private static final String KEEP_ORIGINAL = "keepOriginal";
    private static final String MIGRATION_DIR = "migrationDir";
    private static final String RESOURCES_DIRS = "resourcesDirs";
    private static final String STOP_ON_ERROR = "stopOnError";
    private static final String CLASSES_DIR = "classesDir";
    private static final String BASE_DIR = "baseDir";
    private static final String ANNOTATION_REWRITE = "annRewrite";

    /**
     * Runs migration tool using command line {@code args}.
     *
     * @param args
     *            command line arguments
     * @throws MigrationFailureException
     *             if migration failed because of errors during execution
     * @throws MigrationToolsException
     *             if migration failed because some necessary tools installation
     *             failed
     *
     * @see #runMigration(String[])
     */
    public static void main(String[] args)
            throws MigrationToolsException, MigrationFailureException {
        MigrationTool tool = new MigrationTool();

        HelpFormatter formatter = new HelpFormatter();
        try {
            tool.runMigration(args);
        } catch (CommandArgumentException exception) {
            System.out.println(exception.getCause().getMessage());
            if (exception.getOptions().isPresent()) {
                formatter.printHelp("migration tool",
                        exception.getOptions().get());
            }

            System.exit(1);
        }

    }

    /**
     * Runs migration tool using command line {@code args}.
     *
     * @param args
     *            command line arguments
     * @throws MigrationFailureException
     *             if migration failed because of errors during execution
     * @throws MigrationToolsException
     *             if migration failed because some necessary tools installation
     *             failed
     */
    protected void runMigration(String[] args) throws CommandArgumentException,
            MigrationToolsException, MigrationFailureException {
        Options options = makeOptions();

        CommandLineParser parser = new DefaultParser();

        CommandLine command = null;

        try {
            command = parser.parse(options, args);
        } catch (ParseException exception) {
            throw new CommandArgumentException(options, exception);
        }

        File baseDirValue = new File(command.getOptionValue(BASE_DIR));
        Builder builder = new Builder(baseDirValue);

        getLogger().debug("The base dir is {}",
                command.getOptionValue(BASE_DIR));

        File compiledClasses = setCompiledClasses(command, builder);

        setIgnoreModulizerErrors(command, builder);

        setResourcesDirs(command, baseDirValue, builder);

        setMigrationDir(command, builder);

        setTargetDir(command, baseDirValue, builder);

        setKeepOriginal(command, builder);

        setSourceDirs(command, builder);

        setClassFinder(command, builder, compiledClasses);

        setAnnotationRewriteStrategy(command, builder);

        doMigration(builder.build());
    }

    /**
     * Runs migration using the provided {@code configuration}.
     *
     * @param configuration
     *            the configuration
     * @throws MigrationFailureException
     *             if migration failed because of errors during execution
     * @throws MigrationToolsException
     *             if migration failed because some necessary tools installation
     *             failed
     */
    protected void doMigration(MigrationConfiguration configuration)
            throws MigrationToolsException, MigrationFailureException {
        Migration migration = new Migration(configuration);
        migration.migrate();
    }

    private void setAnnotationRewriteStrategy(CommandLine command,
            Builder builder) throws CommandArgumentException {
        String annotationRewrite = command.getOptionValue(ANNOTATION_REWRITE);
        if (annotationRewrite != null) {
            try {
                AnnotationsRewriteStrategy strategy = AnnotationsRewriteStrategy
                        .valueOf(annotationRewrite);
                builder.setAnnotationRewriteStrategy(strategy);
                getLogger().debug(
                        "Annotation rewrite strategy is set to " + strategy);
            } catch (IllegalArgumentException exception) {
                throw new CommandArgumentException(exception);
            }
        } else {
            getLogger().debug(
                    "Annotation rewrite strategy is not explicitly set");
        }
    }

    private void setClassFinder(CommandLine command, Builder builder,
            File compiledClasses) throws CommandArgumentException {
        URL compiledClassesURL;
        try {
            compiledClassesURL = compiledClasses.toURI().toURL();
        } catch (MalformedURLException exception) {
            throw new CommandArgumentException(exception);
        }
        String[] urls = command.getOptionValues(DEP_URLS);
        URL[] depUrls = new URL[urls.length + 1];
        depUrls[0] = compiledClassesURL;
        for (int i = 0; i < urls.length; i++) {
            try {
                depUrls[i + 1] = new URL(urls[i]);
            } catch (MalformedURLException exception) {
                throw new CommandArgumentException(exception);
            }
        }

        builder.setClassFinder(new ReflectionsClassFinder(depUrls));
    }

    private void setSourceDirs(CommandLine command, Builder builder) {
        String[] sourceDirs = command.getOptionValues(SOURCE_DIRS);
        List<File> sourceRoots = Stream.of(sourceDirs).map(File::new)
                .collect(Collectors.toList());
        builder.setJavaSourceRoots(
                sourceRoots.toArray(new File[sourceRoots.size()]));
        getLogger().debug("The java source directories are {}", sourceRoots);
    }

    private void setKeepOriginal(CommandLine command, Builder builder) {
        if (command.hasOption(KEEP_ORIGINAL)) {
            builder.setKeepOriginalFiles(true);
            getLogger().debug("Keep original resources value is true");
        } else {
            getLogger().debug("Keep original resources value is false");
        }
    }

    private void setTargetDir(CommandLine command, File baseDirValue,
            Builder builder) {
        String targetDir = command.getOptionValue(TARGET_DIR);
        if (targetDir != null) {
            builder.setTargetDirectory(new File(targetDir));
            getLogger().debug("The target directory is {}", targetDir);
        } else {
            getLogger().debug(
                    "The target directory is not set explicitly. "
                            + "The value is implicitly set to {}",
                    new File(baseDirValue, "frontend"));
        }
    }

    private void setMigrationDir(CommandLine command, Builder builder) {
        String tempMigrationFolder = command.getOptionValue(MIGRATION_DIR);
        if (tempMigrationFolder != null) {
            builder.setTemporaryMigrationFolder(new File(tempMigrationFolder));
            getLogger().debug("The temporary migration directory is {}",
                    tempMigrationFolder);
        } else {
            getLogger().debug(
                    "The temporary migration directory is not set explicitely");
        }
    }

    private void setResourcesDirs(CommandLine command, File baseDirValue,
            Builder builder) {
        String[] resourceDirs = command.getOptionValues(RESOURCES_DIRS);
        if (resourceDirs != null) {
            List<File> folders = Stream.of(resourceDirs).map(File::new)
                    .collect(Collectors.toList());
            builder.setResourceDirectories(
                    folders.toArray(new File[folders.size()]));
            getLogger().debug("The resource directories are {}", folders);
        } else {
            getLogger().debug(
                    "The resource directories is not set explicitely. "
                            + "The value is implicitely set to {}",
                    new File(baseDirValue, "src/main/webapp"));
        }
    }

    private void setIgnoreModulizerErrors(CommandLine command,
            Builder builder) {
        if (command.hasOption(STOP_ON_ERROR)) {
            builder.setIgnoreModulizerErrors(false);
            getLogger().debug("Ignore modulizer errors value is false");
        } else {
            getLogger().debug("Ignore modulizer errors value is true");
        }
    }

    private File setCompiledClasses(CommandLine command, Builder builder) {
        File compiledClasses = new File(command.getOptionValue(CLASSES_DIR));
        builder.setCompiledClassDirectory(compiledClasses);

        getLogger().debug("The classes directory is {}",
                command.getOptionValue(CLASSES_DIR));
        return compiledClasses;
    }

    private Options makeOptions() {
        Options options = new Options();

        // Not required
        Option migrationDir = new Option("md", MIGRATION_DIR, true,
                "temporary migration directory");
        options.addOption(migrationDir);

        Option baseDir = new Option("b", BASE_DIR, true,
                "base project directory. Normally it is the root of the files to migrate. "
                        + "The directory will be used to search and install "
                        + "(if necessary) external tools like node, npm, etc..");
        baseDir.setRequired(true);
        options.addOption(baseDir);

        Option resourceDirectories = new Option("res", RESOURCES_DIRS, true,
                "comma separated resource directories relative to the baseDir, by default the value is one path 'src/main/webapp' inside base directory");
        options.addOption(resourceDirectories);

        Option target = new Option("t", TARGET_DIR, true,
                "target directory for converted resource files. "
                        + "By default it's the path 'frontend' inside the base directory");
        options.addOption(target);

        Option stopOnError = new Option("se", STOP_ON_ERROR, false,
                "whether migration should "
                        + "stop execution with error if modulizer has exited with not 0 status");
        options.addOption(stopOnError);

        Option javaSourceDirectories = new Option("src", SOURCE_DIRS, true,
                "comma separated java source directories");
        javaSourceDirectories.setRequired(true);
        options.addOption(javaSourceDirectories);

        Option compiledClassesDir = new Option("c", CLASSES_DIR, true,
                "compiled classes directory. Java classes have to be compiled into "
                        + "this directory to be able to apply migration");
        compiledClassesDir.setRequired(true);
        options.addOption(compiledClassesDir);

        Option dependenciesUrls = new Option("d", DEP_URLS, true,
                "comma separated classpath URLs. The URLs should include all dependencies for "
                        + "the project such as Jars or filesystem paths to binary classes");
        dependenciesUrls.setRequired(true);
        options.addOption(dependenciesUrls);

        Option keepOriginal = new Option("ko", KEEP_ORIGINAL, false,
                "whether the original "
                        + "resource files should be preserved. By default the migrated files are removed.");
        options.addOption(keepOriginal);

        Option annotationRewrite = new Option("ars", ANNOTATION_REWRITE, true,
                "annotation rewrite strategy. By default the value is ALWAYS. Other choices are SKIP, SKIP_ON_ERROR");
        options.addOption(annotationRewrite);
        return options;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(MigrationTool.class);
    }

}
