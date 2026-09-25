import { createRequire } from "node:module";
import path, { dirname } from "node:path";
import { access, cp, mkdir, readFile, rm, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import lockfile from "proper-lockfile";
//#region src/checkers/vueTsc/prepareVueTsc.ts
const _require = createRequire(import.meta.url);
const _dirname = dirname(fileURLToPath(import.meta.url));
const vueTscDir = dirname(_require.resolve("vue-tsc/package.json"));
const proxyApiPath = _require.resolve("@volar/typescript/lib/node/proxyCreateProgram", { paths: [vueTscDir] });
const extraSupportedExtensions = [".vue"];
const LOCK_TIMEOUT_MS = 6e4;
const STALE_TIMEOUT_MS = 3e4;
async function isFixtureValid(targetTsDir, vueTscFlagFile, currTsVersion) {
	try {
		await access(targetTsDir);
		const targetTsVersion = JSON.parse(await readFile(path.resolve(targetTsDir, "package.json"), "utf8")).version;
		await access(vueTscFlagFile);
		const fixtureFlagContent = await readFile(vueTscFlagFile, "utf8");
		return targetTsVersion === currTsVersion && fixtureFlagContent === proxyApiPath;
	} catch {
		return false;
	}
}
async function prepareVueTsc() {
	const targetTsDir = path.resolve(_dirname, "typescript-vue-tsc");
	const vueTscFlagFile = path.resolve(targetTsDir, "vue-tsc-resolve-path");
	const currTsVersion = _require("typescript/package.json").version;
	if (Number(currTsVersion.split(".")[0]) < 5) throw new Error("\x1B[35m[vite-plugin-checker] Since 0.7.0, vue-tsc checkers requires TypeScript 5.0.0 or newer version.\nPlease upgrade TypeScript, or use v0.6.4 which works with vue-tsc^1 if you can't upgrade. Check the pull request https://github.com/fi3ework/vite-plugin-checker/pull/327 for detail.\x1B[39m\n");
	if (await isFixtureValid(targetTsDir, vueTscFlagFile, currTsVersion)) return { targetTsDir };
	let release;
	try {
		release = await lockfile.lock(_dirname, {
			lockfilePath: path.resolve(_dirname, ".vue-tsc-fixture.lock"),
			stale: STALE_TIMEOUT_MS,
			retries: {
				retries: Math.ceil(LOCK_TIMEOUT_MS / 1e3),
				factor: 1,
				minTimeout: 1e3,
				maxTimeout: 2e3,
				randomize: true
			}
		});
	} catch (err) {
		throw new Error("[vite-plugin-checker] Failed to acquire lock for vue-tsc fixture preparation. Another process may be holding the lock.\n" + String(err));
	}
	try {
		if (await isFixtureValid(targetTsDir, vueTscFlagFile, currTsVersion)) return { targetTsDir };
		await rm(targetTsDir, {
			force: true,
			recursive: true
		});
		await mkdir(targetTsDir, { recursive: true });
		await cp(path.resolve(_require.resolve("typescript"), "../.."), targetTsDir, { recursive: true });
		await writeFile(vueTscFlagFile, proxyApiPath);
		await overrideTscJs(_require.resolve(path.resolve(targetTsDir, "lib/typescript.js")));
	} finally {
		await release();
	}
	return { targetTsDir };
}
async function overrideTscJs(tscJsPath) {
	const languagePluginsFile = path.resolve(_dirname, "languagePlugins.cjs");
	let tsc = await readFile(tscJsPath, "utf8");
	const extsText = extraSupportedExtensions.map((ext) => `"${ext}"`).join(", ");
	tsc = replace(tsc, /supportedTSExtensions = .*(?=;)/, (s) => s + `.map((group, i) => i === 0 ? group.splice(0, 0, ${extsText}) && group : group)`);
	tsc = replace(tsc, /supportedJSExtensions = .*(?=;)/, (s) => s + `.map((group, i) => i === 0 ? group.splice(0, 0, ${extsText}) && group : group)`);
	tsc = replace(tsc, /allSupportedExtensions = .*(?=;)/, (s) => s + `.map((group, i) => i === 0 ? group.splice(0, 0, ${extsText}) && group : group)`);
	const extsText2 = extraSupportedExtensions.map((ext) => `"${ext}"`).join(", ");
	tsc = replace(tsc, /function changeExtension\(/, (s) => `function changeExtension(path, newExtension) {
					return [${extsText2}].some(ext => path.endsWith(ext))
						? path + newExtension
						: _changeExtension(path, newExtension)
					}\n${s.replace("changeExtension", "_changeExtension")}`);
	tsc = replace(tsc, /function createProgram\(.+\) {/, (s) => `var createProgram = require(${JSON.stringify(proxyApiPath)}).proxyCreateProgram(${[
		"new Proxy({}, { get(_target, p, _receiver) { return eval(p); } } )",
		"_createProgram",
		`require(${JSON.stringify(languagePluginsFile)}).getLanguagePlugins`
	].join(", ")});\n${s.replace("createProgram", "_createProgram")}`);
	function replace(_text, ...[search, replace]) {
		const before = _text;
		const after = _text.replace(search, replace);
		if (after === before) throw `Search string not found: ${JSON.stringify(search.toString())}`;
		return after;
	}
	await writeFile(tscJsPath, tsc);
}
//#endregion
export { prepareVueTsc };

//# sourceMappingURL=prepareVueTsc.js.map