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
package com.vaadin.generated.paper.toolbar;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * This element has been deprecated in favor of
 * [app-layout](https://github.com/PolymerElements/app-layout).**
 * 
 * Material design:
 * [Toolbars](https://www.google.com/design/spec/components/toolbars.html)
 * 
 * {@code paper-toolbar} is a horizontal bar containing items that can be used
 * for label, navigation, search and actions. The items placed inside the
 * {@code paper-toolbar} are projected into a
 * {@code class="horizontal center layout"} container inside of
 * {@code paper-toolbar}'s Shadow DOM. You can use flex attributes to control
 * the items' sizing.
 * 
 * Example:
 * 
 * {@code }`html <paper-toolbar> <paper-icon-button icon="menu"
 * on-tap="menuAction"></paper-icon-button> <div class="title">Title</div>
 * <paper-icon-button icon="more-vert" on-tap="moreAction"></paper-icon-button>
 * </paper-toolbar> {@code }`
 * 
 * {@code paper-toolbar} has a standard height, but can made be taller by
 * setting {@code tall} class on the {@code paper-toolbar}. This will make the
 * toolbar 3x the normal height.
 * 
 * {@code }`html <paper-toolbar class="tall"> <paper-icon-button
 * icon="menu"></paper-icon-button> </paper-toolbar> {@code }`
 * 
 * Apply {@code medium-tall} class to make the toolbar medium tall. This will
 * make the toolbar 2x the normal height.
 * 
 * {@code }`html <paper-toolbar class="medium-tall"> <paper-icon-button
 * icon="menu"></paper-icon-button> </paper-toolbar> {@code }`
 * 
 * When {@code tall}, items can pin to either the top (default), middle or
 * bottom. Use {@code middle} slot for middle content and {@code bottom} slot
 * for bottom content.
 * 
 * {@code }`html <paper-toolbar class="tall"> <paper-icon-button
 * icon="menu"></paper-icon-button> <div slot="middle" class="title">Middle
 * Title</div> <div slot="bottom" class="title">Bottom Title</div>
 * </paper-toolbar> {@code }`
 * 
 * For {@code medium-tall} toolbar, the middle and bottom contents overlap and
 * are pinned to the bottom. But {@code middleJustify} and {@code bottomJustify}
 * attributes are still honored separately.
 * 
 * To make an element completely fit at the bottom of the toolbar, use
 * {@code fit} along with {@code bottom}.
 * 
 * {@code }`html <paper-toolbar class="tall"> <div id="progressBar" slot="bottom"
 * class="fit"></div> </paper-toolbar> {@code }`
 * 
 * When inside a {@code paper-header-panel} element with
 * {@code mode="waterfall-tall"}, the class {@code .animate} is toggled to
 * animate the height change in the toolbar.
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- {@code --paper-toolbar-title} |
 * Mixin applied to the title of the toolbar | {@code
 * {@code --paper-toolbar-background} | Toolbar background color |
 * {@code --primary-color} {@code --paper-toolbar-color} | Toolbar foreground
 * color | {@code --dark-theme-text-color} {@code --paper-toolbar-height} |
 * Custom height for toolbar | {@code 64px} {@code --paper-toolbar-sm-height} |
 * Custom height for small screen toolbar | {@code 56px} {@code --paper-toolbar}
 * | Mixin applied to the toolbar | {@code {@code --paper-toolbar-content} |
 * Mixin applied to the content section of the toolbar | {@code
 * {@code --paper-toolbar-medium} | Mixin applied to medium height toolbar |
 * {@code {@code --paper-toolbar-tall} | Mixin applied to tall height toolbar |
 * {@code {@code --paper-toolbar-transition} | Transition applied to the
 * {@code .animate} class | {@code height 0.18s ease-in}
 * 
 * ### Accessibility
 * 
 * {@code <paper-toolbar>} has {@code role="toolbar"} by default. Any elements
 * with the class {@code title} will be used as the label of the toolbar via
 * {@code aria-labelledby}.
 * 
 * ### Breaking change in 2.0
 * 
 * In Polymer 1.x, default content used to be distribuited automatically to the
 * top toolbar. In v2, the you must set {@code slot="top"} on the default
 * content to distribuite the content to the top toolbar.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.14-SNAPSHOT",
		"WebComponent: paper-toolbar#2.0.0", "Flow#0.1.14-SNAPSHOT"})
@Tag("paper-toolbar")
@HtmlImport("frontend://bower_components/paper-toolbar/paper-toolbar.html")
public class GeneratedPaperToolbar<R extends GeneratedPaperToolbar<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Controls how the items are aligned horizontally when they are placed at
	 * the bottom. Options are {@code start}, {@code center}, {@code end},
	 * {@code justified} and {@code around}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getBottomJustify() {
		return getElement().getProperty("bottomJustify");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Controls how the items are aligned horizontally when they are placed at
	 * the bottom. Options are {@code start}, {@code center}, {@code end},
	 * {@code justified} and {@code around}.
	 * 
	 * @param bottomJustify
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setBottomJustify(java.lang.String bottomJustify) {
		getElement().setProperty("bottomJustify",
				bottomJustify == null ? "" : bottomJustify);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Controls how the items are aligned horizontally. Options are
	 * {@code start}, {@code center}, {@code end}, {@code justified} and
	 * {@code around}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getJustify() {
		return getElement().getProperty("justify");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Controls how the items are aligned horizontally. Options are
	 * {@code start}, {@code center}, {@code end}, {@code justified} and
	 * {@code around}.
	 * 
	 * @param justify
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setJustify(java.lang.String justify) {
		getElement().setProperty("justify", justify == null ? "" : justify);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Controls how the items are aligned horizontally when they are placed in
	 * the middle. Options are {@code start}, {@code center}, {@code end},
	 * {@code justified} and {@code around}.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getMiddleJustify() {
		return getElement().getProperty("middleJustify");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Controls how the items are aligned horizontally when they are placed in
	 * the middle. Options are {@code start}, {@code center}, {@code end},
	 * {@code justified} and {@code around}.
	 * 
	 * @param middleJustify
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setMiddleJustify(java.lang.String middleJustify) {
		getElement().setProperty("middleJustify",
				middleJustify == null ? "" : middleJustify);
		return get();
	}

	/**
	 * Adds the given components as children of this component at the slot
	 * 'top'.
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
	public void addToTop(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "top");
			getElement().appendChild(component.getElement());
		}
	}

	/**
	 * Adds the given components as children of this component at the slot
	 * 'middle'.
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
	public void addToMiddle(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "middle");
			getElement().appendChild(component.getElement());
		}
	}

	/**
	 * Adds the given components as children of this component at the slot
	 * 'bottom'.
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
	public void addToBottom(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "bottom");
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

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	@Override
	public R get() {
		return (R) this;
	}
}