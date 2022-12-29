package com.vaadin.base.devserver;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.vaadin.open.OSUtils;
import com.vaadin.open.Open;

public class OpenInIde {

    public static void main(String[] args) {
        System.out.println("=========");
        System.out.println();
        System.out.println();
        boolean vsCode = isVSCode();
        System.out.print("Running in VS Code ? ");
        System.out.println(vsCode ? "Yes" : "No");

        boolean idea = isIdea();
        System.out.print("Running in Idea ? ");
        System.out.println(idea ? "Yes" : "No");

        boolean eclipse = isEclipse();
        System.out.print("Running in Eclipse ? ");
        System.out.println(eclipse ? "Yes" : "No");

        String ideBinary = findIdeCommand().orElse("?");
        System.out.println("Ide command: " + ideBinary);

        // Class<?> c = TestFile.class;
        // File projectDir = Path.of("").toAbsolutePath().toFile();
        // File file = new File(new File(projectDir, "src/main/java"),
        // c.getName().replace(".", File.separator) + ".java");

        // openFile(file.getAbsolutePath(), 5);
    }

    public static boolean openFile(String absolutePath, int lineNumber) {
        if (isVSCode()) {
            Open.open("vscode://file" + absolutePath + ":" + lineNumber);
            // System.err.println("TestFile (1) / methodA is defined at " +
            // file.getAbsolutePath() + ":5");
            // System.err.println(
            // "TestFile (2) / methodE is defined at " +
            // file.getAbsolutePath().replace("test1", "test2") + ":21");
            return true;
        } else if (isIdea()) {
            Open.open(
                    "idea://open?file=" + absolutePath + "&line=" + lineNumber);
            // System.err.println("TestFile (1) / methodA is defined at " +
            // file.getAbsolutePath() + ":5");
            // System.err.println(
            // "TestFile (2) / methodE is defined at " +
            // file.getAbsolutePath().replace("test1", "test2") + ":21");
            return true;
        } else if (isEclipse()) {
            Optional<String> eclipseApp = findIdeCommand();
            if (OSUtils.isMac()) {
                if (eclipseApp.isPresent()) {
                    String cmd = eclipseApp.get();
                    cmd = cmd.replaceFirst("/Contents/MacOS/eclipse$", "");
                    try {
                        run("open", "-a", cmd, absolutePath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (eclipseApp.isPresent()) {
                    try {
                        run(eclipseApp.get(), absolutePath + ":" + lineNumber);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return true;
        }

        return false;

    }

    private static void run(String command, String... arguments)
            throws Exception {
        // System.out.println("Run " + command+"
        // "+Stream.of(arguments).collect(Collectors.joining(" ")));
        List<String> cmd = new ArrayList<>();
        cmd.add(command);
        cmd.addAll(Arrays.asList(arguments));
        ProcessBuilder pb = new ProcessBuilder().command(cmd);
        pb.start().waitFor();
    }

    private static boolean isEclipse() {
        return findIdeCommand().map(cmd -> cmd.toLowerCase(Locale.ENGLISH))
                .filter(cmd -> cmd.contains("eclipse")).isPresent();
    }

    private static boolean isIdea() {
        return findIdeCommand().map(cmd -> cmd.toLowerCase(Locale.ENGLISH))
                .filter(cmd -> cmd.contains("intellij")).isPresent();
    }

    private static Optional<String> findIdeCommand() {
        for (ProcessHandle p : getParentProcesses()) {
            String cmd = p.info().command().orElse("");
            // System.out.println(cmd);
            String cmdLower = cmd.toLowerCase(Locale.ENGLISH);

            if (cmdLower.contains("eclipse")) {
                return Optional.of(cmd);
            }

            if (cmdLower.contains("intellij")) {
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
        return proceses;// .stream().map(proc ->
                        // proc.info().command().orElse("")).collect(Collectors.toList());
    }

    private static boolean isVSCode() {
        String termProgram = System.getenv("TERM_PROGRAM");
        if ("vscode".equalsIgnoreCase(termProgram)) {
            return true;
        }
        return false;
    }

}
