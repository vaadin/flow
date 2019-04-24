package com.vaadin.flow.component.polymertemplate;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jsoup.nodes.Element;

import com.vaadin.flow.internal.AnnotationReader;

public class IdCollector implements Serializable {
    private final Map<String, String> tagById = new HashMap<>();
    private final Map<Field, String> idByField = new HashMap<>();
    private Optional<Element> templateRoot;
    private Class<?> templateClass;
    private String templateFile;

    public IdCollector(Class<?> templateClass, String templateFile,
            Element templateRoot) {
        this.templateClass = templateClass;
        this.templateFile = templateFile;
        this.templateRoot = Optional.ofNullable(templateRoot);
    }

    public void collectInjectedIds(Set<String> notInjectableElementIds) {
        collectInjectedIds(templateClass, notInjectableElementIds);
    }

    private void collectInjectedIds(Class<?> cls,
            Set<String> notInjectableElementIds) {
        if (!AbstractTemplate.class.equals(cls.getSuperclass())) {
            // Parent fields
            collectInjectedIds(cls.getSuperclass(), notInjectableElementIds);
        }

        Stream.of(cls.getDeclaredFields()).filter(field -> !field.isSynthetic())
                .forEach(field -> collectedInjectedId(field,
                        notInjectableElementIds));
    }

    private void collectedInjectedId(Field field,
            Set<String> notInjectableElementIds) {
        Optional<Id> idAnnotation = AnnotationReader.getAnnotationFor(field,
                Id.class);
        if (!idAnnotation.isPresent()) {
            return;
        }
        String id = idAnnotation.get().value();
        boolean emptyValue = id.isEmpty();
        if (emptyValue) {
            id = field.getName();
        }
        if (notInjectableElementIds.contains(id)) {
            throw new IllegalStateException(String.format(
                    "Class '%s' contains field '%s' annotated with @Id%s. "
                            + "Corresponding element was found in a sub template, "
                            + "for which injection is not supported.",
                    templateClass.getName(), field.getName(),
                    emptyValue
                            ? " without value (so the name of the field should match the id of an element in the template)"
                            : "(\"" + id + "\")"));
        }

        if (!addTagName(id, field)) {
            throw new IllegalStateException(String.format(
                    "There is no element with "
                            + "id='%s' in the template file '%s'. Cannot map it using @%s",
                    id, templateFile, Id.class.getSimpleName()));
        }
    }

    /**
     * Stores mapping between the given id and field.
     *
     * @param id
     *            the id value
     * @param field
     *            the Java field
     * @return <code>false</code> if the mapping did not pass validation,
     *         <code>true</code> otherwise
     */
    private boolean addTagName(String id, Field field) {
        idByField.put(field, id);
        if (templateRoot.isPresent()) {
            // The template is available for parsing so check up front if the id
            // exists
            Optional<String> tagName = Optional
                    .ofNullable(templateRoot.get().getElementById(id))
                    .map(org.jsoup.nodes.Element::tagName);
            if (tagName.isPresent()) {
                tagById.put(id, tagName.get());
            }

            return tagName.isPresent();
        }

        return true;
    }

    public Map<Field, String> getIdByField() {
        return idByField;
    }

    public Map<String, String> getTagById() {
        return tagById;
    }

}
