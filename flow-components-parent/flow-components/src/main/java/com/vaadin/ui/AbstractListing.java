package com.vaadin.ui;

import com.vaadin.data.provider.DataCommunicator;

/**
 * 
 * @param <T>
 */
public abstract class AbstractListing<T> extends Component {

    /**
     * Returns the data communicator of this listing.
     *
     * @return the data communicator, not null
     */
    public abstract DataCommunicator<T> getDataCommunicator();
}
