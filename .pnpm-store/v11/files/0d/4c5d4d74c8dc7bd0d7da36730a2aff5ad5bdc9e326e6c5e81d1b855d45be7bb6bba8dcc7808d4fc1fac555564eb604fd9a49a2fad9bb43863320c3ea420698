import * as wd from 'wd';
export declare function normalize(browsers: (string | {
    browserName: string;
})[]): string[];
/**
 * Expands an array of browser identifiers for locally installed browsers into
 * their webdriver capabilities objects.
 *
 * If `names` is empty, or contains `all`, all installed browsers will be used.
 */
export declare function expand(names: string[], browserOptions: {
    [name: string]: string[];
}): Promise<wd.Capabilities[]>;
/**
 * Detects any locally installed browsers that we support.
 *
 * Exported and declared as `let` variables for testabilty in wct.
 */
export declare let detect: (browserOptions: {
    [name: string]: string[];
}) => Promise<{
    [browser: string]: wd.Capabilities;
}>;
/**
 * Exported and declared as `let` variables for testabilty in wct.
 *
 * @return A list of local browser names that are supported by
 *     the current environment.
 */
export declare let supported: () => string[];
