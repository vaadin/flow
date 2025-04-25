import { AtRule, parse } from 'postcss';

export function extractGlobalCSSRules(css: string) {
  const globalRules: AtRule[] = [];

  parse(css).walkAtRules('font-face', (rule) => {
    globalRules.push(rule);
  });

  return globalRules.map((rule) => rule.toString()).join('\n');
}
