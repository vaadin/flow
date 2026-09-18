/**
 * Monitor the presence of an element in the document.
 * This observer fires the callback in situations like:
 * - The element is added to the DOM
 * - The element gets slotted or its slot starts existing (but not when moved to another slot)
 * - The element becomes visible from display: none
 */

export default class RenderedObserver {
	/**
	 * All currently observed targets
	 * @type {WeakSet<Element>}
	 */
	#targets = new Set();

	/**
	 * Documents to IntersectionObserver instances
	 * @type {WeakMap<Document, IntersectionObserver>}
	 */
	#intersectionObservers = new WeakMap();

	constructor (callback) {
		this.callback = callback;
	}

	/**
	 * Begin observing the presence of an element.
	 * @param {Element} element - The element to observe.
	 */
	observe (element) {
		if (this.#targets.has(element)) {
			// Already observing this element
			return;
		}

		let doc = element.ownerDocument;
		let io = this.#intersectionObservers.get(doc);

		if (!io) {
			io = new IntersectionObserver(
				entries => {
					this.callback(entries.map(({ target }) => ({ target })));
				},
				{ root: doc.documentElement },
			);

			this.#intersectionObservers.set(doc, io);
		}

		this.#targets.add(element);
		io.observe(element);
	}

	/**
	 * Stop observing the presence of an element.
	 * @param {Element} [element] - The element to stop observing. If not provided, all targets will be unobserved.
	 */
	unobserve (element) {
		if (!element) {
			// Unobserve all targets
			for (const target of this.#targets) {
				this.unobserve(target);
			}
			return;
		}

		let doc = element.ownerDocument;
		let io = this.#intersectionObservers.get(doc);

		io?.unobserve(element);
		this.#targets.delete(element);
	}
}
