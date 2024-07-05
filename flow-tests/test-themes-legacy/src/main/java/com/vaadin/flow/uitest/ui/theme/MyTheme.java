/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.theme;

import java.util.Collections;
import java.util.List;

import com.vaadin.flow.theme.AbstractTheme;

public class MyTheme implements AbstractTheme {
    @Override
    public String getBaseUrl() {
        return "src/";
    }

    @Override
    public String getThemeUrl() {
        return "theme/myTheme/";
    }

    @Override
    public List<String> getHeaderInlineContents() {
        return Collections.singletonList("<custom-style> <style>   html {"
                + "      font-size: 20px;  color:red;  } </style> </custom-style>");
    }
}
