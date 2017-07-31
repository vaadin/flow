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
package com.vaadin.generated.vaadin.split.layout;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <vaadin-split-layout>} is a Polymer element implementing a split
 * layout for two content elements with a draggable splitter between them.
 * 
 * {@code }`html <vaadin-split-layout> <div>First content element</div>
 * <div>Second content element</div> </vaadin-split-layout> {@code }`
 * 
 * ### Horizontal and Vertical Layouts
 * 
 * By default, the split is horizontal, meaning that the content elements are
 * positioned side by side in a flex container with a horizontal layout.
 * 
 * You can change the split mode to vertical by adding the {@code vertical}
 * attribute:
 * 
 * {@code }`html <vaadin-split-layout vertical> <div>Content on the top</div>
 * <div>Content on the bottom</div> </vaadin-split-layout> {@code }`
 * 
 * ### Layouts Combination
 * 
 * For the layout contents, we usually use {@code <div>} elements in the
 * examples, although you can use any other elements as well.
 * 
 * For instance, in order to have a nested vertical split layout inside a
 * horizontal one, you can include {@code <vaadin-split-layout>} as a content
 * element inside another split layout:
 * 
 * {@code }`html <vaadin-split-layout> <div>First content element</div>
 * <vaadin-split-layout vertical> <div>Second content element</div> <div>Third
 * content element</div> </vaadin-split-layout> </vaadin-split-layout> {@code }`
 * 
 * You can also trigger the vertical mode by setting the property:
 * {@code splitLayout.vertical = true;}.
 * 
 * ### Split Layout Element Height
 * 
 * {@code <vaadin-split-layout>} element itself is a flex container. It does not
 * inherit the parent height by default, but rather sets its height depending on
 * the content.
 * 
 * You can use CSS to set the fixed height for the split layout, as usual with
 * any block element:
 * 
 * {@code }`html <vaadin-split-layout style="height: 200px;"> <div>First content
 * element</div> <div>Second content element</div> </vaadin-split-layout>
 * {@code }`
 * 
 * It is possible to define percentage height as well. Note that you have to set
 * the parent height in order to make percentages work correctly. In the
 * following example, the {@code <body>} is resized to fill the entire viewport,
 * and the {@code <vaadin-split-layout>} element is set to take 100% height of
 * the {@code <body>}:
 * 
 * {@code }`html <body style="height: 100vh; margin: 0;"> <vaadin-split-layout
 * style="height: 100%;"> <div>First</div> <div>Second</div>
 * </vaadin-split-layout> </body> {@code }`
 * 
 * Alternatively, you can use a flexbox layout to make
 * {@code <vaadin-split-layout>} fill up the parent:
 * 
 * {@code }`html <body style="height: 100vh; margin: 0; display: flex;">
 * <vaadin-split-layout style="flex: 1;"> <div>First</div> <div>Second</div>
 * </vaadin-split-layout> </body> {@code }`
 * 
 * ### Initial Splitter Position
 * 
 * The initial splitter position is determined from the sizes of the content
 * elements inside the split layout. Therefore, changing {@code width} on the
 * content elements affects the initial splitter position for the horizontal
 * layouts, while {@code height} affects the vertical ones.
 * 
 * Note that when the total size of the content elements does not fit the
 * layout, the content elements are scaled proportionally.
 * 
 * When setting initial sizes with relative units, such as percentages, it is
 * recommended to assing the size for both content elements:
 * 
 * {@code }`html <vaadin-split-layout> <div style="width: 75%;">Three
 * fourths</div> <div style="width: 25%;">One fourth</div>
 * </vaadin-split-layout> {@code }`
 * 
 * ### Size Limits
 * 
 * The {@code min-width}/{@code min-height}, and {@code max-width}/
 * {@code max-height} CSS size values for the content elements are respected and
 * used to limit the splitter position when it is dragged.
 * 
 * It is preferred to set the limits only for a single content element, in order
 * to avoid size conflicts:
 * 
 * {@code }`html <vaadin-split-layout> <div
 * style="min-width: 50px; max-width: 150px;">First</div> <div>Second</div>
 * </vaadin-split-layout> {@code }`
 * 
 * ### Resize Notification
 * 
 * This element implements {@code IronResizableBehavior} to notify the nested
 * resizables when the splitter is dragged. In order to define a resizable and
 * receive that notification in a nested element, include
 * {@code IronResizableBehavior} and listen for the {@code iron-resize} event.
 * 
 * ### Styling
 * 
 * The following shadow DOM parts are available for styling:
 * 
 * Part name | Description | Theme for Element
 * ----------------|----------------|---------------- {@code splitter} | Split
 * element | vaadin-split-layout {@code handle} | The handle of the splitter |
 * vaadin-split-layout
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.16-SNAPSHOT",
		"WebComponent: Vaadin.SplitLayoutElement#3.0.0-alpha1",
		"Flow#0.1.16-SNAPSHOT"})
@Tag("vaadin-split-layout")
@HtmlImport("frontend://bower_components/vaadin-split-layout/vaadin-split-layout.html")
public class GeneratedVaadinSplitLayout<R extends GeneratedVaadinSplitLayout<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Change the split layout to vertical
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isVertical() {
		return getElement().getProperty("vertical", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Change the split layout to vertical
	 * 
	 * @param vertical
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setVertical(boolean vertical) {
		getElement().setProperty("vertical", vertical);
		return get();
	}

	@DomEvent("iron-resize")
	public static class IronResizeEvent<R extends GeneratedVaadinSplitLayout<R>>
			extends
				ComponentEvent<R> {
		public IronResizeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addIronResizeListener(
			ComponentEventListener<IronResizeEvent<R>> listener) {
		return addListener(IronResizeEvent.class,
				(ComponentEventListener) listener);
	}

	/**
	 * Adds the given components as children of this component at the slot
	 * 'primary'.
	 * 
	 * @param components
	 *            The components to add.
	 * @see <a
	 *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
	 *      page about slots</a>
	 * @see <a
	 *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
	 *      website about slots</a>
	 */
	public void addToPrimary(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "primary");
			getElement().appendChild(component.getElement());
		}
	}

	/**
	 * Adds the given components as children of this component at the slot
	 * 'secondary'.
	 * 
	 * @param components
	 *            The components to add.
	 * @see <a
	 *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
	 *      page about slots</a>
	 * @see <a
	 *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
	 *      website about slots</a>
	 */
	public void addToSecondary(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "secondary");
			getElement().appendChild(component.getElement());
		}
	}

	/**
	 * Removes the given child components from this component.
	 * 
	 * @param components
	 *            The components to remove.
	 * @throws IllegalArgumentException
	 *             if any of the components is not a child of this component.
	 */
	public void remove(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			if (getElement().equals(component.getElement().getParent())) {
				component.getElement().removeAttribute("slot");
				getElement().removeChild(component.getElement());
			} else {
				throw new IllegalArgumentException("The given component ("
						+ component + ") is not a child of this component");
			}
		}
	}

	/**
	 * Removes all contents from this component, this includes child components,
	 * text content as well as child elements that have been added directly to
	 * this component using the {@link Element} API.
	 */
	public void removeAll() {
		getElement().getChildren().forEach(
				child -> child.removeAttribute("slot"));
		getElement().removeAllChildren();
	}
}