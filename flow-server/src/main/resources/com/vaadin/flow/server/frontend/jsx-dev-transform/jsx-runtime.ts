import {
  Fragment as reactFragment,
  jsx as reactJsx,
  jsxs as reactJsxs,
} from "react/jsx-runtime";

export const Fragment = reactFragment;
export const jsx = reactJsx;
export const jsxs = reactJsxs;

throw new Error(
  "Do not use this transform for production builds. It is only meant for development."
);
