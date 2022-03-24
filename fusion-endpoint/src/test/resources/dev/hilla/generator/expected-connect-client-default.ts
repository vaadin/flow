import { ConnectClient } from '@hilla/frontend';
import { DefaultEventsMap } from "@socket.io/component-emitter";
import { io, Socket } from 'socket.io-client';

/* Most of the code below should end up in @hilla/frontend but the first version is easier to do this way.*/
/** The following is copied from @hilla/frontend as the methods are not exported in package.json */
export function getCookie(name: string) {
    const prefix = `${name}=`;
    return document.cookie
        .split(/;[ ]?/)
        .filter((cookie) => cookie.startsWith(prefix))
        .map((cookie) => cookie.slice(prefix.length))[0];
}
/** @internal */
export const VAADIN_CSRF_HEADER = 'X-CSRF-Token';
/** @internal */
export const VAADIN_CSRF_COOKIE_NAME = 'csrfToken';
/** @internal */
export const SPRING_CSRF_COOKIE_NAME = 'XSRF-TOKEN';
function extractContentFromMetaTag(element: any) {
    if (element) {
        const value = element.content;
        if (value && value.toLowerCase() !== 'undefined') {
            return value;
        }
    }
    return undefined;
}
/** @internal */
function getSpringCsrfHeaderFromMetaTag(doc: Document) {
    const csrfHeader = doc.head.querySelector('meta[name="_csrf_header"]');
    return extractContentFromMetaTag(csrfHeader);
}
/** @internal */
function getSpringCsrfTokenFromMetaTag(doc: Document) {
    const csrfToken = doc.head.querySelector('meta[name="_csrf"]');
    return extractContentFromMetaTag(csrfToken);
}
/** @internal */
export function getSpringCsrfInfo(doc: Document): any {
    const csrfHeader = getSpringCsrfHeaderFromMetaTag(doc);
    let csrf = getCookie(SPRING_CSRF_COOKIE_NAME);
    if (!csrf || csrf.length === 0) {
        csrf = getSpringCsrfTokenFromMetaTag(doc);
    }
    const headers: any = {};
    if (csrf && csrfHeader) {
        headers._csrf = csrf;
        headers._csrf_header = csrfHeader;
    }
    return headers;
}
/** @internal */
export function getSpringCsrfTokenHeadersForAuthRequest(doc: Document) {
    const csrfInfo = getSpringCsrfInfo(doc);
    const headers:any = {};
    if (csrfInfo._csrf && csrfInfo._csrf_header) {
        headers[csrfInfo._csrf_header] = csrfInfo._csrf;
    }
    return headers;
}
/** @internal */
export function getCsrfTokenHeadersForEndpointRequest(doc: Document) {
    const headers:any = {};
    const csrfInfo = getSpringCsrfInfo(doc);
    if (csrfInfo._csrf && csrfInfo._csrf_header) {
        headers[csrfInfo._csrf_header] = csrfInfo._csrf;
    }
    else {
        headers[VAADIN_CSRF_HEADER] = getCookie(VAADIN_CSRF_COOKIE_NAME) || '';
    }
    return headers;
}

/** End of copying */

const client = new ConnectClient({prefix: 'connect'});
export default client;

interface AbstractMessage {
    id: string;
}

interface ClientErrorMessage extends AbstractMessage {
    "@type": "error";
    message: string;
}
interface ClientCompleteMessage extends AbstractMessage {
    "@type": "complete";
}
interface ClientUpdateMessage extends AbstractMessage {
    "@type": "update";
    item: any;
}

type ClientMessage = ClientUpdateMessage | ClientCompleteMessage | ClientErrorMessage;

interface ServerConnectMessage extends AbstractMessage {
    id: string;
    "@type": "subscribe";
    endpointName: string;
    methodName: string;
    params?: any
}
interface ServerCloseMessage extends AbstractMessage {
    id: string;
    "@type": "unsubscribe";
}

type ServerMessage = ServerConnectMessage | ServerCloseMessage;

export interface Subscription<T> {
    cancel: () => void;
    onNext: (callback: (value: T) => void) => Subscription<T>;
    onError: (callback: () => void) => Subscription<T>;
    onComplete: (callback: () => void) => Subscription<T>;
}

let connection: EndpointConnection | undefined = undefined

class EndpointConnection {
    private nextId = 0;
    private endpointInfos = new Map<string, string>();
    private onNextCallbacks = new Map<string, (value: any) => void>();
    private onCompleteCallbacks = new Map<string, () => void>();
    private onErrorCallbacks = new Map<string, () => void>();

    private socket!: Socket<DefaultEventsMap, DefaultEventsMap>;

    constructor() {
        this.connectWebsocket();
    }
    connectWebsocket() {
        const extraHeaders = getCsrfTokenHeadersForEndpointRequest(document);
        this.socket = io("/hilla", { path: "/VAADIN/hillapush/" ,extraHeaders});
        this.socket.on("message", (message) => {
            this.handleMessage(JSON.parse(message));
        });
    }

    private handleMessage(message: ClientMessage) {
        const id = message.id;
        const endpointInfo = this.endpointInfos.get(id);

        if (message["@type"] === "update") {
            console.debug(`Got value ${JSON.stringify(message.item)} for ${endpointInfo}`);
            const callback = this.onNextCallbacks.get(id);
            if (!callback) {
                console.log("No callback for stream id " + id);
                return;
            }
            callback(message.item);
        } else if (message["@type"] === "complete") {
            console.debug(`Server completed ${endpointInfo}`);
            const callback = this.onCompleteCallbacks.get(id);
            if (callback) {
                callback();
            }

            this.onNextCallbacks.delete(id);
            this.onCompleteCallbacks.delete(id);
            this.onErrorCallbacks.delete(id);
            this.endpointInfos.delete(id);
        } else if (message["@type"] === "error") {
            console.error(`Error in ${endpointInfo}: ${message.message}`);
            const callback = this.onErrorCallbacks.get(id);
            if (callback) {
                callback();
            }
            this.onNextCallbacks.delete(id);
            this.onCompleteCallbacks.delete(id);
            this.onErrorCallbacks.delete(id);
        } else {
            console.error("Unknown message from server: " + message);
        }
    }

    private send(message: ServerMessage) {
        this.socket.send(message);
    }

    subscribe(endpointName: string, methodName: string, params?: Array<any>): Subscription<any> {
        const id: string = "" + this.nextId++;

        const msg: ServerConnectMessage = { "@type": "subscribe", id, endpointName, methodName, params };
        const endpointInfo = `${endpointName}.${methodName}(${JSON.stringify(params)})`;
        console.debug(`Subscribing to ${endpointInfo}`);
        this.send(msg);
        this.endpointInfos.set(id, endpointInfo);
        const hillaSubscription: Subscription<any> = {
            onNext: (callback: ((value: any) => void)): Subscription<any> => {
                this.onNextCallbacks.set(id, callback);
                return hillaSubscription;
            }, onComplete: (callback: () => void): Subscription<any> => {
                this.onCompleteCallbacks.set(id, callback);
                return hillaSubscription;
            }, onError: (callback: () => void): Subscription<any> => {
                this.onErrorCallbacks.set(id, callback);
                return hillaSubscription;
            }, cancel: () => {
                console.debug(`Ending subscription to ${endpointInfo}`);

                const closeMessage: ServerCloseMessage = { "@type": "unsubscribe", id };
                this.send(closeMessage);
            }
        };
        return hillaSubscription;
    }

}

export function subscribe(endpointName: string, methodName: string, params?: any): Subscription<any> {
    if (!connection) {
        connection = new EndpointConnection();
    }

    return connection.subscribe(endpointName, methodName, params ? Object.values(params) : []);
}