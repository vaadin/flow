package com.vaadin.hummingbird.kernel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Reactive {
    private static final ThreadLocal<List<Runnable>> currentComputation = new ThreadLocal<>();

    public static void compute(Runnable computer, Runnable markAsDirty) {
        List<Runnable> computation = currentComputation.get();
        try {
            if (computation == null) {
                computation = new ArrayList<>();
                currentComputation.set(computation);
            }
            computation.add(markAsDirty);
            computer.run();
        } finally {
            int computationCount = computation.size();
            if (computationCount == 1) {
                currentComputation.remove();
            } else {
                computation.remove(computationCount - 1);
            }
        }
    }

    public static HashSet<Runnable> registerRead(HashSet<Runnable> dependents) {
        List<Runnable> computations = currentComputation.get();
        if (computations != null) {
            if (dependents == null) {
                dependents = new HashSet<>(computations);
            } else {
                dependents.addAll(computations);
            }
        }
        return dependents;
    }

    public static HashSet<Runnable> registerWrite(
            HashSet<Runnable> dependents) {
        if (dependents != null) {
            dependents.forEach(Runnable::run);
        }
        return null;
    }
}