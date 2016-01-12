package com.vaadin.client.communication.tree;

import java.util.HashMap;
import java.util.Map;

import elemental.json.JsonObject;

public class ValueType {
    private final Object defaultValue;
    private final Map<String, ValueType> properties;
    private final Map<String, String> computedProperties;
    private final ValueType memberType;

    public ValueType(JsonObject json, ValueTypeMap typeMap) {
        JsonObject propertiesJson = json.getObject("properties");
        if (propertiesJson != null) {
            properties = new HashMap<>();
            for (String name : propertiesJson.keys()) {
                int childId = (int) propertiesJson.getNumber(name);
                properties.put(name, typeMap.get(childId));
            }
        } else {
            properties = null;
        }

        JsonObject computedJson = json.getObject("computed");
        if (computedJson != null) {
            assert properties != null;
            computedProperties = new HashMap<>();

            for (String name : computedJson.keys()) {
                assert properties.containsKey(name);
                computedProperties.put(name, computedJson.getString(name));
            }
        } else {
            computedProperties = null;
        }

        if (json.hasKey("member")) {
            int memberId = (int) json.getNumber("member");
            memberType = typeMap.get(memberId);
        } else {
            memberType = null;
        }

        defaultValue = null;
    }

    public ValueType(Object defaultValue) {
        this.defaultValue = defaultValue;

        properties = null;
        memberType = null;
        computedProperties = null;
    }

    public ValueType(Map<String, ValueType> properties,
            Map<String, String> computedProperties, ValueType memberType) {
        this.properties = properties;
        this.memberType = memberType;
        this.computedProperties = computedProperties;

        defaultValue = null;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Map<String, ValueType> getProperties() {
        return properties;
    }

    public Map<String, String> getComputedProperties() {
        return computedProperties;
    }

    public ValueType getMemberType() {
        return memberType;
    }

    public boolean isBasicType() {
        return properties == null && memberType == null;
    }

    public boolean isArrayType() {
        return memberType != null;
    }

    public boolean isObjectType() {
        return properties != null || isArrayType();
    }
}
