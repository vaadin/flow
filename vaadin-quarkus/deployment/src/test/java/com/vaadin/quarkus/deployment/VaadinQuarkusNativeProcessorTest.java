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
package com.vaadin.quarkus.deployment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.paths.PathTree;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouterLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaadinQuarkusNativeProcessorTest {

    private VaadinQuarkusNativeProcessor processor;
    private Index index;

    @BeforeEach
    void setUp() throws IOException {
        processor = new VaadinQuarkusNativeProcessor();

        // Create an index with test classes
        Indexer indexer = new Indexer();

        // Index the test classes and related types
        indexer.indexClass(Collection.class);
        indexer.indexClass(String.class);
        indexer.indexClass(List.class);
        indexer.indexClass(Integer.class);

        indexer.indexClass(TestComponent.class);
        indexer.indexClass(SimpleDto.class);
        indexer.indexClass(ComplexDto.class);
        indexer.indexClass(NestedDto.class);
        indexer.indexClass(RecordId.class);
        indexer.indexClass(RecordDto.class);

        index = indexer.complete();
    }

    @Test
    void testDetectClientCallablesTypes_withSimpleReturnType() {
        Set<ClassInfo> result = processor.detectClientCallablesTypes(index);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(containsClass(SimpleDto.class)),
                "Should detect SimpleDto from return type");
    }

    @Test
    void testDetectClientCallablesTypes_withParameterTypes() {
        Set<ClassInfo> result = processor.detectClientCallablesTypes(index);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(containsClass(ComplexDto.class)),
                "Should detect ComplexDto from parameter type");
    }

    @Test
    void testDetectClientCallablesTypes_withNestedParameterizedTypes() {
        Set<ClassInfo> result = processor.detectClientCallablesTypes(index);

        assertNotNull(result);
        assertTrue(result.stream().anyMatch(containsClass(NestedDto.class)),
                "Should detect NestedDto from parameterized type argument");
    }

    @Test
    void testDetectClientCallablesTypes_withGenericDefinition() {
        Set<ClassInfo> result = processor.detectClientCallablesTypes(index);

        assertNotNull(result);
        Set<String> classNames = result.stream().map(ci -> ci.name().toString())
                .collect(Collectors.toSet());
        assertTrue(classNames.contains(RecordDto.class.getName()),
                "Should detect RecordDto from parameterized return type");
        assertTrue(classNames.contains(RecordId.class.getName()),
                "Should detect RecordId from parameterized parameter type");
    }

    @Test
    void testDetectClientCallablesTypes_excludesPrimitives() {
        Set<ClassInfo> result = processor.detectClientCallablesTypes(index);

        assertNotNull(result);
        // Verify no primitive types are in the result
        assertFalse(result.stream().anyMatch(containsClass(int.class)),
                "Should not include primitive types");
    }

    @Test
    void testDetectClientCallablesTypes_returnsEmptySetWhenNoClientCallables()
            throws IOException {
        // Create a new index without any ClientCallable annotations
        Indexer indexer = new Indexer();
        indexer.indexClass(String.class);
        Index emptyIndex = indexer.complete();

        Set<ClassInfo> result = processor
                .detectClientCallablesTypes(emptyIndex);

        assertNotNull(result);
        assertTrue(result.isEmpty(),
                "Should return empty set when no ClientCallable methods exist");
    }

    @Test
    void testDetectClientCallablesTypes_onlyIncludesComponentSubclasses()
            throws IOException {
        // Create a new index with both Component and non-Component classes
        Indexer indexer = new Indexer();
        indexer.indexClass(TestComponent.class);
        indexer.indexClass(NonComponentClass.class);
        indexer.indexClass(SimpleDto.class);
        indexer.indexClass(OtherDto.class);
        indexer.indexClass(com.vaadin.flow.component.Component.class);
        Index testIndex = indexer.complete();

        Set<ClassInfo> result = processor.detectClientCallablesTypes(testIndex);

        // Should only include types from TestComponent methods (which extends
        // Component)
        // Should NOT include types from NonComponentClass methods
        assertTrue(result.stream().anyMatch(containsClass(SimpleDto.class)),
                "Should detect SimpleDto from Component subclass");
        assertFalse(result.stream().anyMatch(containsClass(OtherDto.class)),
                "Should NOT detect OtherDto from non-Component class");
    }

    @Test
    void testDetectClientCallablesTypes_handlesMultipleLevelInheritance()
            throws IOException {
        // Create index with multi-level Component hierarchy
        Indexer indexer = new Indexer();
        indexer.indexClass(ExtendedComponent.class);
        indexer.indexClass(TestComponent.class);
        indexer.indexClass(NestedDto.class);
        indexer.indexClass(com.vaadin.flow.component.Component.class);
        indexer.indexClass(List.class);
        Index testIndex = indexer.complete();

        Set<ClassInfo> result = processor.detectClientCallablesTypes(testIndex);

        // Should detect types from ExtendedComponent which extends
        // TestComponent which extends Component
        assertTrue(result.stream().anyMatch(containsClass(NestedDto.class)),
                "Should detect NestedDto from multi-level Component subclass");
    }

    @Test
    void testDetectEventDataTypes_collectsDecodedEventDataTypes()
            throws IOException {
        Indexer indexer = new Indexer();
        indexer.indexClass(ComponentEvent.class);
        indexer.indexClass(Component.class);
        indexer.indexClass(Element.class);
        indexer.indexClass(TestComponent.class);
        indexer.indexClass(TestBeanDataEvent.class);
        indexer.indexClass(SimpleDto.class);
        indexer.indexClass(ComplexDto.class);
        indexer.indexClass(NestedDto.class);
        indexer.indexClass(OtherDto.class);
        indexer.indexClass(List.class);
        Index testIndex = indexer.complete();

        Set<ClassInfo> result = processor.detectEventDataTypes(testIndex);

        assertTrue(result.stream().anyMatch(containsClass(SimpleDto.class)),
                "Should detect SimpleDto from @EventData parameter");
        assertTrue(result.stream().anyMatch(containsClass(ComplexDto.class)),
                "Should detect ComplexDto from @EventData array parameter");
        assertFalse(result.stream().anyMatch(containsClass(NestedDto.class)),
                "Should NOT detect NestedDto from a type argument, the event data is decoded into the raw type");
        assertFalse(result.stream().anyMatch(containsClass(OtherDto.class)),
                "Should NOT detect OtherDto from a non-@EventData parameter");
        assertFalse(
                result.stream().anyMatch(containsClass(TestComponent.class)),
                "Should NOT detect component types resolved from the state tree");
        assertFalse(result.stream().anyMatch(containsClass(Element.class)),
                "Should NOT detect Element resolved from the state tree");
    }

    @Test
    void isI18nClassName_matchesTranslationClassesAndTheirInnerClasses() {
        assertTrue(VaadinQuarkusNativeProcessor
                .isI18nClassName("com.vaadin.flow.component.login.LoginI18n"));
        // The Upload component spells it with a capital N
        assertTrue(VaadinQuarkusNativeProcessor.isI18nClassName(
                "com.vaadin.flow.component.upload.UploadI18N"));
        assertTrue(VaadinQuarkusNativeProcessor.isI18nClassName(
                "com.vaadin.flow.component.login.LoginI18n$Form"));
        assertTrue(VaadinQuarkusNativeProcessor.isI18nClassName(
                "com.vaadin.flow.component.upload.UploadI18N$Uploading$Status"));

        // Neither ends at I18n nor continues into an inner class of one
        assertFalse(VaadinQuarkusNativeProcessor
                .isI18nClassName("com.vaadin.flow.component.UI"));
        assertFalse(VaadinQuarkusNativeProcessor.isI18nClassName(
                "com.vaadin.flow.component.login.LoginI18nProvider"));
        assertFalse(VaadinQuarkusNativeProcessor
                .isI18nClassName("com.vaadin.flow.i18n.I18NProvider"));
    }

    @Test
    void testVaadinNativeSupport_registersRuntimeLoadedClientHelpers() {
        List<NativeImageResourcePatternsBuildItem> resources = new ArrayList<>();

        processor.vaadinNativeSupport(new CombinedIndexBuildItem(index, index),
                item -> {
                }, resources::add, item -> {
                }, item -> {
                });

        assertTrue(isIncluded(resources, "META-INF/frontend/FlowShortcut.js"),
                "Shortcut client helper should be included in the image");
        assertTrue(isIncluded(resources, "META-INF/frontend/FlowWebPush.js"),
                "Web push client helper should be included in the image");
    }

    @Test
    void testVaadinNativeSupport_registersRuntimeLoadedProperties() {
        List<NativeImageResourcePatternsBuildItem> resources = new ArrayList<>();

        processor.vaadinNativeSupport(new CombinedIndexBuildItem(index, index),
                item -> {
                }, resources::add, item -> {
                }, item -> {
                });

        assertTrue(
                isIncluded(resources, "org/atmosphere/util/version.properties"),
                "Atmosphere version properties should be included in the image");
        assertTrue(
                isIncluded(resources,
                        "META-INF/maven/com.vaadin/vaadin-core/pom.properties"),
                "Vaadin version properties should be included in the image");
        assertTrue(isIncluded(resources, "vaadin-featureflags.properties"),
                "Feature flags properties should be included in the image");
    }

    @ParameterizedTest
    @ValueSource(classes = { UrlParameterTarget.class,
            ErrorParameterTarget.class, LayoutTarget.class,
            ConverterTarget.class })
    void testVaadinNativeSupport_registersInterfaceImplementationsForReflection(
            Class<?> implementation) throws IOException {
        Indexer indexer = new Indexer();
        indexer.indexClass(implementation);
        Index implementationIndex = indexer.complete();
        List<ReflectiveClassBuildItem> reflective = new ArrayList<>();

        processor.vaadinNativeSupport(new CombinedIndexBuildItem(
                implementationIndex, implementationIndex), item -> {
                }, item -> {
                }, reflective::add, item -> {
                });

        // The class implements the interface without extending Component,
        // so only a lookup of the interface implementations finds it
        assertTrue(
                reflective.stream()
                        .flatMap(item -> item.getClassNames().stream())
                        .anyMatch(implementation.getName()::equals),
                implementation.getSimpleName()
                        + " should be registered for reflection");
    }

    @Test
    void testVaadinNativeSupport_registersErrorParameterTypesForReflection()
            throws IOException {
        Indexer indexer = new Indexer();
        indexer.indexClass(HasErrorParameter.class);
        indexer.indexClass(Component.class);
        indexer.indexClass(CustomErrorView.class);
        indexer.indexClass(CustomException.class);
        indexer.indexClass(GenericErrorView.class);
        indexer.indexClass(InheritedErrorView.class);
        indexer.indexClass(InheritedException.class);
        // IllegalStateException itself is not indexed, like JDK classes in an
        // application
        indexer.indexClass(JdkExceptionErrorView.class);
        Index errorIndex = indexer.complete();
        List<ReflectiveClassBuildItem> reflective = new ArrayList<>();

        processor.vaadinNativeSupport(
                new CombinedIndexBuildItem(errorIndex, errorIndex), item -> {
                }, item -> {
                }, reflective::add, item -> {
                });

        // Flow creates the exception by reflection when a view reroutes to
        // an error by exception type. The exception types are registered in
        // their own build item, so it can be checked for exact content: the
        // direct type argument, the one given to a generic superclass and
        // the one that is not in the index, but not the bound of the type
        // variable of the generic superclass.
        ReflectiveClassBuildItem exceptionTypes = reflective.stream()
                .filter(item -> item.getClassNames()
                        .contains(CustomException.class.getName()))
                .findFirst().orElseThrow();
        assertTrue(exceptionTypes.isConstructors(),
                "Should register the constructors of the exception types");
        assertEquals(
                Set.of(CustomException.class.getName(),
                        InheritedException.class.getName(),
                        IllegalStateException.class.getName()),
                Set.copyOf(exceptionTypes.getClassNames()));
    }

    @Test
    void testRegisterJsDefinitionProxies_registersTheAnnotatedInterfaces()
            throws IOException {
        Indexer indexer = new Indexer();
        indexer.indexClass(GreeterJs.class);
        indexer.indexClass(Greeter.class);
        Index jsIndex = indexer.complete();
        List<NativeImageProxyDefinitionBuildItem> proxies = new ArrayList<>();
        List<ReflectiveClassBuildItem> reflective = new ArrayList<>();

        processor.registerJsDefinitionProxies(
                new CombinedIndexBuildItem(jsIndex, jsIndex), proxies::add,
                reflective::add);

        // Only an interface can be implemented by a JDK proxy, so the
        // annotated class has to be left out of both registrations.
        assertEquals(List.of(List.of(GreeterJs.class.getName())),
                proxies.stream()
                        .map(NativeImageProxyDefinitionBuildItem::getClasses)
                        .toList(),
                "Should register the annotated interface as a proxy definition");
        assertEquals(Set.of(GreeterJs.class.getName()),
                reflective.stream()
                        .flatMap(item -> item.getClassNames().stream())
                        .collect(Collectors.toSet()),
                "Should register the annotated interface for reflection");
        assertTrue(
                reflective.stream()
                        .allMatch(ReflectiveClassBuildItem::isMethods),
                "The annotations are read off the interface methods");
    }

    @Test
    void testRegisterJsDefinitionProxies_noDefinitions_registersNothing()
            throws IOException {
        Indexer indexer = new Indexer();
        indexer.indexClass(Greeter.class);
        Index emptyIndex = indexer.complete();
        List<NativeImageProxyDefinitionBuildItem> proxies = new ArrayList<>();
        List<ReflectiveClassBuildItem> reflective = new ArrayList<>();

        processor.registerJsDefinitionProxies(
                new CombinedIndexBuildItem(emptyIndex, emptyIndex),
                proxies::add, reflective::add);

        assertTrue(proxies.isEmpty());
        assertTrue(reflective.isEmpty(),
                "An application with no JavaScript definitions needs no "
                        + "reflection registration either");
    }

    private static boolean isIncluded(
            List<NativeImageResourcePatternsBuildItem> resources,
            String resource) {
        return resources.stream()
                .flatMap(item -> item.getIncludePatterns().stream())
                .anyMatch(pattern -> Pattern.compile(pattern).matcher(resource)
                        .matches());
    }

    // Only an interface can be implemented by the JDK proxy the definitions
    // are used through, so an annotated class is not one of them
    @JsDefinition
    public interface GreeterJs {
        void greet(String name);
    }

    @JsDefinition
    public static class Greeter {
    }

    public static class CustomException extends RuntimeException {
    }

    public static class CustomErrorView extends Component
            implements HasErrorParameter<CustomException> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<CustomException> parameter) {
            return 500;
        }
    }

    public static class InheritedException extends RuntimeException {
    }

    public abstract static class GenericErrorView<T extends Exception>
            extends Component implements HasErrorParameter<T> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<T> parameter) {
            return 500;
        }
    }

    public static class InheritedErrorView
            extends GenericErrorView<InheritedException> {
    }

    public static class JdkExceptionErrorView extends Component
            implements HasErrorParameter<IllegalStateException> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<IllegalStateException> parameter) {
            return 500;
        }
    }

    // The fixtures below implement a Vaadin interface without extending
    // Component

    public static class UrlParameterTarget implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            // Intentionally empty: the test only checks that the class is
            // registered for reflection, this method is never called
        }
    }

    public static class ErrorParameterTarget
            implements HasErrorParameter<NotFoundException> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            return 404;
        }
    }

    public static class LayoutTarget implements RouterLayout {
        @Override
        public Element getElement() {
            return null;
        }
    }

    public static class ConverterTarget implements Converter<String, Integer> {
        @Override
        public Result<Integer> convertToModel(String value,
                ValueContext context) {
            return Result.ok(Integer.valueOf(value));
        }

        @Override
        public String convertToPresentation(Integer value,
                ValueContext context) {
            return String.valueOf(value);
        }
    }

    private static Predicate<ClassInfo> containsClass(Class<?> expectedClass) {
        return ci -> ci.name().toString().equals(expectedClass.getName());
    }

    // Test component with ClientCallable methods
    public static class TestComponent extends Component {

        @ClientCallable
        public SimpleDto getSimpleData() {
            return null;
        }

        @ClientCallable
        public void processData(ComplexDto data) {
        }

        @ClientCallable
        public void processDataWithPrimitive(int value) {
        }

        @ClientCallable
        public List<NestedDto> getNestedList() {
            return null;
        }

        @ClientCallable
        public void handleVoid() {
        }

        @ClientCallable
        public <X extends RecordId, Y extends RecordDto> Collection<X> handleGenericDefinition(
                List<Y> id) {
            return null;
        }
    }

    // Test DTOs
    public static class SimpleDto {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ComplexDto {
        private String id;
        private NestedDto nested;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public NestedDto getNested() {
            return nested;
        }

        public void setNested(NestedDto nested) {
            this.nested = nested;
        }
    }

    public static class NestedDto {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public record RecordId(int id) {
    }

    public record RecordDto(String value) {
    }

    // Non-Component class with ClientCallable (should be filtered out)
    public static class NonComponentClass {
        @ClientCallable
        public OtherDto getNonComponentData() {
            return null;
        }
    }

    public static class OtherDto {
        private String data;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    public static class TestBeanDataEvent
            extends ComponentEvent<TestComponent> {
        public TestBeanDataEvent(TestComponent source, boolean fromClient,
                @EventData("event.detail") SimpleDto detail,
                @EventData("event.items") List<NestedDto> items,
                @EventData("event.array") ComplexDto[] array,
                @EventData("event.count") int count,
                @EventData("element") Element element,
                @EventData("element.parent") TestComponent parent) {
            super(source, fromClient);
        }

        public TestBeanDataEvent(TestComponent source, OtherDto other) {
            super(source, false);
        }
    }

    // Extended component for multi-level inheritance testing
    public static class ExtendedComponent extends TestComponent {
        @ClientCallable
        public List<NestedDto> getExtendedData() {
            return null;
        }
    }

    @Test
    void findVaadinServiceInterfaces_onlyVaadinInterfacesOfAllArchives(
            @TempDir Path tempDir) throws IOException {
        Path application = tempDir.resolve("application");
        writeServiceFile(application, "com.vaadin.flow.server.SomeService");
        writeServiceFile(application, "org.example.OtherService");
        writeServiceFile(application, "nested/com.vaadin.NestedService");
        Path addon = tempDir.resolve("addon.jar");
        try (JarOutputStream jar = new JarOutputStream(
                Files.newOutputStream(addon))) {
            jar.putNextEntry(new JarEntry(
                    "META-INF/services/com.vaadin.addon.AddonService"));
            jar.write("com.example.Provider\n".getBytes());
        }
        // A dependency can also be a single file that is not an archive
        Path plainFile = Files.writeString(tempDir.resolve("notes.txt"), "");

        assertEquals(
                Set.of("com.vaadin.addon.AddonService",
                        "com.vaadin.flow.server.SomeService"),
                VaadinQuarkusNativeProcessor.findVaadinServiceInterfaces(
                        Stream.of(PathTree.ofDirectoryOrArchive(application),
                                PathTree.ofDirectoryOrArchive(addon),
                                PathTree.ofDirectoryOrFile(plainFile))));
    }

    private static void writeServiceFile(Path root, String name)
            throws IOException {
        Path file = root.resolve("META-INF/services").resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "com.example.Provider\n");
    }
}
