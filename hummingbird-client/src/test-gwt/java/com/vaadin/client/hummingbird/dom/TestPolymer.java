package com.vaadin.client.hummingbird.dom;

import com.vaadin.client.hummingbird.dom.PolymerDomApiImpl.Polymer;
import com.vaadin.client.hummingbird.dom.PolymerDomApiImpl.UpdateStyles;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface TestPolymer extends Polymer {

    @JsProperty
    void setUpdateStyles(UpdateStyles updateStyles);

}
