<template>
  <div class="agente-informe-view fade-in">
    <!-- ============================================================== -->
    <!-- 1. BARRA SUPERIOR DE NAVEGACIÓN Y ACCIONES RÁPIDAS             -->
    <!-- ============================================================== -->
    <header class="informe-top-bar no-print">
      <div class="top-bar-left">
        <button
          type="button"
          class="btn-back-nav"
          @click="$emit('back')"
          title="Regresar al listado de expedientes"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver a Expedientes</span>
        </button>

        <div class="breadcrumbs">
          <span class="crumb-muted">Agente Aduanero</span>
          <i class="pi pi-angle-right text-xs text-gray-400"></i>
          <span class="crumb-muted">Expedientes</span>
          <i class="pi pi-angle-right text-xs text-gray-400"></i>
          <span class="crumb-active font-mono">{{ expediente?.codigoExpediente }}</span>
        </div>
      </div>

      <div class="top-bar-actions">
        <!-- Acciones operativas contextuales -->
        <template v-if="!isExport">
          <!-- Salida de Origen -->
          <button
            v-if="!hasZarpeConfirmado && !isCerrado"
            type="button"
            class="action-btn btn-primary"
            @click="$emit('salida-origen', expediente)"
          >
            <i class="pi pi-send"></i>
            <span>Registrar Salida de Origen</span>
          </button>

          <!-- Confirmar Arribo a Puerto -->
          <button
            v-if="hasZarpeConfirmado && !expediente?.fechaArriboAduana && !isCerrado"
            type="button"
            class="action-btn btn-success"
            @click="$emit('arribo-destino', expediente)"
          >
            <i class="pi pi-compass"></i>
            <span>Confirmar Arribo a Destino</span>
          </button>

          <!-- Diligencia SUNAT -->
          <button
            v-if="(expediente?.canalSunat === 'ROJO' || expediente?.canalSunat === 'NARANJA' || expediente?.estadoExpediente === 'INSPECCION') && !isCerrado"
            type="button"
            class="action-btn btn-warning"
            @click="$emit('diligencia', expediente)"
          >
            <i class="pi pi-search-plus"></i>
            <span>Registrar Diligencia SUNAT</span>
          </button>

          <!-- Pedir Envío Terrestre SGA -->
          <button
            v-if="expediente?.estadoExpediente === 'LEVANTE_AUTORIZADO' && expediente?.servicioTransporte && !expediente?.idDespachoTransporte"
            type="button"
            class="action-btn btn-success"
            @click="$emit('solicitar-transporte', expediente)"
          >
            <i class="pi pi-truck"></i>
            <span>Solicitar Despacho Flota</span>
          </button>

          <!-- Retiro Directo en Terminal -->
          <button
            v-if="expediente?.estadoExpediente === 'LEVANTE_AUTORIZADO' && !expediente?.servicioTransporte"
            type="button"
            class="action-btn btn-outline"
            @click="$emit('retiro-directo', expediente)"
          >
            <i class="pi pi-check"></i>
            <span>Acta de Retiro Directo</span>
          </button>
        </template>

        <template v-else>
          <!-- Exportación -->
          <button
            v-if="expediente?.servicioTransporte && !expediente?.idDespachoTransporte && (expediente?.estadoExpediente === 'ASIGNADO' || expediente?.estadoExpediente === 'EN_TRAMITE')"
            type="button"
            class="action-btn btn-primary"
            @click="$emit('recojo-exportacion', expediente)"
          >
            <i class="pi pi-truck"></i>
            <span>Solicitar Recojo Planta</span>
          </button>

          <button
            v-if="expediente?.estadoExpediente === 'ASIGNADO' || (expediente?.estadoExpediente === 'EN_TRAMITE' && !expediente?.fechaArriboAduana)"
            type="button"
            class="action-btn btn-outline"
            @click="$emit('ingreso-origen', expediente)"
          >
            <i class="pi pi-inbox"></i>
            <span>Ingreso Depósito VGM</span>
          </button>

          <button
            v-if="(expediente?.canalSunat === 'ROJO' || expediente?.canalSunat === 'NARANJA' || expediente?.estadoExpediente === 'INSPECCION') && !isCerrado"
            type="button"
            class="action-btn btn-warning"
            @click="$emit('diligencia', expediente)"
          >
            <i class="pi pi-search-plus"></i>
            <span>Registrar Diligencia</span>
          </button>

          <button
            v-if="expediente?.estadoExpediente === 'LEVANTE_AUTORIZADO' && !expediente?.observaciones?.includes('EMBARCADO')"
            type="button"
            class="action-btn btn-success"
            @click="$emit('embarque', expediente)"
          >
            <i class="pi pi-send"></i>
            <span>Confirmar Embarque</span>
          </button>
        </template>

        <!-- Actualizar Tránsito General -->
        <button
          v-if="!isCerrado"
          type="button"
          class="action-btn btn-outline"
          @click="$emit('transito', expediente)"
          title="Actualizar datos de travesía, buque o precinto"
        >
          <i class="pi pi-globe"></i>
          <span>Editar Tránsito</span>
        </button>

        <!-- Documentos Digitales -->
        <button
          type="button"
          class="action-btn btn-outline"
          @click="$emit('documentos', expediente)"
          title="Abrir gestor de documentos digitales adjuntos"
        >
          <i class="pi pi-paperclip"></i>
          <span>Documentos</span>
        </button>

        <!-- Imprimir Ficha -->
        <button
          type="button"
          class="action-btn btn-icon-only"
          @click="imprimirInforme"
          title="Imprimir informe del despacho"
        >
          <i class="pi pi-print"></i>
        </button>
      </div>
    </header>

    <!-- ============================================================== -->
    <!-- 2. HERO BANNER EJECUTIVO DEL EXPEDIENTE                        -->
    <!-- ============================================================== -->
    <div class="informe-hero-banner" v-if="expediente">
      <div class="hero-left-col">
        <div class="hero-kicker-row">
          <span class="hero-badge-kicker">CONTROL ADUANERO OPERATIVO // INFORME TÉCNICO</span>
          <span :class="['regimen-tag', isExport ? 'tag-export' : 'tag-import']">
            <i :class="isExport ? 'pi pi-arrow-up-right' : 'pi pi-arrow-down-left'"></i>
            <span>{{ isExport ? 'EXPORTACIÓN DEFINITIVA' : 'IMPORTACIÓN PARA EL CONSUMO' }}</span>
          </span>
        </div>

        <h1 class="hero-main-title">
          {{ expediente.codigoExpediente }}
        </h1>

        <div class="hero-meta-row">
          <span class="meta-item">
            <i class="pi pi-calendar"></i>
            <span>Apertura: {{ formatDateTime(expediente.fechaApertura) }}</span>
          </span>
          <span class="meta-divider">•</span>
          <span class="meta-item">
            <i class="pi pi-file"></i>
            <span>Cotización: <strong>{{ expediente.codigoCotizacion || '—' }}</strong></span>
          </span>
          <span class="meta-divider">•</span>
          <span class="meta-item">
            <i class="pi pi-user"></i>
            <span>Agente: <strong>{{ expediente.agenteAduaneroNombre || 'Agente Asignado' }}</strong></span>
          </span>
        </div>
      </div>

      <div class="hero-right-col">
        <!-- Semáforo Canal SUNAT -->
        <div class="canal-sunat-box" :class="getCanalBoxClass(expediente.canalSunat)">
          <span class="canal-box-label">CANAL DE CONTROL SUNAT</span>
          <div class="canal-box-content">
            <i :class="getCanalIcon(expediente.canalSunat)" class="canal-big-icon"></i>
            <span class="canal-name">
              {{ expediente.canalSunat ? `CANAL ${expediente.canalSunat}` : (expediente.fechaArriboAduana ? 'PENDIENTE' : 'EN ESPERA DE ARRIBO') }}
            </span>
          </div>
          <span class="canal-help-text">
            {{ getCanalHelpText(expediente) }}
          </span>
        </div>

        <!-- DAM & Estado General -->
        <div class="dam-and-status-box">
          <div class="dam-sub-box">
            <span class="dam-sub-label">DECLARACIÓN ADUANERA (DAM)</span>
            <span class="dam-sub-number font-mono">{{ expediente.numeroDam || 'Pendiente de Asignación' }}</span>
          </div>

          <div class="status-sub-pill" :class="getEstadoPillClass(expediente.estadoExpediente)">
            <span class="status-dot"></span>
            <span>{{ formatEstado(expediente.estadoExpediente) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ============================================================== -->
    <!-- 3. STEPPER VISUAL DE TRAZABILIDAD OPERATIVA                     -->
    <!-- ============================================================== -->
    <section class="trazabilidad-stepper-card" v-if="expediente">
      <div class="stepper-header">
        <h3 class="stepper-title">
          <i class="pi pi-directions"></i>
          <span>Trazabilidad Operativa y Cadena de Custodia</span>
        </h3>
        <span class="stepper-summary-tag">
          {{ getStepperSummaryText(expediente) }}
        </span>
      </div>

      <div class="stepper-horizontal-track">
        <!-- Hito 1: Asignación -->
        <div class="track-node completed">
          <div class="node-circle">
            <i class="pi pi-check"></i>
          </div>
          <div class="node-content">
            <span class="node-title">1. Asignado</span>
            <span class="node-date">{{ formatDateShort(expediente.fechaApertura) }}</span>
            <span class="node-desc">Agente designado</span>
          </div>
        </div>

        <div class="track-connector" :class="{ filled: hasZarpeConfirmado }"></div>

        <!-- Hito 2: Salida de Origen -->
        <div class="track-node" :class="{ completed: hasZarpeConfirmado, active: !hasZarpeConfirmado }">
          <div class="node-circle">
            <i v-if="hasZarpeConfirmado" class="pi pi-check"></i>
            <span v-else>2</span>
          </div>
          <div class="node-content">
            <span class="node-title">{{ isExport ? '2. Ingreso Almacén' : '2. Salida Origen' }}</span>
            <span class="node-date">{{ expediente.fechaZarpeEtd || 'Pendiente' }}</span>
            <span class="node-desc">{{ hasZarpeConfirmado ? 'Zarpe confirmado' : 'En espera de salida' }}</span>
          </div>
        </div>

        <div class="track-connector" :class="{ filled: !!expediente.fechaArriboAduana }"></div>

        <!-- Hito 3: Arribo a Destino -->
        <div class="track-node" :class="{ completed: !!expediente.fechaArriboAduana, active: hasZarpeConfirmado && !expediente.fechaArriboAduana }">
          <div class="node-circle">
            <i v-if="expediente.fechaArriboAduana" class="pi pi-check"></i>
            <span v-else>3</span>
          </div>
          <div class="node-content">
            <span class="node-title">3. Arribo Destino</span>
            <span class="node-date">{{ formatDateTimeShort(expediente.fechaArriboAduana) || (expediente.fechaLlegadaEta ? `ETA: ${expediente.fechaLlegadaEta}` : 'Pendiente') }}</span>
            <span class="node-desc">{{ expediente.fechaArriboAduana ? 'Arribo a puerto' : 'En navegación' }}</span>
          </div>
        </div>

        <div class="track-connector" :class="{ filled: !!expediente.canalSunat }"></div>

        <!-- Hito 4: Canal SUNAT -->
        <div class="track-node" :class="{ completed: !!expediente.canalSunat, active: !!expediente.fechaArriboAduana && !expediente.canalSunat }">
          <div class="node-circle">
            <i v-if="expediente.canalSunat" class="pi pi-check"></i>
            <span v-else>4</span>
          </div>
          <div class="node-content">
            <span class="node-title">4. Canal SUNAT</span>
            <span class="node-date">{{ expediente.canalSunat ? `CANAL ${expediente.canalSunat}` : 'Por asignar' }}</span>
            <span class="node-desc">{{ expediente.canalSunat ? 'Revisión asignada' : 'En espera supervisor' }}</span>
          </div>
        </div>

        <div class="track-connector" :class="{ filled: isDiligenciaCompletada }"></div>

        <!-- Hito 5: Diligencia / Inspección -->
        <div class="track-node" :class="{ completed: isDiligenciaCompletada, active: expediente.canalSunat && !isDiligenciaCompletada && expediente.estadoExpediente !== 'LEVANTE_AUTORIZADO' }">
          <div class="node-circle">
            <i v-if="isDiligenciaCompletada" class="pi pi-check"></i>
            <span v-else>5</span>
          </div>
          <div class="node-content">
            <span class="node-title">5. Diligencia</span>
            <span class="node-date">{{ formatDiligenciaDate(expediente) }}</span>
            <span class="node-desc">{{ formatDiligenciaDesc(expediente) }}</span>
          </div>
        </div>

        <div class="track-connector" :class="{ filled: isLevanteAutorizado }"></div>

        <!-- Hito 6: Levante y Despacho -->
        <div class="track-node" :class="{ completed: isLevanteAutorizado, active: isDiligenciaCompletada && !isLevanteAutorizado }">
          <div class="node-circle">
            <i v-if="isLevanteAutorizado" class="pi pi-check"></i>
            <span v-else>6</span>
          </div>
          <div class="node-content">
            <span class="node-title">6. Levante</span>
            <span class="node-date">{{ formatDateShort(expediente.fechaLevanteAutorizacion) || 'Pendiente' }}</span>
            <span class="node-desc">{{ isLevanteAutorizado ? 'Retiro autorizado' : 'Pendiente de autorización' }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ============================================================== -->
    <!-- 4. CUADRÍCULA DE FICHAS TÉCNICAS MODULARES                      -->
    <!-- ============================================================== -->
    <div class="informe-modules-grid" v-if="expediente">
      <!-- MÓDULO 1: CLIENTE Y OPERACIÓN COMERCIAL -->
      <section class="module-card">
        <header class="module-card-header">
          <div class="header-icon-box">
            <i class="pi pi-building"></i>
          </div>
          <div>
            <h4 class="module-title">1. Cliente & Operación Comercial</h4>
            <span class="module-sub">Datos del consignatario y cotización base</span>
          </div>
        </header>

        <div class="module-body">
          <div class="data-item">
            <span class="data-lbl">Razón Social:</span>
            <span class="data-val strong">{{ expediente.clienteRazonSocial || '—' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">RUC / Identificación Fiscal:</span>
            <span class="data-val font-mono">{{ expediente.clienteNumeroDocumento || '—' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Código de Cotización:</span>
            <span class="data-val doc-tag">{{ expediente.codigoCotizacion || '—' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Régimen Aduanero:</span>
            <span class="data-val">{{ isExport ? 'Exportación Definitiva' : 'Importación para el Consumo' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Modalidad de Transporte:</span>
            <span class="data-val">
              <span v-if="expediente.servicioTransporte" class="pill-flota">
                <i class="pi pi-truck"></i> Flota Propia TransAduanas SGA
              </span>
              <span v-else class="pill-retiro">
                <i class="pi pi-user"></i> Retiro Directo por Cliente
              </span>
            </span>
          </div>
        </div>
      </section>

      <!-- MÓDULO 2: LOGÍSTICA DE TRAVESÍA Y EMBARQUE -->
      <section class="module-card">
        <header class="module-card-header">
          <div class="header-icon-box">
            <i class="pi pi-globe"></i>
          </div>
          <div>
            <h4 class="module-title">2. Logística de Travesía & Embarque</h4>
            <span class="module-sub">Identificación de nave, contenedor y precintos</span>
          </div>
        </header>

        <div class="module-body">
          <div class="data-item">
            <span class="data-lbl">Nave / Buque / Vuelo:</span>
            <span class="data-val strong">{{ expediente.naveViaje || 'Sin nave asignada' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Número de Contenedor:</span>
            <span class="data-val font-mono highlight">{{ expediente.numeroContenedor || 'Pendiente' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Precinto Aduanero de Origen:</span>
            <span class="data-val font-mono">{{ expediente.precintoAduanero || 'Pendiente' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Fecha de Salida (ETD):</span>
            <span class="data-val">{{ expediente.fechaZarpeEtd || 'No confirmada' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Llegada Estimada (ETA):</span>
            <span class="data-val">{{ expediente.fechaLlegadaEta || 'No programada' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Fecha y Hora Real de Arribo:</span>
            <span class="data-val strong text-primary">
              {{ expediente.fechaArriboAduana ? formatDateTime(expediente.fechaArriboAduana) : 'Pendiente de arribo' }}
            </span>
          </div>
          <div class="data-item" v-if="expediente.terminalAlmacenDestino">
            <span class="data-lbl">Terminal Portuario Destino:</span>
            <span class="data-val strong">{{ expediente.terminalAlmacenDestino }}</span>
          </div>
        </div>
      </section>

      <!-- MÓDULO 3: FISCALIZACIÓN SUNAT & DILIGENCIA -->
      <section class="module-card">
        <header class="module-card-header">
          <div class="header-icon-box">
            <i class="pi pi-shield"></i>
          </div>
          <div>
            <h4 class="module-title">3. Control Aduanero & SUNAT</h4>
            <span class="module-sub">Asignación de canal, aforo y estado de levante</span>
          </div>
        </header>

        <div class="module-body">
          <div class="data-item">
            <span class="data-lbl">Declaración DAM:</span>
            <span class="data-val font-mono strong">{{ expediente.numeroDam || 'Sin numeración DAM' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Canal de Selección:</span>
            <span class="data-val">
              <span v-if="expediente.canalSunat" :class="['canal-tag', `canal-${expediente.canalSunat.toLowerCase()}`]">
                <i :class="getCanalIcon(expediente.canalSunat)"></i>
                CANAL {{ expediente.canalSunat }}
              </span>
              <span v-else class="text-muted">Pendiente de designación por SUNAT</span>
            </span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Especialista SUNAT Asignado:</span>
            <span class="data-val">{{ expediente.especialistaSunat || 'No asignado' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Fecha de Diligencia:</span>
            <span class="data-val">{{ expediente.fechaDiligencia ? formatDateTime(expediente.fechaDiligencia) : 'Sin diligencia registrada' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Resultado de Inspección:</span>
            <span class="data-val">{{ expediente.resultadoInspeccion || 'En espera de aforo o canal verde' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Autorización de Levante:</span>
            <span class="data-val strong text-success">
              {{ expediente.fechaLevanteAutorizacion ? formatDateTime(expediente.fechaLevanteAutorizacion) : 'No otorgado' }}
            </span>
          </div>
        </div>
      </section>

      <!-- MÓDULO 4: ESPECIALISTAS Y CONTACTO EN EL EXTERIOR -->
      <section class="module-card">
        <header class="module-card-header">
          <div class="header-icon-box">
            <i class="pi pi-users"></i>
          </div>
          <div>
            <h4 class="module-title">4. Contactos & Equipo Responsable</h4>
            <span class="module-sub">Personal operativo local e internacional</span>
          </div>
        </header>

        <div class="module-body">
          <div class="data-item">
            <span class="data-lbl">Agente Aduanero Responsable:</span>
            <span class="data-val strong">{{ expediente.agenteAduaneroNombre || 'Agente Asignado' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Contacto en el Exterior:</span>
            <span class="data-val">{{ expediente.contactoExteriorNombre || 'No especificado' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Teléfono de Contacto Exterior:</span>
            <span class="data-val font-mono">{{ expediente.contactoExteriorTelefono || '—' }}</span>
          </div>
          <div class="data-item">
            <span class="data-lbl">Correo Electrónico Exterior:</span>
            <span class="data-val">{{ expediente.contactoExteriorCorreo || '—' }}</span>
          </div>
          <div class="data-item" v-if="expediente.ticketBalanzaVgm">
            <span class="data-lbl">Ticket Balanza VGM / Depósito:</span>
            <span class="data-val font-mono">{{ expediente.ticketBalanzaVgm }}</span>
          </div>
        </div>
      </section>
    </div>

    <!-- ============================================================== -->
    <!-- 5. BITÁCORA Y OBSERVACIONES TÉCNICAS                           -->
    <!-- ============================================================== -->
    <section class="observaciones-section-card" v-if="expediente">
      <header class="obs-header">
        <i class="pi pi-book"></i>
        <span>Bitácora Operativa & Observaciones del Expediente</span>
      </header>
      <div class="obs-body">
        <div v-if="expediente.observaciones" class="obs-text-content">
          <p>{{ expediente.observaciones }}</p>
        </div>
        <div v-else class="obs-empty">
          <span>No se han registrado observaciones técnicas en la bitácora operativa de este despacho.</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  expediente: {
    type: Object,
    default: null
  }
})

defineEmits([
  'back',
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
  'documentos'
])

const isExport = computed(() => {
  return (props.expediente?.regimenAduanero || '').toUpperCase().includes('EXPORT')
})

const isCerrado = computed(() => {
  return props.expediente?.estadoExpediente === 'CERRADO' || props.expediente?.estadoExpediente === 'ANULADO'
})

const hasZarpeConfirmado = computed(() => {
  const obs = props.expediente?.observaciones || ''
  return (
    obs.includes('SALIDA ORIGEN CONFIRMADA') ||
    props.expediente?.estadoExpediente === 'EN_TRAMITE' ||
    props.expediente?.estadoExpediente === 'INSPECCION' ||
    props.expediente?.estadoExpediente === 'LEVANTE_AUTORIZADO'
  )
})

const isDiligenciaCompletada = computed(() => {
  const exp = props.expediente
  if (!exp) return false
  if (exp.canalSunat === 'VERDE') return true
  return !!exp.fechaDiligencia || exp.estadoExpediente === 'LEVANTE_AUTORIZADO'
})

const isLevanteAutorizado = computed(() => {
  const exp = props.expediente
  return exp?.estadoExpediente === 'LEVANTE_AUTORIZADO' || !!exp?.fechaLevanteAutorizacion
})

const formatEstado = (st) => {
  const map = {
    ASIGNADO: 'Asignado a Agente',
    EN_TRAMITE: 'En Tránsito Internacional',
    INSPECCION: 'En Diligencia / Inspección SUNAT',
    LEVANTE_AUTORIZADO: 'Levante Autorizado',
    REGULARIZADO: 'Despacho Regularizado',
    CERRADO: 'Expediente Cerrado',
    ANULADO: 'Expediente Anulado'
  }
  return map[st] || st || 'Asignado'
}

const getEstadoPillClass = (st) => {
  const map = {
    ASIGNADO: 'pill-asignado',
    EN_TRAMITE: 'pill-transito',
    INSPECCION: 'pill-inspeccion',
    LEVANTE_AUTORIZADO: 'pill-levante',
    REGULARIZADO: 'pill-levante',
    CERRADO: 'pill-neutral',
    ANULADO: 'pill-anulado'
  }
  return map[st] || 'pill-neutral'
}

const getCanalBoxClass = (canal) => {
  if (canal === 'VERDE') return 'canal-box-verde'
  if (canal === 'NARANJA') return 'canal-box-naranja'
  if (canal === 'ROJO') return 'canal-box-rojo'
  return 'canal-box-pendiente'
}

const getCanalIcon = (canal) => {
  if (canal === 'VERDE') return 'pi pi-check-circle'
  if (canal === 'NARANJA') return 'pi pi-file'
  if (canal === 'ROJO') return 'pi pi-exclamation-triangle'
  return 'pi pi-clock'
}

const getCanalHelpText = (exp) => {
  if (exp.canalSunat === 'VERDE') return 'Levante automático sin revisión documental ni física.'
  if (exp.canalSunat === 'NARANJA') return 'Revisión documentaria por especialista de aduanas.'
  if (exp.canalSunat === 'ROJO') return 'Requiere aforo físico de mercancía en terminal.'
  if (exp.fechaArriboAduana) return 'Mercancía en puerto. Supervisor asignará el canal oficial.'
  return 'Se asignará una vez confirmado el arribo de la nave a destino.'
}

const getStepperSummaryText = (exp) => {
  if (isLevanteAutorizado.value) return 'Trámite Culminado: Mercancía con Levante Autorizado'
  if (exp.canalSunat === 'ROJO' || exp.canalSunat === 'NARANJA') return `En Gestión SUNAT: Diligencia requerida para Canal ${exp.canalSunat}`
  if (exp.canalSunat === 'VERDE') return 'Canal Verde: Habilitado para levante inmediato'
  if (exp.fechaArriboAduana) return 'Mercancía en Puerto Nacional: Esperando asignación de Canal SUNAT'
  if (hasZarpeConfirmado.value) return 'En Navegación / Travesía Internacional'
  return 'Fase Inicial: En espera de confirmación de salida de origen'
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return isNaN(d) ? dateStr : `${d.toLocaleDateString()} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

const formatDateShort = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return isNaN(d) ? dateStr : d.toLocaleDateString()
}

const formatDateTimeShort = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return isNaN(d) ? dateStr : `${d.toLocaleDateString()} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

const formatDiligenciaDate = (exp) => {
  if (exp.canalSunat === 'VERDE') return 'Exento'
  if (exp.fechaDiligencia) return formatDateShort(exp.fechaDiligencia)
  return 'Pendiente'
}

const formatDiligenciaDesc = (exp) => {
  if (exp.canalSunat === 'VERDE') return 'Sin aforo'
  if (exp.fechaDiligencia) return 'Acta registrada'
  return 'Aforo de campo'
}

const imprimirInforme = () => {
  window.print()
}
</script>

<style scoped>
.agente-informe-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  width: 100%;
  padding: 0.25rem 0 2rem 0;
  color: var(--color-text-main);
}

/* 1. Barra de Navegación */
.informe-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  background: var(--color-bg-surface);
  padding: 0.85rem 1.25rem;
  border-radius: var(--radius-md, 8px);
  border: 1px solid var(--color-border);
  box-shadow: var(--shadow-subtle);
}

.top-bar-left {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.btn-back-nav {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.45rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  color: var(--color-text-main);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-back-nav:hover {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

.breadcrumbs {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: var(--font-size-sm, 0.8125rem);
}

.crumb-muted {
  color: var(--color-text-subtle);
}

.crumb-active {
  color: var(--color-text-main);
  font-weight: 700;
}

.top-bar-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.85rem;
  border-radius: var(--radius-sm, 6px);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
  border: 1px solid transparent;
}

.btn-primary {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

.btn-primary:hover {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.btn-success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border-color: var(--color-success-border);
}

.btn-success:hover {
  filter: brightness(0.95);
}

.btn-warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border-color: var(--color-warning-border);
}

.btn-warning:hover {
  filter: brightness(0.95);
}

.btn-outline {
  background: var(--color-bg-surface);
  border-color: var(--color-border);
  color: var(--color-text-main);
}

.btn-outline:hover {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-strong);
}

.btn-icon-only {
  width: 34px;
  height: 34px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-muted);
  border-radius: var(--radius-sm, 6px);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-icon-only:hover {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  border-color: var(--color-border-strong);
}

/* 2. Hero Banner */
.informe-hero-banner {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 10px);
  padding: 1.5rem 1.75rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
  box-shadow: var(--shadow-subtle);
}

.hero-left-col {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
  flex: 1;
}

.hero-kicker-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.hero-badge-kicker {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  text-transform: uppercase;
}

.regimen-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 800;
  padding: 0.15rem 0.55rem;
  border-radius: 4px;
}

.tag-import {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.tag-export {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.hero-main-title {
  margin: 0;
  font-size: 1.85rem;
  font-weight: 800;
  color: var(--color-text-main);
  letter-spacing: -0.02em;
}

.hero-meta-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-muted);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.meta-divider {
  color: var(--color-border-strong);
}

.hero-right-col {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

/* Semáforo SUNAT */
.canal-sunat-box {
  display: flex;
  flex-direction: column;
  padding: 0.85rem 1.15rem;
  border-radius: var(--radius-sm, 6px);
  border: 1px solid;
  min-width: 200px;
}

.canal-box-label {
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  margin-bottom: 0.25rem;
}

.canal-box-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.canal-big-icon {
  font-size: 1.25rem;
}

.canal-name {
  font-size: 1.1rem;
  font-weight: 900;
  letter-spacing: 0.02em;
}

.canal-help-text {
  font-size: var(--font-size-2xs, 0.6875rem);
  margin-top: 0.35rem;
  line-height: 1.25;
}

.canal-box-verde {
  background: var(--color-success-bg);
  border-color: var(--color-success-border);
  color: var(--color-success-text);
}

.canal-box-verde .canal-help-text {
  color: var(--color-success-text);
}

.canal-box-naranja {
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
  color: var(--color-warning-text);
}

.canal-box-naranja .canal-help-text {
  color: var(--color-warning-text);
}

.canal-box-rojo {
  background: var(--color-danger-bg);
  border-color: var(--color-danger-border);
  color: var(--color-danger-text);
}

.canal-box-rojo .canal-help-text {
  color: var(--color-danger-text);
}

.canal-box-pendiente {
  background: var(--color-bg-subtle);
  border-color: var(--color-border);
  color: var(--color-text-muted);
}

.canal-box-pendiente .canal-help-text {
  color: var(--color-text-subtle);
}

/* DAM & Estado Box */
.dam-and-status-box {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: flex-end;
}

.dam-sub-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.4rem 0.75rem;
  border-radius: var(--radius-sm, 6px);
}

.dam-sub-label {
  font-size: 0.6rem;
  font-weight: 700;
  color: var(--color-text-subtle);
  letter-spacing: 0.04em;
}

.dam-sub-number {
  font-size: var(--font-size-base, 0.875rem);
  font-weight: 700;
  color: var(--color-text-main);
  font-family: var(--font-mono);
}

.status-sub-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.7rem;
  border-radius: 14px;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.pill-asignado {
  background: var(--color-info-bg);
  color: var(--color-info-text);
}

.pill-transito {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.pill-inspeccion {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
}

.pill-levante {
  background: var(--color-success-bg);
  color: var(--color-success-text);
}

.pill-neutral {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
}

.pill-anulado {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
}

/* 3. Stepper Card */
.trazabilidad-stepper-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 10px);
  padding: 1.25rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  box-shadow: var(--shadow-subtle);
}

.stepper-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-border-subtle);
  padding-bottom: 0.65rem;
}

.stepper-title {
  margin: 0;
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.stepper-summary-tag {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  padding: 0.2rem 0.6rem;
  border-radius: 4px;
  border: 1px solid var(--color-border);
}

.stepper-horizontal-track {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  position: relative;
  overflow-x: auto;
  padding: 0.5rem 0;
}

.track-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 0.4rem;
  min-width: 110px;
  z-index: 2;
}

.node-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border: 2px solid var(--color-border);
  transition: all var(--transition-normal, 0.2s ease);
}

.track-node.completed .node-circle {
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-color: var(--color-primary);
}

.track-node.active .node-circle {
  background: var(--color-bg-surface);
  color: var(--color-primary);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(180, 83, 9, 0.15);
}

.node-content {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.node-title {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.node-date {
  font-size: var(--font-size-2xs, 0.6875rem);
  color: var(--color-accent-kicker);
  font-weight: 600;
}

.node-desc {
  font-size: 0.65rem;
  color: var(--color-text-subtle);
}

.track-connector {
  flex: 1;
  height: 3px;
  background: var(--color-border);
  margin-top: 15px;
  z-index: 1;
}

.track-connector.filled {
  background: var(--color-primary);
}

/* 4. Módulos Grid */
.informe-modules-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.25rem;
}

.module-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 10px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-subtle);
}

.module-card-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1.15rem;
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border);
}

.header-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: var(--color-bg-surface);
  color: var(--color-accent-kicker);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  border: 1px solid var(--color-border-subtle);
}

.module-title {
  margin: 0;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.module-sub {
  font-size: var(--font-size-2xs, 0.6875rem);
  color: var(--color-text-subtle);
}

.module-body {
  padding: 1rem 1.15rem;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.data-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 1rem;
  font-size: var(--font-size-sm, 0.8125rem);
  padding-bottom: 0.4rem;
  border-bottom: 1px dashed var(--color-border-subtle);
}

.data-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.data-lbl {
  color: var(--color-text-muted);
  font-weight: 500;
  flex-shrink: 0;
}

.data-val {
  color: var(--color-text-main);
  text-align: right;
  word-break: break-word;
}

.data-val.strong {
  font-weight: 700;
}

.data-val.highlight {
  color: var(--color-accent-copper);
  font-weight: 700;
}

.doc-tag {
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  border: 1px solid var(--color-border-subtle);
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-weight: 600;
}

.pill-flota {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-success-text);
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.pill-retiro {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 0.15rem 0.5rem;
  border-radius: 4px;
}

.canal-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 800;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
}

/* 5. Observaciones */
.observaciones-section-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 10px);
  overflow: hidden;
  box-shadow: var(--shadow-subtle);
}

.obs-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.15rem;
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.obs-body {
  padding: 1.15rem;
}

.obs-text-content p {
  margin: 0;
  font-size: var(--font-size-base, 0.875rem);
  line-height: 1.6;
  color: var(--color-text-main);
  white-space: pre-line;
}

.obs-empty {
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-subtle);
  font-style: italic;
}

.text-primary {
  color: var(--color-primary);
}

.text-success {
  color: var(--color-success-text);
}

.text-muted {
  color: var(--color-text-subtle);
}

.font-mono {
  font-family: var(--font-mono);
}

@media print {
  .no-print {
    display: none !important;
  }
  .agente-informe-view {
    padding: 0;
  }
}

@media (max-width: 900px) {
  .informe-hero-banner {
    flex-direction: column;
    align-items: flex-start;
  }
  .hero-right-col {
    width: 100%;
    justify-content: space-between;
  }
  .informe-modules-grid {
    grid-template-columns: 1fr;
  }
}
</style>
