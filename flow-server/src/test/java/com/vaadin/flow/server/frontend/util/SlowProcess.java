package com.vaadin.flow.server.frontend.util;

import java.io.PrintStream;

/**
 * Test helper: a simple Java main that writes to stdout and stderr with delays
 * and then exits with code 0.
 */
public class SlowProcess {

    public static void main(String[] args) throws Exception {
        int lines = 5;
        int delayMs = 200; // keep tests fast but non-zero
        if (args.length > 0) {
            try {
                lines = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        if (args.length > 1) {
            try {
                delayMs = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        PrintStream out = System.out;
        PrintStream err = System.err;
        for (int i = 1; i <= lines; i++) {
            out.println("OUT-" + i);
            if (i % 2 == 0) {
                err.println("ERR-" + i);
            }
            out.flush();
            err.flush();
            Thread.sleep(delayMs);
        }
    }
}
