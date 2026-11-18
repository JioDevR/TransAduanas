import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const r = (p) => fileURLToPath(new URL(p, import.meta.url))

const sgaMocksPlugin = () => ({
  name: 'sga-relative-mocks',
  resolveId(source, importer) {
    if (!importer || !importer.includes('sga-components')) return null
    if (source.startsWith('../services/')) return r('./src/sga-mocks/storesAndServices.js')
    if (source.startsWith('../composables/')) return r('./src/sga-mocks/composables.js')
    if (source.startsWith('../utils/') || source.startsWith('../data/')) return r('./src/sga-mocks/utils.js')
    return null
  }
})

export default defineConfig({
  plugins: [vue(), sgaMocksPlugin()],
  resolve: {
    alias: {
      '@primevue/themes': r('./node_modules/@primevue/themes'),
      '@/core/auth/roles': r('./src/sga-components/roles.js'),
      '@/shared/components': r('./src/sga-components/shared'),
      '@/shared/composables/useNotification': r('./src/sga-mocks/composables.js'),
      '@/shared/composables/useConfirmation': r('./src/sga-mocks/composables.js'),
      '@/shared/composables/usePagination': r('./src/sga-mocks/composables.js'),
      '@/features/comercial/composables/useCotizacionCalculator': r('./src/sga-mocks/composables.js'),
      '@/features/comercial/composables/useModalidadPuertos': r('./src/sga-mocks/composables.js'),
      '@/shared/utils/dateUtils': r('./src/sga-mocks/utils.js'),
      '@/shared/utils/download': r('./src/sga-mocks/utils.js'),
      '@/shared/utils/formatters': r('./src/sga-mocks/utils.js'),
      '@/core/stores/authStore': r('./src/sga-mocks/storesAndServices.js'),
      '@/core/stores/navigationStore': r('./src/sga-mocks/storesAndServices.js'),
      '@/features/auth/stores/authStore': r('./src/sga-mocks/storesAndServices.js'),
      '@/features/admin/catalogos/services/catalogoService': r('./src/sga-mocks/storesAndServices.js'),
      '@/features/admin/sedes/services/sedeService': r('./src/sga-mocks/storesAndServices.js'),
      '@/features/admin/users/services/userService': r('./src/sga-mocks/storesAndServices.js'),
      '@/features/aduanas/services/expedienteService': r('./src/sga-mocks/storesAndServices.js'),
      '@/features/aduanas/services/gestionAduaneraService': r('./src/sga-mocks/storesAndServices.js'),
      '@/features/documentos/services/documentoService': r('./src/sga-mocks/storesAndServices.js'),
      '@/features/finanzas/services/finanzasService': r('./src/sga-mocks/storesAndServices.js'),
      '@/features/transporte/services/transporteFlotaService': r('./src/sga-mocks/storesAndServices.js'),
      'vue-router': r('./src/sga-mocks/vue-router.js')
    }
  },
  server: {
    port: 5190,
    host: '0.0.0.0'
  }
})
