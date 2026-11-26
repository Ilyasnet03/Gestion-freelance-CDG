const eslint = require('@eslint/js');
const tseslint = require('typescript-eslint');

module.exports = [
  {
    ignores: ['dist/**', 'coverage/**', '.angular/**'],
  },
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
];
