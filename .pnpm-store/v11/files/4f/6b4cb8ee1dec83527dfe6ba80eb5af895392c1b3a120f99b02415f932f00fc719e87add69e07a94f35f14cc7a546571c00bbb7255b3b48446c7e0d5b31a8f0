import { ReadonlySignal, Signal } from "@preact/signals-core";
import { ReactNode } from "react";
interface ShowProps<T = boolean> {
    when: Signal<T> | ReadonlySignal<T> | (() => T);
    fallback?: ReactNode | (() => ReactNode);
    children: ReactNode | ((value: NonNullable<T>) => ReactNode);
}
export declare function Show<T = boolean>(props: ShowProps<T>): JSX.Element | null;
export declare namespace Show {
    var displayName: string;
}
type ForEach<T> = ReadonlyArray<T> | Signal<ReadonlyArray<T>> | ReadonlySignal<ReadonlyArray<T>>;
interface ForProps<T> {
    each: ForEach<T> | (() => ForEach<T>);
    fallback?: ReactNode | (() => ReactNode);
    getKey?: (item: T, index: number) => string | number;
    children: (value: T, index: number) => ReactNode;
}
export declare function For<T>(props: ForProps<T>): JSX.Element | null;
export declare namespace For {
    var displayName: string;
}
export declare function useLiveSignal<T>(value: T): Signal<T>;
export declare function useSignalRef<T>(value: T): Signal<T> & {
    current: T;
};
export {};
