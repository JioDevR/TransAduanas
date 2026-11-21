<template>
  <section class="sga-card-surface wizard-step-card">
    <!-- 1. Régimen Aduanero -->
    <div class="sga-form-field">
      <label class="sga-form-label">Régimen Aduanero Oficial *</label>
      <div class="option-cards-grid">
        <div
          v-for="reg in regimenOptions"
          :key="reg.value"
          role="button"
          tabindex="0"
          :class="['select-card', { 'is-selected': form.regimenAduanero === reg.value }]"
          @click="$emit('select-regimen', reg.value)"
          @keydown.enter.prevent="$emit('select-regimen', reg.value)"
          @keydown.space.prevent="$emit('select-regimen', reg.value)"
        >
          <div class="select-card-top">
            <span class="sga-kicker sga-mono">{{ reg.codigo }}</span>
            <i v-if="form.regimenAduanero === reg.value" class="pi pi-check-circle select-check"></i>
          </div>
          <h4 class="select-card-title">{{ reg.label }}</h4>
          <p class="select-card-desc">{{ reg.desc }}</p>
        </div>
      </div>
    </div>

    <!-- 2. Modalidad de Transporte Internacional -->
    <div class="sga-form-field">
      <div class="field-head-row">
        <label class="sga-form-label">Modalidad de Transporte Internacional *</label>
        <Tag
          :value="modalidadTransporte === 'AEREO' ? 'VÍA AÉREA' : 'VÍA MARÍTIMA'"
          severity="secondary"
          class="sga-mini-tag"
        />
      </div>

      <div class="option-cards-grid">
        <button
          type="button"
          :class="['select-card is-button', { 'is-selected': modalidadTransporte === 'MARITIMO' }]"
          @click="$emit('set-modalidad', 'MARITIMO')"
        >
          <div class="select-card-row">
            <span class="card-icon-badge"><i class="pi pi-compass"></i></span>
            <div class="card-text-col">
              <strong class="select-card-title">Carga Marítima</strong>
              <span class="select-card-desc">Puertos oceánicos, terminales y muelles</span>
            </div>
            <i v-if="modalidadTransporte === 'MARITIMO'" class="pi pi-check-circle select-check"></i>
          </div>
        </button>

        <button
          type="button"
          :class="['select-card is-button', { 'is-selected': modalidadTransporte === 'AEREO' }]"
          @click="$emit('set-modalidad', 'AEREO')"
        >
          <div class="select-card-row">
            <span class="card-icon-badge"><i class="pi pi-send"></i></span>
            <div class="card-text-col">
              <strong class="select-card-title">Carga Aérea</strong>
              <span class="select-card-desc">Aeropuertos internacionales y carga aérea</span>
            </div>
            <i v-if="modalidadTransporte === 'AEREO'" class="pi pi-check-circle select-check"></i>
          </div>
        </button>
      </div>
    </div>

    <!-- 3. Cascada País -> Puerto / Aeropuerto -->
    <div class="route-grid">
      <div class="subpanel-box">
        <div class="subpanel-head">
          <span class="sga-kicker">ORIGEN / SALIDA</span>
          <Tag
            v-if="form.regimenAduanero === 'EXPORTACION_DEFINITIVA'"
            value="Default Perú"
            severity="info"
            class="sga-mini-tag"
          />
        </div>

        <div class="sga-form-field">
          <label for="portal-pais-envio" class="sga-form-label">País de Origen *</label>
          <Select
            id="portal-pais-envio"
            :modelValue="paisOrigen"
            :options="paisesOrigenDisponibles"
            filter
            filterPlaceholder="Buscar país..."
            placeholder="Seleccione país de salida..."
            size="small"
            fluid
            :disabled="submitting || loadingCatalogos"
            @update:modelValue="$emit('update:paisOrigen', $event)"
          />
        </div>

        <div class="sga-form-field">
          <label for="portal-puerto-envio" class="sga-form-label">
            {{ modalidadTransporte === 'AEREO' ? 'Aeropuerto de Origen' : 'Puerto de Origen' }} *
          </label>
          <Select
            id="portal-puerto-envio"
            v-model="form.idPuertoEnvio"
            :options="puertosOrigenFiltrados"
            optionLabel="nombre"
            optionValue="idPuerto"
            filter
            filterMatchMode="smart"
            :filterFields="['searchCorpus', 'codigoPuerto', 'nombre']"
            :filterPlaceholder="modalidadTransporte === 'AEREO' ? 'Buscar aeropuerto...' : 'Buscar puerto...'"
            :placeholder="!paisOrigen ? 'Primero seleccione país' : 'Seleccione terminal...'"
            size="small"
            fluid
            :disabled="submitting || loadingCatalogos || !paisOrigen"
          >
            <template #value="slotProps">
              <div v-if="selectedPuertoEnvio" class="select-val-row">
                <span class="sga-mono code-chip">{{ selectedPuertoEnvio.codigoPuerto }}</span>
                <span class="select-text">{{ selectedPuertoEnvio.nombre }}</span>
              </div>
              <span v-else>{{ slotProps.placeholder }}</span>
            </template>
            <template #option="{ option }">
              <div class="select-opt-row">
                <span class="sga-mono code-chip">{{ option.codigoPuerto }}</span>
                <span class="select-text">{{ option.nombre }} ({{ option.pais }})</span>
              </div>
            </template>
          </Select>
        </div>
      </div>

      <div class="subpanel-box">
        <div class="subpanel-head">
          <span class="sga-kicker">DESTINO / ARRIBO</span>
          <Tag
            v-if="form.regimenAduanero === 'IMPORTACION_DEFINITIVA'"
            value="Default Perú"
            severity="info"
            class="sga-mini-tag"
          />
        </div>

        <div class="sga-form-field">
          <label for="portal-pais-destino" class="sga-form-label">País de Destino *</label>
          <Select
            id="portal-pais-destino"
            :modelValue="paisDestino"
            :options="paisesDestinoDisponibles"
            filter
            filterPlaceholder="Buscar país..."
            placeholder="Seleccione país de arribo..."
            size="small"
            fluid
            :disabled="submitting || loadingCatalogos"
            @update:modelValue="$emit('update:paisDestino', $event)"
          />
        </div>

        <div class="sga-form-field">
          <label for="portal-puerto-destino" class="sga-form-label">
            {{ modalidadTransporte === 'AEREO' ? 'Aeropuerto de Destino' : 'Puerto de Destino' }} *
          </label>
          <Select
            id="portal-puerto-destino"
            v-model="form.idPuertoDestino"
            :options="puertosDestinoFiltrados"
            optionLabel="nombre"
            optionValue="idPuerto"
            filter
            filterMatchMode="smart"
            :filterFields="['searchCorpus', 'codigoPuerto', 'nombre']"
            :filterPlaceholder="modalidadTransporte === 'AEREO' ? 'Buscar aeropuerto...' : 'Buscar puerto...'"
            :placeholder="!paisDestino ? 'Primero seleccione país' : 'Seleccione terminal...'"
            size="small"
            fluid
            :disabled="submitting || loadingCatalogos || !paisDestino"
          >
            <template #value="slotProps">
              <div v-if="selectedPuertoDestino" class="select-val-row">
                <span class="sga-mono code-chip">{{ selectedPuertoDestino.codigoPuerto }}</span>
                <span class="select-text">{{ selectedPuertoDestino.nombre }}</span>
              </div>
              <span v-else>{{ slotProps.placeholder }}</span>
            </template>
            <template #option="{ option }">
              <div class="select-opt-row">
                <span class="sga-mono code-chip">{{ option.codigoPuerto }}</span>
                <span class="select-text">{{ option.nombre }} ({{ option.pais }})</span>
              </div>
            </template>
          </Select>
        </div>
      </div>
    </div>

    <!-- 4. Tipo de Carga y Mercancía -->
    <div class="two-col-grid">
      <div class="sga-form-field">
        <label for="portal-tipo-carga" class="sga-form-label">Tipo de Carga / Unidad Logística</label>
        <Select
          id="portal-tipo-carga"
          v-model="form.idTipoCarga"
          :options="tiposCarga"
          optionLabel="nombre"
          optionValue="idTipoCarga"
          filter
          filterMatchMode="smart"
          :filterFields="['searchCorpus', 'codigo', 'nombre']"
          filterPlaceholder="Buscar tipo de carga..."
          placeholder="Seleccione tipo de carga..."
          size="small"
          fluid
          :disabled="submitting || loadingCatalogos"
        >
          <template #value="slotProps">
            <div v-if="selectedTipoCarga" class="select-val-row">
              <span class="sga-mono code-chip">{{ selectedTipoCarga.codigo }}</span>
              <span class="select-text">{{ selectedTipoCarga.nombre }}</span>
            </div>
            <span v-else>{{ slotProps.placeholder }}</span>
          </template>
          <template #option="{ option }">
            <div class="select-opt-row">
              <span class="sga-mono code-chip">{{ option.codigo }}</span>
              <span class="select-text">{{ option.nombre }}</span>
            </div>
          </template>
        </Select>
        <div v-if="selectedTipoCarga?.descripcion" class="sga-banner is-info compact-banner">
          <i class="pi pi-box"></i>
          <span>{{ selectedTipoCarga.descripcion }}</span>
        </div>
      </div>

      <div class="sga-form-field">
        <label for="portal-mercancia" class="sga-form-label">Mercancía / Partida Arancelaria</label>
        <Select
          id="portal-mercancia"
          v-model="form.idMercancia"
          :options="mercancias"
          optionLabel="nombre"
          optionValue="idMercancia"
          filter
          filterMatchMode="smart"
          :filterFields="['searchCorpus', 'codigo', 'nombre', 'categoria', 'entidadReguladora']"
          filterPlaceholder="Buscar por código o nombre..."
          placeholder="Seleccione mercancía..."
          size="small"
          fluid
          :disabled="submitting || loadingCatalogos"
        >
          <template #value="slotProps">
            <div v-if="selectedMercancia" class="select-val-row">
              <span class="sga-mono code-chip">{{ selectedMercancia.codigo }}</span>
              <span class="select-text">{{ selectedMercancia.nombre }}</span>
            </div>
            <span v-else>{{ slotProps.placeholder }}</span>
          </template>
          <template #option="{ option }">
            <div class="select-opt-row justify-between">
              <div class="select-opt-row">
                <span class="sga-mono code-chip">{{ option.codigo }}</span>
                <span class="select-text">{{ option.nombre }}</span>
              </div>
              <Tag
                :value="option.requierePermiso ? (option.entidadReguladora || 'Permiso') : 'Libre'"
                :severity="option.requierePermiso ? 'warn' : 'success'"
                rounded
              />
            </div>
          </template>
        </Select>

        <div
          v-if="selectedMercancia"
          :class="['sga-banner compact-banner', selectedMercancia.requierePermiso ? 'is-warn' : 'is-success']"
        >
          <i :class="selectedMercancia.requierePermiso ? 'pi pi-exclamation-triangle' : 'pi pi-check-circle'"></i>
          <span v-if="selectedMercancia.requierePermiso">
            Requiere permiso de <strong>{{ selectedMercancia.entidadReguladora || 'Entidad Reguladora' }}</strong>. Adjúntelo en el Paso 02.
          </span>
          <span v-else>
            Mercancía de libre trámite (<span class="sga-mono">{{ selectedMercancia.codigo }}</span>).
          </span>
        </div>
      </div>
    </div>

    <!-- 5. Transporte Terrestre Local SGA -->
    <div class="sga-form-field">
      <div class="field-head-row">
        <label class="sga-form-label">Servicio de Transporte Terrestre Local SGA *</label>
        <Tag value="FLOTA SGA" severity="secondary" class="sga-mini-tag" />
      </div>

      <div class="option-cards-grid">
        <div
          role="button"
          tabindex="0"
          :class="['select-card', { 'is-selected': form.servicioTransporte === true }]"
          @click="form.servicioTransporte = true"
          @keydown.enter.prevent="form.servicioTransporte = true"
          @keydown.space.prevent="form.servicioTransporte = true"
        >
          <div class="select-card-row">
            <span class="card-icon-badge"><i class="pi pi-truck"></i></span>
            <div class="card-text-col">
              <strong class="select-card-title">Sí, Transporte Interno SGA</strong>
              <span class="select-card-desc">Traslado con flota monitoreada en Perú</span>
            </div>
            <i v-if="form.servicioTransporte === true" class="pi pi-check-circle select-check"></i>
          </div>
        </div>

        <div
          role="button"
          tabindex="0"
          :class="['select-card', { 'is-selected': form.servicioTransporte === false }]"
          @click="form.servicioTransporte = false"
          @keydown.enter.prevent="form.servicioTransporte = false"
          @keydown.space.prevent="form.servicioTransporte = false"
        >
          <div class="select-card-row">
            <span class="card-icon-badge"><i class="pi pi-building"></i></span>
            <div class="card-text-col">
              <strong class="select-card-title">No requerido</strong>
              <span class="select-card-desc">Retiro gestionado por cuenta propia</span>
            </div>
            <i v-if="form.servicioTransporte === false" class="pi pi-check-circle select-check"></i>
          </div>
        </div>
      </div>

      <div v-if="form.servicioTransporte" class="route-grid mt-2">
        <div class="subpanel-box">
          <span class="sga-kicker">PUNTO DE RECOJO (ORIGEN)</span>
          <div class="two-col-grid">
            <div class="sga-form-field">
              <label for="portal-orig-dep" class="sga-form-label">Departamento *</label>
              <Select
                id="portal-orig-dep"
                v-model="form.origenDepartamento"
                :options="departamentosPeru"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>
            <div class="sga-form-field">
              <label for="portal-orig-prov" class="sga-form-label">Provincia *</label>
              <Select
                id="portal-orig-prov"
                v-model="form.origenProvincia"
                :options="provinciasOrigen"
                size="small"
                fluid
                :disabled="submitting || !form.origenDepartamento"
              />
            </div>
          </div>
        </div>

        <div class="subpanel-box">
          <span class="sga-kicker">PUNTO DE ENTREGA (DESTINO)</span>
          <div class="two-col-grid">
            <div class="sga-form-field">
              <label for="portal-dest-dep" class="sga-form-label">Departamento *</label>
              <Select
                id="portal-dest-dep"
                v-model="form.destinoDepartamento"
                :options="departamentosPeru"
                size="small"
                fluid
                :disabled="submitting"
              />
            </div>
            <div class="sga-form-field">
              <label for="portal-dest-prov" class="sga-form-label">Provincia *</label>
              <Select
                id="portal-dest-prov"
                v-model="form.destinoProvincia"
                :options="provinciasDestino"
                size="small"
                fluid
                :disabled="submitting || !form.destinoDepartamento"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 6. Validez y Observaciones -->
    <div class="two-col-grid">
      <div class="sga-form-field">
        <label for="portal-validez" class="sga-form-label">Validez Deseada (Días Calendario) *</label>
        <InputNumber
          id="portal-validez"
          v-model="form.validezDias"
          :min="1"
          :max="90"
          suffix=" días"
          size="small"
          fluid
          :disabled="submitting"
        />
        <div class="chips-row">
          <Button
            v-for="dias in [15, 30, 45, 60]"
            :key="dias"
            type="button"
            :label="`${dias}d`"
            size="small"
            :severity="form.validezDias === dias ? 'primary' : 'secondary'"
            :outlined="form.validezDias !== dias"
            @click="form.validezDias = dias"
          />
        </div>
      </div>

      <div class="sga-form-field">
        <label for="portal-obs" class="sga-form-label">Observaciones o Requerimientos Especiales</label>
        <Textarea
          id="portal-obs"
          v-model="form.observaciones"
          rows="3"
          placeholder="Dimensiones especiales, estiba o fechas estimadas..."
          size="small"
          fluid
          :disabled="submitting"
        />
      </div>
    </div>

    <div class="step-footer-actions">
      <span></span>
      <Button
        type="button"
        label="Siguiente: Documentación"
        icon="pi pi-arrow-right"
        iconPos="right"
        size="small"
        @click="$emit('next')"
      />
    </div>
  </section>
</template>

<script setup>
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Textarea from 'primevue/textarea'

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
  departamentosPeru: { type: Array, default: () => [] },
  provinciasOrigen: { type: Array, default: () => [] },
  provinciasDestino: { type: Array, default: () => [] },
  loadingCatalogos: { type: Boolean, default: false },
  submitting: { type: Boolean, default: false }
})

defineEmits([
  'select-regimen',
  'set-modalidad',
  'update:paisOrigen',
  'update:paisDestino',
  'next'
])
</script>

<style scoped>
.wizard-step-card {
  display: flex;
  flex-direction: column;
  gap: 1.35rem;
}

.field-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.option-cards-grid,
.route-grid,
.two-col-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.select-card {
  padding: 0.9rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  background: var(--color-bg-subtle);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  text-align: left;
  transition: border-color var(--transition-fast), background-color var(--transition-fast);
}

.select-card:hover {
  border-color: var(--color-border-strong);
}

.select-card.is-selected {
  border-color: var(--color-primary);
  background: var(--color-bg-surface);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.select-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.select-card-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.card-icon-badge {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-accent-kicker);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.card-text-col {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  flex: 1;
  min-width: 0;
}

.select-card-title {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.select-card-desc {
  margin: 0;
  font-size: 0.76rem;
  line-height: 1.35;
  color: var(--color-text-muted);
}

.select-check {
  color: var(--color-primary);
  font-size: 1rem;
  flex-shrink: 0;
}

.subpanel-box {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.subpanel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.select-val-row,
.select-opt-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.justify-between {
  justify-content: space-between;
  width: 100%;
}

.code-chip {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-xs);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  flex-shrink: 0;
}

.select-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.82rem;
}

.compact-banner {
  padding: 0.6rem 0.85rem;
  font-size: 0.78rem;
  margin-top: 0.2rem;
}

.mt-2 {
  margin-top: 0.35rem;
}

.chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.step-footer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 0.75rem;
  border-top: 1px solid var(--color-border-subtle);
}

@media (max-width: 860px) {
  .option-cards-grid,
  .route-grid,
  .two-col-grid {
    grid-template-columns: 1fr;
  }
}
</style>
