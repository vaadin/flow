export interface BundlerModuleInfo {
    isEntry: boolean;
    /** rollup only — rolldown (and therefore vite 8) does not provide this. */
    isExternal?: boolean;
    importedIds: readonly string[];
    dynamicallyImportedIds?: readonly string[];
}
export type GetModuleInfo = (moduleId: string) => BundlerModuleInfo | null;
export interface BundlerPluginContext {
    meta: {
        rollupVersion: string;
    };
    warn(log: string): void;
    error(err: string): never;
    emitFile(file: {
        type: "asset";
        fileName: string;
        source: string;
    }): string;
    getModuleInfo: GetModuleInfo;
}
export interface BundlerRenderedModule {
    readonly code: string | null;
    renderedLength: number;
}
export interface BundlerOutputChunk {
    type: "chunk";
    code: string;
    /** `unknown` because rollup and rolldown model source maps differently. */
    map?: unknown;
    modules: Record<string, BundlerRenderedModule>;
    facadeModuleId: string | null;
}
export interface BundlerOutputAsset {
    type: "asset";
}
export type BundlerOutputBundle = Record<string, BundlerOutputChunk | BundlerOutputAsset>;
/** Only the members this plugin reads; see the note above about widening. */
export interface BundlerOutputOptions {
    sourcemap?: boolean | "inline" | "hidden";
    dir?: string;
    file?: string;
}
export interface BundlerNormalizedOutputOptions {
    sourcemap: boolean | "inline" | "hidden";
    dir?: string;
    file?: string;
}
export interface VisualizerPlugin {
    name: string;
    generateBundle(this: BundlerPluginContext, outputOptions: BundlerNormalizedOutputOptions, outputBundle: BundlerOutputBundle): Promise<void>;
}
