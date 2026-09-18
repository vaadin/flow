//#region rolldown:runtime
var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __copyProps = (to, from, except, desc) => {
	if (from && typeof from === "object" || typeof from === "function") for (var keys = __getOwnPropNames(from), i = 0, n = keys.length, key; i < n; i++) {
		key = keys[i];
		if (!__hasOwnProp.call(to, key) && key !== except) __defProp(to, key, {
			get: ((k) => from[k]).bind(null, key),
			enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable
		});
	}
	return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", {
	value: mod,
	enumerable: true
}) : target, mod));

//#endregion
let node_fs = require("node:fs");
node_fs = __toESM(node_fs);
let node_path = require("node:path");
node_path = __toESM(node_path);

//#region src/err.ts
var EtaError = class extends Error {
	constructor(message) {
		super(message);
		this.name = "Eta Error";
	}
};
var EtaParseError = class extends EtaError {
	constructor(message) {
		super(message);
		this.name = "EtaParser Error";
	}
};
var EtaRuntimeError = class extends EtaError {
	constructor(message) {
		super(message);
		this.name = "EtaRuntime Error";
	}
};
var EtaFileResolutionError = class extends EtaError {
	constructor(message) {
		super(message);
		this.name = "EtaFileResolution Error";
	}
};
var EtaNameResolutionError = class extends EtaError {
	constructor(message) {
		super(message);
		this.name = "EtaNameResolution Error";
	}
};
/**
* Throws an EtaError with a nicely formatted error and message showing where in the template the error occurred.
*/
function ParseErr(message, str, indx) {
	const whitespace = str.slice(0, indx).split(/\n/);
	const lineNo = whitespace.length;
	const colNo = whitespace[lineNo - 1].length + 1;
	message += " at line " + lineNo + " col " + colNo + ":\n\n  " + str.split(/\n/)[lineNo - 1] + "\n  " + Array(colNo).join(" ") + "^";
	throw new EtaParseError(message);
}
function RuntimeErr(originalError, str, lineNo, path) {
	const lines = str.split("\n");
	const start = Math.max(lineNo - 3, 0);
	const end = Math.min(lines.length, lineNo + 3);
	const filename = path;
	const context = lines.slice(start, end).map((line, i) => {
		const curr = i + start + 1;
		return (curr === lineNo ? " >> " : "    ") + curr + "| " + line;
	}).join("\n");
	const err = new EtaRuntimeError((filename ? filename + ":" + lineNo + "\n" : "line " + lineNo + "\n") + context + "\n\n" + originalError.message);
	err.name = originalError.name;
	err.cause = originalError;
	throw err;
}

//#endregion
//#region src/file-handling.ts
function readFile(path) {
	let res = "";
	try {
		res = node_fs.readFileSync(path, "utf8");
	} catch (err) {
		if (err?.code === "ENOENT") throw new EtaFileResolutionError(`Could not find template: ${path}`);
		else throw err;
	}
	return res;
}
function resolvePath(templatePath, options) {
	let resolvedFilePath = "";
	const views = this.config.views;
	if (!views) throw new EtaFileResolutionError("Views directory is not defined");
	const baseFilePath = options?.filepath;
	const defaultExtension = this.config.defaultExtension === void 0 ? ".eta" : this.config.defaultExtension;
	const cacheIndex = JSON.stringify({
		filename: baseFilePath,
		path: templatePath,
		views: this.config.views
	});
	templatePath += node_path.extname(templatePath) ? "" : defaultExtension;
	if (baseFilePath) {
		if (this.config.cacheFilepaths && this.filepathCache[cacheIndex]) return this.filepathCache[cacheIndex];
		if (absolutePathRegExp.exec(templatePath)?.length) {
			const formattedPath = templatePath.replace(/^\/*|^\\*/, "");
			resolvedFilePath = node_path.join(views, formattedPath);
		} else resolvedFilePath = node_path.join(node_path.dirname(baseFilePath), templatePath);
	} else resolvedFilePath = node_path.join(views, templatePath);
	if (dirIsChild(views, resolvedFilePath)) {
		if (baseFilePath && this.config.cacheFilepaths) this.filepathCache[cacheIndex] = resolvedFilePath;
		return resolvedFilePath;
	} else throw new EtaFileResolutionError(`Template '${templatePath}' is not in the views directory`);
}
function dirIsChild(parent, dir) {
	const relative = node_path.relative(parent, dir);
	return relative && !relative.startsWith("..") && !node_path.isAbsolute(relative);
}
const absolutePathRegExp = /^\\|^\//;

//#endregion
//#region src/compile.ts
/* istanbul ignore next */
const AsyncFunction = (async () => {}).constructor;
/**
* Takes a template string and returns a template function that can be called with (data, config)
*
* @param str - The template string
* @param config - A custom configuration object (optional)
*/
function compile(str, options) {
	const config = this.config;
	const ctor = options?.async ? AsyncFunction : Function;
	try {
		return new ctor(config.varName, "options", this.compileToString.call(this, str, options));
	} catch (e) {
		if (e instanceof SyntaxError) throw new EtaParseError("Bad template syntax\n\n" + e.message + "\n" + Array(e.message.length + 1).join("=") + "\n" + this.compileToString.call(this, str, options) + "\n");
		else throw e;
	}
}

//#endregion
//#region src/compile-string.ts
/**
* Compiles a template string to a function string. Most often users just use `compile()`, which calls `compileToString` and creates a new function using the result
*/
function compileToString(str, options) {
	const config = this.config;
	const isAsync = options?.async;
	const compileBody$1 = this.compileBody;
	const buffer = this.parse.call(this, str);
	let res = `${config.functionHeader}
let include = (__eta_t, __eta_d) => this.render(__eta_t, {...${config.varName}, ...(__eta_d ?? {})}, options);
let includeAsync = (__eta_t, __eta_d) => this.renderAsync(__eta_t, {...${config.varName}, ...(__eta_d ?? {})}, options);

let __eta = {res: "", e: this.config.escapeFunction, f: this.config.filterFunction, blocks: {}${config.debug ? ", line: 1, templateStr: \"" + str.replace(/\\|"/g, "\\$&").replace(/\r\n|\n|\r/g, "\\n") + "\"" : ""}};

function layout(path, data) {
  __eta.layout = path;
  __eta.layoutData = data;
}${config.debug ? "try {" : ""}${config.useWith ? "with(" + config.varName + "||{}){" : ""}

function ${config.outputFunctionName}(s){__eta.res+=s;}
function capture(fn){const s=__eta.res;__eta.res='';try{fn();return __eta.res}finally{__eta.res=s;}}
async function captureAsync(fn){const s=__eta.res;__eta.res='';try{await fn();return __eta.res}finally{__eta.res=s;}}
function block(name,fn){if(__eta.layout){if(fn){__eta.blocks[name]=capture(fn);}return '';}const b=${config.varName}.__blocks||{};if(name in b){return b[name];}return fn?capture(fn):'';}
async function blockAsync(name,fn){if(__eta.layout){if(fn){__eta.blocks[name]=await captureAsync(fn);}return '';}const b=${config.varName}.__blocks||{};if(name in b){return b[name];}return fn?await captureAsync(fn):'';}

${compileBody$1.call(this, buffer)}
if (__eta.layout) {
  __eta.res = ${isAsync ? "await includeAsync" : "include"} (__eta.layout, {...${config.varName}, body: __eta.res, ...__eta.layoutData, __blocks: __eta.blocks});
}
${config.useWith ? "}" : ""}${config.debug ? "} catch (e) { this.RuntimeErr(e, __eta.templateStr, __eta.line, options.filepath) }" : ""}
return __eta.res;
`;
	if (config.plugins) for (let i = 0; i < config.plugins.length; i++) {
		const plugin = config.plugins[i];
		if (plugin.processFnString) res = plugin.processFnString(res, config);
	}
	return res;
}
/**
* Loops through the AST generated by `parse` and transform each item into JS calls
*
* **Example**
*
* ```js
* let templateAST = ['Hi ', { val: 'it.name', t: 'i' }]
* compileBody.call(Eta, templateAST)
* // => "__eta.res+='Hi '\n__eta.res+=__eta.e(it.name)\n"
* ```
*/
function compileBody(buff) {
	const config = this.config;
	let i = 0;
	const buffLength = buff.length;
	let returnStr = "";
	for (; i < buffLength; i++) {
		const currentBlock = buff[i];
		if (typeof currentBlock === "string") returnStr += "__eta.res+='" + currentBlock + "';\n";
		else {
			const type = currentBlock.t;
			let content = currentBlock.val || "";
			if (config.debug) returnStr += "__eta.line=" + currentBlock.lineNo + "\n";
			if (type === "r") {
				if (config.autoFilter) content = "__eta.f(" + content + ")";
				returnStr += "__eta.res+=" + content + ";\n";
			} else if (type === "i") {
				if (config.autoFilter) content = "__eta.f(" + content + ")";
				if (config.autoEscape) content = "__eta.e(" + content + ")";
				returnStr += "__eta.res+=" + content + ";\n";
			} else if (type === "e") returnStr += content + "\n";
			else if (Object.hasOwn(config.customTags, type)) returnStr += `__eta.res+=this.config.customTags[${JSON.stringify(type)}](${JSON.stringify(content)},${config.varName});\n`;
		}
	}
	return returnStr;
}

//#endregion
//#region src/utils.ts
/**
* Takes a string within a template and trims it, based on the preceding tag's whitespace control and `config.autoTrim`
*/
function trimWS(str, config, wsLeft, wsRight) {
	let leftTrim;
	let rightTrim;
	if (Array.isArray(config.autoTrim)) {
		leftTrim = config.autoTrim[1];
		rightTrim = config.autoTrim[0];
	} else leftTrim = rightTrim = config.autoTrim;
	if (wsLeft || wsLeft === false) leftTrim = wsLeft;
	if (wsRight || wsRight === false) rightTrim = wsRight;
	if (!rightTrim && !leftTrim) return str;
	if (leftTrim === "slurp" && rightTrim === "slurp") return str.trim();
	if (leftTrim === "_" || leftTrim === "slurp") str = str.trimStart();
	else if (leftTrim === "-" || leftTrim === "nl") str = str.replace(/^(?:\r\n|\n|\r)/, "");
	if (rightTrim === "_" || rightTrim === "slurp") str = str.trimEnd();
	else if (rightTrim === "-" || rightTrim === "nl") str = str.replace(/(?:\r\n|\n|\r)$/, "");
	return str;
}
/**
* A map of special HTML characters to their XML-escaped equivalents
*/
const escMap = {
	"&": "&amp;",
	"<": "&lt;",
	">": "&gt;",
	"\"": "&quot;",
	"'": "&#39;"
};
function replaceChar(s) {
	return escMap[s];
}
/**
* XML-escapes an input value after converting it to a string
*
* @param str - Input value (usually a string)
* @returns XML-escaped string
*/
function XMLEscape(str) {
	const newStr = String(str);
	if (/[&<>"']/.test(newStr)) return newStr.replace(/[&<>"']/g, replaceChar);
	else return newStr;
}

//#endregion
//#region src/config.ts
/** Eta's base (global) configuration */
const defaultConfig = {
	autoEscape: true,
	autoFilter: false,
	autoTrim: [false, "nl"],
	cache: false,
	cacheFilepaths: true,
	customTags: {},
	debug: false,
	escapeFunction: XMLEscape,
	filterFunction: (val) => String(val),
	outputFunctionName: "output",
	functionHeader: "",
	parse: {
		exec: "",
		interpolate: "=",
		raw: "~"
	},
	plugins: [],
	rmWhitespace: false,
	tags: ["<%", "%>"],
	useWith: false,
	varName: "it",
	defaultExtension: ".eta"
};

//#endregion
//#region src/parse.ts
const templateLitReg = /`(?:\\[\s\S]|\${(?:[^{}]|{(?:[^{}]|{[^}]*})*})*}|(?!\${)[^\\`])*`/g;
const singleQuoteReg = /'(?:\\[\s\w"'\\`]|[^\n\r'\\])*?'/g;
const doubleQuoteReg = /"(?:\\[\s\w"'\\`]|[^\n\r"\\])*?"/g;
/** Escape special regular expression characters inside a string */
function escapeRegExp(string) {
	return string.replace(/[.*+\-?^${}()|[\]\\]/g, "\\$&");
}
function getLineNo(str, index) {
	return str.slice(0, index).split("\n").length;
}
function parse(str) {
	const config = this.config;
	let buffer = [];
	let trimLeftOfNextStr = false;
	let lastIndex = 0;
	const parseOptions = config.parse;
	const customTagPrefixes = Object.keys(config.customTags);
	if (config.plugins) for (let i = 0; i < config.plugins.length; i++) {
		const plugin = config.plugins[i];
		if (plugin.processTemplate) str = plugin.processTemplate(str, config);
	}
	if (config.rmWhitespace) str = str.replace(/[\r\n]+/g, "\n").replace(/^\s+|\s+$/gm, "");
	templateLitReg.lastIndex = 0;
	singleQuoteReg.lastIndex = 0;
	doubleQuoteReg.lastIndex = 0;
	function pushString(strng, shouldTrimRightOfString) {
		if (strng) {
			strng = trimWS(strng, config, trimLeftOfNextStr, shouldTrimRightOfString);
			if (strng) {
				strng = strng.replace(/\\|'/g, "\\$&").replace(/\r\n|\n|\r/g, "\\n");
				buffer.push(strng);
			}
		}
	}
	const prefixes = [
		parseOptions.exec,
		parseOptions.interpolate,
		parseOptions.raw,
		...customTagPrefixes
	].reduce((accumulator, prefix) => {
		if (accumulator && prefix) return accumulator + "|" + escapeRegExp(prefix);
		else if (prefix) return escapeRegExp(prefix);
		else return accumulator;
	}, "");
	const parseOpenReg = new RegExp(escapeRegExp(config.tags[0]) + "(-|_)?\\s*(" + prefixes + ")?\\s*", "g");
	const parseCloseReg = new RegExp("'|\"|`|\\/\\*|(\\s*(-|_)?" + escapeRegExp(config.tags[1]) + ")", "g");
	let m;
	while (m = parseOpenReg.exec(str)) {
		const precedingString = str.slice(lastIndex, m.index);
		lastIndex = m[0].length + m.index;
		const wsLeft = m[1];
		const prefix = m[2] || "";
		pushString(precedingString, wsLeft);
		parseCloseReg.lastIndex = lastIndex;
		let closeTag;
		let currentObj = false;
		while (closeTag = parseCloseReg.exec(str)) if (closeTag[1]) {
			const content = str.slice(lastIndex, closeTag.index);
			parseOpenReg.lastIndex = lastIndex = parseCloseReg.lastIndex;
			trimLeftOfNextStr = closeTag[2];
			currentObj = {
				t: prefix === parseOptions.exec ? "e" : prefix === parseOptions.raw ? "r" : prefix === parseOptions.interpolate ? "i" : customTagPrefixes.includes(prefix) ? prefix : "",
				val: content
			};
			break;
		} else {
			const char = closeTag[0];
			if (char === "/*") {
				const commentCloseInd = str.indexOf("*/", parseCloseReg.lastIndex);
				if (commentCloseInd === -1) ParseErr("unclosed comment", str, closeTag.index);
				parseCloseReg.lastIndex = commentCloseInd;
			} else if (char === "'") {
				singleQuoteReg.lastIndex = closeTag.index;
				if (singleQuoteReg.exec(str)) parseCloseReg.lastIndex = singleQuoteReg.lastIndex;
				else ParseErr("unclosed string", str, closeTag.index);
			} else if (char === "\"") {
				doubleQuoteReg.lastIndex = closeTag.index;
				if (doubleQuoteReg.exec(str)) parseCloseReg.lastIndex = doubleQuoteReg.lastIndex;
				else ParseErr("unclosed string", str, closeTag.index);
			} else if (char === "`") {
				templateLitReg.lastIndex = closeTag.index;
				if (templateLitReg.exec(str)) parseCloseReg.lastIndex = templateLitReg.lastIndex;
				else ParseErr("unclosed string", str, closeTag.index);
			}
		}
		if (currentObj) {
			if (config.debug) currentObj.lineNo = getLineNo(str, m.index);
			buffer.push(currentObj);
		} else ParseErr("unclosed tag", str, m.index);
	}
	pushString(str.slice(lastIndex, str.length), false);
	if (config.plugins) for (let i = 0; i < config.plugins.length; i++) {
		const plugin = config.plugins[i];
		if (plugin.processAST) buffer = plugin.processAST(buffer, config);
	}
	return buffer;
}

//#endregion
//#region src/render.ts
function handleCache(template, options) {
	const templateStore = options?.async ? this.templatesAsync : this.templatesSync;
	if (this.resolvePath && this.readFile && !template.startsWith("@")) {
		const templatePath = options.filepath;
		const cachedTemplate = templateStore.get(templatePath);
		if (this.config.cache && cachedTemplate) return cachedTemplate;
		else {
			const templateString = this.readFile(templatePath);
			const templateFn = this.compile(templateString, options);
			if (this.config.cache) templateStore.define(templatePath, templateFn);
			return templateFn;
		}
	} else {
		const cachedTemplate = templateStore.get(template);
		if (cachedTemplate) return cachedTemplate;
		else throw new EtaNameResolutionError(`Failed to get template '${template}'`);
	}
}
function render(template, data, meta) {
	let templateFn;
	const options = {
		...meta,
		async: false
	};
	if (typeof template === "string") {
		if (this.resolvePath && this.readFile && !template.startsWith("@")) options.filepath = this.resolvePath(template, options);
		templateFn = handleCache.call(this, template, options);
	} else templateFn = template;
	return templateFn.call(this, data, options);
}
function renderAsync(template, data, meta) {
	let templateFn;
	const options = {
		...meta,
		async: true
	};
	if (typeof template === "string") {
		if (this.resolvePath && this.readFile && !template.startsWith("@")) options.filepath = this.resolvePath(template, options);
		templateFn = handleCache.call(this, template, options);
	} else templateFn = template;
	const res = templateFn.call(this, data, options);
	return Promise.resolve(res);
}
function renderString(template, data) {
	const templateFn = this.compile(template, { async: false });
	return render.call(this, templateFn, data);
}
function renderStringAsync(template, data) {
	const templateFn = this.compile(template, { async: true });
	return renderAsync.call(this, templateFn, data);
}

//#endregion
//#region src/storage.ts
/**
* Handles storage and accessing of values
*
* In this case, we use it to store compiled template functions
* Indexed by their `name` or `filename`
*/
var Cacher = class {
	constructor(cache) {
		this.cache = cache;
	}
	define(key, val) {
		this.cache[key] = val;
	}
	get(key) {
		return this.cache[key];
	}
	remove(key) {
		delete this.cache[key];
	}
	reset() {
		this.cache = {};
	}
	load(cacheObj) {
		this.cache = {
			...this.cache,
			...cacheObj
		};
	}
};

//#endregion
//#region src/internal.ts
var Eta$1 = class {
	constructor(customConfig) {
		if (customConfig) this.config = {
			...defaultConfig,
			...customConfig
		};
		else this.config = { ...defaultConfig };
		const reserved = [
			this.config.parse.exec,
			this.config.parse.interpolate,
			this.config.parse.raw,
			"-",
			"_"
		];
		for (const prefix of Object.keys(this.config.customTags)) if (reserved.includes(prefix)) throw new EtaError(`Custom tag prefix "${prefix}" conflicts with a built-in prefix`);
	}
	config;
	RuntimeErr = RuntimeErr;
	compile = compile;
	compileToString = compileToString;
	compileBody = compileBody;
	parse = parse;
	render = render;
	renderAsync = renderAsync;
	renderString = renderString;
	renderStringAsync = renderStringAsync;
	filepathCache = {};
	templatesSync = new Cacher({});
	templatesAsync = new Cacher({});
	resolvePath = null;
	readFile = null;
	configure(customConfig) {
		this.config = {
			...this.config,
			...customConfig
		};
	}
	withConfig(customConfig) {
		return {
			...this,
			config: {
				...this.config,
				...customConfig
			}
		};
	}
	loadTemplate(name, template, options) {
		if (typeof template === "string") (options?.async ? this.templatesAsync : this.templatesSync).define(name, this.compile(template, options));
		else {
			let templates = this.templatesSync;
			if (template.constructor.name === "AsyncFunction" || options?.async) templates = this.templatesAsync;
			templates.define(name, template);
		}
	}
};

//#endregion
//#region src/index.ts
var Eta = class extends Eta$1 {
	readFile = readFile;
	resolvePath = resolvePath;
};

//#endregion
exports.Eta = Eta;
exports.EtaError = EtaError;
exports.EtaFileResolutionError = EtaFileResolutionError;
exports.EtaNameResolutionError = EtaNameResolutionError;
exports.EtaParseError = EtaParseError;
exports.EtaRuntimeError = EtaRuntimeError;
//# sourceMappingURL=index.cjs.map