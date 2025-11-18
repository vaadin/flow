package com.vaadin.flow.server.frontend;

import com.vaadin.flow.internal.Pair;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for process IO consumption using the default executor path.
 */
public class FrontendProcessIOIT {

    @Test
    public void consumeProcessStreams_readsStdoutAndStderr_withoutDeadlock() throws Exception {
        String cp = System.getProperty("java.class.path");
        List<String> cmd = Arrays.asList(
                getJavaBin(),
                "-cp", cp,
                "com.vaadin.flow.server.frontend.util.SlowProcess",
                "20", // lines
                "5"   // delay ms
        );

        Process process = new ProcessBuilder(cmd).start();
        CompletableFuture<Pair<String, String>> fut = FrontendUtils.consumeProcessStreams(process);
        int exit = process.waitFor();
        assertEquals(0, exit);
        Pair<String, String> out = fut.get(5, TimeUnit.SECONDS);
        assertTrue(out.getFirst().contains("OUT-1"));
        assertTrue(out.getSecond().contains("ERR-2"));
    }

    private static String getJavaBin() {
        String javaHome = System.getProperty("java.home");
        File bin = new File(javaHome, "bin");
        String exe = FrontendUtils.isWindows() ? "java.exe" : "java";
        return new File(bin, exe).getAbsolutePath();
    }
}
