package com.vaadin.guice.annotation;

import com.google.inject.Module;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks an {@link java.lang.annotation.Annotation} as creator of a {@link Module}. The module needs
 * to have a standard-constructor or a constructor accepting the annotated Annotation as a parameter
 * <p>
 * <pre>
 * &#064;Import(MyModule.class)
 * public &#064;interface MyCreationAnnotation {
 *     // ...
 *
 *     String databaseToUse();
 * }
 *
 *
 *
 * public class MyModule extends AbstractModule {
 *
 *     public MyModule(MyCreationAnnotation annotation){
 *         String dataBaseToUse = annotation.databaseToUse();
 *
 *         //...
 *     }
 *
 *     //...
 * }
 *
 * &#064;MyCreationAnnotation(databaseToUse = "my-database-connection-name")
 * &#064;PackagesToScan(basePackages = "com.test")
 * public class MyServlet extends GuiceVaadinServletServlet{
 *  //guice-context will have MyModule with databaseToUse = my-database-connection-name installed
 * }
 * </pre>
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@Retention(RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface Import {
    /**
     * the {@link Module to be created}
     */
    Class<? extends Module> value();

    /**
     * new packages to be included in packagesToScan
     *
     * @see PackagesToScan#value()
     */
    String[] packagesToScan() default {};
}
