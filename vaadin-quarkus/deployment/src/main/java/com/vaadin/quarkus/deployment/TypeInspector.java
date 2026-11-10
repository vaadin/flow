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
final class TypeInspector {

    private TypeInspector() {
        // utility class
    }

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
        collectConcreteTypes(type, index, concreteTypes);
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
        collectConcreteTypes(methodInfo.returnType(), index, concreteTypes);
        for (Type paramType : methodInfo.parameterTypes()) {
            collectConcreteTypes(paramType, index, concreteTypes);
        }
        return concreteTypes;
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
            Set<ClassInfo> collector) {
        if (type == null) {
            return;
        }

        switch (type.kind()) {
        case PRIMITIVE, VOID, UNRESOLVED_TYPE_VARIABLE -> {
            // no class to collect
        }
        case TYPE_VARIABLE_REFERENCE -> {
            // Only occurs inside the bounds of the type variable it refers
            // to, which are already being inspected. Following it would
            // recurse forever.
        }
        case CLASS -> {
            ClassInfo classInfo = index.getClassByName(type.name());
            if (classInfo != null) {
                collector.add(classInfo);
            }
        }
        case PARAMETERIZED_TYPE -> {
            // For parameterized types (e.g., List<String>), inspect both the
            // owner and type arguments
            ParameterizedType paramType = type.asParameterizedType();
            collectConcreteTypes(paramType.owner(), index, collector);
            for (Type arg : paramType.arguments()) {
                collectConcreteTypes(arg, index, collector);
            }
        }
        case TYPE_VARIABLE -> {
            // For type variables (e.g., T in <T>), inspect bounds
            TypeVariable typeVar = type.asTypeVariable();
            for (Type bound : typeVar.bounds()) {
                collectConcreteTypes(bound, index, collector);
            }
        }
        case ARRAY -> collectConcreteTypes(type.asArrayType().constituent(),
                index, collector);
        case WILDCARD_TYPE -> {
            // For wildcard types (e.g., ? extends Foo), inspect bounds
            collectConcreteTypes(type.asWildcardType().extendsBound(), index,
                    collector);
            collectConcreteTypes(type.asWildcardType().superBound(), index,
                    collector);
        }
        }
    }

}
