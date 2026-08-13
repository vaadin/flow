import { LitElement, type PropertyValues } from 'lit';
/**
 * The loading indicator states
 */
export declare const enum LoadingBarState {
    IDLE = "",
    FIRST = "first",
    SECOND = "second",
    THIRD = "third"
}
declare global {
    interface HTMLElementTagNameMap {
        'vaadin-connection-indicator': ConnectionIndicator;
    }
}
/**
 * Component showing loading and connection indicator. When added to DOM,
 * listens for changes on `window.Vaadin.connectionState` ConnectionStateStore.
 */
export declare class ConnectionIndicator extends LitElement {
    #private;
    static get instance(): ConnectionIndicator;
    /**
     * Initialize global connection indicator instance at
     * window.Vaadin.connectionIndicator and add instance to the document body.
     */
    static create(): ConnectionIndicator;
    /**
     * The delay before showing the loading indicator, in ms.
     */
    accessor firstDelay: number;
    /**
     * The delay before the loading indicator goes into "second" state, in ms.
     */
    accessor secondDelay: number;
    /**
     * The delay before the loading indicator goes into "third" state, in ms.
     */
    accessor thirdDelay: number;
    /**
     * The duration for which the connection state change message is visible,
     * in ms.
     */
    accessor expandedDuration: number;
    /**
     * The message shown when the connection goes to connected state.
     */
    accessor onlineText: string;
    /**
     * The message shown when the connection goes to lost state.
     */
    accessor offlineText: string;
    /**
     * The message shown when the connection goes to reconnecting state.
     */
    accessor reconnectingText: string;
    accessor offline: boolean;
    accessor reconnecting: boolean;
    accessor expanded: boolean;
    accessor loading: boolean;
    readonly connectionStateListener: () => void;
    constructor();
    protected render(): import("lit-html").TemplateResult<1>;
    connectedCallback(): void;
    disconnectedCallback(): void;
    protected updated(props: PropertyValues): void;
    get applyDefaultTheme(): boolean;
    set applyDefaultTheme(applyDefaultTheme: boolean);
    protected createRenderRoot(): this;
}
/**
 * The global connection indicator object. Its appearance and behavior can be
 * configured via properties:
 *
 * connectionIndicator.firstDelay = 0;
 * connectionIndicator.onlineText = 'The application is online';
 *
 * To avoid altering the appearance while the indicator is active, apply the
 * configuration in your application 'frontend/index.ts' file.
 */
export declare const connectionIndicator: ConnectionIndicator;
//# sourceMappingURL=ConnectionIndicator.d.ts.map