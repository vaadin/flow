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
package com.vaadin.flow.spring.springnative;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.type.filter.AssignableTypeFilter;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientCallableAotProcessorTest {

    @Test
    void processAheadOfTime_multipleClientCallables_typesDetected() {
        RuntimeHints hints = processAotForComponents(TestComponent.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(SimpleDto.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(ComplexDto.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(NestedDto.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(RecordId.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(RecordDto.class))
                .accepts(hints);

        assertThat(RuntimeHintsPredicates.reflection().onType(List.class))
                .as("List should not be registered (java.* package)")
                .rejects(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(Collection.class))
                .as("Collection should not be registered (java.* package)")
                .rejects(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(int.class))
                .as("Primitive should not be registered").rejects(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(String.class))
                .as("String should not be registered (java.* package)")
                .rejects(hints);
    }

    @Test
    void processAheadOfTime_componentSubClass_typesDetected() {
        RuntimeHints hints = processAotForComponents(ExtendedComponent.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(SimpleDto.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(ComplexDto.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(NestedDto.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(RecordId.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(RecordDto.class))
                .accepts(hints);

        assertThat(RuntimeHintsPredicates.reflection().onType(List.class))
                .as("List should not be registered (java.* package)")
                .rejects(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(Collection.class))
                .as("Collection should not be registered (java.* package)")
                .rejects(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(int.class))
                .as("Primitive should not be registered").rejects(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(String.class))
                .as("String should not be registered (java.* package)")
                .rejects(hints);
    }

    @Test
    void processAheadOfTime_multipleClasses_typesDetected() {
        RuntimeHints hints = processAotForComponents(
                SimpleReturnComponent.class, ComplexParameterComponent.class,
                PrimitiveParameterComponent.class,
                NestedListReturnComponent.class, VoidMethodComponent.class,
                GenericMethodComponent.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(SimpleDto.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(ComplexDto.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(NestedDto.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(RecordId.class))
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(RecordDto.class))
                .accepts(hints);

        assertThat(RuntimeHintsPredicates.reflection().onType(List.class))
                .as("List should not be registered (java.* package)")
                .rejects(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(Collection.class))
                .as("Collection should not be registered (java.* package)")
                .rejects(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(int.class))
                .as("Primitive should not be registered").rejects(hints);
    }

    @Test
    @Disabled("@ClientCallable on interfaces are currently not supported")
    void processAheadOfTime_clientCallableOnInterface_typesDetected() {
        RuntimeHints hints = processAotForComponents(
                InterfaceBasedComponent.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(ComplexDto.class))
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_returnType_typesDetected() {
        RuntimeHints hints = processAotForComponents(
                SimpleReturnComponent.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(SimpleDto.class))
                .as("SimpleDto should be registered when used as return type")
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(ComplexDto.class))
                .as("SimpleDto should be registered when used as return type")
                .rejects(hints);
    }

    @Test
    void processAheadOfTime_parameterTypes_typesDetected() {
        RuntimeHints hints = processAotForComponents(
                ComplexParameterComponent.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(ComplexDto.class))
                .as("ComplexDto should be registered when used as parameter")
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(OtherDto.class))
                .as("OtherDto should be registered when used as parameter")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_arrays_typesDetected() {
        RuntimeHints hints = processAotForComponents(
                ArrayMethodComponent.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(SimpleDto.class))
                .as("SimpleDto should be registered when used as array parameter")
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(OtherDto.class))
                .as("OtherDto should be registered when used as array parameter")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_genericArrays_typesDetected() {
        RuntimeHints hints = processAotForComponents(
                GenericArrayMethodComponent.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(SimpleDto.class))
                .as("SimpleDto should be registered when used as generic array parameter")
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(OtherDto.class))
                .as("OtherDto should be registered when used as generic array parameter")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_nonComponentClass_typesFromComponentsRegistered() {
        RuntimeHints hints = processAotForComponents(NonComponentClass.class);

        assertThat(hints.reflection().typeHints())
                .as("Should not register types from non-component classes")
                .isEmpty();
    }

    @Test
    void processAheadOfTime_primitiveTypes_noTypesRegistered() {
        RuntimeHints hints = processAotForComponents(
                PrimitiveParameterComponent.class);

        assertThat(hints.reflection().typeHints())
                .as("Should not register types from primitive types").isEmpty();
    }

    @Test
    void processAheadOfTime_genericInterface_genericTypeDetected() {
        RuntimeHints hints = processAotForComponents(
                NestedListReturnComponent.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(NestedDto.class))
                .as("NestedDto should be registered when used in List return type")
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(List.class))
                .as("List should not be registered (java.* package)")
                .rejects(hints);
    }

    @Test
    void processAheadOfTime_voidReturnType_noTypesRegistered() {
        RuntimeHints hints = processAotForComponents(VoidMethodComponent.class);

        assertThat(hints.reflection().typeHints())
                .as("Should not register hints from void return type")
                .isEmpty();
    }

    @Test
    void processAheadOfTime_genericBoundTypes_typesDetected() {
        RuntimeHints hints = processAotForComponents(
                GenericMethodComponent.class);

        assertThat(RuntimeHintsPredicates.reflection().onType(RecordId.class))
                .as("RecordId should be registered from generic type variable bound")
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(RecordDto.class))
                .as("RecordDto should be registered from generic type variable bound")
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection().onType(Collection.class))
                .as("Collection should not be registered (java.* package)")
                .rejects(hints);
    }

    @Test
    void processAheadOfTime_noComponents_returnsNull() {
        ConfigurableListableBeanFactory beanFactory = mock(
                ConfigurableListableBeanFactory.class);
        ConfigurableEnvironment environment = mock(
                ConfigurableEnvironment.class);
        MutablePropertySources propertySources = new MutablePropertySources();

        try (var mockedStatic = Mockito
                .mockStatic(AutoConfigurationPackages.class)) {

            when(beanFactory.getBeanClassLoader())
                    .thenReturn(getClass().getClassLoader());
            when(beanFactory.getBean(ConfigurableEnvironment.class))
                    .thenReturn(environment);
            when(environment.getPropertySources()).thenReturn(propertySources);

            ClientCallableAotProcessor processor = new ClientCallableAotProcessor() {
                @Override
                void configureScanner(
                        ClassPathScanningCandidateComponentProvider scanner) {
                    // Simulate no component class found
                    scanner.addExcludeFilter(
                            (metadataReader, metadataReaderFactory) -> false);
                }
            };
            BeanFactoryInitializationAotContribution contribution = processor
                    .processAheadOfTime(beanFactory);

            assertThat(contribution).as(
                    "Should not return contribution when scanning package with no components")
                    .isNull();
        }
    }

    private RuntimeHints processAotForComponents(Class<?>... componentClasses) {
        ConfigurableListableBeanFactory beanFactory = mock(
                ConfigurableListableBeanFactory.class);
        ConfigurableEnvironment environment = mock(
                ConfigurableEnvironment.class);
        MutablePropertySources propertySources = new MutablePropertySources();

        // Mock AutoConfigurationPackages to return the test package
        try (var mockedStatic = Mockito
                .mockStatic(AutoConfigurationPackages.class)) {
            mockedStatic.when(() -> AutoConfigurationPackages.get(beanFactory))
                    .thenReturn(List.of(getClass().getPackageName()));

            // Mock class loader and environment
            when(beanFactory.getBeanClassLoader())
                    .thenReturn(getClass().getClassLoader());
            when(beanFactory.getBean(ConfigurableEnvironment.class))
                    .thenReturn(environment);
            when(environment.getPropertySources()).thenReturn(propertySources);

            ClientCallableAotProcessor processor = new ClientCallableAotProcessor() {
                @Override
                void configureScanner(
                        ClassPathScanningCandidateComponentProvider scanner) {
                    // Make sure the scanner only finds the specified component
                    // classes
                    // super.configureScanner(scanner);
                    Arrays.stream(componentClasses)
                            .map(AssignableTypeFilter::new)
                            .forEach(scanner::addIncludeFilter);
                }
            };
            BeanFactoryInitializationAotContribution contribution = processor
                    .processAheadOfTime(beanFactory);

            RuntimeHints hints = new RuntimeHints();
            if (contribution != null) {
                GenerationContext generationContext = mock(
                        GenerationContext.class);
                when(generationContext.getRuntimeHints()).thenReturn(hints);
                BeanFactoryInitializationCode code = mock(
                        BeanFactoryInitializationCode.class);
                contribution.applyTo(generationContext, code);
            }

            return hints;
        }
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
        public List<NestedDto> getNestedList(String value) {
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

    // Individual test components - one per ClientCallable method signature
    public static class SimpleReturnComponent extends Component {
        @ClientCallable
        public SimpleDto getSimpleData() {
            return null;
        }
    }

    public static class ComplexParameterComponent extends Component {
        @ClientCallable
        public void processData(ComplexDto data, OtherDto otherData) {
        }
    }

    public static class PrimitiveParameterComponent extends Component {
        @ClientCallable
        public String processDataWithPrimitive(int value) {
            return null;
        }
    }

    public static class NestedListReturnComponent extends Component {
        @ClientCallable
        public List<NestedDto> getNestedList() {
            return null;
        }
    }

    public static class VoidMethodComponent extends Component {
        @ClientCallable
        public void handleVoid() {
        }
    }

    public static class ArrayMethodComponent extends Component {
        @ClientCallable
        public OtherDto[] handleVoid(SimpleDto[] data) {
            return null;
        }
    }

    public static class GenericArrayMethodComponent extends Component {
        @ClientCallable
        public <X extends OtherDto, Y extends SimpleDto> X[] handleVoid(
                Y[] data) {
            return null;
        }
    }

    public static class GenericMethodComponent extends Component {
        @ClientCallable
        public <X extends RecordId, Y extends RecordDto> Collection<X> handleGenericDefinition(
                List<Y> id) {
            return null;
        }
    }

    public interface InterfaceWithClientCallable {

        @ClientCallable
        default void processData(ComplexDto data) {

        }
    }

    public static class InterfaceBasedComponent extends Component
            implements InterfaceWithClientCallable {
    }

    // Non-Component class with ClientCallable (should be filtered out)
    public static class NonComponentClass {
        @ClientCallable
        public OtherDto getNonComponentData() {
            return null;
        }
    }

    // Extended component for multi-level inheritance testing
    public static class ExtendedComponent extends TestComponent {
        @ClientCallable
        public List<NestedDto> getExtendedData() {
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
        private SimpleDto nested;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public SimpleDto getNested() {
            return nested;
        }

        public void setNested(SimpleDto nested) {
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

    public static class OtherDto {
        private String data;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
