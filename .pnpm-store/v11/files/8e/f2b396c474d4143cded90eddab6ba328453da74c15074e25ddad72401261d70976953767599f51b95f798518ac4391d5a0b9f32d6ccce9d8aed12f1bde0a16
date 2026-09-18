/**
 * Monitor the presence of an element in the document.
 * This observer fires the callback in situations like:
 * - The element is added to the DOM
 * - The element gets slotted or its slot starts existing (but not when moved to another slot)
 * - The element becomes visible from display: none
 */
export default class RenderedObserver {
    constructor(callback: any);
    callback: any;
    /**
     * Begin observing the presence of an element.
     * @param {Element} element - The element to observe.
     */
    observe(element: Element): void;
    /**
     * Stop observing the presence of an element.
     * @param {Element} [element] - The element to stop observing. If not provided, all targets will be unobserved.
     */
    unobserve(element?: Element): void;
    #private;
}
