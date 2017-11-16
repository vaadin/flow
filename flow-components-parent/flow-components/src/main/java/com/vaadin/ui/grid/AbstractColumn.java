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
package com.vaadin.ui.grid;

import com.vaadin.data.provider.DataKeyMapper;
import com.vaadin.flow.dom.Element;
import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.renderers.ComponentRenderer;
import com.vaadin.ui.renderers.TemplateRenderer;

/**
 * Base class with common implementation for different types of columns used
 * inside a {@link Grid}.
 * 
 * @author Vaadin Ltd.
 * @param <T>
 *            the subclass type
 */
public class AbstractColumn<T extends AbstractColumn<T>> extends Component
        implements ColumnBase<T>, HasStyle {

    protected Grid<?> grid;
    protected Element headerTemplate;
    protected Element footerTemplate;

    /**
     * Base constructor with the destination Grid.
     * 
     * @param grid
     *            the grid that is the owner of this column
     */
    public AbstractColumn(Grid<?> grid) {
        this.grid = grid;
    }

    /**
     * Gets the owner of this column.
     * 
     * @return the grid which owns this column
     */
    public Grid<?> getGrid() {
        return grid;
    }

    @Override
    public T setHeader(TemplateRenderer<?> renderer) {
        if (headerTemplate == null) {
            headerTemplate = new Element("template").setAttribute("class",
                    "header");
            getElement().appendChild(headerTemplate);
        }

        setupHeaderOrFooter(true, renderer, headerTemplate);
        return (T) this;
    }

    @Override
    public T setFooter(TemplateRenderer<?> renderer) {
        if (footerTemplate == null) {
            footerTemplate = new Element("template").setAttribute("class",
                    "footer");
            getElement().appendChild(footerTemplate);
        }

        setupHeaderOrFooter(false, renderer, footerTemplate);
        return (T) this;
    }

    private void setupHeaderOrFooter(boolean header,
            TemplateRenderer<?> renderer, Element headerOrFooter) {
        if (renderer instanceof ComponentRenderer) {
            GridTemplateRendererUtil.setupHeaderOrFooterComponentRenderer(this,
                    (ComponentRenderer) renderer);
        }

        headerOrFooter.setProperty("innerHTML",
                header ? getHeaderRendererTemplate(renderer)
                        : getFooterRendererTemplate(renderer));
        GridTemplateRendererUtil.setupTemplateRenderer(
                (TemplateRenderer) renderer, headerOrFooter, getElement(),
                getGrid().getDataGenerator(), () -> (DataKeyMapper) getGrid()
                        .getDataCommunicator().getKeyMapper());
    }

    protected String getHeaderRendererTemplate(TemplateRenderer<?> renderer) {
        return renderer.getTemplate();
    }

    protected String getFooterRendererTemplate(TemplateRenderer<?> renderer) {
        return renderer.getTemplate();
    }

}
