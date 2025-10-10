/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.vaadin.flow.dom;

/**
 * A helper for JS CustomEvent's that automatically maps the 'detail' from the
 * JS event into a DTO.
 * 
 * @param <T> The DTO type used for the detail.
 */
public class CustomEvent<T> extends DomEvent {

    private final Class<T> clazz;

    CustomEvent(DomEvent wrapped, Class<T> clazz) {
        super(wrapped.getSource(), wrapped.getType(), wrapped.getEventData());
        this.clazz = clazz;
    }

    /**
     * @return the detail property of the original JS event mapped into a Java
     * object.
     */
    public T getDetail() {
        return getEventDetail(clazz);
    }

}
