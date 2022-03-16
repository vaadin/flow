import { ConnectClient } from '@hilla/frontend';
import { APPLICATION_JSON, BufferEncoders, encodeAndAddWellKnownMetadata, encodeRoute, MESSAGE_RSOCKET_COMPOSITE_METADATA, MESSAGE_RSOCKET_ROUTING, RSocketClient } from 'rsocket-core';
import { Flowable, Single } from 'rsocket-flowable';
import type { ISubscriber, Payload, ReactiveSocket } from 'rsocket-types';
import RSocketWebSocketClient from 'rsocket-websocket-client';
const client = new ConnectClient({ prefix: 'connect' });
export default client;



interface AbstractMessage {
    id: string;
}

interface ClientErrorMessage extends AbstractMessage {
    "@type": "error";
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
    "@type": "connect";
    endpointName: string;
    methodName: string;
    params?: any
}
interface ServerCloseMessage extends AbstractMessage {
    id: string;
    "@type": "close";
}

type ServerMessage = ServerConnectMessage | ServerCloseMessage;

export interface Subscription<T> {
    cancel: () => void;
    onNext: (callback: (value: T) => void) => Subscription<T>;
    onError: (callback: () => void) => Subscription<T>;
    onComplete: (callback: () => void) => Subscription<T>;
}

const metadata = encodeAndAddWellKnownMetadata(Buffer.alloc(0), MESSAGE_RSOCKET_ROUTING, encodeRoute('rs'));
let connection: EndpointConnection | undefined = undefined

class EndpointConnection {
    private nextId = 0;
    private onNextCallbacks = new Map<string, (value: any) => void>();
    private onCompleteCallbacks = new Map<string, () => void>();
    private onErrorCallbacks = new Map<string, () => void>();
    private rsocketClient: RSocketClient<AbstractMessage, unknown>;
    private pendingSocket: Single<ReactiveSocket<AbstractMessage, unknown>>;
    private sink: Promise<{ send(message: ServerMessage): void; complete(): void; }>;

    constructor(port: number) {

        const wsUrl = location.protocol.replace(/^http/, 'ws') + "//" + location.hostname + ":" + port;
        this.rsocketClient = new RSocketClient({
            setup: {
                keepAlive: 60 * 1000, // ms between sending keepalive to server
                lifetime: 180 * 1000, // ms timeout if no keepalive response
                dataMimeType: APPLICATION_JSON.string,
                metadataMimeType: MESSAGE_RSOCKET_COMPOSITE_METADATA.string, // format of `metadata`
            },
            transport: new RSocketWebSocketClient({
                url: `${wsUrl}`,
                debug: true
            }, BufferEncoders),
        });

        this.pendingSocket = this.rsocketClient.connect();
        let sinkResolve: (value: {
            send(message: ServerMessage): void;
            complete(): void;
        }) => void;
        this.sink = new Promise((resolve, _reject) => {
            sinkResolve = resolve;
        });

        this.pendingSocket.then(socket => {
            const publisher = new Flowable<ServerMessage>(sub => {
                // Number of the requests requested by subscriber.
                let requestedRequests = 0;
                // Buffer for requests which should be sent but not requested yet.
                const pendingRequests: ServerMessage[] = [];
                let completed = false;

                const sink = {
                    send(message: ServerMessage) {
                        if (completed) {
                            // It's completed, nobody expects this request.
                            console.log("Dropping message as flowable has completed", message);
                            return;
                        }
                        if (requestedRequests > 0) {
                            --requestedRequests;
                            sub.onNext(message);
                        } else {
                            pendingRequests.push(message);
                        }
                    },
                    complete() {
                        if (!completed) {
                            completed = true;
                            sub.onComplete();
                        }
                    },
                }


                sub.onSubscribe({
                    cancel: () => {
                        // TODO This happens when the server closes the connection, e.g. the endpoint does not exist. 
                        debugger;
                    },
                    request: (n) => {
                        const toSend = pendingRequests.slice(0, n);
                        requestedRequests += n - toSend.length;
                        for (const pending of toSend) {
                            sub.onNext(pending);
                        }

                        // resolve(sub);
                    },
                });

                sinkResolve(sink);
            });
            const mappedPublisher = publisher.map(message => {
                return {
                    data: Buffer.from(JSON.stringify(message), 'utf8'),
                    metadata
                }
            })
            socket.requestChannel(mappedPublisher as any).subscribe({
                onComplete: () => console.log('complete'),
                onError: (error: Error) => console.log('error', error),
                onNext: (payload: Payload<AbstractMessage, unknown>) => {
                    const message: ClientMessage = JSON.parse(payload.data!.toString()); // Data is a Buffer
                    console.log("Value from server", message);
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
                },
                onSubscribe: (subscription => {
                    subscription.request(10000000); // set it to some max value
                })
            })
        });


    }
    private send(message: ServerMessage) {
        console.log("Sending to server", message);
        this.sink.then(sink => sink.send(message));
    }

    open(endpointName: string, methodName: string, params?: any): Subscription<any> {
        const id: string = "" + this.nextId++;

        const msg: ServerConnectMessage = { "@type": "connect", id, endpointName, methodName, params };
        this.send(msg);

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
                const closeMessage: ServerCloseMessage = { "@type": "close", id };
                this.send(closeMessage);
            }
        };
        return hillaSubscription;
    }

}

export function open(endpointName: string, methodName: string, params?: any): Subscription<any> {
    if (!connection) {
        connection = new EndpointConnection(Number(location.port) + 1);
    }
    return connection.open(endpointName, methodName, params);
}