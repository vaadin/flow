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
package com.vaadin.packaging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

public class SplitPackagesTest {
    /*
     * Modules that contain known split packages that we don't care about
     */
    private static final Set<String> ignoredModules = new HashSet<>(
            Arrays.asList("demo-flow-components", "flow-tests", "flow-test-generic",
                    "flow-code-generator", "flow-generated-components", "vaadin-grid-flow", "vaadin-select-flow"));

    /*
     * Scans through all Maven modules to collect for which Java packages that
     * module declares at least one Java class. The test fails if any Java
     * package contains classes from multiple modules.
     */
    @Test
    public void findSplitPackages() throws IOException {
        Collection<File> modules = findModules();
        Map<String, Set<File>> packageToModules = mapPackagesToModules(modules);
        String errors = collectErrors(packageToModules);

        if (!errors.isEmpty()) {
            Assert.fail(errors);
        }
    }

    private String collectErrors(Map<String, Set<File>> packages) {
        return packages.entrySet().stream()
                .flatMap(entry -> getErrors(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private Stream<? extends String> getErrors(String pck, Set<File> modules) {
        if (modules.size() != 1) {
            return Stream.of(pck + " contains classes from multiple modules: "
                    + modules);
        } else {
            return Stream.empty();
        }
    }

    private Map<String, Set<File>> mapPackagesToModules(
            Collection<File> modules) {
        Map<String, Set<File>> packageToModules = new HashMap<>();

        for (File module : modules) {
            File srcDir = new File(module, "src/main/java");

            if (!srcDir.exists()) {
                continue;
            }

            Set<String> packages = new HashSet<>();

            collectPackages("", srcDir, packages);

            for (String pck : packages) {
                packageToModules.computeIfAbsent(pck, key -> new HashSet<>())
                        .add(module);
            }
        }
        return packageToModules;
    }

    private void collectPackages(String packageName, File dir,
            Set<String> packages) {
        boolean containsJavaFile = false;

        for (File child : dir.listFiles()) {
            if (child.isDirectory()) {
                String childPackageName = packageName + "." + child.getName();

                collectPackages(childPackageName, child, packages);
            } else {
                containsJavaFile |= child.getName().endsWith(".java");
            }
        }

        if (containsJavaFile) {
            if (!packageName.isEmpty()) {
                // Remove leading .
                packageName = packageName.substring(1);
            }

            packages.add(packageName);
        }
    }

    private Collection<File> findModules() throws IOException {
        Collection<File> modules = new ArrayList<>();

        collectModules(new File(".."), modules);

        return modules;
    }

    private void collectModules(File dir, Collection<File> modules) {
        if (ignoredModules.contains(dir.getName())) {
            return;
        }

        if (new File(dir, "pom.xml").exists()) {
            modules.add(dir);

            for (File child : dir.listFiles()) {
                if (child.isDirectory()) {
                    collectModules(child, modules);
                }
            }
        }
    }
}
