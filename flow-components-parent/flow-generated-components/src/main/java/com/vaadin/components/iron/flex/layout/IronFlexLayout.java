package com.vaadin.components.iron.flex.layout;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * The {@code <iron-flex-layout>} component provides simple ways to use [CSS
 * flexible box layout](https://developer.mozilla.org/en-US/docs/Web/Guide/CSS/
 * Flexible_boxes), also known as flexbox. This component provides two different
 * ways to use flexbox:
 * 
 * 1. [Layout
 * classes](https://github.com/PolymerElements/iron-flex-layout/tree/master
 * /iron-flex-layout-classes.html). The layout class stylesheet provides a
 * simple set of class-based flexbox rules, that let you specify layout
 * properties directly in markup. You must include this file in every element
 * that needs to use them.
 * 
 * Sample use:
 * 
 * {@code }` <custom-element-demo> <template> <script
 * src="../webcomponentsjs/webcomponents-lite.js"></script>
 * <next-code-block></next-code-block> </template> </custom-element-demo>
 * {@code }`
 * 
 * {@code }`html <link rel="import" href="iron-flex-layout-classes.html"> <style
 * is="custom-style" include="iron-flex iron-flex-alignment"></style> <style>
 * .test { width: 100px; } </style> <div
 * class="layout horizontal center-center"> <div class="test">horizontal layout
 * center alignment</div> </div> {@code }`
 * 
 * 2. [Custom CSS
 * mixins](https://github.com/PolymerElements/iron-flex-layout/blob
 * /master/iron-flex-layout.html). The mixin stylesheet includes custom CSS
 * mixins that can be applied inside a CSS rule using the {@code @apply}
 * function.
 * 
 * Please note that the old [/deep/ layout
 * classes](https://github.com/PolymerElements
 * /iron-flex-layout/tree/master/classes) are deprecated, and should not be
 * used. To continue using layout properties directly in markup, please switch
 * to using the new {@code dom-module}-based [layout
 * classes](https://github.com/
 * PolymerElements/iron-flex-layout/tree/master/iron-flex-layout-classes.html).
 * Please note that the new version does not use {@code /deep/}, and therefore
 * requires you to import the {@code dom-modules} in every element that needs to
 * use them.
 * 
 * A complete [guide](https://elements.polymer-project.org/guides/flex-layout)
 * to {@code <iron-flex-layout>} is available.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.10-SNAPSHOT",
		"WebComponent: iron-flex-layout#2.0.0", "Flow#0.1.10-SNAPSHOT"})
@Tag("iron-flex-layout")
@HtmlImport("frontend://bower_components/iron-flex-layout/iron-flex-layout.html")
public class IronFlexLayout<R extends IronFlexLayout<R>> extends Component {

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected R getSelf() {
		return (R) this;
	}
}