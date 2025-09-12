// Sample of IntrinsicElements to mimic the react-types loaded
// via node modules. Used by ReactPropertiesPlugin

declare namespace JSX {
    interface IntrinsicElements {
        h1: { text: string };
        div: { [key: string]: any, "aria-label": string };
    }
}