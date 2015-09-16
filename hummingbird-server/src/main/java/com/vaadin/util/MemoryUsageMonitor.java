package com.vaadin.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.vaadin.ui.Component.DetachEvent;
import com.vaadin.ui.Component.DetachListener;
import com.vaadin.ui.UI;

public class MemoryUsageMonitor {

    private static final AtomicInteger uiCount = new AtomicInteger();
    private static final AtomicLong lastUiOpenTime = new AtomicLong();

    static {
        Thread reporterThread = new Thread("MemoryUsageMonitor") {
            @Override
            public void run() {
                Runtime runtime = Runtime.getRuntime();
                int lastUiCount = -1;
                try {
                    while (true) {
                        sleep(1000);
                        int currentUiCount = uiCount.get();
                        if (lastUiCount != currentUiCount && System
                                .currentTimeMillis() > lastUiOpenTime.get()
                                        + 1000) {
                            lastUiCount = currentUiCount;

                            long freeMem = runtime.totalMemory()
                                    - runtime.freeMemory();
                            while (true) {
                                System.gc();
                                sleep(100);
                                long newFreeMem = runtime.totalMemory()
                                        - runtime.freeMemory();
                                if (newFreeMem >= freeMem) {
                                    System.out.println(
                                            "Heap usage with " + currentUiCount
                                                    + " UIs: " + freeMem);
                                    break;
                                } else {
                                    freeMem = newFreeMem;
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
        };
        reporterThread.setDaemon(true);
        reporterThread.start();
    }

    public static void registerUI(UI ui) {
        uiCount.incrementAndGet();
        lastUiOpenTime.set(System.currentTimeMillis());

        ui.addDetachListener(new DetachListener() {
            @Override
            public void detach(DetachEvent event) {
                uiCount.decrementAndGet();
            }
        });
    }
}
