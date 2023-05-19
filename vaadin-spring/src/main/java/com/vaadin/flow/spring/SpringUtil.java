package com.vaadin.flow.spring;

/** Helpers related to Spring. */
public class SpringUtil {

    /**
     * Checks if this is Spring Boot and not plain Spring.
     *
     * @return true if this is Spring Boot, false if it is Spring without Boot
     */
    public static boolean isSpringBoot() {
        Class<?> resourcesClass = resolveClass(
                SpringVaadinServletService.SPRING_BOOT_WEBPROPERTIES_CLASS);
        return (resourcesClass != null);
    }

    private static Class<?> resolveClass(String clazzName) {
        try {
            return Class.forName(clazzName, false,
                    SpringVaadinServletService.class.getClassLoader());
        } catch (LinkageError | ClassNotFoundException e) {
            return null;
        }
    }

}
