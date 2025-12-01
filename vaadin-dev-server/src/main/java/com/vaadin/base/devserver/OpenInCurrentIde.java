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
package com.vaadin.base.devserver;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessHandle.Info;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        IdeAndProcessInfo ideInfo = getIdeAndProcessInfo();
        if (ideInfo.ide == Ide.VSCODE) {
            return Open.open("vscode://file" + absolutePath + ":" + lineNumber);
        } else if (ideInfo.ide == Ide.INTELLIJ) {
            try {
                run(getBinary(ideInfo.processInfo), "--line", lineNumber + "",
                        absolutePath);
                return true;
            } catch (Exception e) {
                getLogger().error("Unable to launch IntelliJ IDEA", e);
            }

        } else if (ideInfo.ide == Ide.ECLIPSE) {
            if (OSUtils.isMac()) {
                try {
                    run("open", "-a", getBinary(ideInfo.processInfo),
                            absolutePath);
                    return true;
                } catch (Exception e) {
                    getLogger().error("Unable to launch Eclipse", e);
                }
            } else {
                try {
                    run(getBinary(ideInfo.processInfo),
                            absolutePath + ":" + lineNumber);
                    return true;
                } catch (Exception e) {
                    getLogger().error("Unable to launch Eclipse", e);
                }
            }
        }

        return false;

    }

    public enum Ide {
        ECLIPSE, VSCODE, INTELLIJ, OTHER
    }

    public record IdeAndProcessInfo(Ide ide, Info processInfo) {
    }

    /**
     * Gets the IDE and process info for the current process.
     *
     * @return the IDE and process info
     */
    public static IdeAndProcessInfo getIdeAndProcessInfo() {
        Optional<Info> maybeIdeCommand = findIdeCommandInfo();
        if (!maybeIdeCommand.isPresent()) {
            getLogger().debug("Unable to detect IDE from process tree");
            printProcessTree(msg -> getLogger().debug(msg));
            return new IdeAndProcessInfo(Ide.OTHER, null);
        }

        Info processInfo = maybeIdeCommand.get();

        Ide ide;
        if (isVSCode(processInfo)) {
            ide = Ide.VSCODE;
        } else if (isIdea(processInfo)) {
            ide = Ide.INTELLIJ;
        } else if (isEclipse(processInfo)) {
            ide = Ide.ECLIPSE;
        } else {
            ide = Ide.OTHER;
        }
        return new IdeAndProcessInfo(ide, processInfo);
    }

    static String getBinary(Info info) {
        String cmd = info.command().get();
        if (isIdea(info)) {
            return getIdeaBinary(info);
        } else if (isEclipse(info)) {
            return cmd.replaceFirst("/Contents/MacOS/eclipse$", "");
        }
        return cmd;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(OpenInCurrentIde.class);
    }

    public static void main(String[] args) {
        // This is so it will be easier to debug problems in the future
        printProcessTree(System.out::println);
    }

    private static void printProcessTree(Consumer<String> printer) {
        for (Info info : getProcessTree()) {
            printer.accept("Process tree:");
            info.command()
                    .ifPresent(value -> printer.accept("Command: " + value));
            info.commandLine().ifPresent(
                    value -> printer.accept("Command line: " + value));
            info.arguments().ifPresent(values -> {
                for (int i = 0; i < values.length; i++) {
                    printer.accept("Arguments[" + i + "]: " + values[i]);
                }
            });
            printer.accept("");
        }

    }

    static void run(String command, String... arguments)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(command);
        cmd.addAll(Arrays.asList(arguments));
        ProcessBuilder pb = new ProcessBuilder().command(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String output = new String(process.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            throw new IOException(
                    "Command " + cmd + " terminated with exit code " + exitCode
                            + ".\nOutput:\n" + output);
        }
    }

    private static List<Info> getProcessTree() {
        return getParentProcesses().stream().map(p -> p.info())
                .collect(Collectors.toList());
    }

    private static Optional<Info> findIdeCommandInfo() {
        return findIdeCommand(getProcessTree());
    }

    static Optional<Info> findIdeCommand(List<Info> processes) {
        for (Info info : processes) {
            if (isIdea(info) || isVSCode(info) || isEclipse(info)) {
                return Optional.of(info);
            }
        }
        return Optional.empty();
    }

    private static String getCommandAndArguments(Info info) {
        return info.commandLine().orElse(null);
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

    static boolean isEclipse(Info info) {
        Optional<String> cmd = info.command();
        if (cmd.isPresent()) {
            String lowerCmd = cmd.get().toLowerCase(Locale.ENGLISH);
            // Eclipse has a lot of other products like Temurin and Adoptium so
            // we cannot check with "contains"
            return lowerCmd.endsWith("eclipse")
                    || lowerCmd.endsWith("eclipse.exe");
        }

        return false;
    }

    static boolean isIdea(Info info) {
        return getIdeaBinary(info) != null;
    }

    private static String getIdeaBinary(Info info) {
        String commandAndArguments = getCommandAndArguments(info);
        if (commandAndArguments != null
                && commandAndArguments.contains("idea_rt.jar")) {
            String replaced = commandAndArguments
                    .replaceFirst(".*[:;]([^:;]*)(idea_rt.jar).*", "$1$2");
            if (!replaced.equals(commandAndArguments)) {
                File binFolder = new File(
                        new File(replaced).getParentFile().getParentFile(),
                        "bin");
                Optional<File> bin = Stream.of("idea", "idea.sh", "idea.bat")
                        .map(binName -> new File(binFolder, binName))
                        .filter(binaryFile -> binaryFile.exists()).findFirst();
                if (bin.isPresent()) {
                    return bin.get().getAbsolutePath();
                }
            }
        }
        return info.command().filter(cmd -> cmd.contains("idea")).orElse(null);
    }

    static boolean isVSCode(Info info) {
        String termProgram = System.getenv("TERM_PROGRAM");
        if ("vscode".equalsIgnoreCase(termProgram)) {
            return true;
        }

        String cmd = getCommandAndArguments(info);
        if (cmd != null) {
            String cmdLower = cmd.toLowerCase(Locale.ENGLISH);
            if (cmdLower.contains("vscode") || cmdLower.contains("vs code")
                    || cmdLower.contains("code helper")
                    || cmdLower.contains("visual studio code")) {
                return true;
            }
        }

        return false;
    }

}
