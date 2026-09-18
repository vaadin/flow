/**
 * A WeakMap that maps keys to Sets of values, allowing multiple values per key.
 */
export default class MultiWeakMap extends WeakMap {
	has (key, value) {
		if (arguments.length === 1) {
			return super.has(key);
		}

		let set = super.get(key);
		return set?.has(value) || false;
	}

	add (key, value) {
		let set = super.get(key) ?? new Set();
		set.add(value);
		super.set(key, set);
	}

	delete (key, ...values) {
		let set = super.get(key);
		if (set) {
			for (let value of values) {
				set.delete(value);
			}

			if (set.size === 0) {
				super.delete(key);
			}
		}
	}
}
