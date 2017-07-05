package com.vaadin.components;

import java.io.Serializable;

import elemental.json.JsonObject;

public interface JsonSerializable extends Serializable {

    JsonObject toJson();

    JsonSerializable fromJson(JsonObject value);

}
