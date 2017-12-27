/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.tests.components;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.grid.AbstractColumn;
import com.vaadin.flow.component.grid.ColumnGroup;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSelectionColumn;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.IronIcon;
import com.vaadin.flow.components.it.TestView;

/**
 * Test class for detecting that Vaadin components implement the correct
 * interfaces.
 */
public class MixinTest {

    private static final Set<String> PACKAGE_WHITELIST = new HashSet<>();
    private static final Set<Class<?>> CLASS_WHITELIST = new HashSet<>();
    private static final Set<Class<?>> REQUIRED_INTERFACES = new HashSet<>();

    static {
        PACKAGE_WHITELIST.addAll(Arrays.asList("com.vaadin.flow.component.html",
                "com.vaadin.flow.component.polymertemplate"));
        CLASS_WHITELIST.addAll(Arrays.asList(Icon.class, IronIcon.class,
                FormItem.class, GridSelectionColumn.class, Dialog.class,
                Grid.Column.class, ColumnGroup.class, AbstractColumn.class,
                Html.class, UI.class, Text.class, Component.class,
                Composite.class));
        REQUIRED_INTERFACES
                .addAll(Arrays.asList(HasSize.class, HasStyle.class));
    }

    @Test
    public void vaadinComponentsImplementBasicMixins() {
        List<Class<?>> componentClasses = ClasspathHelper
                .getVaadinClassesFromClasspath(path -> true,
                        clazz -> Component.class.isAssignableFrom(clazz)
                                && clazz.getPackage().getName()
                                        .startsWith("com.vaadin.flow.component")
                                && !TestView.class.isAssignableFrom(clazz)
                                && !PACKAGE_WHITELIST
                                        .contains(clazz.getPackage().getName())
                                && !clazz.getSimpleName()
                                        .startsWith("Generated"))
                .collect(Collectors.toList());

        componentClasses.stream()
                .filter(clazz -> !CLASS_WHITELIST.contains(clazz))
                .forEach(clazz -> {
                    Set<Class<?>> interfaces = new HashSet<>(
                            ClassUtils.getAllInterfaces(clazz));
                    REQUIRED_INTERFACES.forEach(
                            requiredIface -> Assert.assertTrue(String.format(
                                    "Class %s does not implement interface %s",
                                    clazz.getName(), requiredIface.getName()),
                                    interfaces.contains(requiredIface)));
                });
    }
}
