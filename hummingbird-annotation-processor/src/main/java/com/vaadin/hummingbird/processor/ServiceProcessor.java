/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.processor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.vaadin.hummingbird.processor.annotations.ServiceProvider;

/**
 * Processes classes annotated with {@link ServiceProvider} and registers them
 * in META-INF/services.
 * 
 * @author Vaadin Ltd
 *
 */
public class ServiceProcessor extends AbstractProcessor {

    private static final String VALUE = "value";
    private static final String META_INF_SERVICES = "META-INF/services/";

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {
        assert annotations.size() <= 1;

        if (annotations.isEmpty()) {
            return false;
        }

        assert getSupportedAnnotationTypes().contains(
                annotations.iterator().next().getQualifiedName().toString());

        Set<? extends Element> elements = roundEnv
                .getElementsAnnotatedWith(ServiceProvider.class);

        Map<Name, List<TypeElement>> impls = new HashMap<>();

        elements.forEach(element -> inspectElement(element, impls));
        createMetaInfServices(impls);
        return true;

    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(ServiceProvider.class.getName());
    }

    private void inspectElement(Element element,
            Map<Name, List<TypeElement>> impls) {
        AnnotationMirror annotation = getAnnotation(
                ServiceProvider.class.getName(), element);
        List<DeclaredType> services = getServices(annotation);
        if (checkClassProviderDeclaration(element)) {
            TypeElement typeElement = (TypeElement) element;
            services.stream().forEach(declaredType -> addService(typeElement,
                    declaredType, impls));
        }
    }

    private void addService(TypeElement implElement, DeclaredType serviceType,
            Map<Name, List<TypeElement>> implementations) {
        checkServiceDeclaration(implElement.asType(), serviceType, implElement);

        Element elementType = serviceType.asElement();
        assert elementType instanceof TypeElement;
        Name serviceName = getElements()
                .getBinaryName((TypeElement) elementType);
        List<TypeElement> list = implementations.get(serviceName);
        if (list == null) {
            list = new ArrayList<TypeElement>();
            implementations.put(serviceName, list);
        }
        list.add(implElement);
    }

    private void checkServiceDeclaration(TypeMirror serviceType,
            DeclaredType declaredType, Element serviceElement) {
        if (!getTypes().isSubtype(serviceType, declaredType)) {
            String msg = "Element " + serviceElement.toString() + " doesn't "
                    + "implement/extend declared service " + declaredType
                    + " in its " + "ServiceProvider annotation";
            getProcessingEnvironment().getMessager().printMessage(Kind.ERROR,
                    msg, serviceElement);
            throw new ServiceProcessorException(msg);
        }
    }

    private boolean checkClassProviderDeclaration(Element element) {
        if (!(element instanceof TypeElement)) {
            return false;
        }
        List<ExecutableElement> ctors = ElementFilter
                .constructorsIn(element.getEnclosedElements());
        Optional<?> defaultCtor = ctors.stream()
                .filter(ctor -> ctor.getParameters().isEmpty()
                        && ctor.getModifiers().contains(Modifier.PUBLIC))
                .findFirst();
        if (!defaultCtor.isPresent()) {
            String msg = "Class " + element.toString()
                    + " hasn't default public constructor";
            getProcessingEnvironment().getMessager().printMessage(Kind.ERROR,
                    msg, element);
            throw new ServiceProcessorException(msg);
        }
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(Modifier.ABSTRACT)) {
            String msg = "Class " + element.toString()
                    + " is abstract and cannot be instantiated";
            getProcessingEnvironment().getMessager().printMessage(Kind.ERROR,
                    msg, element);
            throw new ServiceProcessorException(msg);
        }
        if (!modifiers.contains(Modifier.PUBLIC)) {
            String msg = "Class " + element.toString()
                    + " is not public and cannot be instantiated";
            getProcessingEnvironment().getMessager().printMessage(Kind.ERROR,
                    msg, element);
            throw new ServiceProcessorException(msg);
        }
        if (element.getEnclosingElement().getKind() != ElementKind.PACKAGE
                && !modifiers.contains(Modifier.STATIC)) {
            String msg = "Class " + element.toString()
                    + " is nested and isn't static";
            getProcessingEnvironment().getMessager().printMessage(Kind.ERROR,
                    msg, element);
            return false;
        }
        return true;
    }

    private List<DeclaredType> getServices(AnnotationMirror annotation) {
        Object value = getValue(annotation, VALUE);
        List<?> classes;
        if (value instanceof List<?>) {
            classes = (List<?>) value;
        } else {
            return Collections.emptyList();
        }
        return classes.stream().map(AnnotationValue.class::cast)
                .map(AnnotationValue::getValue)
                .filter(DeclaredType.class::isInstance)
                .map(DeclaredType.class::cast).collect(Collectors.toList());
    }

    private Object getValue(AnnotationMirror annotation, String key) {
        return annotation.getElementValues().entrySet().stream()
                .filter(entry -> entry.getKey().getSimpleName()
                        .contentEquals(key))
                .findFirst().map(Entry::getValue).map(AnnotationValue::getValue)
                .orElse(null);
    }

    private ProcessingEnvironment getProcessingEnvironment() {
        return processingEnv;
    }

    private Elements getElements() {
        return getProcessingEnvironment().getElementUtils();
    }

    private Types getTypes() {
        return getProcessingEnvironment().getTypeUtils();
    }

    private AnnotationMirror getAnnotation(String fqn, Element element) {
        List<? extends AnnotationMirror> annotationMirrors = getElements()
                .getAllAnnotationMirrors(element);
        return annotationMirrors.stream()
                .filter(annotation -> hasFqn(annotation, fqn)).findFirst()
                .orElse(null);
    }

    private boolean hasFqn(AnnotationMirror mirror, String fqn) {
        Element annotation = mirror.getAnnotationType().asElement();
        if (annotation instanceof TypeElement) {
            if (((TypeElement) annotation).getQualifiedName()
                    .contentEquals(fqn)) {
                return true;
            }
        }
        return false;
    }

    private void createMetaInfServices(Map<Name, List<TypeElement>> providers) {
        for (Entry<Name, List<TypeElement>> entry : providers.entrySet()) {
            Name service = entry.getKey();
            List<TypeElement> elements = entry.getValue();
            createMetaInfService(service, elements);
        }
    }

    private void createMetaInfService(Name service,
            List<TypeElement> providers) {
        String serviceFileName = META_INF_SERVICES + service.toString();
        BufferedWriter writer = null;
        try {
            FileObject serviceFile = getProcessingEnvironment().getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "",
                            serviceFileName,
                            providers.toArray(new Element[providers.size()]));
            getProcessingEnvironment().getMessager().printMessage(Kind.NOTE,
                    serviceFile.toUri().toString() + " is generated");
            writer = new BufferedWriter(new OutputStreamWriter(
                    serviceFile.openOutputStream(), StandardCharsets.UTF_8));
            for (TypeElement element : providers) {
                writer.write(getElements().getBinaryName(element).toString());
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            String msg = "Unable to generate " + serviceFileName; // NOI18N
            if (providers != null && !providers.isEmpty()) {
                getProcessingEnvironment().getMessager()
                        .printMessage(Kind.ERROR, msg, providers.get(0));
            } else {
                getProcessingEnvironment().getMessager()
                        .printMessage(Kind.ERROR, msg);
            }
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                getProcessingEnvironment().getMessager()
                        .printMessage(Kind.WARNING, e.toString());
            }
        }
    }

}
