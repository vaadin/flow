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
package com.vaadin.flow.spring.test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpringDevToolsReloadUtils {

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
