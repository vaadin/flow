import { ErrorObject } from 'ajv';
import { ValidationError } from './types/ValidationError';
export interface BetterAjvErrorsOptions<S = any> {
    errors: ErrorObject[] | null | undefined;
    data: any;
    schema: S;
    basePath?: string;
}
export declare const betterAjvErrors: <S = any>({ errors, data, schema, basePath, }: BetterAjvErrorsOptions<S>) => ValidationError[];
export { ValidationError };
