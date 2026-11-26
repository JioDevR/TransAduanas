<template>
  <div class="sga-agente-actions">
    <!-- Pestaña EXPEDIENTES / TODOS: Acceso rápido a Documentos e Informe -->
    <template v-if="isTabTodos">
      <Button
        type="button"
        label="Docs"
        icon="pi pi-paperclip"
        severity="secondary"
        outlined
        size="small"
        title="Bóveda de Documentos Digitales"
        @click.stop="$emit('documentos', expediente)"
      />
      <Button
        type="button"
        label="Informe"
        icon="pi pi-file-edit"
        size="small"
        title="Ver informe completo del expediente"
        @click.stop="$emit('detalle', expediente)"
      />
    </template>

    <!-- Pestaña TRÁNSITO / ARRIBO -->
    <template v-else-if="activeTab === 'transito'">
      <template v-if="acc.isExport">
        <Button
          v-if="acc.puedeRecojoExportacion"
          type="button"
          label="Pedir Recojo"
          icon="pi pi-truck"
          severity="success"
          outlined
          size="small"
          @click.stop="$emit('recojo-exportacion', expediente)"
        />
        <Button
          v-if="acc.puedeIngresoOrigen"
          type="button"
          label="Ingreso Almacén"
          icon="pi pi-inbox"
          severity="secondary"
          outlined
          size="small"
          @click.stop="$emit('ingreso-origen', expediente)"
        />
        <Button
          v-if="acc.puedeEditarTransito"
          type="button"
          label="Tránsito"
          icon="pi pi-globe"
          severity="secondary"
          outlined
          size="small"
          @click.stop="$emit('transito', expediente)"
        />
      </template>

      <template v-else>
        <Button
          v-if="acc.puedeVerificacionOrigen"
          type="button"
          label="Verificar Origen"
          icon="pi pi-check-square"
          severity="info"
          outlined
          size="small"
          @click.stop="$emit('verificacion-origen', expediente)"
        />
        <Button
          v-if="acc.puedeSalidaOrigen"
          type="button"
          label="Salida Origen"
          icon="pi pi-send"
          severity="success"
          outlined
          size="small"
          @click.stop="$emit('salida-origen', expediente)"
        />
        <Button
          v-if="acc.puedeConfirmarArribo"
          type="button"
          label="Confirmar Arribo"
          icon="pi pi-compass"
          severity="success"
          outlined
          size="small"
          @click.stop="$emit('arribo-destino', expediente)"
        />
        <Button
          v-if="acc.puedeEditarTransito"
          type="button"
          label="Tránsito"
          icon="pi pi-globe"
          severity="secondary"
          outlined
          size="small"
          @click.stop="$emit('transito', expediente)"
        />
      </template>

      <Tag
        v-if="acc.esperandoCanal"
        value="Esperando Canal"
        icon="pi pi-clock"
        severity="warn"
        rounded
      />

      <Button
        type="button"
        icon="pi pi-paperclip"
        severity="secondary"
        outlined
        size="small"
        title="Bóveda de Documentos"
        aria-label="Documentos"
        @click.stop="$emit('documentos', expediente)"
      />
      <Button
        type="button"
        label="Informe"
        icon="pi pi-file-edit"
        size="small"
        @click.stop="$emit('detalle', expediente)"
      />
    </template>

    <!-- Pestaña INSPECCIÓN / AFORO SUNAT -->
    <template v-else-if="activeTab === 'inspeccion'">
      <Button
        v-if="acc.puedeCuadrillaCitaRojo"
        type="button"
        label="Cuadrilla / Cita"
        icon="pi pi-users"
        severity="danger"
        outlined
        size="small"
        title="Canal Rojo: Solicitar cuadrilla y registrar cita de inspección física"
        @click.stop="$emit('cuadrilla-rojo', expediente)"
      />
      <Button
        v-if="acc.puedeFinalizarRojo"
        type="button"
        label="Finalizar Revisión"
        icon="pi pi-check-circle"
        severity="danger"
        size="small"
        title="Canal Rojo: Finalizar inspección física y autorizar levante"
        @click.stop="$emit('finalizar-rojo', expediente)"
      />
      <Button
        v-if="acc.puedeEnviarSunatNaranja"
        type="button"
        label="Enviar a SUNAT"
        icon="pi pi-cloud-upload"
        severity="warn"
        size="small"
        title="Canal Naranja: Envío simulado de documentos directamente a SUNAT"
        @click.stop="$emit('enviar-sunat', expediente)"
      />
      <Button
        v-if="acc.puedeDiligencia"
        type="button"
        label="Diligencia"
        icon="pi pi-search-plus"
        severity="warn"
        outlined
        size="small"
        title="Registrar Acta de Diligencia / Aforo SUNAT"
        @click.stop="$emit('diligencia', expediente)"
      />
      <Button
        v-if="acc.puedeIngresoAlmacen"
        type="button"
        label="Balanza VGM"
        icon="pi pi-box"
        severity="secondary"
        outlined
        size="small"
        title="Registrar Ingreso a Depósito / Balanza VGM"
        @click.stop="$emit('ingreso-almacen', expediente)"
      />
      <Button
        type="button"
        icon="pi pi-paperclip"
        severity="secondary"
        outlined
        size="small"
        title="Bóveda de Documentos"
        aria-label="Documentos"
        @click.stop="$emit('documentos', expediente)"
      />
      <Button
        type="button"
        label="Informe"
        icon="pi pi-file-edit"
        size="small"
        @click.stop="$emit('detalle', expediente)"
      />
    </template>

    <!-- Pestaña LEVANTE & DESPACHO -->
    <template v-else-if="activeTab === 'levante'">
      <template v-if="!acc.isExport">
        <Button
          v-if="acc.puedeEntregadoAlmacenVerde"
          type="button"
          label="Entregado Almacén"
          icon="pi pi-check-square"
          severity="success"
          outlined
          size="small"
          @click.stop="$emit('entregado-almacen-verde', expediente)"
        />
        <Button
          v-if="acc.puedeSolicitarTransporte"
          type="button"
          label="Pedir Envío"
          icon="pi pi-truck"
          severity="success"
          outlined
          size="small"
          @click.stop="$emit('solicitar-transporte', expediente)"
        />
        <Button
          v-if="acc.puedeRetiroDirecto"
          type="button"
          label="Retiro Directo"
          icon="pi pi-check"
          severity="secondary"
          outlined
          size="small"
          @click.stop="$emit('retiro-directo', expediente)"
        />
        <Button
          v-if="acc.puedeInformeConformidad"
          type="button"
          label="Informe Conformidad"
          icon="pi pi-file-check"
          severity="info"
          outlined
          size="small"
          @click.stop="$emit('informe-conformidad', expediente)"
        />
        <Button
          v-if="acc.puedeIngresoAlmacen"
          type="button"
          label="Balanza VGM"
          icon="pi pi-box"
          severity="secondary"
          outlined
          size="small"
          title="Registrar Ingreso a Depósito / Balanza VGM"
          @click.stop="$emit('ingreso-almacen', expediente)"
        />
      </template>

      <template v-else>
        <Button
          v-if="acc.puedeEmbarque"
          type="button"
          label="Confirmar Embarque"
          icon="pi pi-send"
          severity="success"
          outlined
          size="small"
          @click.stop="$emit('embarque', expediente)"
        />
        <Button
          v-else-if="acc.puedeRecepcionExterior"
          type="button"
          label="Entrega Exterior"
          icon="pi pi-check-circle"
          severity="success"
          outlined
          size="small"
          @click.stop="$emit('recepcion-exterior', expediente)"
        />
      </template>

      <Button
        v-if="acc.puedeFinalizarExpediente"
        type="button"
        label="Finalizar"
        icon="pi pi-lock"
        severity="success"
        size="small"
        @click.stop="$emit('finalizar-expediente', expediente)"
      />

      <Button
        type="button"
        icon="pi pi-paperclip"
        severity="secondary"
        outlined
        size="small"
        title="Bóveda de Documentos"
        aria-label="Documentos"
        @click.stop="$emit('documentos', expediente)"
      />
      <Button
        type="button"
        label="Informe"
        icon="pi pi-file-edit"
        size="small"
        @click.stop="$emit('detalle', expediente)"
      />
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import { getAccionesPermitidasAgente } from '../utils/agenteWorkflowUtils'

const props = defineProps({
  expediente: { type: Object, required: true },
  activeTab: { type: String, default: 'todos' }
})

defineEmits([
  'salida-origen',
  'arribo-destino',
  'transito',
  'diligencia',
  'solicitar-transporte',
  'retiro-directo',
  'ingreso-almacen',
  'recojo-exportacion',
  'ingreso-origen',
  'embarque',
  'recepcion-exterior',
  'verificacion-origen',
  'cuadrilla-rojo',
  'finalizar-rojo',
  'enviar-sunat',
  'entregado-almacen-verde',
  'informe-conformidad',
  'finalizar-expediente',
  'documentos',
  'detalle'
])

const isTabTodos = computed(() => {
  return props.activeTab === 'todos' || props.activeTab === 'expedientes' || !props.activeTab
})

const acc = computed(() => getAccionesPermitidasAgente(props.expediente))
</script>

<style scoped>
.sga-agente-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.4rem;
  flex-wrap: wrap;
}
</style>
