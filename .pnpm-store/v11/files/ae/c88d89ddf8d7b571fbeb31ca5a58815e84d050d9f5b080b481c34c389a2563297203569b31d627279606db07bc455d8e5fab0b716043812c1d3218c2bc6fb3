import { Signal, ReadonlySignal, SignalOptions, EffectOptions, type Model, type ModelConstructor } from "@preact/signals-core";
import type { SignalsDevToolsAPI } from "../../../debug/src/devtools";
export declare function wrapJsx<T>(jsx: T): T;
declare const symDispose: unique symbol;
interface Effect {
    _sources: object | undefined;
    _debugCallback?: () => void;
    _start(): () => void;
    _callback(): void;
    _dispose(): void;
}
/**
 * Use this flag to represent a bare `useSignals` call that doesn't manually
 * close its effect store and relies on auto-closing when the next useSignals is
 * called or after a microtask
 */
declare const UNMANAGED = 0;
/**
 * Use this flag to represent a `useSignals` call that is manually closed by a
 * try/finally block in a component's render method. This is the default usage
 * that the react-transform plugin uses.
 */
declare const MANAGED_COMPONENT = 1;
/**
 * Use this flag to represent a `useSignals` call that is manually closed by a
 * try/finally block in a hook body. This is the default usage that the
 * react-transform plugin uses.
 */
declare const MANAGED_HOOK = 2;
/**
 * An enum defining how this store is used. See the documentation for each enum
 * member for more details.
 * @see {@link UNMANAGED}
 * @see {@link MANAGED_COMPONENT}
 * @see {@link MANAGED_HOOK}
 */
type EffectStoreUsage = typeof UNMANAGED | typeof MANAGED_COMPONENT | typeof MANAGED_HOOK;
export interface EffectStore {
    /**
     * An enum defining how this hook is used and whether it is invoked in a
     * component's body or hook body. See the comment on `EffectStoreUsage` for
     * more details.
     */
    readonly _usage: EffectStoreUsage;
    readonly effect: Effect;
    subscribe(onStoreChange: () => void): () => void;
    getSnapshot(): number;
    /** startEffect - begin tracking signals used in this component */
    _start(): void;
    /** finishEffect - stop tracking the signals used in this component */
    f(): void;
    [symDispose](): void;
}
export declare function ensureFinalCleanup(): void;
/**
 * Custom hook to create the effect to track signals used during render and
 * subscribe to changes to rerender the component when the signals change.
 */
export declare function _useSignalsImplementation(_usage?: EffectStoreUsage, componentName?: string): EffectStore;
export declare function useSignals(usage?: EffectStoreUsage, componentName?: string): EffectStore;
export declare function useSignal<T>(value: T, options?: SignalOptions<T>): Signal<T>;
export declare function useSignal<T = undefined>(): Signal<T | undefined>;
export declare function useComputed<T>(compute: () => T, options?: SignalOptions<T>): ReadonlySignal<T>;
export declare function useSignalEffect(cb: () => void | (() => void), options?: EffectOptions): void;
declare global {
    interface Window {
        __PREACT_SIGNALS_DEVTOOLS__: SignalsDevToolsAPI;
    }
}
export declare function useModel<TModel>(factory: ModelConstructor<TModel, []> | (() => Model<TModel>)): Model<TModel>;
export {};
