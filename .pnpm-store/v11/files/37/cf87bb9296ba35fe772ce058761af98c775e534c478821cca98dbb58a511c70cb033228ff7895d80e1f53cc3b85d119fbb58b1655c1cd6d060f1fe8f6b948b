//#region src/checkers/_shared/lintScheduler.d.ts
declare const DEFAULT_DEBOUNCE_MS = 300;
interface LintScheduler {
  /** Enqueue an absolute file path for the next batch. No-op after dispose. */
  schedule(filePath: string): void;
  /**
   * Stop accepting new events, discard pending files that have not yet been
   * submitted, and return a promise that resolves when any in-flight batch
   * finishes. Callers must ensure their `onBatch` eventually settles — a
   * hanging `onBatch` will hang `dispose()`.
   */
  dispose(): Promise<void>;
}
interface LintSchedulerOptions {
  debounceMs: number;
  onBatch: (files: string[]) => Promise<void>;
  /** Existence probe, overridable for tests. Defaults to `fs.existsSync`. */
  fileExists?: (filePath: string) => boolean;
}
declare function createLintScheduler(opts: LintSchedulerOptions): LintScheduler;
//#endregion
export { DEFAULT_DEBOUNCE_MS, LintScheduler, LintSchedulerOptions, createLintScheduler };
//# sourceMappingURL=lintScheduler.d.ts.map