/**
 * Convert a value to an array. `undefined` and `null` values are converted to an empty array.
 * @param {*} value - The value to convert.
 * @returns {any[]} The converted array.
 */
export function toArray(value) {
	if (value === undefined || value === null) {
		return [];
	}

	if (Array.isArray(value)) {
		return value;
	}

	// Don't convert "foo" into ["f", "o", "o"]
	if (typeof value !== "string" && typeof value[Symbol.iterator] === "function") {
		return Array.from(value);
	}

	return [value];
}

/**
 * Wait for a given number of milliseconds or a `requestAnimationFrame`.
 * @param {number} ms - The number of milliseconds to wait.
 * @returns {Promise<void>}
 */
export function wait (ms) {
	if (ms) {
		return new Promise(resolve => setTimeout(resolve, ms));
	}

	return new Promise(resolve => requestAnimationFrame(resolve));
}

let dummy;

/**
 * Get the longhands for a given property.
 * @param {string} property - The property to get the longhands for.
 * @returns {string[]} The longhands.
 * @see https://lea.verou.me/blog/2020/07/introspecting-css-via-the-css-om-getting-supported-properties-shorthands-longhands/
 */
export function getLonghands (property) {
	dummy ??= document.createElement("div");
	let style = dummy.style;
	style[property] = "inherit"; // a value that works in every property
	let ret = [...style];

	if (ret.length === 0) {
		// Fallback, in case
		ret = [property];
	}

	style.cssText = ""; // clean up

	return ret;
}

/**
 * Parse a CSS `<time>` value.
 * @param {string } cssTime - A string that contains CSS `<time>` values.
 * @return { number[] } Any times found, in milliseconds.
 */
export function parseTimes (cssTime) {
	let matches = cssTime.matchAll(/(?:^|\s)([+-]?(?:\d+|\d*\.\d+))\s*(ms|s)?(?=\s|$)/g);
	let ret = [];

	for (let match of matches) {
		let [, value, unit] = match;
		value = parseFloat(value);

		if (unit === "s") {
			value *= 1000;
		}

		ret.push(value);
	}

	return ret;
}

/**
 * Get the duration and delay of a CSS transition for a given property.
 * @param {string} property - The CSS property name.
 * @param {string} transitions - The computed value of the `transition` property.
 * @returns { { duration: number, delay: number } } The duration and delay, in milliseconds.
 */
export function getTimesFor (property, transitions) {
	transitions = splitCommas(transitions);
	let propertyRegex;

	if (property === "all") {
		propertyRegex = /\b\w+\b/g;
	}
	else {
		let properties = [...new Set([...getLonghands(property), property, "all"])];
		propertyRegex = RegExp(`(?:^|\\s)(${ properties.join("|") })\\b`);
	}

	let lastRelevantTransition = transitions.findLast(transition => propertyRegex.test(transition));
	let times = lastRelevantTransition ? parseTimes(lastRelevantTransition) : [0, 0];

	if (times.length === 0) {
		times = [0, 0];
	}
	else if (times.length === 1) {
		times.push(0);
	}

	let [duration, delay] = times;
	return { duration, delay };
}

/**
 * Split a value by commas, ignoring commas within parentheses and trimming whitespace.
 * @param {string} value - The value to split.
 * @returns {string[]} The split values.
 */
export function splitCommas (value) {
	let ret = [];
	let lastIndex = 0;
	let stack = [];

	for (let match of value.matchAll(/[,()]/g)) {
		let char = match[0];

		if (char === ",") {
			if (stack.length === 0) {
				let item = value.slice(lastIndex, match.index);
				ret.push(item.trim());
				lastIndex = match.index + 1;
			}
		}
		else if (char === "(") {
			stack.push("(");
		}
		else if (char === ")") {
			stack.pop();
		}
	}

	if (lastIndex < value.length) {
		// Push any remaining string
		let item = value.slice(lastIndex);
		ret.push(item.trim());
	}

	return ret;
}
