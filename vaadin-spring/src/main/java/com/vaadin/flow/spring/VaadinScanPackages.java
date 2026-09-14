package com.vaadin.flow.spring;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VaadinScanPackages implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final List<String> scanPackages;

    public VaadinScanPackages(String[] scanPackages) {
        assert scanPackages != null;
        this.scanPackages = new ArrayList<>(Arrays.asList(scanPackages));
    }

    public List<String> getScanPackages() {
        return scanPackages;
    }

    public static String[] merge(String[] existingPackages,
                                 String[] newPackages) {

        String[] merged = Arrays.copyOf(existingPackages,
                existingPackages.length + newPackages.length);

        System.arraycopy(newPackages, 0,
                merged,
                existingPackages.length,
                newPackages.length);

        return merged;
    }
}