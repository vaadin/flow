import availableTypedArrays from 'available-typed-arrays';
import { TypedArray } from 'is-typed-array';

declare namespace typedArrayByteOffset {
	export type TypedArrayName = ReturnType<typeof availableTypedArrays>[number];

	// a consumer's `lib` may not declare every name (eg, `Float16Array`), so only look up the globals it has
	export type TypedArrayConstructor = typeof globalThis[TypedArrayName & keyof typeof globalThis];

	export type ByteOffsetGetter = (x: TypedArray) => number;

	export type Implementation = (value: unknown) => number | false;

	export type { TypedArray };
}

declare function typedArrayByteOffset(value: TypedArray): number;
declare function typedArrayByteOffset(value: unknown): false;

export = typedArrayByteOffset;
