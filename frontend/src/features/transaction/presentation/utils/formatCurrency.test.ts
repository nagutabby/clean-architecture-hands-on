import { describe, it, expect } from 'vitest';
import { formatCurrency } from './formatCurrency';

describe('formatCurrency', () => {
  it('数値を3桁カンマ区切りの日本円表記に整形すること', () => {
    expect(formatCurrency(1280)).toMatch(/^[¥￥]1,280$/);
    expect(formatCurrency(300000)).toMatch(/^[¥￥]300,000$/);
  });

  it('0円を正しく整形すること', () => {
    expect(formatCurrency(0)).toMatch(/^[¥￥]0$/);
  });
});
