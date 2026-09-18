/**
 * Resolve the observer options.
 * @param {StyleObserverOptions} options
 * @returns {StyleObserverOptionsObject}
 */
export function resolveOptions(options: StyleObserverOptions): StyleObserverOptionsObject;
/**
 * @typedef { object } StyleObserverOptionsObject
 * @property { string[] } properties - The properties to observe.
 */
/**
 * @typedef { StyleObserverOptionsObject | string | string[] } StyleObserverOptions
 */
/**
 * @callback StyleObserverCallback
 * @param {Record[]} records
 * @returns {void}
 */
/**
 * @typedef { Object } Record
 * @property {Element} target - The element that changed.
 * @property {string} property - The property that changed.
 * @property {string} value - The new value of the property.
 * @property {string} oldValue - The old value of the property.
 */
export default class ElementStyleObserver {
    /** All properties ever observed by this class. */
    static properties: Set<any>;
    /**
     * All instances ever observed by this class.
     */
    static all: MultiWeakMap;
    /**
     * All root nodes that have observed elements.
     * @type {WeakSet<Document|ShadowRoot>}
     */
    static rootNodes: WeakSet<Document | ShadowRoot>;
    /**
     * @param {Element} target
     * @param {StyleObserverCallback} callback
     * @param {StyleObserverOptions} options
     */
    constructor(target: Element, callback: StyleObserverCallback, options?: StyleObserverOptions);
    /**
     * Observed properties to their old values.
     * @type {Map<string, string>}
     */
    properties: Map<string, string>;
    /**
     * Get the names of all properties currently being observed.
     * @type { string[] }
     */
    get propertyNames(): string[];
    /**
     * The element being observed.
     * @type {Element}
     */
    target: Element;
    /**
     * The callback to call when the element's style changes.
     * @type {StyleObserverCallback}
     */
    callback: StyleObserverCallback;
    /**
     * The observer options.
     * @type {StyleObserverOptions}
     */
    options: StyleObserverOptions;
    renderedObserver: RenderedObserver;
    resolveOptions(options: any): StyleObserverOptionsObject & StyleObserverOptions;
    /**
     * Handle a potential property change
     * @private
     * @param {TransitionEvent} [event]
     */
    private handleEvent;
    /**
     * Observe the target for changes to one or more CSS properties.
     * @param {string | string[]} properties
     * @return {void}
     */
    observe(properties: string | string[]): void;
    /**
     * Update the `--style-observer-transition` property to include all observed properties.
     */
    updateTransitionProperties(): void;
    /**
     * Update the target's transition property or refresh it if it was overwritten.
     * @param {object} options
     * @param {boolean} [options.firstTime] - Whether this is the first time the transition is being set.
     */
    updateTransition({ firstTime }?: {
        firstTime?: boolean;
    }): void;
    /**
     * Set (ones per root) CSS on an element's root node (document or shadow root).
     */
    initRootNode(): void;
    /**
     * Stop observing a target for changes to one or more CSS properties.
     * @param { string | string[] } [properties] Properties to stop observing. Defaults to all observed properties.
     * @return {void}
     */
    unobserve(properties?: string | string[]): void;
    #private;
}
export type StyleObserverOptionsObject = {
    /**
     * - The properties to observe.
     */
    properties: string[];
};
export type StyleObserverOptions = StyleObserverOptionsObject | string | string[];
export type StyleObserverCallback = (records: Record[]) => void;
export type Record = {
    /**
     * - The element that changed.
     */
    target: Element;
    /**
     * - The property that changed.
     */
    property: string;
    /**
     * - The new value of the property.
     */
    value: string;
    /**
     * - The old value of the property.
     */
    oldValue: string;
};
import RenderedObserver from "./rendered-observer.js";
import MultiWeakMap from "./util/MultiWeakMap.js";
