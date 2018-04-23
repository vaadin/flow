package com.vaadin.flow.router;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
 /**
 * This annotation sets the priority of execution on {@link BeforeEnterListener},
 * {@link BeforeLeaveListener} and {@link AfterNavigationListener}. The higher the
 * value of priority, the higher the likelihood of the listener to be the
 * first listener to be executed. Priority-values must be non-negative, with
 * zero being the default-priority, so that any listener with a priority-value of
 * more than 0 will be executed before listeners that are not annotated with
 * {@literal @}ListenerPriority
 *
 * <p>
 *     <code>
 *      //will be executed first
 *      @ListenerPriority(5)
 *      class HighPriorityListener implements BeforeEnterListener {
 *      }
 *
 *      //will be executed second, default priority is 0
 *      class YetAnotherListener implements BeforeEnterListener {
 *      }
 *
 *      //will be executed third
 *      @ListenerPriority(-5)
 *      class LowPriorityListener implements BeforeEnterListener {
 *      }
 *     </code>
 * </p>
 *
 * @author Bernd Hopp
 */
public @interface ListenerPriority {
    /**
     * The priority of the annotated listener, can be any integer. Larger numbers
     * indicate higher priority.
     *
     * @return the priority-value.
     */
    int value();
}
