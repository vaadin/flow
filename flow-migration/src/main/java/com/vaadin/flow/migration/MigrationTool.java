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
import java.util.ArrayList;
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

import com.vaadin.flow.migration.Configuration.Builder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;

/**
 * Main class which allow to call migration from the command line using
 * arguments.
 *
 * @author Vaadin Ltd
 *
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

    /**
     * Runs migration tool using command line {@code args}.
     *
     * @param args
     *            command line arguments
     * @throws MigrationFailureException
     * @throws MigrationToolsException
     */
    public static void main(String[] args)
            throws MigrationToolsException, MigrationFailureException {
        Options options = makeOptions();

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine command = null;

        try {
            command = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("migration tool", options);

            System.exit(1);
        }

        File baseDirValue = new File(command.getOptionValue(BASE_DIR));
        Builder builder = new Builder(baseDirValue);

        getLogger().debug("The base dir  is {}",
                command.getOptionValue(BASE_DIR));

        File compiledClasses = setCompiledClasses(command, builder);

        setIgnoreModulizerErrors(command, builder);

        setResourcesDirs(command, baseDirValue, builder);

        setMigrationDir(command, builder);

        setTargetDir(command, baseDirValue, builder);

        setKeepOriginal(command, builder);

        setSourceDirs(command, builder);

        setClassFinder(command, builder, compiledClasses);

        Migration migration = new Migration(builder.build());
        migration.migrate();
    }

    private static void setClassFinder(CommandLine command, Builder builder,
            File compiledClasses) {
        String[] urls = command.getOptionValues(DEP_URLS);
        URL compiledClassesURL;
        try {
            compiledClassesURL = compiledClasses.toURI().toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException(
                    "Could not make URL from the file path "
                            + compiledClasses.getPath(),
                    exception);
        }
        List<URL> depUrls = new ArrayList<>(urls.length + 1);
        depUrls.add(compiledClassesURL);
        for (String url : urls) {
            try {
                depUrls.add(new URL(url));
            } catch (MalformedURLException exception) {
                throw new IllegalArgumentException(
                        "Could not make URL from the value" + url, exception);
            }
        }

        builder.setClassFinder(new ReflectionsClassFinder(
                depUrls.toArray(new URL[depUrls.size()])));
    }

    private static void setSourceDirs(CommandLine command, Builder builder) {
        String[] sourceDirs = command.getOptionValues(SOURCE_DIRS);
        List<File> sourceRoots = Stream.of(sourceDirs).map(File::new)
                .collect(Collectors.toList());
        builder.setJavaSourceRoots(
                sourceRoots.toArray(new File[sourceRoots.size()]));
        getLogger().debug("The java source directories are {}", sourceRoots);
    }

    private static void setKeepOriginal(CommandLine command, Builder builder) {
        if (command.hasOption(KEEP_ORIGINAL)) {
            builder.setKeepOriginalFiles(true);
            getLogger().debug("Keep original resources value is true");
        } else {
            getLogger().debug("Keep original resources value is false");
        }
    }

    private static void setTargetDir(CommandLine command, File baseDirValue,
            Builder builder) {
        String targetDir = command.getOptionValue(TARGET_DIR);
        if (targetDir != null) {
            builder.setTargetDirectory(new File(targetDir));
            getLogger().debug("The target directory is {}", targetDir);
        } else {
            getLogger().debug(
                    "The target directory is not set explicitely. "
                            + "The value is implicitely set to {}",
                    new File(baseDirValue, "frontend"));
        }
    }

    private static void setMigrationDir(CommandLine command, Builder builder) {
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

    private static void setResourcesDirs(CommandLine command, File baseDirValue,
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

    private static void setIgnoreModulizerErrors(CommandLine command,
            Builder builder) {
        if (command.hasOption(STOP_ON_ERROR)) {
            builder.setIgnoreModulizerErrors(false);
            getLogger().debug("Ignore modulizer errors value is false");
        } else {
            getLogger().debug("Ignore modulizer errors value is true");
        }
    }

    private static File setCompiledClasses(CommandLine command,
            Builder builder) {
        File compiledClasses = new File(command.getOptionValue(CLASSES_DIR));
        builder.setCompiledClassDirectory(compiledClasses);

        getLogger().debug("The classes directory is {}",
                command.getOptionValue(CLASSES_DIR));
        return compiledClasses;
    }

    private static Options makeOptions() {
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
                "comma separated resource directories, by default the value is one path 'src/main/webapp' inside base direcrory");
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
                "compiled classes directory. Java classes has to be compiled into "
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
                        + "resource files should not be removed. By default the migrated files are removed.");
        options.addOption(keepOriginal);
        return options;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(MigrationTool.class);
    }

}
