package com.vaadin.flow.hotswap;

import java.util.HashSet;
import java.util.Set;

import com.vaadin.flow.server.VaadinService;

/*
 * Event fired when hotswap has been completed.
 */
public class HotswapCompleteEvent {

    private final Set<Class<?>> classes;
    private final VaadinService vaadinService;
    private final boolean redefined;

    public HotswapCompleteEvent(VaadinService vaadinService,
            Set<Class<?>> classes, boolean redefined) {
        this.classes = classes;
        this.vaadinService = vaadinService;
        this.redefined = redefined;
    }

    /**
     * Gets the classes that were updated.
     * 
     * @return the updated classes
     */
    public Set<Class<?>> getClasses() {
        return classes;
    }

    /**
     * Checks if the classes were redefined (as opposed to being new classes).
     * 
     * @return {@literal true} if the classes have been redefined by hotswap
     */
    public boolean isRedefined() {
        return redefined;
    }

    /**
     * Gets the Vaadin service.
     * 
     * @return the vaadin service
     */
    public VaadinService getService() {
        return vaadinService;
    }

}
