/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.devloop.test.shared;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * Renders a task's due date for the view.
 * <p>
 * Trivial on purpose. Its value is that it lives in another module and its
 * output is visible in the browser, so an {@code apply} that claims a sibling
 * module's edit is live can be checked against what the page actually shows -
 * and under a deployed WAR that is a real question, because the module's
 * classes reach the application as a jar the container copied into its own
 * webapps directory rather than through a classpath the daemon handed over.
 * <p>
 * Edit {@link #format(LocalDate)} to watch a sibling module's change reach the
 * page.
 */
public final class DueDateFormatter {

    private final DateTimeFormatter formatter;

    public DueDateFormatter(Locale locale) {
        this.formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(locale);
    }

    /** A missing due date is a state, not an absence, so it gets words too. */
    public String format(LocalDate dueDate) {
        return dueDate == null ? "Never" : formatter.format(dueDate);
    }
}
