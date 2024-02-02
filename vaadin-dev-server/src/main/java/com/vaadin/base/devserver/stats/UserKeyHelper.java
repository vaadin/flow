package com.vaadin.base.devserver.stats;

/**
 * Public helper for loading user key
 */
public class UserKeyHelper {

    /**
     * Gets user key if present
     *
     * @return user key if present, null otherwise
     */
    public static String getUserKey() {
        return ProjectHelpers.getUserKey();
    }

}
