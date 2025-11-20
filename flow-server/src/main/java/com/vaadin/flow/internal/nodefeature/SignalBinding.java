package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.shared.Registration;
import com.vaadin.signals.Signal;
import java.io.Serializable;

/**
 * A record that holds an immutable context for a signal binding in an Element instance.
 * @param signal The associated signal object of a generic type, representing the subject of the binding.
 * @param registration A mechanism for managing the lifecycle or unregistration of the signal.
 * @param name The name of the bound property/attribute of an element.
 * @param value A serializable value associated with the binding.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 25.0
 */
public record SignalBinding(Signal<?> signal, Registration registration,
                            String name, Serializable value) implements Serializable {
    public SignalBinding(Signal<?> signal, Registration registration, Serializable value) {
        this(signal, registration, null, value);
    }
}
