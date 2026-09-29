var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);
var argv_exports = {};
__export(argv_exports, {
  default: () => parseArgsStringToArgv,
  parseArgsStringToArgv: () => parseArgsStringToArgv
});
module.exports = __toCommonJS(argv_exports);
function parseArgsStringToArgv(value, env, file) {
  const myRegexp = (
    // @ts-expect-error Bypass typescript validation
    /([^\s'"]([^\s'"]*(['"])([^\3]*?)\3)+[^\s'"]*)|[^\s'"]+|(['"])([^\5]*?)\5/gi
  );
  const myString = value;
  const myArray = [];
  if (env) {
    myArray.push(env);
  }
  if (file) {
    myArray.push(file);
  }
  let match;
  do {
    match = myRegexp.exec(myString);
    if (match !== null) {
      myArray.push(firstString(match[1], match[6], match[0]));
    }
  } while (match !== null);
  return myArray;
}
function firstString(...args) {
  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (typeof arg === "string") {
      return arg;
    }
  }
}
// Annotate the CommonJS export names for ESM import in node:
0 && (module.exports = {
  parseArgsStringToArgv
});
//# sourceMappingURL=argv.js.map