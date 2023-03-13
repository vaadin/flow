const matcher = /^#?([0-9A-F]{3,8})$/i;
export const validHex = (value, alpha) => {
    const match = matcher.exec(value);
    const length = match ? match[1].length : 0;
    return (length === 3 || // '#rgb' format
        length === 6 || // '#rrggbb' format
        (!!alpha && length === 4) || // '#rgba' format
        (!!alpha && length === 8) // '#rrggbbaa' format
    );
};
//# sourceMappingURL=validate.js.map