// Clamps a value between an upper and lower bound.
// We use ternary operators because it makes the minified code
// 2 times shorter then `Math.min(Math.max(a,b),c)`
export const clamp = (number, min = 0, max = 1) => {
    return number > max ? max : number < min ? min : number;
};
export const round = (number, digits = 0, base = Math.pow(10, digits)) => {
    return Math.round(base * number) / base;
};
//# sourceMappingURL=math.js.map