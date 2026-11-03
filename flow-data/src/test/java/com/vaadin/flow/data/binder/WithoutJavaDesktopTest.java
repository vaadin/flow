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
package com.vaadin.flow.data.binder;

import jakarta.servlet.ServletContext;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.BeanUtil;
import com.vaadin.flow.internal.JacksonSerializer;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PwaRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that Flow runs without the {@code java.desktop} module, which is about
 * a quarter of a {@code jlink} image: a JVM without the module loads every
 * class of flow-server and flow-data, and uses the bean properties of
 * flow-server and the property sets of the binder.
 */
class WithoutJavaDesktopTest {

    /**
     * Classes that may need {@code java.desktop} when they are loaded: the
     * runtime generation of PWA icons.
     */
    private static final Set<String> NEED_JAVA_DESKTOP = Set
            .of("com.vaadin.flow.server.PwaIcon$Drawing");

    @Test
    void flowRunsWithoutJavaDesktop()
            throws IOException, InterruptedException, URISyntaxException {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java")
                .toString());
        command.add("--limit-modules");
        command.add(String.join(",", modulesWithoutJavaDesktop()));
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Probe.class.getName());
        command.add(codeSource(BeanUtil.class).toString());
        command.add(codeSource(BeanPropertySet.class).toString());

        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertTrue(process.waitFor(2, TimeUnit.MINUTES));
        assertEquals(0, process.exitValue(), output);

        Set<String> needJavaDesktop = output.lines()
                .filter(line -> line.startsWith(Probe.NEEDS_JAVA_DESKTOP))
                .map(line -> line.substring(Probe.NEEDS_JAVA_DESKTOP.length()))
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(NEED_JAVA_DESKTOP, needJavaDesktop, output);
    }

    /**
     * The system modules that neither are nor require {@code java.desktop}.
     */
    private static Set<String> modulesWithoutJavaDesktop() {
        Map<String, ModuleDescriptor> modules = ModuleFinder.ofSystem()
                .findAll().stream().map(ModuleReference::descriptor)
                .collect(Collectors.toMap(ModuleDescriptor::name,
                        descriptor -> descriptor));
        Set<String> excluded = new HashSet<>(Set.of("java.desktop"));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (ModuleDescriptor descriptor : modules.values()) {
                if (!excluded.contains(descriptor.name()) && descriptor
                        .requires().stream()
                        .filter(requires -> !requires.modifiers().contains(
                                ModuleDescriptor.Requires.Modifier.STATIC))
                        .anyMatch(requires -> excluded
                                .contains(requires.name()))) {
                    excluded.add(descriptor.name());
                    changed = true;
                }
            }
        }
        Set<String> result = new TreeSet<>(modules.keySet());
        result.removeAll(excluded);
        return result;
    }

    private static Path codeSource(Class<?> type) throws URISyntaxException {
        return Path.of(type.getProtectionDomain().getCodeSource().getLocation()
                .toURI());
    }

    /**
     * Runs in the JVM without {@code java.desktop}. Prints the classes that
     * cannot be loaded without it, and exits with a non-zero status if
     * something else fails.
     */
    public static class Probe {

        static final String NEEDS_JAVA_DESKTOP = "needs java.desktop: ";

        public static void main(String[] args) throws Exception {
            if (ModuleLayer.boot().findModule("java.desktop").isPresent()) {
                throw new IllegalStateException(
                        "java.desktop is in the boot layer");
            }
            for (String location : args) {
                for (String className : classNames(Path.of(location))) {
                    loadClass(className);
                }
            }
            useBeanProperties();
            usePwaWithoutGeneratedIcons();
            System.out.println("done");
        }

        private static void loadClass(String className) {
            try {
                // Initializing a class links it: the JVM verifies it and
                // loads the types the verification needs
                Class.forName(className, true, Probe.class.getClassLoader());
            } catch (Throwable e) {
                for (Throwable cause = e; cause != null; cause = cause
                        .getCause()) {
                    String message = String.valueOf(cause.getMessage());
                    if (message.startsWith("java/awt/")
                            || message.startsWith("java/beans/")
                            || message.startsWith("javax/imageio/")) {
                        System.out.println(NEEDS_JAVA_DESKTOP + className);
                        return;
                    }
                }
                // Other classes need what this JVM does not set up, such as
                // a running service, which does not matter here
            }
        }

        private static List<String> classNames(Path location)
                throws IOException {
            Stream<String> entries;
            if (Files.isDirectory(location)) {
                try (Stream<Path> files = Files.walk(location)) {
                    entries = files.map(file -> location.relativize(file)
                            .toString().replace('\\', '/')).toList().stream();
                }
            } else {
                try (JarFile jar = new JarFile(location.toFile())) {
                    entries = jar.stream().map(entry -> entry.getName())
                            .toList().stream();
                }
            }
            return entries
                    .filter(name -> name.endsWith(".class")
                            && !name.contains("module-info")
                            && !name.startsWith("META-INF/"))
                    .map(name -> name
                            .substring(0, name.length() - ".class".length())
                            .replace('/', '.'))
                    .sorted().toList();
        }

        private static void useBeanProperties() throws Exception {
            Person person = new Person();
            person.setName("Ada");
            person.setAge(36);
            person.getAddress().setCity("London");

            PropertySet<Person> propertySet = BeanPropertySet.get(Person.class,
                    true, PropertyFilterDefinition.getDefaultFilter());
            PropertyDefinition<Person, ?> city = propertySet
                    .getProperty("address.city").orElseThrow();
            check("London", city.getGetter().apply(person));
            @SuppressWarnings("unchecked")
            PropertyDefinition<Person, Integer> age = (PropertyDefinition<Person, Integer>) propertySet
                    .getProperty("age").orElseThrow();
            age.getSetter().orElseThrow().accept(person, 37);
            check(37, person.getAge());

            JsonNode json = JacksonSerializer.toJson(person);
            check("Ada", json.get("name").asString());
            Person copy = JacksonSerializer.toObject(Person.class, json);
            check("Ada", copy.getName());

            check(List.of("address.city", "age", "name"),
                    BeanUtil.getBeanPropertyPaths(Person.class));

            Field name = Person.class.getDeclaredField("name");
            check("Ada", ReflectTools.getJavaFieldValue(person, name));

            new Element("div").addEventListener("click", event -> {
            }).addEventData(Person.class);
        }

        /**
         * Without generated icons, a PWA would draw them at run time with AWT:
         * they are left out instead.
         */
        private static void usePwaWithoutGeneratedIcons() throws IOException {
            ServletContext servletContext = (ServletContext) Proxy
                    .newProxyInstance(Probe.class.getClassLoader(),
                            new Class<?>[] { ServletContext.class },
                            (proxy, method, methodArgs) -> null);
            PwaRegistry registry = new PwaRegistry(
                    PwaApplication.class.getAnnotation(PWA.class),
                    servletContext);
            check(List.of(), registry.getIcons());
            check(true, registry.getManifestJson().contains("Probe"));
        }

        private static void check(Object expected, Object actual) {
            if (!expected.equals(actual)) {
                throw new AssertionError(
                        "Expected " + expected + " but got " + actual);
            }
        }
    }

    @PWA(name = "Probe", shortName = "Probe")
    public static class PwaApplication {
    }

    public static class Person {
        private String name;
        private int age;
        private Address address = new Address();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    public static class Address {
        private String city;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }
}
