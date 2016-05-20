package com.vaadin.client.hummingbird.dom;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.ClientEngineTestBase;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.hummingbird.dom.PolymerDomApiImpl.Polymer;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;

public class GwtPolymerApiImplTest extends ClientEngineTestBase {

    @JsProperty(namespace = JsPackage.GLOBAL, name = "Polymer")
    private static native void setPolymer(Polymer polymer);

    static void setPolymerFull() {
        Polymer polymer = WidgetUtil
                .crazyJsCast(JavaScriptObject.createObject());

        setPolymer(polymer);
    }

    static void setPolymerMicro() {
        Polymer polymer = WidgetUtil
                .crazyJsCast(JavaScriptObject.createObject());
        setPolymer(polymer);
    }

    static void clearPolymer() {
        setPolymer(null);
    }

    private void initTest() {
        clearPolymer();
        verifyPolymerMicro(false);
    }

    public void testPolymerMicroLoaded() {
        initTest();

        setPolymerMicro();

        verifyPolymerMicro(true);
    }

    private static void verifyPolymerMicro(boolean loaded) {
        assertEquals(
                "Polymer micro should " + (loaded ? "" : "not ") + "be loaded",
                loaded, PolymerDomApiImpl.isPolymerMicroLoaded());
    }

}
