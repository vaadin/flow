package com.vaadin.flow.router;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
/**
 * This annotation sets the priority of execution on {@link BeforeEnterListener},
 * {@link BeforeLeaveListener} and {@link AfterNavigationListener}. The higher the
 * value of priority, the higher the likelihood of the listener to be the
 * first listener to be executed. Priority-values must be non-negative, with
 * zero being the default-priority, so that any listener with a priority-value of
 * more than 0 will be executed before listeners that are not annotated with
 * @ListenerPriority
 *
 * <p>
 *     <code>
 *      //will be executed first
 *      @ListenerPriority(5)
 *      class MyListener implements BeforeEnterListener {
 *      }
 *
 *      //will be executed second
 *      @ListenerPriority(4)
 *      class MyOtherListener implements BeforeEnterListener {
 *      }
 *     </code>
 * </p>
 *
 * @author Bernd Hopp
 */
public @interface ListenerPriority {
    /**
     * @return the priority-value, needs to be non-negative.
     */
    int value();
}
