/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeButton;

public class SpringDevToolsReloadUtils {

    public static NativeButton createReloadTriggerButton() {
        NativeButton startTriggerButton = new NativeButton("Click to Start",
                event -> {
                    UI.getCurrent().getPage()
                            .executeJs("window.benchmark.start()");
                    Application.triggerReload();
                });
        startTriggerButton.setId("start-button");
        return startTriggerButton;
    }

    public static String runAndCalculateAverageResult(
            int numberOfTimesToRunTest, Supplier<String> test) {
        var allResults = new ArrayList<BigDecimal>();
        IntStream.range(0, numberOfTimesToRunTest).forEach(index -> allResults
                .add(BigDecimal.valueOf(Double.parseDouble(test.get()))));
        System.out.printf("Test run %s times. All results: [%s]%n",
                numberOfTimesToRunTest,
                allResults.stream().map(BigDecimal::toString)
                        .collect(Collectors.joining(",")));

        var result = allResults.stream().reduce(BigDecimal::add)
                .orElse(BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(allResults.size()),
                        RoundingMode.UNNECESSARY);

        return result.toString();
    }
}
