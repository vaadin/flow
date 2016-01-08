package com.vaadin.client.communication.tree;

import java.util.HashMap;
import java.util.Map;

import elemental.json.JsonObject;

public class ValueType {
    private Object defaultValue;
    private Map<String, ValueType> properties;
    private ValueType memberType;

    public ValueType(JsonObject json, ValueTypeMap typeMap) {
        JsonObject propertiesJson = json.getObject("properties");
        if (propertiesJson != null) {
            properties = new HashMap<>();
            for (String name : propertiesJson.keys()) {
                int childId = (int) propertiesJson.getNumber(name);
                properties.put(name, typeMap.get(childId));
            }
        }

        if (json.hasKey("member")) {
            int memberId = (int) json.getNumber("member");
            memberType = typeMap.get(memberId);
        }
    }

    public ValueType(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public ValueType(Map<String, ValueType> properties, ValueType memberType) {
        this.properties = properties;
        this.memberType = memberType;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Map<String, ValueType> getProperties() {
        return properties;
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
