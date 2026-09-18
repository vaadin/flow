/**
 * Convert a value to an array. `undefined` and `null` values are converted to an empty array.
 * @param {*} value - The value to convert.
 * @returns {any[]} The converted array.
 */
export function toArray(value: any): any[];
/**
 * Wait for a given number of milliseconds or a `requestAnimationFrame`.
 * @param {number} ms - The number of milliseconds to wait.
 * @returns {Promise<void>}
 */
export function wait(ms: number): Promise<void>;
/**
 * Get the longhands for a given property.
 * @param {string} property - The property to get the longhands for.
 * @returns {string[]} The longhands.
 * @see https://lea.verou.me/blog/2020/07/introspecting-css-via-the-css-om-getting-supported-properties-shorthands-longhands/
 */
export function getLonghands(property: string): string[];
/**
 * Parse a CSS `<time>` value.
 * @param {string } cssTime - A string that contains CSS `<time>` values.
 * @return { number[] } Any times found, in milliseconds.
 */
export function parseTimes(cssTime: string): number[];
/**
 * Get the duration and delay of a CSS transition for a given property.
 * @param {string} property - The CSS property name.
 * @param {string} transitions - The computed value of the `transition` property.
 * @returns { { duration: number, delay: number } } The duration and delay, in milliseconds.
 */
export function getTimesFor(property: string, transitions: string): {
    duration: number;
    delay: number;
};
/**
 * Split a value by commas, ignoring commas within parentheses and trimming whitespace.
 * @param {string} value - The value to split.
 * @returns {string[]} The split values.
 */
export function splitCommas(value: string): string[];
