import { existsSync } from "node:fs";
//#region src/checkers/_shared/lintScheduler.ts
const DEFAULT_DEBOUNCE_MS = 300;
function createLintScheduler(opts) {
	const { debounceMs, onBatch, fileExists = existsSync } = opts;
	const pending = /* @__PURE__ */ new Set();
	let timer = null;
	let inFlight = null;
	let disposed = false;
	function clearTimer() {
		if (timer !== null) {
			clearTimeout(timer);
			timer = null;
		}
	}
	function drain() {
		if (disposed || inFlight || pending.size === 0) return;
		clearTimer();
		const snapshot = [...pending].filter((f) => fileExists(f));
		pending.clear();
		if (snapshot.length === 0) return;
		const raw = onBatch(snapshot);
		inFlight = raw;
		raw.catch((err) => {
			console.error("[vite-plugin-checker] lint batch failed:", err);
		}).then(() => {
			inFlight = null;
			drain();
		});
	}
	return {
		schedule(filePath) {
			if (disposed) return;
			pending.add(filePath);
			clearTimer();
			timer = setTimeout(() => {
				timer = null;
				drain();
			}, debounceMs);
		},
		dispose() {
			disposed = true;
			clearTimer();
			pending.clear();
			return (inFlight ?? Promise.resolve()).catch(() => {});
		}
	};
}
//#endregion
export { DEFAULT_DEBOUNCE_MS, createLintScheduler };

//# sourceMappingURL=lintScheduler.js.map