//#region src/constants.d.ts
declare enum PriorityName {
  LEFT = "left",
  RIGHT = "right"
}
//#endregion
//#region src/type.d.ts
type UnionToIntersection<U> = (U extends any ? (k: U) => void : never) extends ((k: infer I) => void) ? I : never;
type Options = {
  /**
   * Merge object array properties.
   *
   * default: true
   */
  array: boolean;
  /**
   * Remove duplicates, when merging array elements.
   *
   * default: false
   */
  arrayDistinct: boolean;
  /**
   * Merge sources from left-to-right or right-to-left.
   * From v2 upwards default to left independent of the option priority.
   *
   * default: left (aka. options.priority)
   */
  arrayPriority: `${PriorityName}`;
  /**
   * Strategy to merge different object keys.
   *
   * @param target
   * @param key
   * @param value
   */
  strategy?: (target: Record<string, any>, key: string, value: unknown) => Record<string, any> | undefined;
  /**
   * Merge sources in place.
   *
   * default: false
   */
  inPlace?: boolean;
  /**
   * Deep clone input sources.
   *
   * default: false
   */
  clone?: boolean;
  /**
   * Merge sources from left-to-right or right-to-left.
   * From v2 upwards default to right.
   *
   * default: left
   */
  priority: `${PriorityName}`;
};
type OptionsInput = Partial<Options>;
type MergerSource = any[] | Record<string, any>;
type MergerSourceUnwrap<T extends MergerSource> = T extends Array<infer Return> ? Return : T;
type MergerResult<B extends MergerSource> = UnionToIntersection<MergerSourceUnwrap<B>>;
type MergerContext = {
  options: Options;
  map: WeakMap<any, any>;
};
type Merger = <B extends MergerSource[]>(...sources: B) => MergerResult<B>;
//#endregion
//#region src/module.d.ts
declare function createMerger(input?: OptionsInput): Merger;
declare const merge: Merger;
//#endregion
//#region src/utils/array.d.ts
declare function distinctArray<T = any>(arr: T[]): T[];
//#endregion
//#region src/utils/check.d.ts
declare function isObject(item: unknown): item is Record<string, any>;
declare function isSafeKey(key: string): boolean;
declare function isEqual(x: any, y: any): boolean;
//#endregion
//#region src/utils/clone.d.ts
declare function polyfillClone<T>(input: T): T;
declare function clone<T>(value: T): T;
//#endregion
//#region src/utils/object.d.ts
declare function hasOwnProperty<X extends {}, Y extends PropertyKey>(obj: X, prop: Y): obj is X & Record<Y, unknown>;
//#endregion
//#region src/utils/options.d.ts
declare function buildOptions(options?: OptionsInput): Options;
declare function togglePriority(priority: `${PriorityName}`): "right" | "left";
//#endregion
//#region src/presets.d.ts
/**
 * Assign source attributes to a target object.
 *
 * @param target
 * @param sources
 */
declare function assign<A extends Record<string, any>, B extends Record<string, any>[]>(target: A, ...sources: B): A & MergerResult<B>;
//#endregion
export { Merger, MergerContext, MergerResult, MergerSource, MergerSourceUnwrap, Options, OptionsInput, PriorityName, assign, buildOptions, clone, createMerger, distinctArray, hasOwnProperty, isEqual, isObject, isSafeKey, merge, polyfillClone, togglePriority };
//# sourceMappingURL=index.d.cts.map