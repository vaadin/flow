package com.vaadin.hummingbird.test.performance;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.ui.UI;

public class MemoryUsageMonitor {

    private static final AtomicInteger uiCount = new AtomicInteger();

    private static final Runtime runtime = Runtime.getRuntime();

    public static final String PATH = "/sessionsize/";

    @WebServlet(urlPatterns = PATH
            + "*", name = "SessionSizeServlet", asyncSupported = true)
    public static class Servlet extends HttpServlet {
        @Override
        protected void service(HttpServletRequest request,
                HttpServletResponse response)
                throws ServletException, IOException {
            response.setContentType("text/plain");

            PrintWriter writer = response.getWriter();
            writer.println(MemoryUsageMonitor.getStatus());
            writer.close();

            return;
        }
    }

    public static void registerUI(UI ui) {
        // Keep track of participating UIs to have a chance of detecting if a UI
        // fails to load or if it's detached
        uiCount.incrementAndGet();
        ui.addDetachListener(e -> uiCount.decrementAndGet());
    }

    public static String getStatus() {
        try {
            while (true) {
                long memoryUsageBefore = getUsedMemory();

                for (int i = 0; i < 10; i++) {
                    System.gc();
                    Thread.sleep(10);
                }

                if (getUsedMemory() == memoryUsageBefore) {
                    // GC didn't cause any change -> we have a stable result
                    String message = "Heap usage with " + uiCount.get()
                            + " UIs: " + memoryUsageBefore;
                    return message;
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static long getUsedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }
}