/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.base.devserver;

import java.io.File;
import java.lang.ProcessHandle.Info;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.open.OSUtils;
import com.vaadin.open.Open;

/**
 * Util for opening a file in the currently used IDE.
 * <p>
 * Supports detecting VS Code, Eclipse and IntelliJ.
 */
public final class OpenInCurrentIde {

    private static final String ECLIPSE_IDENTIFIER = "eclipse";
    private static final String INTELLIJ_IDENTIFIER = "intellij";

    private OpenInCurrentIde() {
        // Utils only
    }

    /**
     * Opens the given file at the given line number in the IDE used to launch
     * the current Java application.
     * <p>
     * If you are running the Java application from the command line or from an
     * unsupported IDE, then this method does nothing.
     *
     * @param file
     *            the file to open
     * @param lineNumber
     *            the line number to highlight
     * @return true if the file was opened, false otherwise
     */
    public static boolean openFile(File file, int lineNumber) {
        String absolutePath = file.getAbsolutePath();

        Optional<Info> maybeProcessInfo = findIdeCommand();
        if (!maybeProcessInfo.isPresent()) {
            getLogger().debug("Unable to detect IDE from process tree:");
            for (Info i : getProcessTree()) {
                if (i.commandLine().isPresent()) {
                    getLogger().debug("- " + i.commandLine().get());
                }
            }

            return false;
        }

        Info processInfo = maybeProcessInfo.get();

        if (isVSCode(processInfo)) {
            return Open.open("vscode://file" + absolutePath + ":" + lineNumber);
        } else if (isIdea(processInfo)) {
            return Open.open(
                    "idea://open?file=" + absolutePath + "&line=" + lineNumber);
        } else if (isEclipse(processInfo)) {
            String cmd = processInfo.command().get();
            if (OSUtils.isMac()) {
                cmd = getBinary(cmd);
                try {
                    run("open", "-a", cmd, absolutePath);
                } catch (Exception e) {
                    getLogger().error("Unable to launch Eclipse", e);
                }
            } else {
                try {
                    run(cmd, absolutePath + ":" + lineNumber);
                } catch (Exception e) {
                    getLogger().error("Unable to launch Eclipse", e);
                }
            }
            return true;
        }

        return false;

    }

    static String getBinary(String cmd) {
        return cmd.replaceFirst("/Contents/MacOS/eclipse$", "");
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(OpenInCurrentIde.class);
    }

    public static void main(String[] args) {
        // This is so it will be easier to debug problems in the future
        for (Info info : getProcessTree()) {
            System.out.println("Process:");
            info.command().ifPresent(
                    value -> System.out.println("Command: " + value));
            info.commandLine().ifPresent(
                    value -> System.out.println("Command line: " + value));
            info.arguments().ifPresent(values -> {
                for (int i = 0; i < values.length; i++) {
                    System.out.println("Arguments[" + i + "]: " + values[i]);
                }
            });
            System.out.println("");
        }
    }

    private static void run(String command, String... arguments)
            throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(command);
        cmd.addAll(Arrays.asList(arguments));
        ProcessBuilder pb = new ProcessBuilder().command(cmd);
        pb.start().waitFor();
    }

    private static List<Info> getProcessTree() {
        return getParentProcesses().stream().map(p -> p.info())
                .collect(Collectors.toList());
    }

    private static Optional<Info> findIdeCommand() {
        return findIdeCommand(getProcessTree());
    }

    static Optional<Info> findIdeCommand(List<Info> processes) {
        for (Info info : processes) {
            if (isEclipse(info) || isIdea(info)) {
                return Optional.of(info);
            }

            String cmd = info.command().get().toLowerCase(Locale.ENGLISH);
            if (cmd.contains("vscode") || cmd.contains("vs code")
                    || cmd.contains("code helper")
                    || cmd.contains("visual studio code")) {
                return Optional.of(info);
            }

        }
        return Optional.empty();
    }

    private static String getLowerCommandAndArguments(Info info) {
        return info.commandLine().get().toLowerCase(Locale.ENGLISH);
    }

    private static List<ProcessHandle> getParentProcesses() {
        List<ProcessHandle> proceses = new ArrayList<>();
        ProcessHandle p = ProcessHandle.current();
        while (p != null) {
            proceses.add(p);
            p = p.parent().orElse(null);
        }
        return proceses;
    }

    private static boolean isEclipse(Info info) {
        return info.command().get().toLowerCase(Locale.ENGLISH)
                .contains(ECLIPSE_IDENTIFIER);
    }

    private static boolean isIdea(Info info) {
        return getLowerCommandAndArguments(info).contains(INTELLIJ_IDENTIFIER);
    }

    private static boolean isVSCode(Info info) {
        String termProgram = System.getenv("TERM_PROGRAM");
        if ("vscode".equalsIgnoreCase(termProgram)) {
            return true;
        }
        return false;
    }

}
