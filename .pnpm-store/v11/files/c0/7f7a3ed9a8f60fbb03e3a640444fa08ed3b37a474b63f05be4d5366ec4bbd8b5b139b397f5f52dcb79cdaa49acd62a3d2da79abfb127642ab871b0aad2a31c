import { normalizePath } from "../../sources.js";
//#region src/checkers/_shared/applyBatchedDiagnostics.ts
/**
* Bucket a flat diagnostic list by file and write per-file diagnostics
* to the manager. Every file in `scannedFiles` is written (empty list
* clears stale diagnostics). Files not in `scannedFiles` are untouched.
*
* Paths are normalized via `sources.ts#normalizePath(root)` on both sides
* before keying to defend against checker normalizers that differ in
* casing, separators, or unresolved `.` segments.
*/
function applyBatchedDiagnostics(manager, scannedFiles, diagnostics, root) {
	const byFile = /* @__PURE__ */ new Map();
	for (const d of diagnostics) {
		if (!d.id) continue;
		const key = normalizePath(d.id, root);
		const normalized = {
			...d,
			id: key
		};
		const bucket = byFile.get(key);
		if (bucket) bucket.push(normalized);
		else byFile.set(key, [normalized]);
	}
	for (const file of scannedFiles) {
		const key = normalizePath(file, root);
		manager.updateByFileId(key, byFile.get(key) ?? []);
	}
}
//#endregion
export { applyBatchedDiagnostics };

//# sourceMappingURL=applyBatchedDiagnostics.js.map