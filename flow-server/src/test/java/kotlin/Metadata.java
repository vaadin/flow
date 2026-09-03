/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package kotlin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test stand-in for the annotation that the Kotlin compiler adds to every class
 * it compiles.
 * <p>
 * kotlin-stdlib is not on the Flow classpath, so only the members that Flow
 * reads are declared here. Defaults match what kotlinc 2.1 emits.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Metadata {

    /**
     * The kind of the compiled element, {@code 1} for a class.
     *
     * @return the element kind
     */
    int k() default 1;

    /**
     * The version of the metadata, which follows the Kotlin language version
     * that produced the class.
     *
     * @return the metadata version as major, minor and patch
     */
    int[] mv() default { 2, 1, 0 };
}
