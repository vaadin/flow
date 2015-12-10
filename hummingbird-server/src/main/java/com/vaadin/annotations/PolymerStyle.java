package com.vaadin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for declaring custom Polymer style modules that contain theming
 * for Polymer components.
 * <p>
 * The actual style sheets should be .html files with proper syntax and must be
 * imported separately, e.g. with the {@link com.vaadin.annotations.HTML}
 * annotation. The value returned by {@link #value()} should match the defined
 * dom-module id in the file. Example of file content:
 * <p>
 * {@code
 * <dom-module id="custom-style">
 *   <template>
 *     <style>}...styling...{@code</style>
 *   </template>
 * </dom-module>}
 *
 * @see {@link https://www.polymer-project.org/1.0/docs/devguide/styling.html#style-modules}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PolymerStyle {
    /**
     * Polymer custom style module id. Each value returned will be appended into
     * the document head as a style element:
     * <p>
     * {@code <style is="custom-style" include="}value{@code"></style>}
     * <p>
     * The value returned by {@link #value()} maps to the dom-module's id, and
     * will be the value of the include attribute for the element.
     *
     * @return an array of custom polymer style module ids.
     */
    public String[] value();

}
