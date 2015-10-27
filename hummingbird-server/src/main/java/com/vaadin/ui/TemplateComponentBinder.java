package com.vaadin.ui;

import com.vaadin.annotations.Id;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.shared.util.SharedUtil;

public class TemplateComponentBinder {

    public void bindComponents(Template template) {
        Class<? extends Template> templateClass = template.getClass();
        for (java.lang.reflect.Field memberField : FieldGroup
                .getFieldsInDeclareOrder(templateClass)) {

            if (!Component.class.isAssignableFrom(memberField.getType())) {
                // Not a Component field - move on
                continue;
            }

            Class<? extends Component> componentType = (Class<? extends Component>) memberField
                    .getType();

            // TODO Add support for @Id or similar
            String componentId = memberField.getName();
            componentId = SharedUtil.camelCaseToDashSeparated(componentId);
            if (memberField.getAnnotation(Id.class) != null) {
                componentId = memberField.getAnnotation(Id.class).value();
            }
            Element element = template.getElementById(componentId);
            if (element == null) {
                throw new IllegalStateException("No element with id "
                        + componentId + " found to bind to field "
                        + memberField.getName());
            }

            if (element.getComponents().size() == 1) {
                // Element already attached to a component. If the type is
                // correct, use that
                Class<? extends Component> componentClass = element
                        .getComponents().get(0).getClass();
                if (componentType.isAssignableFrom(componentClass)) {
                    Component c = element.getComponents().get(0);
                    assignComponent(template, memberField, c);
                    continue;
                } else {
                    throw new IllegalStateException("Element " + element
                            + " is already bound to a component of type "
                            + componentClass.getName());
                }
            }

            Component c;
            try {
                // Get the field from the object
                memberField.setAccessible(true);
                c = (Component) memberField.get(template);
                if (c != null) {
                    // Only replace uninitialized fields
                    continue;
                }
            } catch (Exception e) {
                // If we cannot determine the value, just skip the field and
                // try the next one
                continue;
            }

            try {
                c = createComponent(componentType);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(
                        "Unable to create component instance of type "
                                + componentType.getName(),
                        e);
            }

            bindComponent(c, element);
            assignComponent(template, memberField, c);

        }

    }

    private void assignComponent(Object template,
            java.lang.reflect.Field memberField, Component c) {
        // Store it in the field
        try {
            memberField.setAccessible(true);
            memberField.set(template, c);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Unable to assign component instance to field "
                            + memberField.getName(),
                    e);
        }
    }

    private void bindComponent(Component c, Element element) {
        assert c != null;
        assert element != null;
        AbstractComponent.mapComponent((AbstractComponent) c, element);

    }

    protected Component createComponent(
            Class<? extends Component> componentType)
                    throws InstantiationException, IllegalAccessException {
        return componentType.newInstance();
    }

}
