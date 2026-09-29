import { types as BabelTypes, template as BabelTemplate, PluginObj } from "@babel/core";
interface PluginArgs {
    types: typeof BabelTypes;
    template: typeof BabelTemplate;
}
export interface PluginOptions {
    /**
     * Specify the mode to use:
     * - `auto`: Automatically wrap all components that use signals.
     * - `manual`: Only wrap components that are annotated with `@useSignals` in a JSX comment.
     * - `all`: Makes all components reactive to signals.
     */
    mode?: "auto" | "manual" | "all";
    /** Specify a custom package to import the `useSignals` hook from. */
    importSource?: string;
    experimental?: {
        /**
         * If set to true, the component body will not be wrapped in a try/finally
         * block and instead the next component render or a microtick will stop
         * tracking signals for this component. This is an experimental feature and
         * may be removed in the future.
         * @default false
         */
        noTryFinally?: boolean;
    };
}
export default function signalsTransform({ types: t }: PluginArgs, options: PluginOptions): PluginObj;
export {};
