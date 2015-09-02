package com.vaadin.hummingbird.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelStructure {

    private Set<String> simpleKeys;
    private Map<String, ModelStructure> subStructures;

    public ModelStructure(Set<String> simpleKeys,
            Map<String, ModelStructure> subStructures) {
        this.simpleKeys = Collections
                .unmodifiableSet(new HashSet<>(simpleKeys));
        this.subStructures = Collections
                .unmodifiableMap(new HashMap<>(subStructures));
    }

    public Set<String> getSimpleKeys() {
        return simpleKeys;
    }

    public Map<String, ModelStructure> getSubStructures() {
        return subStructures;
    }

    public static ModelStructure build(Collection<List<String>> paths) {
        Map<String, List<List<String>>> groupedByPrefix = paths.stream()
                .collect(Collectors.groupingBy(l -> l.get(0)));

        Map<String, ModelStructure> subStructures = new HashMap<>();
        groupedByPrefix.forEach((prefix, subPaths) -> {
            // Remove prefix and filter out empty paths
            List<List<String>> childPaths = subPaths.stream()
                    .map(l -> l.subList(1, l.size())).filter(l -> !l.isEmpty())
                    .collect(Collectors.toList());
            if (!childPaths.isEmpty()) {
                subStructures.put(prefix, build(childPaths));
            }
        });

        Set<String> simpleKeys = groupedByPrefix.keySet().stream()
                .filter(key -> !subStructures.containsKey(key))
                .collect(Collectors.toSet());
        return new ModelStructure(simpleKeys, subStructures);
    }

}
