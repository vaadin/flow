/*
 * Copyright 2000-2014 Vaadin Ltd.
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
package com.vaadin.tests.themes.valo;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.Position;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Page;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class CommonParts extends VerticalLayout implements View {
    public CommonParts() {
        setMargin(true);

        Label h1 = new Label("Common UI Elements");
        h1.addStyleName("h1");
        addComponent(h1);

        GridLayout row = new GridLayout(2, 3);
        row.setWidth("100%");
        row.setSpacing(true);
        addComponent(row);

        row.addComponent(loadingIndicators());
        row.addComponent(notifications(), 1, 0, 1, 2);
        row.addComponent(windows());

    }

    Panel loadingIndicators() {
        Panel p = new Panel("Loading Indicator");
        VerticalLayout content = new VerticalLayout();
        p.setContent(content);
        content.setSpacing(true);
        content.setMargin(true);
        content.addComponent(new Label(
                "You can test the loading indicator by pressing the buttons."));

        CssLayout group = new CssLayout();
        group.setCaption("Show the loading indicator for…");
        group.addStyleName("v-component-group");
        content.addComponent(group);
        Button loading = new Button("0.8");
        loading.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                }
            }
        });
        group.addComponent(loading);

        Button delay = new Button("3");
        delay.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                }
            }
        });
        group.addComponent(delay);

        Button wait = new Button("15");
        wait.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                }
            }
        });
        wait.addStyleName("last");
        group.addComponent(wait);
        Label label = new Label("&nbsp;&nbsp; seconds", ContentMode.HTML);
        label.setSizeUndefined();
        group.addComponent(label);

        Label spinnerDesc = new Label(
                "The theme also provides a mixin that you can use to include a spinner anywhere in your application. Below is a Label with a custom style name, for which the spinner mixin is added.");
        spinnerDesc.addStyleName("small");
        spinnerDesc.setCaption("Spinner");
        content.addComponent(spinnerDesc);

        if (!ValoThemeUI.isTestMode()) {
            Label spinner = new Label();
            spinner.addStyleName("spinner");
            content.addComponent(spinner);
        }

        return p;
    }

    Panel notifications() {
        Panel p = new Panel("Notifications");
        VerticalLayout content = new VerticalLayout() {
            Notification notification = new Notification("");
            TextField title = new TextField("Title");
            TextArea description = new TextArea("Description");
            MenuBar style = new MenuBar();
            MenuBar type = new MenuBar();
            String typeString = "";
            String styleString = "";
            TextField delay = new TextField();

            {
                setSpacing(true);
                setMargin(true);

                title.setInputPrompt("Title for the notification");
                title.addValueChangeListener(new ValueChangeListener() {
                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        if (title.getValue() == null
                                || title.getValue().length() == 0) {
                            notification.setCaption(null);
                        } else {
                            notification.setCaption(title.getValue());
                        }
                    }
                });
                title.setValue("Notification Title");
                title.setWidth("100%");
                addComponent(title);

                description.setInputPrompt("Description for the notification");
                description.addStyleName("small");
                description.addValueChangeListener(new ValueChangeListener() {
                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        if (description.getValue() == null
                                || description.getValue().length() == 0) {
                            notification.setDescription(null);
                        } else {
                            notification.setDescription(description.getValue());
                        }
                    }
                });
                description.setValue(
                        "A more informative message about what has happened. Nihil hic munitissimus habendi senatus locus, nihil horum? Inmensae subtilitatis, obscuris et malesuada fames. Hi omnes lingua, institutis, legibus inter se differunt.");
                description.setWidth("100%");
                addComponent(description);

                Command typeCommand = new Command() {
                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        if (selectedItem.getText().equals("Humanized")) {
                            typeString = "";
                            notification.setStyleName(styleString.trim());
                        } else {
                            typeString = selectedItem.getText().toLowerCase();
                            notification.setStyleName(
                                    (typeString + " " + styleString.trim())
                                            .trim());
                        }
                        for (MenuItem item : type.getItems()) {
                            item.setChecked(false);
                        }
                        selectedItem.setChecked(true);
                    }
                };

                type.setCaption("Type");
                MenuItem humanized = type.addItem("Humanized", typeCommand);
                humanized.setCheckable(true);
                humanized.setChecked(true);
                type.addItem("Tray", typeCommand).setCheckable(true);
                type.addItem("Warning", typeCommand).setCheckable(true);
                type.addItem("Error", typeCommand).setCheckable(true);
                type.addItem("System", typeCommand).setCheckable(true);
                addComponent(type);
                type.addStyleName("small");

                Command styleCommand = new Command() {
                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        styleString = "";
                        for (MenuItem item : style.getItems()) {
                            if (item.isChecked()) {
                                styleString += " "
                                        + item.getText().toLowerCase();
                            }
                        }
                        if (styleString.trim().length() > 0) {
                            notification.setStyleName(
                                    (typeString + " " + styleString.trim())
                                            .trim());
                        } else if (typeString.length() > 0) {
                            notification.setStyleName(typeString.trim());
                        } else {
                            notification.setStyleName(null);
                        }
                    }
                };

                style.setCaption("Additional style");
                style.addItem("Dark", styleCommand).setCheckable(true);
                style.addItem("Success", styleCommand).setCheckable(true);
                style.addItem("Failure", styleCommand).setCheckable(true);
                style.addItem("Bar", styleCommand).setCheckable(true);
                style.addItem("Small", styleCommand).setCheckable(true);
                style.addItem("Closable", styleCommand).setCheckable(true);
                addComponent(style);
                style.addStyleName("small");

                CssLayout group = new CssLayout();
                group.setCaption("Fade delay");
                group.addStyleName("v-component-group");
                addComponent(group);

                delay.setInputPrompt("Infinite");
                delay.addStyleName("align-right");
                delay.addStyleName("small");
                delay.setWidth("7em");
                delay.addValueChangeListener(new ValueChangeListener() {
                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        try {
                            notification.setDelayMsec(
                                    Integer.parseInt(delay.getValue()));
                        } catch (Exception e) {
                            notification.setDelayMsec(-1);
                            delay.setValue("");
                        }

                    }
                });
                delay.setValue("1000");
                group.addComponent(delay);

                Button clear = new Button(null, new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        delay.setValue("");
                    }
                });
                clear.setIcon(FontAwesome.TIMES_CIRCLE);
                clear.addStyleName("last");
                clear.addStyleName("small");
                clear.addStyleName("icon-only");
                group.addComponent(clear);
                group.addComponent(new Label("&nbsp; msec", ContentMode.HTML));

                GridLayout grid = new GridLayout(3, 3);
                grid.setCaption("Show in position");
                addComponent(grid);
                grid.setSpacing(true);

                Button pos = new Button("", new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        notification.setPosition(Position.TOP_LEFT);
                        notification.show(Page.getCurrent());
                    }
                });
                pos.addStyleName("small");
                grid.addComponent(pos);

                pos = new Button("", new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        notification.setPosition(Position.TOP_CENTER);
                        notification.show(Page.getCurrent());
                    }
                });
                pos.addStyleName("small");
                grid.addComponent(pos);

                pos = new Button("", new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        notification.setPosition(Position.TOP_RIGHT);
                        notification.show(Page.getCurrent());
                    }
                });
                pos.addStyleName("small");
                grid.addComponent(pos);

                pos = new Button("", new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        notification.setPosition(Position.MIDDLE_LEFT);
                        notification.show(Page.getCurrent());
                    }
                });
                pos.addStyleName("small");
                grid.addComponent(pos);

                pos = new Button("", new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        notification.setPosition(Position.MIDDLE_CENTER);
                        notification.show(Page.getCurrent());
                    }
                });
                pos.addStyleName("small");
                grid.addComponent(pos);

                pos = new Button("", new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        notification.setPosition(Position.MIDDLE_RIGHT);
                        notification.show(Page.getCurrent());
                    }
                });
                pos.addStyleName("small");
                grid.addComponent(pos);

                pos = new Button("", new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        notification.setPosition(Position.BOTTOM_LEFT);
                        notification.show(Page.getCurrent());
                    }
                });
                pos.addStyleName("small");
                grid.addComponent(pos);

                pos = new Button("", new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        notification.setPosition(Position.BOTTOM_CENTER);
                        notification.show(Page.getCurrent());
                    }
                });
                pos.addStyleName("small");
                grid.addComponent(pos);

                pos = new Button("", new ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        notification.setPosition(Position.BOTTOM_RIGHT);
                        notification.show(Page.getCurrent());
                    }
                });
                pos.addStyleName("small");
                grid.addComponent(pos);

            }
        };
        p.setContent(content);

        return p;
    }

    Panel windows() {
        Panel p = new Panel("Dialogs");
        VerticalLayout content = new VerticalLayout() {
            String prevHeight = "300px";
            boolean footerVisible = true;
            boolean autoHeight = false;
            boolean tabsVisible = false;
            boolean toolbarVisible = false;
            boolean footerToolbar = false;
            boolean toolbarLayout = false;
            String toolbarStyle = null;

            {
                setSpacing(true);
                setMargin(true);

                Command optionsCommand = new Command() {
                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        if (selectedItem.getText().equals("Footer")) {
                            footerVisible = selectedItem.isChecked();
                        }
                        if (selectedItem.getText().equals("Tabs")) {
                            tabsVisible = selectedItem.isChecked();
                        }

                        if (selectedItem.getText().equals("Top Toolbar")) {
                            toolbarVisible = selectedItem.isChecked();
                        }

                        if (selectedItem.getText().equals("Footer Toolbar")) {
                            footerToolbar = selectedItem.isChecked();
                        }

                        if (selectedItem.getText()
                                .equals("Top Toolbar layout")) {
                            toolbarLayout = selectedItem.isChecked();
                        }

                        if (selectedItem.getText()
                                .equals("Borderless Toolbars")) {
                            toolbarStyle = selectedItem.isChecked()
                                    ? "borderless" : null;
                        }

                    }
                };

                MenuBar options = new MenuBar();
                options.setCaption("Content");
                options.addItem("Tabs", optionsCommand).setCheckable(true);
                MenuItem option = options.addItem("Footer", optionsCommand);
                option.setCheckable(true);
                option.setChecked(true);
                options.addStyleName("small");
                addComponent(options);

                options = new MenuBar();
                options.setCaption("Toolbars");
                options.addItem("Footer Toolbar", optionsCommand)
                        .setCheckable(true);
                options.addItem("Top Toolbar", optionsCommand)
                        .setCheckable(true);
                options.addItem("Top Toolbar layout", optionsCommand)
                        .setCheckable(true);
                options.addItem("Borderless Toolbars", optionsCommand)
                        .setCheckable(true);
                options.addStyleName("small");
                addComponent(options);

            }
        };
        p.setContent(content);
        return p;

    }

    @Override
    public void enter(ViewChangeEvent event) {
        // TODO Auto-generated method stub

    }
}
