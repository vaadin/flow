/**
 * @typedef {import("./element-style-observer.js").StyleObserverCallback} StyleObserverCallback
 */
/**
 * @typedef { Object } StyleObserverOptions
 * @property {string | string[]} [properties] - The properties to observe.
 * @property {Element | Element[]} [targets] - The elements to observe.
 */
export default class StyleObserver {
    /**
     * @param {StyleObserverCallback} callback
     * @param {StyleObserverOptions | string | string[]} options
     */
    constructor(callback: StyleObserverCallback, options: StyleObserverOptions | string | string[]);
    /**
     * @type { WeakMap<Element, ElementStyleObserver> }
     */
    elementObservers: WeakMap<Element, ElementStyleObserver>;
    callback: import("./element-style-observer.js").StyleObserverCallback;
    options: StyleObserverOptions;
    changed(records: import("./element-style-observer.js").Record[]): void;
    /**
     * Observe one or more targets for changes to one or more CSS properties.
     * @param {Element | Element[]} targets
     * @param {string | string[]} properties
     *
     * @overload
     * @param {string | string[]} properties
     * @param {Element | Element[]} targets
    
     * @return {void}
     */
    observe(properties: string | string[], targets: Element | Element[]): void;
    /**
     * Stop observing one or more targets for changes to one or more CSS properties.
     * @param {Element | Element[]} targets
     * @param {string | string[]} properties
     *
     * @overload
     * @param {string | string[]} properties
     * @param {Element | Element[]} targets
     * @return {void}
     */
    unobserve(properties: string | string[], targets: Element | Element[]): void;
    /**
     * Update the transition for one or more targets.
     * @param {Element | Element[]} targets
     * @returns {void}
     */
    updateTransition(targets: Element | Element[]): void;
}
export type StyleObserverCallback = import("./element-style-observer.js").StyleObserverCallback;
export type StyleObserverOptions = {
    /**
     * - The properties to observe.
     */
    properties?: string | string[];
    /**
     * - The elements to observe.
     */
    targets?: Element | Element[];
};
import ElementStyleObserver from "./element-style-observer.js";
