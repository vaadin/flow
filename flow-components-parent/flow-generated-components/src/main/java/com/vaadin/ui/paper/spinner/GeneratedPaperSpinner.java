/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.ui.paper.spinner;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;

@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-spinner#2.0.0", "Flow#1.0-SNAPSHOT" })
@Tag("paper-spinner")
@HtmlImport("frontend://bower_components/paper-spinner/paper-spinner.html")
public class GeneratedPaperSpinner<R extends GeneratedPaperSpinner<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Displays the spinner.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code active} property from the webcomponent
     */
    public boolean isActive() {
        return getElement().getProperty("active", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Displays the spinner.
     * </p>
     * 
     * @param active
     *            the boolean value to set
     */
    public void setActive(boolean active) {
        getElement().setProperty("active", active);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Alternative text content for accessibility support. If alt is present, it
     * will add an aria-label whose content matches alt when active. If alt is
     * not present, it will default to 'loading' as the alt value.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code alt} property from the webcomponent
     */
    public String getAlt() {
        return getElement().getProperty("alt");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Alternative text content for accessibility support. If alt is present, it
     * will add an aria-label whose content matches alt when active. If alt is
     * not present, it will default to 'loading' as the alt value.
     * </p>
     * 
     * @param alt
     *            the String value to set
     */
    public void setAlt(String alt) {
        getElement().setProperty("alt", alt == null ? "" : alt);
    }
}