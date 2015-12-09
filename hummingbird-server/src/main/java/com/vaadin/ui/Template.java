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
package com.vaadin.ui;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.SimpleBindings;

import com.vaadin.annotations.Implemented;
import com.vaadin.annotations.JS;
import com.vaadin.annotations.TemplateEventHandler;
import com.vaadin.annotations.TemplateHTML;
import com.vaadin.data.util.BeanUtil;
import com.vaadin.hummingbird.kernel.ComputedProperty;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.JsonConverter;
import com.vaadin.hummingbird.kernel.LazyList;
import com.vaadin.hummingbird.kernel.ModelDescriptor;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.TemplateScriptHelper;
import com.vaadin.hummingbird.parser.TemplateParser;

import elemental.json.JsonArray;
import elemental.json.JsonType;
import elemental.json.JsonValue;

@Implemented("Missing quite a few features such as computed properties")
public abstract class Template extends AbstractComponent
        implements HasComponents {
    private static class ProxyHandler implements InvocationHandler {

        private StateNode node;

        public ProxyHandler(StateNode node) {
            this.node = node;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass == Object.class) {
                return handleObjectMethod(proxy, method, args);
            } else if (declaringClass == Model.class) {
                return handleModelMethod(proxy, method, args);
            }

            String name = method.getName();
            if (name.startsWith("get") || name.startsWith("is")) {
                assert args == null || args.length == 0;
                assert method.getReturnType() != void.class;

                return get(node, getPropertyName(method.getName()),
                        method.getGenericReturnType());
            } else if (name.startsWith("set")) {
                assert args.length == 1;
                assert method.getReturnType() == void.class;

                set(node, getPropertyName(method.getName()),
                        method.getGenericParameterTypes()[0], args[0]);
                return null;
            } else {
                throw new RuntimeException("Method not supported: " + method);
            }
        }

        private Object handleModelMethod(Object proxy, Method method,
                Object[] args) {
            switch (method.getName()) {
            default:
                throw new RuntimeException("Method not supported " + method);
            }
        }

        private void set(StateNode node, String propertyName, Type type,
                Object value) {
            if (type == boolean.class && Boolean.FALSE.equals(value)) {
                value = null;
            }

            if (value == null) {
                if (node.containsKey(propertyName)) {
                    node.remove(propertyName);
                }
                return;
            }

            if (type instanceof Class<?>) {
                Class<?> clazz = (Class<?>) type;

                if (LazyList.class.isAssignableFrom(clazz)) {
                    LazyList lazyList = (LazyList) value;
                    node.put(propertyName, lazyList);
                    return;
                }

                if (clazz.isInterface()
                        && !JsonValue.class.isAssignableFrom(clazz)) {
                    value = unwrapProxy(value);

                    if (Objects.equals(value, node.get(propertyName))) {
                        return;
                    }
                }

                if (!Objects.equals(value, get(node, propertyName, type))) {
                    node.put(propertyName, value);
                }

                return;
            } else if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType() instanceof Class<?>) {
                    Class<?> rawType = (Class<?>) pt.getRawType();
                    if (List.class.isAssignableFrom(rawType)) {
                        assert (pt
                                .getActualTypeArguments()[0] instanceof Class<?>) : "Multi-level generics not supported for now";
                        Class<?> childType = (Class<?>) pt
                                .getActualTypeArguments()[0];
                        if (childType.isInterface()) {
                            throw new RuntimeException("This is complicated");
                        }

                        List<?> values = (List<?>) value;
                        List<Object> nodeValues = node
                                .getMultiValued(propertyName);
                        if (!Objects.equals(values, nodeValues)) {
                            nodeValues.clear();
                            nodeValues.addAll(values);
                        }
                        return;
                    }
                }
            }

            throw new RuntimeException(
                    type.getClass().toString() + ": " + type.toString());
        }

        private StateNode unwrapProxy(Object value) {
            if (!Proxy.isProxyClass(value.getClass())) {
                throw new RuntimeException("class " + value.getClass().getName()
                        + " is not a proxy class");
            }
            InvocationHandler handler = Proxy.getInvocationHandler(value);
            if (handler instanceof ProxyHandler) {
                return ((ProxyHandler) handler).node;
            } else {
                String handlerType = "null";
                if (handler != null) {
                    handlerType = handler.getClass().getName();
                }

                throw new RuntimeException("class " + value.getClass().getName()
                        + " handler " + handlerType + " is not a ProxyHandler");
            }
        }

        private Object get(StateNode node, String propertyName, Type type) {
            if (type == boolean.class) {
                return Boolean.valueOf(node.containsKey(propertyName));
            }

            if (type instanceof Class<?> && ((Class<?>) type).isPrimitive()) {
                if (!node.containsKey(propertyName)) {
                    // Find the default value, somehow
                    return primitiveDefaults.get(type);
                } else {
                    // Can't get by type int if value is Integer
                    return node.get(propertyName);
                }
            }

            if (!node.containsKey(propertyName)) {
                return null;
            }

            if (type instanceof Class<?>) {
                Class<?> clazz = (Class<?>) type;
                if (LazyList.class.isAssignableFrom(clazz)) {
                    return node.get(propertyName, LazyList.class);
                }
                if (clazz.isInterface()) {
                    StateNode childNode = node.get(propertyName,
                            StateNode.class);
                    return Model.wrap(childNode, clazz);
                }

                return node.get(propertyName, clazz);
            } else if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType() instanceof Class<?>) {
                    Class<?> rawType = (Class<?>) pt.getRawType();
                    if (List.class.isAssignableFrom(rawType)) {
                        assert (pt
                                .getActualTypeArguments()[0] instanceof Class<?>) : "Multi-level generics not supported for now";
                        Class<?> childType = (Class<?>) pt
                                .getActualTypeArguments()[0];
                        List<Object> backingList = node
                                .getMultiValued(propertyName);
                        if (childType.isInterface()) {
                            return new AbstractList<Object>() {
                                @Override
                                public Object get(int index) {
                                    StateNode childNode = (StateNode) backingList
                                            .get(index);
                                    if (childNode == null) {
                                        return null;
                                    }
                                    return Model.wrap(childNode, childType);
                                }

                                @Override
                                public void add(int index, Object element) {
                                    if (element == null) {
                                        backingList.add(index, null);
                                    } else {
                                        backingList.add(index,
                                                unwrapProxy(element));
                                    }
                                }

                                @Override
                                public Object remove(int index) {
                                    Object oldValue = get(index);
                                    backingList.remove(index);
                                    return oldValue;
                                }

                                @Override
                                public int size() {
                                    return backingList.size();
                                }
                            };
                        } else {
                            return backingList;
                        }
                    }
                }
            }
            throw new RuntimeException(
                    type.getClass().toString() + ": " + type.toString());
        }

        private Object handleObjectMethod(Object proxy, Method method,
                Object[] args) {
            switch (method.getName()) {
            case "equals": {
                assert args.length == 1;
                Object other = args[0];
                if (other == null) {
                    return Boolean.FALSE;
                }
                StateNode otherNode = unwrapProxy(other);
                return Boolean.valueOf(Objects.equals(otherNode, node));
            }
            case "hashCode": {
                assert args == null;
                return Integer.valueOf(node.hashCode());
            }
            case "toString": {
                assert args == null;
                return "Model for " + node.getId();
            }
            default:
                throw new RuntimeException(
                        "Method not yet supported: " + method);
            }
        }
    }

    public interface Model {
        public static <T> T create(Class<T> type) {
            return wrap(StateNode.create(), type);
        }

        /**
         * Converts the given bean to a stand alone StateNode, disconnected from
         * the bean
         *
         * @param bean
         * @return
         */
        public static <T> StateNode beanToStateNode(Class<T> beanType, T bean) {
            StateNode node = StateNode.create();
            beanToStateNode(beanType, bean, node);
            return node;
        }

        public static <T> void beanToStateNode(Class<T> beanType, T bean,
                StateNode node) {
            List<PropertyDescriptor> propertyDescriptors;
            try {
                propertyDescriptors = BeanUtil
                        .getBeanPropertyDescriptor(beanType);

                for (PropertyDescriptor pd : propertyDescriptors) {
                    try {
                        if (pd.getReadMethod() != null
                                && pd.getWriteMethod() != null) {
                            Object value = pd.getReadMethod().invoke(bean);
                            if (isSimpleType(pd.getPropertyType())) {
                                node.put(pd.getName(), value);
                            } else {
                                node.put(pd.getName(), beanToStateNode(
                                        (Class) pd.getPropertyType(), value));
                            }
                        }
                    } catch (IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e) {
                        // Ignore what cannot be read
                    }
                }
            } catch (IntrospectionException e1) {
                getLogger().log(Level.SEVERE,
                        "Unable to convert bean to state node", e1);
            }
        }

        public static Logger getLogger() {
            return Logger.getLogger(Model.class.getName());
        }

        public static boolean isSimpleType(Class<?> type) {
            // FIXME Is this really the correct check to do?
            return JsonConverter.isSupportedType(type);
        }

        public static <T> T wrap(StateNode node, Class<T> type) {
            ModelDescriptor modelDescriptor = node.get(ModelDescriptor.class,
                    ModelDescriptor.class);
            if (modelDescriptor == null) {
                modelDescriptor = ModelDescriptor.get(type);
                node.put(ModelDescriptor.class, modelDescriptor);
            } else if (modelDescriptor.getModelType() != type) {
                throw new RuntimeException(
                        "Trying to use state node " + node.getId() + " as "
                                + type + ", but it has already been used as "
                                + modelDescriptor.getModelType());
            }
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                    new Class[] { type }, new ProxyHandler(node)));
        }
    }

    private final Model model;

    private final StateNode node = StateNode.create();

    public Template() {
        this(null);
    }

    protected Template(ElementTemplate elementTemplate) {
        if (elementTemplate == null) {
            String htmlFileName = getTemplateFilename();
            elementTemplate = TemplateParser.parse(getClass(), htmlFileName);
        }
        setElement(Element.getElement(elementTemplate, node));

        Map<String, ComputedProperty> computedProperties = findComputedProperties();
        if (!computedProperties.isEmpty()) {
            node.setComputedProperties(computedProperties);

            computedProperties.values().stream()
                    .filter(ComputedProperty::hasClientCode).forEach(p -> {
                        node.enqueueRpc("}computed", node, p.getName(),
                                p.getClientCode());
                    });
        }

        getNode().put(TemplateCallbackHandler.class,
                new TemplateCallbackHandler() {
                    @Override
                    public void handleCallback(StateNode node,
                            String callbackName, JsonArray parameters,
                            int promiseId) {
                        onBrowserEvent(node, callbackName, parameters,
                                promiseId);
                    }
                });

        getBinder().bindComponents(this);

        // Always trigger so that model type is stored in the state node
        model = createModel();
    }

    private Map<String, ComputedProperty> findComputedProperties() {
        // Should be made static so that the result can be cached
        Map<String, ComputedProperty> properites = new HashMap<>();

        Class<? extends Model> modelType = getModelType();
        Method[] methods = modelType.getMethods();
        for (Method method : methods) {
            ComputedProperty computedProperty = findComputedProperty(modelType,
                    method);

            if (computedProperty != null) {
                properites.put(computedProperty.getName(), computedProperty);
            }
        }

        return Collections.unmodifiableMap(properites);
    }

    private ComputedProperty findComputedProperty(
            Class<? extends Model> modelType, Method method) {
        String methodName = method.getName();
        String name = getPropertyName(methodName);

        if (!methodName.startsWith("get") && !methodName.startsWith("is")) {
            return null;
        }

        if (method.isDefault()) {
            if (method.getReturnType() == void.class) {
                throw new IllegalStateException("Computed property "
                        + method.toString() + " has no return type");
            } else if (method.getParameterCount() != 0) {
                throw new IllegalStateException(
                        "Computed property " + method.toString()
                                + " should require zero parameters");
            }

            return new ComputedProperty(name, null) {
                @Override
                public Object compute(StateNode context) {
                    try {
                        // Invoke the default method implementation instead
                        // of triggering the proxy handler
                        // http://zeroturnaround.com/rebellabs/recognize-and-conquer-java-proxies-default-methods-and-method-handles/
                        Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                                .getDeclaredConstructor(Class.class, int.class);
                        constructor.setAccessible(true);
                        return constructor
                                .newInstance(modelType,
                                        MethodHandles.Lookup.PRIVATE)
                                .unreflectSpecial(method, modelType)
                                .bindTo(model).invokeWithArguments();

                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        JS jsAnnotation = method.getAnnotation(JS.class);
        if (jsAnnotation != null) {
            String script = jsAnnotation.value();
            Class<?> type = method.getReturnType();

            return new ComputedProperty(name, script) {
                @Override
                public Object compute(StateNode context) {
                    SimpleBindings bindings = new SimpleBindings();
                    bindings.put("model", model);

                    return TemplateScriptHelper.evaluateScript(bindings, script,
                            type);
                }
            };
        }

        return null;
    }

    private String getTemplateFilename() {
        Class<?> c = getClass();
        Class<?> classExtendingTemplate = null;
        while (c != null) {
            TemplateHTML templateAnnotation = c
                    .getAnnotation(TemplateHTML.class);
            if (templateAnnotation != null) {
                return c.getPackage().getName().replace(".", "/") + "/"
                        + templateAnnotation.value();
            }
            if (c.getSuperclass() == Template.class) {
                classExtendingTemplate = c;
            }
            c = c.getSuperclass();
        }

        return classExtendingTemplate.getPackage().getName().replace(".", "/")
                + "/" + classExtendingTemplate.getSimpleName() + ".html";
    }

    protected TemplateComponentBinder getBinder() {
        return new TemplateComponentBinder();
    }

    protected void onBrowserEvent(StateNode node, String methodName,
            JsonArray params, int promiseId) {
        Method method = findTemplateEventHandlerMethod(getClass(), methodName,
                params.length());
        if (method == null) {
            throw new RuntimeException("Couldn't find any @"
                    + com.vaadin.annotations.TemplateEventHandler.class
                            .getSimpleName()
                    + " method named " + methodName + " with " + params.length()
                    + " parameters in " + getClass().getName());
        }

        int paramIndex = 0;

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] methodParams = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            JsonValue param = params.get(paramIndex++);
            if (type == Element.class && param.getType() == JsonType.ARRAY) {
                JsonArray elementArray = (JsonArray) param;
                int nodeId = (int) elementArray.getNumber(0);
                int templateId = (int) elementArray.getNumber(1);

                StateNode elementNode = node.getRoot().getById(nodeId);
                ElementTemplate template = getUI().getTemplate(templateId);
                methodParams[i] = Element.getElement(template, elementNode);
            } else {
                methodParams[i] = JsonConverter.fromJson(type, param);
            }
        }

        method.setAccessible(true);
        Object promiseResult = "Unkonwn error, consult server logs";
        boolean promiseSuccess = false;
        try {
            promiseResult = method.invoke(this, methodParams);
            promiseSuccess = true;
        } catch (InvocationTargetException e) {
            promiseResult = e.getCause().getMessage();

            throw new RuntimeException(
                    "Exception from template handler " + method, e);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            promiseResult = e.getMessage();

            throw new RuntimeException("Couldn't invoke " + method, e);
        } finally {
            getNode().enqueueRpc("}promise", promiseId, promiseSuccess,
                    promiseResult);
        }
    }

    private static Method findTemplateEventHandlerMethod(Class<?> type,
            String methodName, int paramCount) {
        while (type != null && type != Template.class) {
            Method[] declaredMethods = type.getDeclaredMethods();
            for (Method method : declaredMethods) {
                if (method.getName().equals(methodName)
                        && method.getAnnotation(
                                TemplateEventHandler.class) != null
                        && method.getParameterCount() == paramCount) {
                    return method;
                }
            }

            type = type.getSuperclass();
        }

        return null;
    }

    protected StateNode getNode() {
        return node;
    }

    protected Model getModel() {
        return model;
    }

    private Class<? extends Model> getModelType() {
        // TODO cache the result (preferably without leaking classloaders)
        Class<?> type = getClass();
        while (type != Template.class) {
            try {
                Method method = type.getDeclaredMethod("getModel");
                return method.getReturnType().asSubclass(Model.class);
            } catch (NoSuchMethodException e) {
                type = type.getSuperclass();
            }
        }

        return Model.class;
    }

    private static final Map<Class<?>, Object> primitiveDefaults = new HashMap<>();

    static {
        primitiveDefaults.put(byte.class, Byte.valueOf((byte) 0));
        primitiveDefaults.put(short.class, Short.valueOf((short) 0));
        primitiveDefaults.put(char.class, Character.valueOf((char) 0));
        primitiveDefaults.put(int.class, Integer.valueOf(0));
        primitiveDefaults.put(long.class, Long.valueOf(0));
        primitiveDefaults.put(float.class, Float.valueOf(0f));
        primitiveDefaults.put(double.class, Double.valueOf(0));
    }

    private Model createModel() {
        Class<? extends Model> modelType = getModelType();
        Object proxy = Model.wrap(getNode(), modelType);
        return (Model) proxy;
    }

    /**
     * Finds an element in the template based on a local (primarily) and global
     * (secondarily) id.
     *
     * @param id
     * @return
     */
    public Element getElementById(String id) {
        // TODO Optimize
        Element e = getElementByLocalId(getElement(), id);
        if (e == null) {
            e = getElementById(getElement(), id);
        }

        return e;
    }

    private Element getElementById(Element element, String id) {
        if (Element.isTextNode(element)) {
            return null;
        }

        if (id.equals(element.getAttribute("id"))) {
            return element;
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            Element e = getElementById(element.getChild(i), id);
            if (e != null) {
                return e;
            }
        }

        return null;
    }

    private Element getElementByLocalId(Element element, String id) {
        if (Element.isTextNode(element)) {
            return null;
        }
        if (id.equals(element.getAttribute("LOCAL_ID"))) {
            return element;
        }
        for (int i = 0; i < element.getChildCount(); i++) {
            Element e = getElementByLocalId(element.getChild(i), id);
            if (e != null) {
                return e;
            }
        }

        return null;
    }

    private static String getPropertyName(String methodName) {
        String propertyName = methodName.replaceFirst("^(set|get|is)", "");
        propertyName = Character.toLowerCase(propertyName.charAt(0))
                + propertyName.substring(1);
        return propertyName;
    }
}
