package com.vaadin.tests.util;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class UIOpenerUtil {
    public static void open100(String url) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < 20; i++) {
                        openUI(url);
                    }
                }
            };
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    public static void openUI(String url) {
        PhantomJSDriver driver = new PhantomJSDriver();
        try {
            driver.get(url);
            new WebDriverWait(driver, 10).until(ExpectedConditions
                    .presenceOfElementLocated(By.tagName("input")));
        } finally {
            driver.close();
            driver.quit();
        }
    }
}
