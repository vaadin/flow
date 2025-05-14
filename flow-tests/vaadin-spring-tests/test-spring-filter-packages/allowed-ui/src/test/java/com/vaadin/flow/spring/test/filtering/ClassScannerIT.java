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
package com.vaadin.flow.spring.test.filtering;

import java.util.List;
import java.util.stream.Stream;

import com.vaadin.flow.spring.test.exclude.ExcludedRoute;
import com.vaadin.flow.spring.test.allowed.AllowedView;
import com.vaadin.flow.spring.test.allowed.startup.CustomVaadinServiceInitListener;
import com.vaadin.flow.spring.test.allowed.startup.vaadin.AllowedRoute;
import com.vaadin.flow.spring.test.allowed.BlockedRoute;
import com.vaadin.flow.spring.test.allowed.ScannedAllowedRoute;
import com.vaadin.flow.spring.test.blocked.startup.BlockedCustomVaadinServiceInitListener;
import com.vaadin.flow.spring.test.blocked.startup.vaadin.ScannedBlockedRoute;
import com.vaadin.flow.spring.test.blocked.BlockedView;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

/**
 * Primary target of this IT is class scanning of DevModeServletContextListener
 * in {@link com.vaadin.flow.spring.VaadinServletContextInitializer} and
 * especially usage of {@code vaadin.blocked-packages} and
 * {@code vaadin.allowed-packages} in a multi-module Maven project with jar
 * packaged dependencies.
 */
public class ClassScannerIT extends ChromeBrowserTest {

    @Test
    public void allowedUiModule_withAllowedPackages() {
        open();
        assertClassAllowed(ClassScannerView.class.getSimpleName());
        assertClassAllowed(AllowedView.class.getSimpleName());
        assertClassBlocked(BlockedView.class.getSimpleName());
    }

    @Test
    public void libAllowedModule_withAllowedPackagesJar() {
        open();
        assertClassAllowed(AllowedRoute.class.getSimpleName());
        assertClassAllowed(
                CustomVaadinServiceInitListener.class.getSimpleName());
        assertClassBlocked(BlockedRoute.class.getSimpleName());
    }

    @Test
    public void libBlockedModule_withBlockedPackagesJar() {
        open();
        assertClassBlocked(ScannedBlockedRoute.class.getSimpleName());
        assertClassBlocked(
                BlockedCustomVaadinServiceInitListener.class.getSimpleName());
        assertClassAllowed(ScannedAllowedRoute.class.getSimpleName());
    }

    @Test
    public void libExcludedModule_withExcludedJar() {
        open();
        assertClassBlocked(ExcludedRoute.class.getSimpleName());
    }

    private void assertClassAllowed(String className) {
        Assert.assertTrue(className + " should be allowed.",
                getScannedClasses().contains(className));
    }

    private void assertClassBlocked(String className) {
        Assert.assertFalse(className + " should be blocked.",
                getScannedClasses().contains(className));
    }

    private List<String> getScannedClasses() {
        return Stream.of(findElement(By.id(ClassScannerView.SCANNED_CLASSES))
                .getText().split(",")).map(String::trim).toList();
    }

    @Override
    protected String getTestPath() {
        return "/";
    }
}
