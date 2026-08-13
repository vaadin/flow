import path from "node:path";
import fs from "node:fs/promises";
//#region src/sources.ts
function normalizePath(p, cwd) {
	let filename = p;
	if (filename && filename.startsWith("file://")) filename = filename.slice(7);
	if (filename) {
		filename = path.isAbsolute(filename) ? filename : path.resolve(cwd, filename);
		filename = path.normalize(filename);
	}
	return filename;
}
async function readSources(files) {
	const cache = /* @__PURE__ */ new Map();
	await Promise.all(files.map(async (file) => {
		try {
			const source = await fs.readFile(file, "utf8");
			cache.set(file, source);
		} catch {}
	}));
	return cache;
}
//#endregion
export { normalizePath, readSources };

//# sourceMappingURL=sources.js.map