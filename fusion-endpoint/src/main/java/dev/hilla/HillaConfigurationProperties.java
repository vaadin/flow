/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package dev.hilla;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Hilla.
 *
 * @author Vaadin Ltd
 * @see <a href=
 *      "http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html">http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html</a>
 */
@Component
@ConfigurationProperties(prefix = "hilla")
public class HillaConfigurationProperties {

    /**
     * Whether all types used in endpoints should be interpreted as having
     * a @Nonnull annotation.
     */
    private boolean globalNonnull = false;

    /**
     * Returns whether all types used in endpoints should be interpreted as
     * having a @Nonnull annotation
     *
     * @return {@code true} to use @Nonnull everywhere, {@code false} otherwise
     */
    public boolean isGlobalNonnull() {
        return globalNonnull;
    }

    /**
     * Sets whether all types used in endpoints should be interpreted as having
     * a @Nonnull annotation
     *
     * @param globalNonnull
     *            {@code true} to use @Nonnull everywhere, {@code false}
     *            otherwise
     */
    public void setGlobalNonnull(boolean globalNonnull) {
        this.globalNonnull = globalNonnull;
    }

}
