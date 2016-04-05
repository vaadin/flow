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
package com.vaadin.hummingbird.namespace;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.TextNodeNamespace;
import com.vaadin.hummingbird.namespace.PushConfigurationMap.PushConfigurationParametersMap;

/**
 * A registry of namespaces that are available based on type.
 *
 * @since
 * @author Vaadin Ltd
 */
public class NamespaceRegistry {
    private static int nextNamespaceId = 0;

    // Non-private for testing purposes
    static final Map<Class<? extends Namespace>, NamespaceData> namespaces = new HashMap<>();

    private static class NamespaceData {
        private Function<StateNode, ? extends Namespace> factory;
        private int id = nextNamespaceId++;

        private <T extends Namespace> NamespaceData(
                Function<StateNode, T> factory) {
            this.factory = factory;
        }
    }

    static {
        registerNamespace(ElementDataNamespace.class,
                ElementDataNamespace::new);
        registerNamespace(ElementPropertyNamespace.class,
                ElementPropertyNamespace::new);
        registerNamespace(ElementChildrenNamespace.class,
                ElementChildrenNamespace::new);
        registerNamespace(ElementAttributeNamespace.class,
                ElementAttributeNamespace::new);
        registerNamespace(ElementListenersNamespace.class,
                ElementListenersNamespace::new);
        registerNamespace(PushConfigurationMap.class,
                PushConfigurationMap::new);
        registerNamespace(PushConfigurationParametersMap.class,
                PushConfigurationParametersMap::new);
        registerNamespace(TextNodeNamespace.class, TextNodeNamespace::new);
        registerNamespace(PollConfigurationNamespace.class,
                PollConfigurationNamespace::new);
        registerNamespace(ReconnectDialogConfigurationNamespace.class,
                ReconnectDialogConfigurationNamespace::new);
        registerNamespace(LoadingIndicatorConfigurationNamespace.class,
                LoadingIndicatorConfigurationNamespace::new);
        registerNamespace(ClassListNamespace.class, ClassListNamespace::new);
        registerNamespace(DependencyListNamespace.class,
                DependencyListNamespace::new);
        registerNamespace(ElementStylePropertyNamespace.class,
                ElementStylePropertyNamespace::new);
        registerNamespace(SynchronizedPropertiesNamespace.class,
                SynchronizedPropertiesNamespace::new);
        registerNamespace(TemplateNamespace.class, TemplateNamespace::new);
    }

    private NamespaceRegistry() {
        // Static only
    }

    private static <T extends Namespace> void registerNamespace(Class<T> type,
            Function<StateNode, T> factory) {
        namespaces.put(type, new NamespaceData(factory));
    }

    /**
     * Creates a namespace of the given type for a node.
     *
     * @param namespaceType
     *            the type of the namespace to create
     * @param node
     *            the node for which the namespace should be created
     * @return a newly created namespace
     */
    public static Namespace create(Class<? extends Namespace> namespaceType,
            StateNode node) {
        assert node != null;

        return getData(namespaceType).factory.apply(node);
    }

    /**
     * Gets the id of a namespace type.
     *
     * @param namespace
     *            the namespace type
     * @return the id of the namespace type
     */
    public static int getId(Class<? extends Namespace> namespace) {
        return getData(namespace).id;
    }

    private static NamespaceData getData(Class<? extends Namespace> namespace) {
        assert namespace != null;

        NamespaceData data = namespaces.get(namespace);

        assert data != null : "Namespace " + namespace.getName()
                + " has not been registered in NamespaceRegistry";

        return data;
    }
}
