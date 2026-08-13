import { NormalizedDiagnostic } from "../../logger.js";
import { FileDiagnosticManager } from "../../FileDiagnosticManager.js";

//#region src/checkers/_shared/applyBatchedDiagnostics.d.ts
/**
 * Bucket a flat diagnostic list by file and write per-file diagnostics
 * to the manager. Every file in `scannedFiles` is written (empty list
 * clears stale diagnostics). Files not in `scannedFiles` are untouched.
 *
 * Paths are normalized via `sources.ts#normalizePath(root)` on both sides
 * before keying to defend against checker normalizers that differ in
 * casing, separators, or unresolved `.` segments.
 */
declare function applyBatchedDiagnostics(manager: FileDiagnosticManager, scannedFiles: string[], diagnostics: NormalizedDiagnostic[], root: string): void;
//#endregion
export { applyBatchedDiagnostics };
//# sourceMappingURL=applyBatchedDiagnostics.d.ts.map