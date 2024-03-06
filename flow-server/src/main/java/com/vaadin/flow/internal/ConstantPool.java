/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Keeps track of {@link ConstantPoolKey} values that have already been sent to
 * the client.
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class ConstantPool implements Serializable {

    private Set<String> knownValues = new HashSet<>();

    private Set<ConstantPoolKey> newKeys = new HashSet<>();

    /**
     * Gets the id of a given constant, registering the constant with this
     * constant pool if it hasn't already been encountered.
     *
     * @see #dumpConstants()
     *
     * @param constant
     *            the constant reference to get an id for, not <code>null</code>
     * @return the constant id of the given constant, not <code>null</code>
     */
    public String getConstantId(ConstantPoolKey constant) {
        assert constant != null;

        String id = constant.getId();

        if (knownValues.add(id)) {
            newKeys.add(constant);
        }

        return id;
    }

    /**
     * Checks if any new constants have been added to this constant pool since
     * the last time {@link #dumpConstants()} was called.
     *
     * @return <code>true</code> if there are new constants, <code>false</code>
     *         otherwise
     */
    public boolean hasNewConstants() {
        return !newKeys.isEmpty();
    }

    /**
     * Encodes all new constants to a JSON object and marks those constants as
     * non-new.
     *
     * @return a JSON object describing all new constants
     */
    public JsonObject dumpConstants() {
        JsonObject json = Json.createObject();

        newKeys.forEach(key -> key.export(json));
        newKeys.clear();

        return json;
    }

}
