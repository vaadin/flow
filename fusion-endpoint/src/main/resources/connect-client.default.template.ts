import { ConnectClient } from '@hilla/frontend';
const client = new ConnectClient({prefix: '{{PREFIX}}'});
export default client;

export interface Subscription<T> {
    cancel: () => void;
    onNext: (callback: (value: T) => void) => Subscription<T>;
    onError: (callback: () => void) => Subscription<T>;
    onComplete: (callback: () => void) => Subscription<T>;
}


export function subscribe(endpointName: string, methodName: string, params?: any): Subscription<any> {
    throw new Error("Push support in Hilla is not enabled. Enable it in the debug window or by adding com.vaadin.experimental.hillaPush=true to src/main/resources/vaadin-featureflags.properties");
}