/*
 * Copyright 2000-2020 Vaadin Ltd.
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

package com.vaadin.flow.uitest.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.ThemeLiveReloadView", layout = ViewTestLayout.class)
public class ThemeLiveReloadView extends Div {
    public static final String CHANGE_THEME_BUTTON_ID = "change-theme-button";

    private static final String APP_THEME = "app-theme";
    private static final String OTHER_THEME = "other-theme";

    public ThemeLiveReloadView() {
        NativeButton changeThemeButton = new NativeButton("Change Theme Name",
                click -> {
                    File baseDir = new File(
                            System.getProperty("user.dir", "."));
                    File appShellClassFile = Paths
                            .get(baseDir.getPath(), "src", "main", "java",
                                    AppShell.class.getCanonicalName()
                                            .replace(".", File.separator)
                                            .concat(".java"))
                            .toFile();
                    try {
                        String content = FileUtils.readFileToString(
                                appShellClassFile, StandardCharsets.UTF_8);
                        if (content.contains(APP_THEME)) {
                            content = content.replace(APP_THEME, OTHER_THEME);
                        } else if (content.contains(OTHER_THEME)) {
                            content = content.replace(OTHER_THEME, APP_THEME);
                        } else {
                            throw new IllegalStateException(String.format(
                                    "No '%s' or '%s' found in AppShell class",
                                    APP_THEME, OTHER_THEME));
                        }
                        FileUtils.writeStringToFile(appShellClassFile, content,
                                StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException(
                                "Failed to change theme name in AppShell class",
                                e);
                    }
                });
        changeThemeButton.setId(CHANGE_THEME_BUTTON_ID);
        add(changeThemeButton);

        add(new Paragraph("This is a Paragraph to test the applied font"));
    }
}
