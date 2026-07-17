import { defineConfig } from 'vitest/config';

// @jsonforms/angular's ESM bundle imports deep lodash submodules (e.g. `lodash/cloneDeep`)
// without a file extension. Node's native ESM resolver - which Vitest defers to for
// node_modules packages it treats as external/SSR - can't resolve that bare specifier the
// way bundlers used by Angular's production build can, so it 404s only under Vitest.
// Inlining the package forces Vitest to process it through Vite (applying resolve.alias)
// instead of letting Node import it directly.
export default defineConfig({
  resolve: {
    alias: [{ find: /^lodash\/(.*)$/, replacement: 'lodash/$1.js' }]
  },
  ssr: {
    noExternal: ['@jsonforms/angular', 'lodash']
  },
  test: {
    server: {
      deps: {
        inline: ['@jsonforms/angular', 'lodash']
      }
    }
  }
});
