/**
 * @license
 * Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
 * This code may only be used under the BSD style license found at
 * http://polymer.github.io/LICENSE.txt
 * The complete set of authors may be found at
 * http://polymer.github.io/AUTHORS.txt
 * The complete set of contributors may be found at
 * http://polymer.github.io/CONTRIBUTORS.txt
 * Code distributed by Google as part of the polymer project is also
 * subject to an additional IP rights grant found at
 * http://polymer.github.io/PATENTS.txt
 */
/**
 * The global namespace of features contained in an analysis.
 *
 * Top-level members are unnamespaced, everything else is contained
 * in a namespace. Often an analysis will contain only one namespace at
 * the root.
 */
export interface Analysis {
    schema_version: string;
    /** All elements found. */
    elements?: Element[];
    /** All toplevel functions found. */
    functions?: Function[];
    /** All element mixins found. */
    mixins?: ElementMixin[];
    /** All toplevel namespaces found. */
    namespaces?: Namespace[];
    /**
     * All toplevel classes found that were not covered by one of the other types.
     *
     * e.g. classes that are elements are only found in `elements`
     */
    classes?: Class[];
    /**
     * An extension point for framework-specific metadata, as well as any
     * metadata not yet standardized here such as what polyfills are needed,
     * behaviors and mixins used, the framework that the package was written in,
     * tags/categories, framework config files, etc.
     *
     * Framework-specific metadata should be put into a sub-object with the name
     * of that framework.
     */
    metadata?: any;
}
/**
 * The base interface, holding properties common to many nodes.
 */
export interface Feature {
    /** Where this feature is defined in source code. */
    sourceRange?: SourceRange;
    /**
     * An extension point for framework-specific metadata, as well as any
     * metadata not yet standardized here such as what polyfills are needed,
     * behaviors and mixins used, the framework that the element was written in,
     * tags/categories, links to specs that the element implements, etc.
     *
     * Framework-specific metadata should be put into a sub-object with the name
     * of that framework.
     */
    metadata?: any;
}
export interface SourceRange {
    /**
     * Path to the file containing the definition of the feature,
     * relative to the parent feature, or the package if the feature has no parent
     * (e.g. elements).
     *
     * If blank, the feature is defined in the same file as its parent.
     */
    file?: string;
    start: Position;
    end: Position;
}
export interface Position {
    /** Line number, starting from zero. */
    line: number;
    /** Column offset within the line, starting from zero. */
    column: number;
}
export declare type Privacy = 'public' | 'private' | 'protected';
export interface Function extends Feature {
    /**
     * The globally accessible property-path of the namespace. e.q. `Polymer.dom`
     */
    name: string;
    /** A markdown description for the namespace. */
    description?: string;
    summary?: string;
    params?: {
        name: string;
        type?: string;
    }[];
    return?: {
        type?: string;
        desc?: string;
    };
    privacy: Privacy;
}
export interface Namespace extends Feature {
    /**
     * The globally accessible property-path of the namespace. e.q. `Polymer.dom`
     */
    name: string;
    /** A markdown description for the namespace. */
    description?: string;
    /** A markdown summary for the namespace. */
    summary?: string;
    elements?: Element[];
    functions?: Function[];
    mixins?: ElementMixin[];
    namespaces?: Namespace[];
    classes?: Class[];
}
export interface Class extends Feature {
    /** The name of the class. */
    name?: string;
    /**
     * The path, relative to the base directory of the package.
     *
     * e.g. `paper-input.html` or `app-toolbar/app-toolbar.html` (given that
     * app-toolbar lives in the app-layout package).
     */
    path: string | undefined;
    /** A markdown description. */
    description: string;
    /** A markdown summary. */
    summary: string;
    /**
     * Paths, relative to the base directory of the package, to demo pages for
     * this feauture.
     *
     * e.g. `[{url: 'demos/index.html', description: 'How it works'}, ...]`
     */
    demos: Demo[];
    /** Names of mixins applied.  */
    mixins?: string[];
    /** The properties that this feature has. */
    properties?: Property[];
    /** The instance methods that this feature has. */
    methods?: Method[];
    /** The static, class-level methods that this feature has. */
    staticMethods?: Method[];
    privacy: Privacy;
    /**
     * The class, if any, that this class extends.
     */
    superclass?: string;
}
export interface ElementLike extends Class {
    /** The attributes that this element is known to understand. */
    attributes?: Attribute[];
    /** The events that this element fires. */
    events?: Event[];
    /** The shadow dom content slots that this element accepts. */
    'slots': Slot[];
    /** Information useful for styling the element and its children. */
    styling: {
        /** CSS selectors that the element recognizes on itself for styling. */
        selectors: {
            /** The CSS selector. e.g. `.bright`, `[height=5]`, `[cascade]`. */
            value: string;
            /**
             * A markdown description of the effect of this selector matching
             * on the element.
             */
            description: string;
        }[];
        /** CSS Variables that the element understands. */
        cssVariables: {
            /** The name of the variable. e.g. `--header-color`, `--my-element-size`*/
            name: string;
            /** The type of the variable. Advisory. e.g. `color`, `size` */
            type?: string;
            /** A markdown description of the variable. */
            description?: string;
            /**
             * A markdown description of how the element will fallback if the variable
             * isn't defined.
             */
            fallbackBehavior?: string;
        }[];
        /** If true, the element must be given an explicit size by its context. */
        needsExplicitSize?: boolean;
    };
}
export interface Element extends ElementLike {
    /** The tagname that the element registers itself as. e.g. `paper-input` */
    tagname?: string;
    /**
     * The class name for this element.
     *
     * e.g. `MyElement`, `Polymer.PaperInput`
     */
    name?: string;
    /**
     * The tagname that the element extends, if any. The value of the `extends`
     * option that's passed into `customElements.define`.
     *
     * e.g. `input`, `paper-button`, `my-super-element`
     */
    extends?: string;
    /**
     * The class that this element extends.
     *
     * This is non-optional, as every custom element must have HTMLElement in
     * its prototype change.
     *
     * e.g. `HTMLElement`, `HTMLInputElement`, `MyNamespace.MyBaseElement`
     */
    superclass: string;
}
export interface ElementMixin extends ElementLike {
    /**
     * The name for this mixin.
     *
     * e.g. `MyMixin`, `Polymer.PaperInputMixin`
     */
    name: string;
}
export interface Attribute extends Feature {
    /** The name of the attribute. e.g. `value`, `icon`, `should-collapse`. */
    name: string;
    /** A markdown description for the attribute. */
    description?: string;
    /**
     * The type that the attribute will be serialized/deserialized as.
     *
     * e.g. `string`, `number`, `boolean`, `RegExp`, `Array`, `Object`.
     */
    type?: string;
    /**
     * The default value of the attribute, if any.
     *
     * As attributes are always strings, this is the actual value, not a human
     * readable description.
     */
    defaultValue?: string;
    /** The identifier of the class or mixin that declared this property. */
    inheritedFrom?: string;
}
export interface Property extends Feature {
    /** The name of the property. e.g. `value`, `icon`, `shouldCollapse`. */
    name: string;
    /** A markdown description of the property. */
    description: string;
    /**
     * The javascript type of the property.
     *
     * There's no standard here. Common choices are closure compiler syntax
     * and typescript syntax.
     */
    type: string;
    /**
     * A string representation of the default value. Intended only to be human
     * readable, so may be a description, an identifier name, etc.
     */
    defaultValue?: string;
    /** Nested subproperties hanging off of this property. */
    properties?: Property[];
    privacy: Privacy;
    /** The identifier of the class or mixin that declared this property. */
    inheritedFrom?: string;
}
export interface Method extends Feature {
    /** The name of the property. e.g. `value`, `icon`, `shouldCollapse`. */
    name: string;
    /** A markdown description of the property. */
    description: string;
    /**
     * An array of data objects describing the method signature. This data may be
     * incomplete. For example, only argument names can be detected from an
     * undocumented JavaScript function. Argument types can only be read from
     * associated JSDoc via @param tags
     */
    params?: Parameter[];
    /**
     * Data describing the method return type. This data may be incomplete.
     * For example, the return type can be detected from a documented JavaScript
     * function with associated JSDoc and a @return tag.
     */
    return?: {
        type?: string;
        desc?: string;
    };
    privacy: Privacy;
    /** The identifier of the class or mixin that declared this property. */
    inheritedFrom?: string;
}
export interface Parameter {
    name: string;
    type?: string;
    description?: string;
}
export interface Event extends Feature {
    /** The name of the event. */
    name: string;
    /** A markdown description of the event. */
    description: string;
    /**
     * The type of the event object that's fired.
     *
     * e.g. `Event`, `CustomEvent`, `KeyboardEvent`, `MyCustomEvent`.
     */
    type: string;
    /** Information about the `detail` field of the event. */
    detail?: {
        properties: Property[];
    };
    /** The identifier of the class or mixin that declared this property. */
    inheritedFrom?: string;
}
export interface Slot extends Feature {
    /** The name of the slot. e.g. `banner`, `body`, `tooltipContents` */
    name: string;
    /** A markdown description of the slot. */
    description: string;
}
export interface Demo {
    /** A markdown description of the demo. */
    description?: string;
    /** Relative URL of the demo. */
    url: string;
}
