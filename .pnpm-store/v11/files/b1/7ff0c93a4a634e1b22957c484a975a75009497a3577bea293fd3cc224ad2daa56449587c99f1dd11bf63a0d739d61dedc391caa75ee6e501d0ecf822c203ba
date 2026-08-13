import { isMainThread as isMainThread$1, threadId } from "node:worker_threads";
//#region src/utils.ts
const isInVitestEntryThread = threadId === 0 && process.env.VITEST;
const isMainThread = isMainThread$1 || isInVitestEntryThread;
const TRANSIENT_FS_ERROR_CODES = new Set([
	"ENOENT",
	"EBUSY",
	"EPERM",
	"EACCES"
]);
function isTransientFsError(error) {
	if (typeof error !== "object" || error === null) return false;
	const { code } = error;
	return code !== void 0 && TRANSIENT_FS_ERROR_CODES.has(code);
}
function ignoreTransientFsError(error) {
	if (isTransientFsError(error)) return;
	throw error;
}
//#endregion
export { ignoreTransientFsError, isInVitestEntryThread, isMainThread, isTransientFsError };

//# sourceMappingURL=utils.js.map