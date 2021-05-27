/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.component.polymertemplate;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.template.Id;

/**
 * Creates or maps Element instances to fields mapped using {@link Id @Id}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 * @deprecated Use {@link com.vaadin.flow.component.template.internal.IdMapper}
 *             instead.Polymer template support is deprecated - we recommend you
 *             to use {@code LitTemplate} instead. Read more details from
 *             <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public class IdMapper
        extends com.vaadin.flow.component.template.internal.IdMapper {

    /**
     * Creates a mapper for the given template.
     *
     * @param template
     *            a template instance
     */
    public IdMapper(Component template) {
        super(template);
    }

}
