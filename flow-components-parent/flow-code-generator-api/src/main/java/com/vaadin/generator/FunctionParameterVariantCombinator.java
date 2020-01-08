/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.vaadin.generator.metadata.ComponentFunctionData;
import com.vaadin.generator.metadata.ComponentFunctionParameterData;
import com.vaadin.generator.metadata.ComponentType;

/**
 * Utility class for generating all possible combinations of allowed parameters
 * for ComponentFunctionData.
 * 
 * @author Vaadin Ltd
 * @since 1.0
 */
public class FunctionParameterVariantCombinator {

    private FunctionParameterVariantCombinator() {
    }

    /**
     * Utility method for generating all possible combinations of allowed
     * parameters for ComponentFunctionData. Returns a list of all possible
     * lists of ComponentTypes the given function accepts as parameters.
     * 
     * @param function
     *            the function to generate parameter variants for
     * @return all the valid combinations of function parameter types
     */
    public static Set<List<ComponentType>> generateVariants(
            ComponentFunctionData function) {
        if (function.getParameters() == null
                || function.getParameters().isEmpty()) {
            return Collections.singleton(Arrays.asList());
        }
        List<ComponentFunctionParameterData> parameterData = new ArrayList<>(
                function.getParameters());
        Set<List<ComponentType>> paramVariants = generateCombinations(
                parameterData.remove(0), parameterData);
        return paramVariants;
    }

    private static Set<List<ComponentType>> generateCombinations(
            ComponentFunctionParameterData paramData,
            List<ComponentFunctionParameterData> rest) {
        Comparator<List<ComponentType>> listComparator = Comparator
                .comparing(List::toString);
        if (rest.isEmpty()) {
            return getTypeVariants(paramData).stream().map(Arrays::asList)
                    .sorted(listComparator).collect(Collectors.toSet());
        }
        List<ComponentFunctionParameterData> copy = new ArrayList<>(rest);
        Set<List<ComponentType>> ret = new TreeSet<>(listComparator);
        for (List<ComponentType> subCombinations : generateCombinations(
                copy.remove(0), copy)) {
            for (ComponentType typeVariants : getTypeVariants(paramData)) {
                List<ComponentType> tmp = new ArrayList<>(subCombinations);
                tmp.add(0, typeVariants);
                ret.add(tmp);
            }
        }
        return ret;
    }

    private static List<ComponentType> getTypeVariants(
            ComponentFunctionParameterData paramData) {
        List<ComponentType> typeVariants = new ArrayList<>();
        if (paramData.getObjectType() != null) {
            paramData.getObjectType().forEach(typeVariants::add);
        }
        if (paramData.getType() != null) {
            paramData.getType().forEach(typeVariants::add);
        }
        typeVariants.sort(Comparator.comparing(ComponentType::toString));
        return typeVariants;
    }
}
