module.exports = {
  env: {
    es6: true,
    es2022: true,
    node: true,
  },
  parserOptions: {
    ecmaVersion: 2022,
  },
  extends: [
    "eslint:recommended",
    "google",
  ],
  rules: {
    "no-restricted-globals": ["error", "name", "length"],
    "prefer-arrow-callback": "error",
    "quotes": ["error", "double", {"allowTemplateLiterals": true}],
    "require-jsdoc": "off",
    "max-len": ["error", {"code": 120, "ignoreUrls": true, "ignoreStrings": true}],
  },
  overrides: [
    {
      files: ["**/*.spec.*", "**/*.test.*"],
      env: {
        mocha: true,
      },
      rules: {
        "require-jsdoc": "off",
        "valid-jsdoc": "off",
      },
    },
  ],
  globals: {},
};
