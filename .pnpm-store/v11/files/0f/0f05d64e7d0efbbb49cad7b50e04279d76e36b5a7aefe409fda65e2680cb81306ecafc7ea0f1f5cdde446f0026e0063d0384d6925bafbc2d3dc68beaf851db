/**
 * Check if a CSS custom property is registered.
 * This function will return `false` for custom properties that behave identically to non-registered properties (e.g., registered inherited properties with syntax "*").
 * @param {string} property - The property to check.
 * @param {Window} [window] - The window to check in.
 * @returns {boolean}
 */
export default function isRegisteredProperty (property, window = globalThis) {
	let document = window.document;

	let dummy = document.createElement("div");
	document.body.append(dummy);

	let invalidValue = "foo(bar)"; // a value that is invalid for any registered syntax
	dummy.style.setProperty(property, invalidValue);
	let value = getComputedStyle(dummy).getPropertyValue(property);

	let ret = true;
	if (value === invalidValue) {
		// We might have either unregistered or registered custom property with syntax "*".
		// If it's non-inherited, we can be sure it's registered.
		// But if it's inherited, it's OK if we (re-)register it with syntax "*" in any case.
		let child = dummy.appendChild(document.createElement("div"));
		let inheritedValue = getComputedStyle(child).getPropertyValue(property);
		ret = inheritedValue !== invalidValue;
	}

	dummy.remove();

	return ret;
}
