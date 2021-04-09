package com.vaadin.flow.testutil;

import com.vaadin.testbench.TestBench;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public interface ChromeHeadlessInitializer {

    default ChromeOptions createHeadlessChromeOptions() {
        final ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu");
        return options;
    }

    default WebDriver createHeadlessChromeDriver() {
        return TestBench
                .createDriver(new ChromeDriver(createHeadlessChromeOptions()));
    }

}
