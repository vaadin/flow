import { ConnectClient } from '@hilla/frontend';
const client = new ConnectClient({ prefix: 'connect' });
export default client;

export interface Subscription<T> {
    cancel: () => void;
    onNext: (callback: (value: T) => void) => Subscription<T>;
    onError: (callback: () => void) => Subscription<T>;
    onComplete: (callback: () => void) => Subscription<T>;
}

let channel: Channel;

class Channel {
    readonly name: string;
    private inited!: Promise<WebSocket>
    private nextId = 0;
    private onNextCallbacks = new Map<string, (value: any) => void>();
    private onCompleteCallbacks = new Map<string, () => void>();
    private onErrorCallbacks = new Map<string, () => void>();

    constructor(name: string) {
        this.name = name;
        this.connectWebsocket();
    }

    connectWebsocket() {
        this.inited = new Promise((resolve, reject) => {
            const socket = new WebSocket(location.origin.replace(/^http/, 'ws') + "/" + this.name);
            socket.onerror = (event) => {
                reject(event);
            }
            socket.onopen = (event) => {
                resolve(socket);
            }
            socket.onmessage = (event) => {
                const message = JSON.parse(event.data);
                const id = message.id;

                if (message["@type"] === "update") {
                    const callback = this.onNextCallbacks.get(id);
                    if (!callback) {
                        console.log("No callback for stream id " + id);
                        return;
                    }
                    callback(message.item);
                } else if (message["@type"] === "complete") {
                    const callback = this.onCompleteCallbacks.get(id);
                    if (callback) {
                        callback();
                    }
                    this.onNextCallbacks.delete(id);
                    this.onCompleteCallbacks.delete(id);
                    this.onErrorCallbacks.delete(id);
                } else if (message["@type"] === "error") {
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
            };
            socket.onclose = (event) => {
                // Typically closed after a timeout. We need to reopen
                this.connectWebsocket();
            }
        });
    }

    open(endpointName: string, methodName: string, params?: any): Subscription<any> {
        const id: string = "" + this.nextId++;
        let socket: WebSocket;
        this.inited.then(s => {
            socket = s; socket.send(JSON.stringify({ "@type": "connect", id, endpointName, methodName, params }))
        });

        const subscription: Subscription<any> = {
            onNext: (callback: ((value: any) => void)): Subscription<any> => {
                this.onNextCallbacks.set(id, callback);
                return subscription;
            }, onComplete: (callback: () => void): Subscription<any> => {
                this.onCompleteCallbacks.set(id, callback);
                return subscription;
            }, onError: (callback: () => void): Subscription<any> => {
                this.onErrorCallbacks.set(id, callback);
                return subscription;
            }, cancel: () => {
                socket.send(JSON.stringify({ "@type": "close", id }))
            }

        };
        return subscription;
    }
}

export function open(endpointName: string, methodName: string, params?: any): Subscription<any> {
    if (!channel) {
        channel = new Channel("hilla");
    }
    return channel.open(endpointName, methodName, params);
}