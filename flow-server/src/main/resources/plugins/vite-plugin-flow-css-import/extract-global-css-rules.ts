import { AtRule, parse } from 'postcss';

export function extractGlobalCSSRules(css: string) {
  const globalRules: AtRule[] = [];

  parse(css).walkAtRules((rule) => {
    if (['import', 'font-face'].includes(rule.name)) {
      globalRules.push(rule);
    }
  });

  return globalRules.map((rule) => rule.toString()).join('\n');
}
