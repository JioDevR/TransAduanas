<template>
  <section class="sga-step-canvas">
    <!-- 1. Régimen Aduanero -->
    <div class="sga-field-block">
      <label class="sga-field-label">RÉGIMEN ADUANERO</label>
      <div class="sga-option-grid cols-2">
        <button
          v-for="reg in regimenOptions"
          :key="reg.value"
          type="button"
          :class="['sga-select-card', { 'is-selected': form.regimenAduanero === reg.value }]"
          @click="$emit('select-regimen', reg.value)"
        >
          <div class="sga-select-card-top">
            <span class="sga-code-pill font-mono">{{ reg.codigo }}</span>
            <i v-if="form.regimenAduanero === reg.value" class="pi pi-check sga-check-icon"></i>
          </div>
          <span class="sga-select-card-title">{{ reg.label }}</span>
        </button>
      </div>
    </div>

    <!-- 2. Modalidad de Transporte Internacional -->
    <div class="sga-field-block">
      <label class="sga-field-label">MODALIDAD DE TRANSPORTE INTERNACIONAL *</label>
      <div class="sga-option-grid cols-2">
        <button
          type="button"
          :class="['sga-select-card horizontal', { 'is-selected': modalidadTransporte === 'MARITIMO' }]"
          @click="$emit('set-modalidad', 'MARITIMO')"
        >
          <i class="pi pi-compass sga-mod-icon"></i>
          <div class="sga-mod-info">
            <span class="sga-select-card-title">Carga Marítima</span>
            <span class="sga-select-card-desc">Puertos oceánicos y terminales</span>
          </div>
          <i v-if="modalidadTransporte === 'MARITIMO'" class="pi pi-check sga-check-icon"></i>
        </button>

        <button
          type="button"
          :class="['sga-select-card horizontal', { 'is-selected': modalidadTransporte === 'AEREO' }]"
          @click="$emit('set-modalidad', 'AEREO')"
        >
          <i class="pi pi-send sga-mod-icon"></i>
          <div class="sga-mod-info">
            <span class="sga-select-card-title">Carga Aérea</span>
            <span class="sga-select-card-desc">Aeropuertos internacionales</span>
          </div>
          <i v-if="modalidadTransporte === 'AEREO'" class="pi pi-check sga-check-icon"></i>
        </button>
      </div>
    </div>

    <!-- 3. Ruta Logística: Origen y Destino -->
    <div class="sga-route-grid">
      <div class="sga-route-col">
        <div class="sga-subpanel-header">
          <span class="sga-field-label">ORIGEN / SALIDA</span>
          <Tag v-if="form.regimenAduanero === 'EXPORTACION_DEFINITIVA'" value="Default Perú" severity="secondary" />
        </div>

        <div class="sga-field">
          <label for="cot-pais-envio" class="sga-sub-label">País de Origen *</label>
          <Select
            inputId="cot-pais-envio"
            :modelValue="paisOrigen"
            :options="paisesOrigenDisponibles"
            filter
            filterPlaceholder="Buscar país..."
            placeholder="Seleccione país de salida"
            size="small"
            fluid
            :disabled="submitting || loadingCatalogos"
            @update:modelValue="$emit('update:paisOrigen', $event)"
            @change="$emit('pais-origen-change')"
          />
        </div>

        <div class="sga-field">
          <label for="cot-puerto-envio" class="sga-sub-label">
            {{ modalidadTransporte === 'AEREO' ? 'Aeropuerto de Origen' : 'Puerto de Origen' }} *
          </label>
          <Select
            inputId="cot-puerto-envio"
            v-model="form.idPuertoEnvio"
            :options="puertosOrigenFiltrados"
            optionLabel="nombre"
            optionValue="idPuerto"
            filter
            filterMatchMode="smart"
            :filterFields="['searchCorpus', 'codigoPuerto', 'nombre']"
            :filterPlaceholder="modalidadTransporte === 'AEREO' ? 'Buscar aeropuerto...' : 'Buscar puerto...'"
            :placeholder="!paisOrigen ? 'Primero seleccione país' : 'Seleccione terminal de salida'"
            size="small"
            fluid
            :disabled="submitting || loadingCatalogos || !paisOrigen"
          >
            <template #value="slotProps">
              <div v-if="selectedPuertoEnvio" class="sga-select-val">
                <span class="sga-code-pill font-mono">{{ selectedPuertoEnvio.codigoPuerto }}</span>
                <span>{{ selectedPuertoEnvio.nombre }}</span>
              </div>
              <span v-else>{{ slotProps.placeholder }}</span>
            </template>
            <template #option="slotProps">
              <div class="sga-select-opt">
                <span class="sga-code-pill font-mono">{{ slotProps.option.codigoPuerto }}</span>
                <span>{{ slotProps.option.nombre }} ({{ slotProps.option.pais }})</span>
              </div>
            </template>
          </Select>
        </div>
      </div>

      <div class="sga-route-col">
        <div class="sga-subpanel-header">
          <span class="sga-field-label">DESTINO / ARRIBO</span>
          <Tag v-if="form.regimenAduanero === 'IMPORTACION_DEFINITIVA'" value="Default Perú" severity="secondary" />
        </div>

        <div class="sga-field">
          <label for="cot-pais-destino" class="sga-sub-label">País de Destino *</label>
          <Select
            inputId="cot-pais-destino"
            :modelValue="paisDestino"
            :options="paisesDestinoDisponibles"
            filter
            filterPlaceholder="Buscar país..."
            placeholder="Seleccione país de arribo"
            size="small"
            fluid
            :disabled="submitting || loadingCatalogos"
            @update:modelValue="$emit('update:paisDestino', $event)"
            @change="$emit('pais-destino-change')"
          />
        </div>

        <div class="sga-field">
          <label for="cot-puerto-destino" class="sga-sub-label">
            {{ modalidadTransporte === 'AEREO' ? 'Aeropuerto de Destino' : 'Puerto de Destino' }} *
          </label>
          <Select
            inputId="cot-puerto-destino"
            v-model="form.idPuertoDestino"
            :options="puertosDestinoFiltrados"
            optionLabel="nombre"
            optionValue="idPuerto"
            filter
            filterMatchMode="smart"
            :filterFields="['searchCorpus', 'codigoPuerto', 'nombre']"
            :filterPlaceholder="modalidadTransporte === 'AEREO' ? 'Buscar aeropuerto...' : 'Buscar puerto...'"
            :placeholder="!paisDestino ? 'Primero seleccione país' : 'Seleccione terminal de arribo'"
            size="small"
            fluid
            :disabled="submitting || loadingCatalogos || !paisDestino"
          >
            <template #value="slotProps">
              <div v-if="selectedPuertoDestino" class="sga-select-val">
                <span class="sga-code-pill font-mono">{{ selectedPuertoDestino.codigoPuerto }}</span>
                <span>{{ selectedPuertoDestino.nombre }}</span>
              </div>
              <span v-else>{{ slotProps.placeholder }}</span>
            </template>
            <template #option="slotProps">
              <div class="sga-select-opt">
                <span class="sga-code-pill font-mono">{{ slotProps.option.codigoPuerto }}</span>
                <span>{{ slotProps.option.nombre }} ({{ slotProps.option.pais }})</span>
              </div>
            </template>
          </Select>
        </div>
      </div>
    </div>

    <!-- 4. Tipo de Carga y Catálogo de Mercancía -->
    <div class="sga-route-grid">
      <div class="sga-field">
        <label for="cot-tipo-carga" class="sga-field-label">TIPO DE CARGA / UNIDAD LOGÍSTICA</label>
        <Select
          inputId="cot-tipo-carga"
          v-model="form.idTipoCarga"
          :options="tiposCarga"
          optionLabel="nombre"
          optionValue="idTipoCarga"
          filter
          filterMatchMode="smart"
          :filterFields="['searchCorpus', 'codigo', 'nombre']"
          filterPlaceholder="Buscar tipo de carga..."
          placeholder="Seleccione tipo de carga"
          size="small"
          fluid
          :disabled="submitting || loadingCatalogos"
        >
          <template #value="slotProps">
            <div v-if="selectedTipoCarga" class="sga-select-val">
              <span class="sga-code-pill font-mono">{{ selectedTipoCarga.codigo }}</span>
              <span>{{ selectedTipoCarga.nombre }}</span>
            </div>
            <span v-else>{{ slotProps.placeholder }}</span>
          </template>
          <template #option="slotProps">
            <div class="sga-select-opt-col">
              <div class="sga-select-opt">
                <span class="sga-code-pill font-mono">{{ slotProps.option.codigo }}</span>
                <span>{{ slotProps.option.nombre }}</span>
              </div>
              <span v-if="slotProps.option.descripcion" class="sga-opt-sub">{{ slotProps.option.descripcion }}</span>
            </div>
          </template>
        </Select>
        <span v-if="selectedTipoCarga?.descripcion" class="sga-field-hint">
          {{ selectedTipoCarga.descripcion }}
        </span>
      </div>

      <div class="sga-field">
        <label for="cot-mercancia" class="sga-field-label">CATÁLOGO DE MERCANCÍA (PARTIDA ARANCELARIA)</label>
        <Select
          inputId="cot-mercancia"
          v-model="form.idMercancia"
          :options="mercancias"
          optionLabel="nombre"
          optionValue="idMercancia"
          filter
          filterMatchMode="smart"
          :filterFields="['searchCorpus', 'codigo', 'nombre', 'categoria', 'entidadReguladora']"
          filterPlaceholder="Buscar partida o sector..."
          placeholder="Seleccione mercancía"
          size="small"
          fluid
          :disabled="submitting || loadingCatalogos"
          @change="$emit('mercancia-change')"
        >
          <template #value="slotProps">
            <div v-if="selectedMercancia" class="sga-select-val">
              <span class="sga-code-pill font-mono">{{ selectedMercancia.codigo }}</span>
              <span>{{ selectedMercancia.nombre }}</span>
            </div>
            <span v-else>{{ slotProps.placeholder }}</span>
          </template>
          <template #option="slotProps">
            <div class="sga-select-opt-spread">
              <div class="sga-select-opt-col">
                <div class="sga-select-opt">
                  <span class="sga-code-pill font-mono">{{ slotProps.option.codigo }}</span>
                  <span>{{ slotProps.option.nombre }}</span>
                </div>
                <span class="sga-opt-sub">{{ slotProps.option.categoria }} · Arancel {{ slotProps.option.arancelBasePct }}%</span>
              </div>
              <Tag
                :value="slotProps.option.requierePermiso ? `Req: ${slotProps.option.entidadReguladora}` : 'Libre'"
                :severity="slotProps.option.requierePermiso ? 'warn' : 'success'"
                rounded
              />
            </div>
          </template>
        </Select>
      </div>
    </div>

    <!-- Alerta de Mercancía -->
    <Message
      v-if="selectedMercancia"
      :severity="selectedMercancia.requierePermiso ? 'warn' : 'success'"
      :closable="false"
    >
      <template v-if="selectedMercancia.requierePermiso">
        <strong>Mercancía Restringida ({{ selectedMercancia.entidadReguladora }}):</strong>
        La partida {{ selectedMercancia.codigo }} requiere autorización sectorial antes de la numeración. Arancel base: {{ selectedMercancia.arancelBasePct }}%.
      </template>
      <template v-else>
        <strong>Libre Trámite:</strong>
        La partida {{ selectedMercancia.codigo }} no requiere permisos sectoriales previos. Arancel base: {{ selectedMercancia.arancelBasePct }}%.
      </template>
    </Message>

    <!-- 5. Transporte Terrestre Local -->
    <div class="sga-field-block">
      <label class="sga-field-label">SERVICIO DE TRANSPORTE TERRESTRE LOCAL</label>
      <div class="sga-option-grid cols-2">
        <button
          type="button"
          :class="['sga-select-card horizontal', { 'is-selected': form.servicioTransporte === true }]"
          @click="form.servicioTransporte = true"
        >
          <i class="pi pi-truck sga-mod-icon"></i>
          <span class="sga-select-card-title">Sí, Transporte Interno SGA</span>
          <i v-if="form.servicioTransporte === true" class="pi pi-check sga-check-icon"></i>
        </button>

        <button
          type="button"
          :class="['sga-select-card horizontal', { 'is-selected': form.servicioTransporte === false }]"
          @click="form.servicioTransporte = false"
        >
          <i class="pi pi-building sga-mod-icon"></i>
          <span class="sga-select-card-title">No, Transporte Externo</span>
          <i v-if="form.servicioTransporte === false" class="pi pi-check sga-check-icon"></i>
        </button>
      </div>

      <!-- Ruta Nacional Condicional -->
      <div v-if="form.servicioTransporte" class="sga-subpanel sga-transport-panel">
        <div class="sga-route-grid">
          <div class="sga-field">
            <label class="sga-sub-label">Origen (Departamento / Provincia)</label>
            <div class="sga-inline-2">
              <Select
                v-model="form.origenDepartamento"
                :options="departamentosList"
                size="small"
                fluid
                @change="$emit('origen-departamento-change')"
              />
              <Select
                v-model="form.origenProvincia"
                :options="provinciasOrigenList"
                :disabled="!form.origenDepartamento"
                size="small"
                fluid
              />
            </div>
          </div>

          <div class="sga-field">
            <label class="sga-sub-label">Destino (Departamento / Provincia)</label>
            <div class="sga-inline-2">
              <Select
                v-model="form.destinoDepartamento"
                :options="departamentosList"
                size="small"
                fluid
                @change="$emit('destino-departamento-change')"
              />
              <Select
                v-model="form.destinoProvincia"
                :options="provinciasDestinoList"
                :disabled="!form.destinoDepartamento"
                size="small"
                fluid
              />
            </div>
          </div>
        </div>

        <div class="sga-field sga-cost-inline">
          <label class="sga-sub-label">Tarifa Flete Terrestre SGA (USD)</label>
          <InputNumber
            v-model="form.costoTransporteInterno"
            mode="currency"
            currency="USD"
            locale="en-US"
            :min="0"
            :disabled="submitting"
            size="small"
          />
        </div>
      </div>
    </div>

    <!-- 6. Vigencia y Observaciones -->
    <div class="sga-route-grid">
      <div class="sga-field">
        <label for="cot-validez" class="sga-field-label">VALIDEZ DE LA OFERTA</label>
        <InputNumber
          inputId="cot-validez"
          v-model="form.validezDias"
          :min="1"
          :max="90"
          suffix=" días"
          :disabled="submitting"
          size="small"
          fluid
        />
        <div class="sga-chips-row">
          <Button
            v-for="dias in [15, 30, 45, 60]"
            :key="dias"
            type="button"
            :label="`${dias}d`"
            :severity="form.validezDias === dias ? 'primary' : 'secondary'"
            :outlined="form.validezDias !== dias"
            size="small"
            @click="form.validezDias = dias"
          />
        </div>
      </div>

      <div class="sga-field">
        <label for="cot-obs" class="sga-field-label">OBSERVACIONES Y TÉRMINOS</label>
        <Textarea
          inputId="cot-obs"
          v-model="form.observaciones"
          rows="3"
          placeholder="Detalles de bultos, cuadrilla, almacenaje temporal, etc."
          :disabled="submitting"
          fluid
        />
      </div>
    </div>
  </section>
</template>

<script setup>
import Select from 'primevue/select'
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'
import Message from 'primevue/message'

defineProps({
  form: { type: Object, required: true },
  regimenOptions: { type: Array, required: true },
  modalidadTransporte: { type: String, required: true },
  paisOrigen: { type: String, default: '' },
  paisDestino: { type: String, default: '' },
  paisesOrigenDisponibles: { type: Array, default: () => [] },
  paisesDestinoDisponibles: { type: Array, default: () => [] },
  puertosOrigenFiltrados: { type: Array, default: () => [] },
  puertosDestinoFiltrados: { type: Array, default: () => [] },
  selectedPuertoEnvio: { type: Object, default: null },
  selectedPuertoDestino: { type: Object, default: null },
  tiposCarga: { type: Array, default: () => [] },
  selectedTipoCarga: { type: Object, default: null },
  mercancias: { type: Array, default: () => [] },
  selectedMercancia: { type: Object, default: null },
  departamentosList: { type: Array, default: () => [] },
  provinciasOrigenList: { type: Array, default: () => [] },
  provinciasDestinoList: { type: Array, default: () => [] },
  loadingCatalogos: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false }
})

defineEmits([
  'select-regimen',
  'set-modalidad',
  'update:paisOrigen',
  'update:paisDestino',
  'pais-origen-change',
  'pais-destino-change',
  'mercancia-change',
  'origen-departamento-change',
  'destino-departamento-change'
])
</script>

<style scoped>
.sga-step-canvas {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 0.25rem 0;
}

.sga-field-block,
.sga-field,
.sga-route-col {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.sga-route-col {
  gap: 0.85rem;
}

.sga-field-label {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--color-text-muted);
}

.sga-sub-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.sga-field-hint {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sga-option-grid.cols-2,
.sga-route-grid,
.sga-inline-2 {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1rem;
}

.sga-select-card {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  padding: 1rem 1.25rem;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-bg-surface);
  text-align: left;
  cursor: pointer;
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
}

.sga-select-card.horizontal {
  flex-direction: row;
  align-items: center;
  gap: 0.75rem;
}

.sga-select-card:hover {
  border-color: var(--color-primary);
}

.sga-select-card.is-selected {
  border-color: var(--color-primary);
}

.sga-select-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sga-code-pill {
  font-size: 0.6875rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 6px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
}

.sga-check-icon {
  color: var(--color-primary);
  margin-left: auto;
  font-size: 0.875rem;
}

.sga-mod-icon {
  font-size: 1.05rem;
  color: var(--color-primary);
}

.sga-mod-info {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.sga-select-card-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.sga-select-card-desc {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sga-subpanel {
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: var(--sga-subpanel-padding, 1.25rem 1.35rem);
  background: var(--color-bg-surface);
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.sga-subpanel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 24px;
}

.sga-transport-panel {
  margin-top: 0.35rem;
}

.sga-cost-inline {
  max-width: 260px;
}

.sga-select-val,
.sga-select-opt {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.sga-select-opt-col {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.sga-select-opt-spread {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
}

.sga-opt-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.sga-chips-row {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.font-mono {
  font-family: var(--font-mono);
}

@media (max-width: 900px) {
  .sga-option-grid.cols-2,
  .sga-route-grid,
  .sga-inline-2 {
    grid-template-columns: 1fr;
  }
}
</style>
