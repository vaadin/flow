/*
 * Copyright 2000-2024 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import {createRoot, Root} from "react-dom/client";
import {createElement, Dispatch, ReactNode, useReducer} from "react";

type FlowStateKeyChangedAction<K extends string, V> = Readonly<{
    type: 'stateKeyChanged',
    key: K,
    value: V,
}>;

type FlowStateReducerAction = FlowStateKeyChangedAction<string, unknown>;

function stateReducer<S extends Readonly<Record<string, unknown>>>(state: S, action: FlowStateReducerAction): S {
    switch (action.type) {
        case "stateKeyChanged":
            const {key, value} = action satisfies FlowStateKeyChangedAction<string, unknown>;
            return {
                ...state,
                key: value
            } as S;
    }
}

type DispatchEvent<T> = T extends undefined
    ? () => boolean
    : { dispatch(value: T): boolean }["dispatch"];

const emptyAction: Dispatch<unknown> = () => {};

type RenderHooks = Readonly<{
    useState: ReactAdapterElement["useState"]
    useCustomEvent: ReactAdapterElement["useCustomEvent"]
}>;

/**
 * A base class for Web Components that render using React. Enables creating
 * adapters for integrating React components with Flow. Intended for use with
 * `ReactAdapterComponent` Flow Java class.
 */
export abstract class ReactAdapterElement extends HTMLElement {
    #root: Root | undefined = undefined;
    #rootRendered: boolean = false;

    #state: Record<string, unknown> = Object.create(null);
    #stateSetters = new Map<string, Dispatch<unknown>>();
    #customEvents = new Map<string, DispatchEvent<unknown>>();
    #dispatchFlowState: Dispatch<FlowStateReducerAction> = emptyAction;

    readonly #renderHooks: RenderHooks;

    readonly #Wrapper: () => ReactNode;

    #unmountComplete = Promise.resolve();

    constructor() {
        super();
        this.#renderHooks = {
            useState: this.useState.bind(this),
            useCustomEvent: this.useCustomEvent.bind(this)
        };
        this.#Wrapper = this.#renderWrapper.bind(this);
    }

    public async connectedCallback() {
        await this.#unmountComplete;
        this.#root = createRoot(this);
        this.#maybeRenderRoot();
    }

    public async disconnectedCallback() {
        this.#unmountComplete = Promise.resolve();
        await this.#unmountComplete;
        this.#root?.unmount();
        this.#root = undefined;
        this.#rootRendered = false;
    }

    /**
     * A hook-like API for using stateful JS properties of the Web Component
     * from the React `render()`.
     *
     * @typeParam T - Type of the state value
     *
     * @param key - Web Component property name, which is used for two-way
     * value propagation from the server and back.
     * @param initialValue - Fallback initial value (optional). Only applies if
     * the Java component constructor does not invoke `setState`.
     * @returns A tuple with two values:
     * 1. The current state.
     * 2. The `set` function for changing the state and triggering render
     * @protected
     */
    protected useState<T>(key: string, initialValue?: T): [value: T, setValue: Dispatch<T>] {
        if (!this.#stateSetters.has(key)) {
            const value = ((this as Record<string, unknown>)[key] as T) ?? initialValue!;
            this.#state[key] = value;
            Object.defineProperty(this, key, {
                enumerable: true,
                get(): T {
                    return this.#state[key];
                },
                set(nextValue: T) {
                    this.#state[key] = nextValue;
                    this.#dispatchFlowState({type: 'stateKeyChanged', key, value});
                }
            });

            const dispatchChangedEvent = this.useCustomEvent<{value: T}>(`${key}-changed`, {detail: {value}});
            const setValue = (value: T) => {
                this.#state[key] = value;
                dispatchChangedEvent({value});
                this.#dispatchFlowState({type: 'stateKeyChanged', key, value});
            };
            this.#stateSetters.set(key, setValue as Dispatch<unknown>);
            return [value, setValue];
        }
        return [this.#state[key] as T, this.#stateSetters.get(key)!];
    }

    /**
     * A React hook-like helper to simplify dispatching a `CustomEvent` on the
     * Web Component from React.
     *
     * @typeParam T - The type for `event.detail` value (optional).
     *
     * @param type - The `CustomEvent` type string.
     * @param options - The settings for the `CustomEvent`.
     * @returns The `dispatch` function. The function parameters change
     * depending on the `T` generic type:
     * - For `undefined` type (default), has no parameters.
     * - For other types, has one parameter for the `event.detail` value of that type.
     * @protected
     */
    protected useCustomEvent<T = undefined>(type: string, options: CustomEventInit<T> = {}): DispatchEvent<T> {
        if (!this.#customEvents.has(type)) {
            const dispatch = ((detail?: T) => {
                const eventInitDict = "detail" in options ? {
                    ...options,
                    detail
                } : options;
                const event = new CustomEvent(type, eventInitDict);
                return this.dispatchEvent(event);
            }) as DispatchEvent<T>;
            this.#customEvents.set(type, dispatch);
            return dispatch;
        }
        return this.#customEvents.get(type)! as DispatchEvent<T>;
    }

    /**
     * The Web Component render function. To be implemented by users with React.
     *
     * @param hooks - the Web Component APIs exposed for React render.
     * @protected
     */
    protected abstract render(hooks: RenderHooks): ReactNode;

    #maybeRenderRoot() {
        if (this.#rootRendered || !this.#root) {
            return;
        }

        this.#root.render(createElement(this.#Wrapper));
        this.#rootRendered = true;
    }

    #renderWrapper(): ReactNode {
        const [state, dispatchFlowState] = useReducer(stateReducer, this.#state);
        this.#state = state;
        this.#dispatchFlowState = dispatchFlowState;
        return this.render(this.#renderHooks);
    }
}