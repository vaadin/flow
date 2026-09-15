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
package com.vaadin.quarkus.deployment;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;

/**
 * A utility class for inspecting and extracting concrete class types from
 * various type structures, including parameterized types, type variables,
 * method signatures, and complex type hierarchies.
 */
class TypeInspector {

    /**
     * Recursively collects concrete class types from a Type, including those
     * nested within parameterized types and type variables.
     *
     * @param type
     *            the type to inspect
     * @param index
     *            the index to resolve class information
     */
    static Set<ClassInfo> collectTypes(Type type, IndexView index) {
        Set<ClassInfo> concreteTypes = new HashSet<>();
        Set<Type> visited = new HashSet<>();
        collectConcreteTypes(type, index, concreteTypes, visited);
        return concreteTypes;
    }

    /**
     * Extracts all concrete types from a method's parameters and return type.
     * This method ignores primitive types and recursively inspects
     * parameterized types and type variables to collect all class types.
     *
     * @param methodInfo
     *            the method to inspect
     * @param index
     *            the index to resolve class information
     * @return a set of all concrete class types found in the method signature
     */
    static Set<ClassInfo> collectTypes(MethodInfo methodInfo, IndexView index) {
        Set<ClassInfo> concreteTypes = new HashSet<>();
        Set<Type> visited = new HashSet<>();
        collectConcreteTypesFromMethod(methodInfo, index, concreteTypes,
                visited);
        return concreteTypes;
    }

    private static void collectConcreteTypesFromMethod(MethodInfo methodInfo,
            IndexView index, Set<ClassInfo> concreteTypes, Set<Type> visited) {
        // Inspect return type
        collectConcreteTypes(methodInfo.returnType(), index, concreteTypes,
                visited);

        // Inspect parameter types
        for (Type paramType : methodInfo.parameterTypes()) {
            collectConcreteTypes(paramType, index, concreteTypes, visited);
        }
    }

    /**
     * Recursively collects concrete class types from a Type, including those
     * nested within parameterized types and type variables.
     *
     * @param type
     *            the type to inspect
     * @param index
     *            the index to resolve class information
     * @param collector
     *            the set to collect concrete types into
     */
    private static void collectConcreteTypes(Type type, IndexView index,
            Set<ClassInfo> collector, Set<Type> visited) {
        if (type == null) {
            return;
        }
        if (visited.contains(type)) {
            return;
        }

        switch (type.kind()) {
        case PRIMITIVE:
        case VOID:
            // Ignore primitive types
            break;

        case CLASS:
            // Add the class type
            ClassInfo classInfo = index.getClassByName(type.name());
            if (classInfo != null) {
                collector.add(classInfo);
            }
            break;

        case PARAMETERIZED_TYPE:
            // For parameterized types (e.g., List<String>), inspect both the
            // owner and type arguments
            ParameterizedType paramType = type.asParameterizedType();

            // Collect the owner type (e.g., List)
            collectConcreteTypes(paramType.owner(), index, collector, visited);

            // Collect type arguments (e.g., String in List<String>)
            for (Type arg : paramType.arguments()) {
                collectConcreteTypes(arg, index, collector, visited);
            }
            break;

        case TYPE_VARIABLE:
            // For type variables (e.g., T in <T>), inspect bounds
            TypeVariable typeVar = type.asTypeVariable();
            for (Type bound : typeVar.bounds()) {
                collectConcreteTypes(bound, index, collector, visited);
            }
            break;

        case ARRAY:
            // For arrays, inspect the component type
            collectConcreteTypes(type.asArrayType().component(), index,
                    collector, visited);
            break;

        case WILDCARD_TYPE:
            // For wildcard types (e.g., ? extends Foo), inspect bounds
            Type extendsBound = type.asWildcardType().extendsBound();
            if (extendsBound != null) {
                collectConcreteTypes(extendsBound, index, collector, visited);
            }
            Type superBound = type.asWildcardType().superBound();
            if (superBound != null) {
                collectConcreteTypes(superBound, index, collector, visited);
            }
            break;

        case UNRESOLVED_TYPE_VARIABLE:
            // Skip unresolved type variables
            break;
        }
    }

}
