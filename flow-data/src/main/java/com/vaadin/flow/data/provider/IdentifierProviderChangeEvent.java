package com.vaadin.flow.data.provider;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Event notifying the component that its identifier provider has been changed
 * through a data view.
 *
 * @param <T>
 *            the type of item used by the identifier provider
 * @param <C>
 *            the event source type
 */
public class IdentifierProviderChangeEvent<T, C extends Component>
        extends ComponentEvent<C> {

    private final IdentifierProvider<T> identifierProvider;

    /**
     * Creates a new event using the given source and the new identifier
     * provider.
     *
     * @param source
     *            the source component
     * @param identifierProvider
     *            the new identifier provider
     */
    public IdentifierProviderChangeEvent(C source,
            IdentifierProvider<T> identifierProvider) {
        super(source, false);
        this.identifierProvider = identifierProvider;
    }

    /**
     * Returns the new identifier provider for the component.
     *
     * @return the new identifier provider
     */
    public IdentifierProvider<T> getIdentifierProvider() {
        return identifierProvider;
    }
}
