import { encode } from "@jridgewell/sourcemap-codec";
//#region src/BitSet.ts
var BitSet = class BitSet {
	constructor(arg) {
		this.bits = arg instanceof BitSet ? arg.bits.slice() : [];
	}
	add(n) {
		this.bits[n >> 5] |= 1 << (n & 31);
	}
	has(n) {
		return !!(this.bits[n >> 5] & 1 << (n & 31));
	}
};
//#endregion
//#region src/Chunk.ts
var Chunk = class Chunk {
	constructor(start, end, content) {
		this.start = start;
		this.end = end;
		this.original = content;
		this.intro = "";
		this.outro = "";
		this.content = content;
		this.storeName = false;
		this.edited = false;
		this.previous = null;
		this.next = null;
	}
	appendLeft(content) {
		this.outro += content;
	}
	appendRight(content) {
		this.intro = this.intro + content;
	}
	clone() {
		const chunk = new Chunk(this.start, this.end, this.original);
		chunk.intro = this.intro;
		chunk.outro = this.outro;
		chunk.content = this.content;
		chunk.storeName = this.storeName;
		chunk.edited = this.edited;
		return chunk;
	}
	contains(index) {
		return this.start < index && index < this.end;
	}
	eachNext(fn) {
		fn(this);
		let chunk = this.next;
		while (chunk) {
			fn(chunk);
			chunk = chunk.next;
		}
	}
	eachPrevious(fn) {
		fn(this);
		let chunk = this.previous;
		while (chunk) {
			fn(chunk);
			chunk = chunk.previous;
		}
	}
	edit(content, storeName, contentOnly) {
		this.content = content;
		if (!contentOnly) {
			this.intro = "";
			this.outro = "";
		}
		this.storeName = storeName;
		this.edited = true;
		return this;
	}
	prependLeft(content) {
		this.outro = content + this.outro;
	}
	prependRight(content) {
		this.intro = content + this.intro;
	}
	reset() {
		this.intro = "";
		this.outro = "";
		if (this.edited) {
			this.content = this.original;
			this.storeName = false;
			this.edited = false;
		}
	}
	split(index) {
		const sliceIndex = index - this.start;
		const originalBefore = this.original.slice(0, sliceIndex);
		const originalAfter = this.original.slice(sliceIndex);
		this.original = originalBefore;
		const newChunk = new Chunk(index, this.end, originalAfter);
		newChunk.outro = this.outro;
		this.outro = "";
		this.end = index;
		if (this.edited) {
			newChunk.edit("", false);
			this.content = "";
		} else this.content = originalBefore;
		newChunk.next = this.next;
		if (newChunk.next) newChunk.next.previous = newChunk;
		newChunk.previous = this;
		this.next = newChunk;
		return newChunk;
	}
	toString() {
		return this.intro + this.content + this.outro;
	}
	trimEnd(rx) {
		this.outro = this.outro.replace(rx, "");
		if (this.outro.length) return true;
		const trimmed = this.content.replace(rx, "");
		if (trimmed.length) {
			if (trimmed !== this.content) if (this.edited) this.edit(trimmed, this.storeName, true);
			else this.split(this.start + trimmed.length).edit("", void 0, true);
			return true;
		} else {
			this.edit("", void 0, true);
			this.intro = this.intro.replace(rx, "");
			if (this.intro.length) return true;
		}
	}
	trimStart(rx) {
		this.intro = this.intro.replace(rx, "");
		if (this.intro.length) return true;
		const trimmed = this.content.replace(rx, "");
		if (trimmed.length) {
			if (trimmed !== this.content) if (this.edited) this.edit(trimmed, this.storeName, true);
			else {
				this.split(this.end - trimmed.length);
				this.edit("", void 0, true);
			}
			return true;
		} else {
			this.edit("", void 0, true);
			this.outro = this.outro.replace(rx, "");
			if (this.outro.length) return true;
		}
	}
};
//#endregion
//#region src/SourceMap.ts
function getBtoa() {
	if (typeof globalThis !== "undefined" && typeof globalThis.btoa === "function") return (str) => globalThis.btoa(unescape(encodeURIComponent(str)));
	const buffer = globalThis["Buffer"];
	if (buffer) return (str) => buffer.from(str, "utf-8").toString("base64");
	return () => {
		throw new Error("Unsupported environment: `window.btoa` or `Buffer` should be supported.");
	};
}
const btoa = /* #__PURE__ */ getBtoa();
var SourceMap = class {
	constructor(properties) {
		this.version = 3;
		this.file = properties.file;
		this.sources = properties.sources;
		this.sourcesContent = properties.sourcesContent;
		this.names = properties.names;
		this.mappings = encode(properties.mappings);
		if (typeof properties.x_google_ignoreList !== "undefined") this.x_google_ignoreList = properties.x_google_ignoreList;
		if (typeof properties.debugId !== "undefined") this.debugId = properties.debugId;
	}
	/**
	* Returns the equivalent of `JSON.stringify(map)`
	*/
	toString() {
		return JSON.stringify(this);
	}
	/**
	* Returns a DataURI containing the sourcemap. Useful for doing this sort of thing:
	* `generateMap(options?: SourceMapOptions): SourceMap;`
	*/
	toUrl() {
		return `data:application/json;charset=utf-8;base64,${btoa(this.toString())}`;
	}
};
//#endregion
//#region src/utils/getLocator.ts
function getLocator(source) {
	const originalLines = source.split("\n");
	const lineOffsets = [];
	for (let i = 0, pos = 0; i < originalLines.length; i++) {
		lineOffsets.push(pos);
		pos += originalLines[i].length + 1;
	}
	return function locate(index) {
		let i = 0;
		let j = lineOffsets.length;
		while (i < j) {
			const m = i + j >> 1;
			if (index < lineOffsets[m]) j = m;
			else i = m + 1;
		}
		const line = i - 1;
		return {
			line,
			column: index - lineOffsets[line]
		};
	};
}
//#endregion
//#region src/utils/getRelativePath.ts
function getRelativePath(from, to) {
	const fromParts = from.split(/[/\\]/);
	const toParts = to.split(/[/\\]/);
	fromParts.pop();
	while (fromParts[0] === toParts[0]) {
		fromParts.shift();
		toParts.shift();
	}
	if (fromParts.length) {
		let i = fromParts.length;
		while (i--) fromParts[i] = "..";
	}
	return fromParts.concat(toParts).join("/");
}
//#endregion
//#region src/utils/guessIndent.ts
function guessIndent(code) {
	const lines = code.split("\n");
	const tabbed = lines.filter((line) => /^\t+/.test(line));
	const spaced = lines.filter((line) => /^ {2,}/.test(line));
	if (tabbed.length === 0 && spaced.length === 0) return null;
	if (tabbed.length >= spaced.length) return "	";
	const min = spaced.reduce((previous, current) => {
		const numSpaces = /^ +/.exec(current)[0].length;
		return Math.min(numSpaces, previous);
	}, Infinity);
	return " ".repeat(min);
}
//#endregion
//#region src/utils/isObject.ts
const toString = Object.prototype.toString;
function isObject(thing) {
	return toString.call(thing) === "[object Object]";
}
//#endregion
//#region src/utils/Mappings.ts
const wordRegex = /\w/;
var Mappings = class {
	constructor(hires) {
		this.hires = hires;
		this.generatedCodeLine = 0;
		this.generatedCodeColumn = 0;
		this.raw = [];
		this.rawSegments = this.raw[this.generatedCodeLine] = [];
		this.pending = null;
	}
	addEdit(sourceIndex, content, loc, nameIndex) {
		if (content.length) {
			const contentLengthMinusOne = content.length - 1;
			let contentLineEnd = content.indexOf("\n", 0);
			let previousContentLineEnd = -1;
			while (contentLineEnd >= 0 && contentLengthMinusOne > contentLineEnd) {
				const segment = [
					this.generatedCodeColumn,
					sourceIndex,
					loc.line,
					loc.column
				];
				if (nameIndex >= 0) segment.push(nameIndex);
				this.rawSegments.push(segment);
				this.generatedCodeLine += 1;
				this.raw[this.generatedCodeLine] = this.rawSegments = [];
				this.generatedCodeColumn = 0;
				previousContentLineEnd = contentLineEnd;
				contentLineEnd = content.indexOf("\n", contentLineEnd + 1);
			}
			const segment = [
				this.generatedCodeColumn,
				sourceIndex,
				loc.line,
				loc.column
			];
			if (nameIndex >= 0) segment.push(nameIndex);
			this.rawSegments.push(segment);
			this.advance(content.slice(previousContentLineEnd + 1));
		} else if (this.pending) {
			this.rawSegments.push(this.pending);
			this.advance(content);
		}
		this.pending = null;
	}
	addUneditedChunk(sourceIndex, chunk, original, loc, sourcemapLocations) {
		let originalCharIndex = chunk.start;
		let first = true;
		let charInHiresBoundary = false;
		while (originalCharIndex < chunk.end) {
			if (original[originalCharIndex] === "\n") {
				loc.line += 1;
				loc.column = 0;
				this.generatedCodeLine += 1;
				this.raw[this.generatedCodeLine] = this.rawSegments = [];
				this.generatedCodeColumn = 0;
				first = true;
				charInHiresBoundary = false;
			} else {
				if (this.hires || first || sourcemapLocations.has(originalCharIndex)) {
					const segment = [
						this.generatedCodeColumn,
						sourceIndex,
						loc.line,
						loc.column
					];
					if (this.hires === "boundary") if (wordRegex.test(original[originalCharIndex])) {
						if (!charInHiresBoundary) {
							this.rawSegments.push(segment);
							charInHiresBoundary = true;
						}
					} else {
						this.rawSegments.push(segment);
						charInHiresBoundary = false;
					}
					else this.rawSegments.push(segment);
				}
				loc.column += 1;
				this.generatedCodeColumn += 1;
				first = false;
			}
			originalCharIndex += 1;
		}
		this.pending = null;
	}
	advance(str) {
		if (!str) return;
		const lines = str.split("\n");
		if (lines.length > 1) {
			for (let i = 0; i < lines.length - 1; i++) {
				this.generatedCodeLine++;
				this.raw[this.generatedCodeLine] = this.rawSegments = [];
			}
			this.generatedCodeColumn = 0;
		}
		this.generatedCodeColumn += lines[lines.length - 1].length;
	}
};
//#endregion
//#region src/MagicString.ts
const n = "\n";
const NEWLINE_CHAR = "\n".charCodeAt(0);
const CR_CHAR = "\r".charCodeAt(0);
const warned = {
	insertLeft: false,
	insertRight: false,
	storeName: false
};
var MagicString = class MagicString {
	constructor(string, options = {}) {
		const chunk = new Chunk(0, string.length, string);
		Object.defineProperties(this, {
			original: {
				writable: true,
				value: string
			},
			outro: {
				writable: true,
				value: ""
			},
			intro: {
				writable: true,
				value: ""
			},
			firstChunk: {
				writable: true,
				value: chunk
			},
			lastChunk: {
				writable: true,
				value: chunk
			},
			lastSearchedChunk: {
				writable: true,
				value: chunk
			},
			byStart: {
				writable: true,
				value: {}
			},
			byEnd: {
				writable: true,
				value: {}
			},
			filename: {
				writable: true,
				value: options.filename
			},
			indentExclusionRanges: {
				writable: true,
				value: options.indentExclusionRanges
			},
			sourcemapLocations: {
				writable: true,
				value: new BitSet()
			},
			storedNames: {
				writable: true,
				value: {}
			},
			indentStr: {
				writable: true,
				value: void 0
			},
			ignoreList: {
				writable: true,
				value: options.ignoreList
			},
			offset: {
				writable: true,
				value: options.offset || 0
			}
		});
		this.byStart = /* @__PURE__ */ new Map([[0, chunk]]);
		this.byEnd = /* @__PURE__ */ new Map([[string.length, chunk]]);
	}
	/**
	* Adds the specified character index (with respect to the original string) to sourcemap mappings, if `hires` is false.
	*/
	addSourcemapLocation(char) {
		this.sourcemapLocations.add(char);
	}
	/**
	* Appends the specified content to the end of the string.
	*/
	append(content) {
		if (typeof content !== "string") throw new TypeError("outro content must be a string");
		this.outro += content;
		return this;
	}
	/**
	* Appends the specified content at the index in the original string.
	* If a range *ending* with index is subsequently moved, the insert will be moved with it.
	* See also `s.prependLeft(...)`.
	*/
	appendLeft(index, content) {
		index = index + this.offset;
		if (typeof content !== "string") throw new TypeError("inserted content must be a string");
		this._split(index);
		const chunk = this.byEnd.get(index);
		if (chunk) chunk.appendLeft(content);
		else this.intro += content;
		return this;
	}
	/**
	* Appends the specified content at the index in the original string.
	* If a range *starting* with index is subsequently moved, the insert will be moved with it.
	* See also `s.prependRight(...)`.
	*/
	appendRight(index, content) {
		index = index + this.offset;
		if (typeof content !== "string") throw new TypeError("inserted content must be a string");
		this._split(index);
		const chunk = this.byStart.get(index);
		if (chunk) chunk.appendRight(content);
		else this.outro += content;
		return this;
	}
	/**
	* Does what you'd expect.
	*/
	clone() {
		const cloned = new MagicString(this.original, {
			filename: this.filename,
			offset: this.offset
		});
		let originalChunk = this.firstChunk;
		let clonedChunk = cloned.firstChunk = cloned.lastSearchedChunk = originalChunk.clone();
		while (originalChunk) {
			cloned.byStart.set(clonedChunk.start, clonedChunk);
			cloned.byEnd.set(clonedChunk.end, clonedChunk);
			const nextOriginalChunk = originalChunk.next;
			const nextClonedChunk = nextOriginalChunk && nextOriginalChunk.clone();
			if (nextClonedChunk) {
				clonedChunk.next = nextClonedChunk;
				nextClonedChunk.previous = clonedChunk;
				clonedChunk = nextClonedChunk;
			}
			originalChunk = nextOriginalChunk;
		}
		cloned.lastChunk = clonedChunk;
		if (this.indentExclusionRanges) cloned.indentExclusionRanges = this.indentExclusionRanges.slice();
		cloned.sourcemapLocations = new BitSet(this.sourcemapLocations);
		cloned.intro = this.intro;
		cloned.outro = this.outro;
		return cloned;
	}
	/**
	* Generates a sourcemap object with raw mappings in array form, rather than encoded as a string.
	* Useful if you need to manipulate the sourcemap further, but most of the time you will use `generateMap` instead.
	*/
	generateDecodedMap(options) {
		options = options || {};
		const sourceIndex = 0;
		const names = Object.keys(this.storedNames);
		const mappings = new Mappings(options.hires);
		const locate = getLocator(this.original);
		if (this.intro) mappings.advance(this.intro);
		this.firstChunk.eachNext((chunk) => {
			const loc = locate(chunk.start);
			if (chunk.intro.length) mappings.advance(chunk.intro);
			if (chunk.edited) mappings.addEdit(sourceIndex, chunk.content, loc, chunk.storeName ? names.indexOf(chunk.original) : -1);
			else mappings.addUneditedChunk(sourceIndex, chunk, this.original, loc, this.sourcemapLocations);
			if (chunk.outro.length) mappings.advance(chunk.outro);
		});
		if (this.outro) mappings.advance(this.outro);
		return {
			file: options.file ? options.file.split(/[/\\]/).pop() : void 0,
			sources: [options.source ? getRelativePath(options.file || "", options.source) : options.file || ""],
			sourcesContent: options.includeContent ? [this.original] : void 0,
			names,
			mappings: mappings.raw,
			x_google_ignoreList: this.ignoreList ? [sourceIndex] : void 0
		};
	}
	/**
	* Generates a version 3 sourcemap.
	*/
	generateMap(options) {
		return new SourceMap(this.generateDecodedMap(options));
	}
	/** @internal */
	_ensureindentStr() {
		if (this.indentStr === void 0) this.indentStr = guessIndent(this.original);
	}
	/** @internal */
	_getRawIndentString() {
		this._ensureindentStr();
		return this.indentStr;
	}
	getIndentString() {
		this._ensureindentStr();
		return this.indentStr === null ? "	" : this.indentStr;
	}
	indent(indentStr, options) {
		const pattern = /^[^\r\n]/gm;
		if (isObject(indentStr)) {
			options = indentStr;
			indentStr = void 0;
		}
		if (indentStr === void 0) {
			this._ensureindentStr();
			indentStr = this.indentStr || "	";
		}
		if (indentStr === "") return this;
		const resolvedIndentStr = indentStr;
		options = options || {};
		const isExcluded = {};
		if (options.exclude) (typeof options.exclude[0] === "number" ? [options.exclude] : options.exclude).forEach((exclusion) => {
			for (let i = exclusion[0]; i < exclusion[1]; i += 1) isExcluded[i] = true;
		});
		let shouldIndentNextCharacter = options.indentStart !== false;
		const replacer = (match) => {
			if (shouldIndentNextCharacter) return `${resolvedIndentStr}${match}`;
			shouldIndentNextCharacter = true;
			return match;
		};
		this.intro = this.intro.replace(pattern, replacer);
		let charIndex = 0;
		let chunk = this.firstChunk;
		const indentAt = (index) => {
			shouldIndentNextCharacter = false;
			if (index === chunk.start) chunk.prependRight(resolvedIndentStr);
			else {
				this._splitChunk(chunk, index);
				chunk = chunk.next;
				chunk.prependRight(resolvedIndentStr);
			}
		};
		while (chunk) {
			const end = chunk.end;
			if (chunk.edited) {
				if (!isExcluded[charIndex]) {
					chunk.content = chunk.content.replace(pattern, replacer);
					if (chunk.content.length) shouldIndentNextCharacter = chunk.content[chunk.content.length - 1] === "\n";
				}
			} else if (options.exclude) {
				charIndex = chunk.start;
				while (charIndex < end) {
					if (!isExcluded[charIndex]) {
						const char = this.original.charCodeAt(charIndex);
						if (char === NEWLINE_CHAR) shouldIndentNextCharacter = true;
						else if (char !== CR_CHAR && shouldIndentNextCharacter) indentAt(charIndex);
					}
					charIndex += 1;
				}
			} else {
				charIndex = chunk.start;
				while (charIndex < end) {
					if (!shouldIndentNextCharacter) {
						const nextLine = this.original.indexOf(n, charIndex);
						if (nextLine === -1 || nextLine >= end) break;
						shouldIndentNextCharacter = true;
						charIndex = nextLine + 1;
						continue;
					}
					const char = this.original.charCodeAt(charIndex);
					if (char === NEWLINE_CHAR || char === CR_CHAR) {
						charIndex += 1;
						continue;
					}
					indentAt(charIndex);
					charIndex += 1;
				}
			}
			charIndex = chunk.end;
			chunk = chunk.next;
		}
		this.outro = this.outro.replace(pattern, replacer);
		return this;
	}
	/** @internal */
	insert() {
		throw new Error("magicString.insert(...) is deprecated. Use prependRight(...) or appendLeft(...)");
	}
	/** @internal */
	insertLeft(index, content) {
		if (!warned.insertLeft) {
			console.warn("magicString.insertLeft(...) is deprecated. Use magicString.appendLeft(...) instead");
			warned.insertLeft = true;
		}
		return this.appendLeft(index, content);
	}
	/** @internal */
	insertRight(index, content) {
		if (!warned.insertRight) {
			console.warn("magicString.insertRight(...) is deprecated. Use magicString.prependRight(...) instead");
			warned.insertRight = true;
		}
		return this.prependRight(index, content);
	}
	/**
	* Moves the characters from `start` and `end` to `index`.
	*/
	move(start, end, index) {
		start = start + this.offset;
		end = end + this.offset;
		index = index + this.offset;
		if (start === end) return this;
		if (index >= start && index <= end) throw new Error("Cannot move a selection inside itself");
		this._split(start);
		this._split(end);
		this._split(index);
		const first = this.byStart.get(start);
		const last = this.byEnd.get(end);
		const oldLeft = first.previous;
		const oldRight = last.next;
		const newRight = this.byStart.get(index);
		if (!newRight && last === this.lastChunk) return this;
		const newLeft = newRight ? newRight.previous : this.lastChunk;
		if (oldLeft) oldLeft.next = oldRight;
		if (oldRight) oldRight.previous = oldLeft;
		if (newLeft) newLeft.next = first;
		if (newRight) newRight.previous = last;
		if (!first.previous) this.firstChunk = last.next;
		if (!last.next) {
			this.lastChunk = first.previous;
			this.lastChunk.next = null;
		}
		first.previous = newLeft;
		last.next = newRight || null;
		if (!newLeft) this.firstChunk = first;
		if (!newRight) this.lastChunk = last;
		return this;
	}
	/**
	* Replaces the characters from `start` to `end` with `content`, along with the appended/prepended content in
	* that range. The same restrictions as `s.remove()` apply.
	*
	* The fourth argument is optional. It can have a storeName property - if true, the original name will be stored
	* for later inclusion in a sourcemap's names array - and a contentOnly property which determines whether only
	* the content is overwritten, or anything that was appended/prepended to the range as well.
	*
	* It may be preferred to use `s.update(...)` instead if you wish to avoid overwriting the appended/prepended content.
	*/
	overwrite(start, end, content, options) {
		const optionObject = typeof options === "object" && options ? options : {};
		return this.update(start, end, content, {
			...optionObject,
			overwrite: !optionObject.contentOnly
		});
	}
	/**
	* Replaces the characters from `start` to `end` with `content`. The same restrictions as `s.remove()` apply.
	*
	* The fourth argument is optional. It can have a storeName property - if true, the original name will be stored
	* for later inclusion in a sourcemap's names array - and an overwrite property which determines whether only
	* the content is overwritten, or anything that was appended/prepended to the range as well.
	*/
	update(start, end, content, options) {
		start = start + this.offset;
		end = end + this.offset;
		if (typeof content !== "string") throw new TypeError("replacement content must be a string");
		if (this.original.length !== 0) {
			while (start < 0) start += this.original.length;
			while (end < 0) end += this.original.length;
		}
		if (end > this.original.length) throw new Error("end is out of bounds");
		if (start === end) throw new Error("Cannot overwrite a zero-length range – use appendLeft or prependRight instead");
		this._split(start);
		this._split(end);
		if (options === true) {
			if (!warned.storeName) {
				console.warn("The final argument to magicString.overwrite(...) should be an options object. See https://github.com/rich-harris/magic-string");
				warned.storeName = true;
			}
			options = { storeName: true };
		}
		const optionObject = typeof options === "object" && options ? options : {};
		const storeName = optionObject.storeName || false;
		const overwrite = optionObject.overwrite || false;
		if (storeName) {
			const original = this.original.slice(start, end);
			Object.defineProperty(this.storedNames, original, {
				writable: true,
				value: true,
				enumerable: true
			});
		}
		const first = this.byStart.get(start);
		const last = this.byEnd.get(end);
		if (first) {
			let chunk = first;
			while (chunk !== last) {
				if (chunk.next !== this.byStart.get(chunk.end)) throw new Error("Cannot overwrite across a split point");
				chunk = chunk.next;
				chunk.edit("", false);
			}
			first.edit(content, storeName, !overwrite);
		} else {
			const newChunk = new Chunk(start, end, "").edit(content, storeName);
			last.next = newChunk;
			newChunk.previous = last;
		}
		return this;
	}
	/**
	* Prepends the string with the specified content.
	*/
	prepend(content) {
		if (typeof content !== "string") throw new TypeError("outro content must be a string");
		this.intro = content + this.intro;
		return this;
	}
	/**
	* Same as `s.appendLeft(...)`, except that the inserted content will go *before* any previous appends or prepends at index
	*/
	prependLeft(index, content) {
		index = index + this.offset;
		if (typeof content !== "string") throw new TypeError("inserted content must be a string");
		this._split(index);
		const chunk = this.byEnd.get(index);
		if (chunk) chunk.prependLeft(content);
		else this.intro = content + this.intro;
		return this;
	}
	/**
	* Same as `s.appendRight(...)`, except that the inserted content will go *before* any previous appends or prepends at `index`
	*/
	prependRight(index, content) {
		index = index + this.offset;
		if (typeof content !== "string") throw new TypeError("inserted content must be a string");
		this._split(index);
		const chunk = this.byStart.get(index);
		if (chunk) chunk.prependRight(content);
		else this.outro = content + this.outro;
		return this;
	}
	/**
	* Removes the characters from `start` to `end` (of the original string, **not** the generated string).
	* Removing the same content twice, or making removals that partially overlap, will cause an error.
	*/
	remove(start, end) {
		start = start + this.offset;
		end = end + this.offset;
		if (this.original.length !== 0) {
			while (start < 0) start += this.original.length;
			while (end < 0) end += this.original.length;
		}
		if (start === end) return this;
		if (start < 0 || end > this.original.length) throw new Error("Character is out of bounds");
		if (start > end) throw new Error("end must be greater than start");
		this._split(start);
		this._split(end);
		let chunk = this.byStart.get(start);
		while (chunk) {
			chunk.intro = "";
			chunk.outro = "";
			chunk.edit("");
			chunk = end > chunk.end ? this.byStart.get(chunk.end) : null;
		}
		return this;
	}
	/**
	* Reset the modified characters from `start` to `end` (of the original string, **not** the generated string).
	*/
	reset(start, end) {
		start = start + this.offset;
		end = end + this.offset;
		if (this.original.length !== 0) {
			while (start < 0) start += this.original.length;
			while (end < 0) end += this.original.length;
		}
		if (start === end) return this;
		if (start < 0 || end > this.original.length) throw new Error("Character is out of bounds");
		if (start > end) throw new Error("end must be greater than start");
		this._split(start);
		this._split(end);
		let chunk = this.byStart.get(start);
		while (chunk) {
			chunk.reset();
			chunk = end > chunk.end ? this.byStart.get(chunk.end) : null;
		}
		return this;
	}
	lastChar() {
		if (this.outro.length) return this.outro[this.outro.length - 1];
		let chunk = this.lastChunk;
		while (chunk) {
			if (chunk.outro.length) return chunk.outro[chunk.outro.length - 1];
			if (chunk.content.length) return chunk.content[chunk.content.length - 1];
			if (chunk.intro.length) return chunk.intro[chunk.intro.length - 1];
			chunk = chunk.previous;
		}
		if (this.intro.length) return this.intro[this.intro.length - 1];
		return "";
	}
	lastLine() {
		let lineIndex = this.outro.lastIndexOf(n);
		if (lineIndex !== -1) return this.outro.substr(lineIndex + 1);
		let lineStr = this.outro;
		let chunk = this.lastChunk;
		while (chunk) {
			if (chunk.outro.length > 0) {
				lineIndex = chunk.outro.lastIndexOf(n);
				if (lineIndex !== -1) return chunk.outro.substr(lineIndex + 1) + lineStr;
				lineStr = chunk.outro + lineStr;
			}
			if (chunk.content.length > 0) {
				lineIndex = chunk.content.lastIndexOf(n);
				if (lineIndex !== -1) return chunk.content.substr(lineIndex + 1) + lineStr;
				lineStr = chunk.content + lineStr;
			}
			if (chunk.intro.length > 0) {
				lineIndex = chunk.intro.lastIndexOf(n);
				if (lineIndex !== -1) return chunk.intro.substr(lineIndex + 1) + lineStr;
				lineStr = chunk.intro + lineStr;
			}
			chunk = chunk.previous;
		}
		lineIndex = this.intro.lastIndexOf(n);
		if (lineIndex !== -1) return this.intro.substr(lineIndex + 1) + lineStr;
		return this.intro + lineStr;
	}
	/**
	* Returns the content of the generated string that corresponds to the slice between `start` and `end` of the original string.
	* Throws error if the indices are for characters that were already removed.
	*/
	slice(start = 0, end = this.original.length - this.offset) {
		start = start + this.offset;
		end = end + this.offset;
		if (this.original.length !== 0) {
			while (start < 0) start += this.original.length;
			while (end < 0) end += this.original.length;
		}
		let result = "";
		let chunk = this.firstChunk;
		while (chunk && (chunk.start > start || chunk.end <= start)) {
			if (chunk.start < end && chunk.end >= end) return result;
			chunk = chunk.next;
		}
		if (chunk && chunk.edited && chunk.start !== start) throw new Error(`Cannot use replaced character ${start} as slice start anchor.`);
		const startChunk = chunk;
		while (chunk) {
			if (chunk.intro && (startChunk !== chunk || chunk.start === start)) result += chunk.intro;
			const containsEnd = chunk.start < end && chunk.end >= end;
			if (containsEnd && chunk.edited && chunk.end !== end) throw new Error(`Cannot use replaced character ${end} as slice end anchor.`);
			const sliceStart = startChunk === chunk ? start - chunk.start : 0;
			const sliceEnd = containsEnd ? chunk.content.length + end - chunk.end : chunk.content.length;
			result += chunk.content.slice(sliceStart, sliceEnd);
			if (chunk.outro && (!containsEnd || chunk.end === end)) result += chunk.outro;
			if (containsEnd) break;
			chunk = chunk.next;
		}
		return result;
	}
	/**
	* Returns a clone of `s`, with all content before the `start` and `end` characters of the original string removed.
	*/
	snip(start, end) {
		const clone = this.clone();
		clone.remove(0, start);
		clone.remove(end, clone.original.length);
		return clone;
	}
	/** @internal */
	_split(index) {
		if (this.byStart.get(index) || this.byEnd.get(index)) return;
		let chunk = this.lastSearchedChunk;
		let previousChunk = chunk;
		const searchForward = index > chunk.end;
		while (chunk) {
			if (chunk.contains(index)) return this._splitChunk(chunk, index);
			chunk = searchForward ? this.byStart.get(chunk.end) : this.byEnd.get(chunk.start);
			if (chunk === previousChunk) return;
			previousChunk = chunk;
		}
	}
	/** @internal */
	_splitChunk(chunk, index) {
		if (chunk.edited && chunk.content.length) {
			const loc = getLocator(this.original)(index);
			throw new Error(`Cannot split a chunk that has already been edited (${loc.line}:${loc.column} – "${chunk.original}")`);
		}
		const newChunk = chunk.split(index);
		this.byEnd.set(index, chunk);
		this.byStart.set(index, newChunk);
		this.byEnd.set(newChunk.end, newChunk);
		if (chunk === this.lastChunk) this.lastChunk = newChunk;
		this.lastSearchedChunk = chunk;
		return true;
	}
	/**
	* Returns the generated string.
	*/
	toString() {
		let str = this.intro;
		let chunk = this.firstChunk;
		while (chunk) {
			str += chunk.toString();
			chunk = chunk.next;
		}
		return str + this.outro;
	}
	/**
	* Returns true if the resulting source is empty (disregarding white space).
	*/
	isEmpty() {
		let chunk = this.firstChunk;
		while (chunk) {
			if (chunk.intro.length && chunk.intro.trim() || chunk.content.length && chunk.content.trim() || chunk.outro.length && chunk.outro.trim()) return false;
			chunk = chunk.next;
		}
		return true;
	}
	length() {
		let chunk = this.firstChunk;
		let length = 0;
		while (chunk) {
			length += chunk.intro.length + chunk.content.length + chunk.outro.length;
			chunk = chunk.next;
		}
		return length;
	}
	/**
	* Removes empty lines from the start and end.
	*/
	trimLines() {
		return this.trim("[\\r\\n]");
	}
	/**
	* Trims content matching `charType` (defaults to `\s`, i.e. whitespace) from the start and end.
	*/
	trim(charType) {
		return this.trimStart(charType).trimEnd(charType);
	}
	/** @internal */
	trimEndAborted(charType) {
		const rx = new RegExp(`${charType || "\\s"}+$`);
		this.outro = this.outro.replace(rx, "");
		if (this.outro.length) return true;
		let chunk = this.lastChunk;
		do {
			const end = chunk.end;
			const aborted = chunk.trimEnd(rx);
			if (chunk.end !== end) {
				if (this.lastChunk === chunk) this.lastChunk = chunk.next;
				this.byEnd.set(chunk.end, chunk);
				this.byStart.set(chunk.next.start, chunk.next);
				this.byEnd.set(chunk.next.end, chunk.next);
			}
			if (aborted) return true;
			chunk = chunk.previous;
		} while (chunk);
		return false;
	}
	/**
	* Trims content matching `charType` (defaults to `\s`, i.e. whitespace) from the end.
	*/
	trimEnd(charType) {
		this.trimEndAborted(charType);
		return this;
	}
	/** @internal */
	trimStartAborted(charType) {
		const rx = new RegExp(`^${charType || "\\s"}+`);
		this.intro = this.intro.replace(rx, "");
		if (this.intro.length) return true;
		let chunk = this.firstChunk;
		do {
			const end = chunk.end;
			const aborted = chunk.trimStart(rx);
			if (chunk.end !== end) {
				if (chunk === this.lastChunk) this.lastChunk = chunk.next;
				this.byEnd.set(chunk.end, chunk);
				this.byStart.set(chunk.next.start, chunk.next);
				this.byEnd.set(chunk.next.end, chunk.next);
			}
			if (aborted) return true;
			chunk = chunk.next;
		} while (chunk);
		return false;
	}
	/**
	* Trims content matching `charType` (defaults to `\s`, i.e. whitespace) from the start.
	*/
	trimStart(charType) {
		this.trimStartAborted(charType);
		return this;
	}
	/**
	* Indicates if the string has been changed.
	*/
	hasChanged() {
		return this.original !== this.toString();
	}
	/** @internal */
	_replaceRegexp(searchValue, replacement) {
		function getReplacement(match, str) {
			if (typeof replacement === "string") return replacement.replace(/\$(\$|&|\d+)/g, (_, i) => {
				if (i === "$") return "$";
				if (i === "&") return match[0];
				if (+i < match.length) return match[+i];
				return `$${i}`;
			});
			else return replacement(match[0], ...match.slice(1), match.index, str, match.groups);
		}
		function matchAll(re, str) {
			const matches = [];
			while (true) {
				const match = re.exec(str);
				if (!match) break;
				matches.push(match);
			}
			return matches;
		}
		if (searchValue.global) matchAll(searchValue, this.original).forEach((match) => {
			if (match.index != null) {
				const replacement = getReplacement(match, this.original);
				if (replacement !== match[0]) this.overwrite(match.index, match.index + match[0].length, replacement);
			}
		});
		else {
			const match = this.original.match(searchValue);
			if (match && match.index != null) {
				const replacement = getReplacement(match, this.original);
				if (replacement !== match[0]) this.overwrite(match.index, match.index + match[0].length, replacement);
			}
		}
		return this;
	}
	/** @internal */
	_replaceString(string, replacement) {
		const { original } = this;
		const index = original.indexOf(string);
		if (index !== -1) {
			if (typeof replacement === "function") replacement = replacement(string, index, original);
			if (string !== replacement) this.overwrite(index, index + string.length, replacement);
		}
		return this;
	}
	/**
	* String replacement with RegExp or string.
	*/
	replace(searchValue, replacement) {
		if (typeof searchValue === "string") return this._replaceString(searchValue, replacement);
		return this._replaceRegexp(searchValue, replacement);
	}
	/** @internal */
	_replaceAllString(string, replacement) {
		const { original } = this;
		const stringLength = string.length;
		for (let index = original.indexOf(string); index !== -1; index = original.indexOf(string, index + stringLength)) {
			const previous = original.slice(index, index + stringLength);
			const _replacement = typeof replacement === "function" ? replacement(previous, index, original) : replacement;
			if (previous !== _replacement) this.overwrite(index, index + stringLength, _replacement);
		}
		return this;
	}
	/**
	* Same as `s.replace`, but replace all matched strings instead of just one.
	*/
	replaceAll(searchValue, replacement) {
		if (typeof searchValue === "string") return this._replaceAllString(searchValue, replacement);
		if (!searchValue.global) throw new TypeError("MagicString.prototype.replaceAll called with a non-global RegExp argument");
		return this._replaceRegexp(searchValue, replacement);
	}
};
//#endregion
//#region src/Bundle.ts
const hasOwnProp = Object.prototype.hasOwnProperty;
var Bundle = class Bundle {
	constructor(options = {}) {
		this.intro = options.intro || "";
		this.separator = options.separator !== void 0 ? options.separator : "\n";
		this.sources = [];
		this.uniqueSources = [];
		this.uniqueSourceIndexByFilename = {};
	}
	/**
	* Adds the specified source to the bundle, which can either be a `MagicString` object directly,
	* or an options object that holds a magic string `content` property and optionally provides
	* a `filename` for the source within the bundle, as well as an optional `ignoreList` hint
	* (which defaults to `false`). The `filename` is used when constructing the source map for the
	* bundle, to identify this `source` in the source map's `sources` field. The `ignoreList` hint
	* is used to populate the `x_google_ignoreList` extension field in the source map, which is a
	* mechanism for tools to signal to debuggers that certain sources should be ignored by default
	* (depending on user preferences).
	*/
	addSource(source) {
		if (source instanceof MagicString) return this.addSource({
			content: source,
			filename: source.filename,
			separator: this.separator
		});
		if (!isObject(source) || !source.content) throw new Error("bundle.addSource() takes an object with a `content` property, which should be an instance of MagicString, and an optional `filename`");
		[
			"filename",
			"ignoreList",
			"indentExclusionRanges",
			"separator"
		].forEach((option) => {
			if (!hasOwnProp.call(source, option)) source[option] = source.content[option];
		});
		if (source.separator === void 0) source.separator = this.separator;
		if (source.filename) if (!hasOwnProp.call(this.uniqueSourceIndexByFilename, source.filename)) {
			this.uniqueSourceIndexByFilename[source.filename] = this.uniqueSources.length;
			this.uniqueSources.push({
				filename: source.filename,
				content: source.content.original
			});
		} else {
			const uniqueSource = this.uniqueSources[this.uniqueSourceIndexByFilename[source.filename]];
			if (source.content.original !== uniqueSource.content) throw new Error(`Illegal source: same filename (${source.filename}), different contents`);
		}
		this.sources.push(source);
		return this;
	}
	append(str, options) {
		this.addSource({
			content: new MagicString(str),
			separator: options && options.separator || ""
		});
		return this;
	}
	clone() {
		const bundle = new Bundle({
			intro: this.intro,
			separator: this.separator
		});
		this.sources.forEach((source) => {
			bundle.addSource({
				filename: source.filename,
				content: source.content.clone(),
				separator: source.separator
			});
		});
		return bundle;
	}
	generateDecodedMap(options = {}) {
		const names = [];
		let x_google_ignoreList;
		this.sources.forEach((source) => {
			Object.keys(source.content.storedNames).forEach((name) => {
				if (!names.includes(name)) names.push(name);
			});
		});
		const mappings = new Mappings(options.hires);
		if (this.intro) mappings.advance(this.intro);
		this.sources.forEach((source, i) => {
			if (i > 0) mappings.advance(this.separator);
			const sourceIndex = source.filename ? this.uniqueSourceIndexByFilename[source.filename] : -1;
			const magicString = source.content;
			const locate = getLocator(magicString.original);
			if (magicString.intro) mappings.advance(magicString.intro);
			magicString.firstChunk.eachNext((chunk) => {
				const loc = locate(chunk.start);
				if (chunk.intro.length) mappings.advance(chunk.intro);
				if (source.filename) if (chunk.edited) mappings.addEdit(sourceIndex, chunk.content, loc, chunk.storeName ? names.indexOf(chunk.original) : -1);
				else mappings.addUneditedChunk(sourceIndex, chunk, magicString.original, loc, magicString.sourcemapLocations);
				else mappings.advance(chunk.content);
				if (chunk.outro.length) mappings.advance(chunk.outro);
			});
			if (magicString.outro) mappings.advance(magicString.outro);
			if (source.ignoreList && sourceIndex !== -1) {
				if (x_google_ignoreList === void 0) x_google_ignoreList = [];
				x_google_ignoreList.push(sourceIndex);
			}
		});
		return {
			file: options.file ? options.file.split(/[/\\]/).pop() : void 0,
			sources: this.uniqueSources.map((source) => {
				return options.file ? getRelativePath(options.file, source.filename) : source.filename;
			}),
			sourcesContent: this.uniqueSources.map((source) => {
				return options.includeContent ? source.content : null;
			}),
			names,
			mappings: mappings.raw,
			x_google_ignoreList
		};
	}
	generateMap(options) {
		return new SourceMap(this.generateDecodedMap(options));
	}
	getIndentString() {
		const indentStringCounts = {};
		this.sources.forEach((source) => {
			const indentStr = source.content._getRawIndentString();
			if (indentStr === null) return;
			if (!indentStringCounts[indentStr]) indentStringCounts[indentStr] = 0;
			indentStringCounts[indentStr] += 1;
		});
		return Object.keys(indentStringCounts).sort((a, b) => {
			return indentStringCounts[a] - indentStringCounts[b];
		})[0] || "	";
	}
	indent(indentStr) {
		if (!arguments.length) indentStr = this.getIndentString();
		if (indentStr === "") return this;
		let trailingNewline = !this.intro || this.intro.slice(-1) === "\n";
		this.sources.forEach((source, i) => {
			const separator = source.separator !== void 0 ? source.separator : this.separator;
			const indentStart = trailingNewline || i > 0 && /\r?\n$/.test(separator);
			source.content.indent(indentStr, {
				exclude: source.indentExclusionRanges,
				indentStart
			});
			trailingNewline = source.content.lastChar() === "\n";
		});
		if (this.intro) this.intro = indentStr + this.intro.replace(/^[^\n]/gm, (match, index) => {
			return index > 0 ? indentStr + match : match;
		});
		return this;
	}
	prepend(str) {
		this.intro = str + this.intro;
		return this;
	}
	toString() {
		const body = this.sources.map((source, i) => {
			const separator = source.separator !== void 0 ? source.separator : this.separator;
			return (i > 0 ? separator : "") + source.content.toString();
		}).join("");
		return this.intro + body;
	}
	isEmpty() {
		if (this.intro.length && this.intro.trim()) return false;
		if (this.sources.some((source) => !source.content.isEmpty())) return false;
		return true;
	}
	length() {
		return this.sources.reduce((length, source) => length + source.content.length(), this.intro.length);
	}
	trimLines() {
		return this.trim("[\\r\\n]");
	}
	trim(charType) {
		return this.trimStart(charType).trimEnd(charType);
	}
	trimStart(charType) {
		const rx = new RegExp(`^${charType || "\\s"}+`);
		this.intro = this.intro.replace(rx, "");
		if (!this.intro) {
			let source;
			let i = 0;
			do {
				source = this.sources[i++];
				if (!source) break;
			} while (!source.content.trimStartAborted(charType));
		}
		return this;
	}
	trimEnd(charType) {
		const rx = new RegExp(`${charType || "\\s"}+$`);
		let source;
		let i = this.sources.length - 1;
		do {
			source = this.sources[i--];
			if (!source) {
				this.intro = this.intro.replace(rx, "");
				break;
			}
		} while (!source.content.trimEndAborted(charType));
		return this;
	}
};
//#endregion
export { Bundle, MagicString, MagicString as default, SourceMap };
