import { LitElement } from 'lit';
/**
 * The loading indicator states
 */
export declare const enum LoadingBarState {
    IDLE = "",
    FIRST = "first",
    SECOND = "second",
    THIRD = "third"
}
/**
 * Component showing loading and connection indicator. When added to DOM,
 * listens for changes on `window.Vaadin.connectionState` ConnectionStateStore.
 */
export declare class ConnectionIndicator extends LitElement {
    /**
     * Initialize global connection indicator instance at
     * window.Vaadin.connectionIndicator and add instance to the document body.
     */
    static create(): ConnectionIndicator;
    /**
     * The delay before showing the loading indicator, in ms.
     */
    firstDelay: number;
    /**
     * The delay before the loading indicator goes into "second" state, in ms.
     */
    secondDelay: number;
    /**
     * The delay before the loading indicator goes into "third" state, in ms.
     */
    thirdDelay: number;
    /**
     * The duration for which the connection state change message is visible,
     * in ms.
     */
    expandedDuration: number;
    /**
     * The message shown when the connection goes to connected state.
     */
    onlineText: string;
    /**
     * The message shown when the connection goes to lost state.
     */
    offlineText: string;
    /**
     * The message shown when the connection goes to reconnecting state.
     */
    reconnectingText: string;
    private offline;
    private reconnecting;
    private expanded;
    private loading;
    private loadingBarState;
    private applyDefaultThemeState;
    private firstTimeout;
    private secondTimeout;
    private thirdTimeout;
    private expandedTimeout;
    private connectionStateStore?;
    private readonly connectionStateListener;
    private lastMessageState;
    constructor();
    protected render(): import("lit-html").TemplateResult<1>;
    connectedCallback(): void;
    disconnectedCallback(): void;
    get applyDefaultTheme(): boolean;
    set applyDefaultTheme(applyDefaultTheme: boolean);
    protected createRenderRoot(): this;
    /**
     * Update state flags.
     *
     * @returns true if the connection message changes, and therefore a new
     * message should be shown
     */
    private updateConnectionState;
    private updateLoading;
    private renderMessage;
    private updateTheme;
    private getDefaultStyle;
    private getLoadingBarStyle;
    private timeoutFor;
    static get instance(): ConnectionIndicator;
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