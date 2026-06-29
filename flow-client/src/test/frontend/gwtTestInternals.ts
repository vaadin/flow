import { registerInternals } from '../../main/frontend/internal/registerInternals';

// esbuild entry: bundled to a classic IIFE and embedded/eval'd by
// ClientEngineTestBase so GwtTests register the same TypeScript implementations
// the GWT engine calls into (window.Vaadin.Flow.internal.*) as production does.
// GwtTests do not run the engine's init(), so they would otherwise be missing
// these. See the gwt-test-internals build step and MIGRATION_STRATEGY.md.
registerInternals();
