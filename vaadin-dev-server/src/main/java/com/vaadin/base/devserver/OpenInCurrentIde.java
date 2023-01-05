package com.vaadin.base.devserver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.open.OSUtils;
import com.vaadin.open.Open;

/**
 * Util for opening a file in the currently used IDE.
 * <p>
 * Supports detecting VS Code, Eclipse and IntelliJ.
 */
public class OpenInCurrentIde {

    private static final String ECLIPSE_IDENTIFIER = "eclipse";
    private static final String INTELLIJ_IDENTIFIER = "intellij";

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
        if (isVSCode()) {
            return Open.open("vscode://file" + absolutePath + ":" + lineNumber);
        } else if (isIdea()) {
            return Open.open(
                    "idea://open?file=" + absolutePath + "&line=" + lineNumber);
        } else if (isEclipse()) {
            Optional<String> eclipseApp = findIdeCommand();
            if (OSUtils.isMac()) {
                if (eclipseApp.isPresent()) {
                    String cmd = eclipseApp.get();
                    cmd = cmd.replaceFirst("/Contents/MacOS/eclipse$", "");
                    try {
                        run("open", "-a", cmd, absolutePath);
                    } catch (Exception e) {
                        getLogger().error("Unable to launch Eclipse", e);
                    }
                }
            } else {
                if (eclipseApp.isPresent()) {
                    try {
                        run(eclipseApp.get(), absolutePath + ":" + lineNumber);
                    } catch (Exception e) {
                        getLogger().error("Unable to launch Eclipse", e);
                    }
                }
            }
            return true;
        }

        return false;

    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(OpenInCurrentIde.class);
    }

    private static void run(String command, String... arguments)
            throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(command);
        cmd.addAll(Arrays.asList(arguments));
        ProcessBuilder pb = new ProcessBuilder().command(cmd);
        pb.start().waitFor();
    }

    private static Optional<String> findIdeCommand() {
        for (ProcessHandle p : getParentProcesses()) {
            String cmd = p.info().command().orElse("");
            String cmdLower = cmd.toLowerCase(Locale.ENGLISH);

            if (cmdLower.contains(ECLIPSE_IDENTIFIER)) {
                return Optional.of(cmd);
            }

            if (cmdLower.contains(INTELLIJ_IDENTIFIER)) {
                return Optional.of(cmd);
            }
            if (cmdLower.contains("vscode") || cmdLower.contains("vs code")
                    || cmdLower.contains("code helper")
                    || cmdLower.contains("visual studio code")) {
                return Optional.of(cmd);
            }

        }
        return Optional.empty();
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

    private static boolean isEclipse() {
        return findIdeCommand().map(cmd -> cmd.toLowerCase(Locale.ENGLISH))
                .filter(cmd -> cmd.contains(ECLIPSE_IDENTIFIER)).isPresent();
    }

    private static boolean isIdea() {
        return findIdeCommand().map(cmd -> cmd.toLowerCase(Locale.ENGLISH))
                .filter(cmd -> cmd.contains(INTELLIJ_IDENTIFIER)).isPresent();
    }

    private static boolean isVSCode() {
        String termProgram = System.getenv("TERM_PROGRAM");
        if ("vscode".equalsIgnoreCase(termProgram)) {
            return true;
        }
        return false;
    }

}
