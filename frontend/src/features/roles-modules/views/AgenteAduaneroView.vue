<template>
  <div class="logistica-view">
    <!-- 1. Toolbar de Acciones Warm Editorial -->
    <div class="agente-action-toolbar">
      <div class="toolbar-left-info">
        <span class="badge-kicker">OPERACIONES ADUANERAS // AGENTE EN CAMPO</span>
        <h3 class="toolbar-title">Gestión de Despachos de Importación y Control de Almacén</h3>
      </div>

      <div class="toolbar-actions-group">
        <!-- Navegación por Pestañas Principales -->
        <div class="tabs-nav-bar">
          <button
            type="button"
            :class="['tab-pill-btn', { active: currentTab === 'despachos' }]"
            @click="currentTab = 'despachos'"
          >
            <i class="pi pi-compass"></i>
            <span>Mis Despachos ({{ misExpedientes.length }})</span>
          </button>

          <button
            type="button"
            :class="['tab-pill-btn', { active: currentTab === 'almacen' }]"
            @click="currentTab = 'almacen'"
          >
            <i class="pi pi-box"></i>
            <span>Depósito & Balanza VGM ({{ inventario.length }})</span>
          </button>
        </div>

        <button
          type="button"
          class="btn-outline-warm"
          @click="loadData"
          :disabled="loading"
          title="Recargar datos desde el servidor"
        >
          <i :class="['pi pi-sync', { 'pi-spin': loading }]"></i>
          <span>Actualizar</span>
        </button>

        <button
          v-if="currentTab === 'almacen'"
          type="button"
          class="btn-espresso"
          @click="showIngresoModal = true"
        >
          <i class="pi pi-plus"></i>
          <span>+ Ingreso VGM</span>
        </button>
      </div>
    </div>

    <!-- ============================================================== -->
    <!-- PESTAÑA 1: MIS DESPACHOS DE IMPORTACIÓN ASIGNADOS (BPMN)       -->
    <!-- ============================================================== -->
    <div v-if="currentTab === 'despachos'" class="fade-in">
      <!-- 2. Grid de Métricas KPI Técnicas de Importación -->
      <div class="metrics-kpi-grid">
        <div class="kpi-card accent-card" @click="filterEstadoExp = ''">
          <div class="kpi-card-header">
            <span class="kpi-num">01</span>
            <span class="kpi-label">Mis Despachos</span>
            <i class="pi pi-folder-open kpi-icon accent"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ misExpedientes.length }}</span>
            <span class="kpi-badge accent">Asignados</span>
          </div>
          <div class="kpi-footer-note">
            <span>Expedientes aduaneros asignados a mi cargo</span>
          </div>
        </div>

        <div class="kpi-card pending-card" @click="filterEstadoExp = 'EN_TRAMITE'">
          <div class="kpi-card-header">
            <span class="kpi-num">02</span>
            <span class="kpi-label">En Tránsito / Arribo</span>
            <i class="pi pi-globe kpi-icon pending"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-pending">{{ enTransitoCount }}</span>
            <span class="kpi-badge pending">Travesía / Llegada</span>
          </div>
          <div class="kpi-footer-note">
            <span>Nave en zarpe o arribo a puerto de aduana</span>
          </div>
        </div>

        <div class="kpi-card warning-card" @click="filterEstadoExp = 'INSPECCION'">
          <div class="kpi-card-header">
            <span class="kpi-num">03</span>
            <span class="kpi-label">En Diligencia / Aforo</span>
            <i class="pi pi-search kpi-icon warning"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-warning">{{ enInspeccionCount }}</span>
            <span class="kpi-badge warning">Canal Naranja/Rojo</span>
          </div>
          <div class="kpi-footer-note">
            <span>Reconocimiento físico o documental SUNAT</span>
          </div>
        </div>

        <div class="kpi-card success-card" @click="filterEstadoExp = 'LEVANTE_AUTORIZADO'">
          <div class="kpi-card-header">
            <span class="kpi-num">04</span>
            <span class="kpi-label">Levante Concedido</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-success">{{ conLevanteCount }}</span>
            <span class="kpi-badge success">Con Levante</span>
          </div>
          <div class="kpi-footer-note">
            <span>Listos para despacho terrestre o retiro</span>
          </div>
        </div>
      </div>

      <!-- 3. Contenedor de Listado de Expedientes Asignados -->
      <div class="content-table-card">
        <div class="card-header-bar">
          <div>
            <span class="badge-kicker">BANDEJA OPERATIVA DE CAMPO</span>
            <h3 class="section-title">Despachos Aduaneros Asignados (Importación y Exportación)</h3>
          </div>

          <div class="header-actions">
            <!-- Filtro por Régimen -->
            <select v-model="filterRegimenExp" class="filter-select">
              <option value="">Todos los Regímenes</option>
              <option value="IMPORTACION">Importación para el Consumo</option>
              <option value="EXPORTACION">Exportación Definitiva</option>
            </select>

            <!-- Filtro por Canal SUNAT -->
            <select v-model="filterCanalExp" class="filter-select">
              <option value="">Todos los Canales SUNAT</option>
              <option value="VERDE">Canal VERDE (Libre)</option>
              <option value="NARANJA">Canal NARANJA (Documental)</option>
              <option value="ROJO">Canal ROJO (Aforo Físico)</option>
              <option value="PENDIENTE">Canal Pendiente</option>
            </select>

            <!-- Filtro por Estado -->
            <select v-model="filterEstadoExp" class="filter-select">
              <option value="">Todos los Estados</option>
              <option value="ASIGNADO">Asignado</option>
              <option value="EN_TRAMITE">En Tránsito</option>
              <option value="INSPECCION">En Inspección / Aforo</option>
              <option value="LEVANTE_AUTORIZADO">Levante Autorizado</option>
              <option value="CERRADO">Cerrado</option>
            </select>

            <!-- Búsqueda Rápida -->
            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchExpQuery"
                type="text"
                class="search-input"
                placeholder="Buscar por código, DAM, cliente, contenedor..."
              />
            </div>
          </div>
        </div>

        <!-- Tabla de Despachos con PrimeVue DataTable -->
        <div class="table-container">
          <DataTable
            :value="filteredExpedientes"
            :loading="loading"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idExpediente"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-folder-open empty-icon"></i>
                <p class="empty-title">No hay despachos aduaneros asignados</p>
                <p class="empty-desc">Los expedientes son asignados por el Supervisor Aduanero al aperturar la cotización comercial.</p>
              </div>
            </template>

            <template #loading>
              <div class="loading-state">
                <i class="pi pi-spin pi-spinner"></i>
                <span>Cargando despachos asignados...</span>
              </div>
            </template>

            <Column header="Expediente">
              <template #body="{ data: exp }">
                <div class="exp-code-badge" @click="openDetalleExp(exp)" title="Ver ficha técnica completa">
                  <i class="pi pi-folder"></i>
                  <span class="font-bold font-mono">{{ exp.codigoExpediente }}</span>
                </div>
                <div class="client-sub font-mono text-2xs mt-1">
                  Cotiz: {{ exp.codigoCotizacion || '—' }}
                </div>
                <div class="mt-1">
                  <span class="badge-regimen" :class="isExport(exp) ? 'badge-export' : 'badge-import'">
                    {{ isExport(exp) ? 'EXPORTACIÓN' : 'IMPORTACIÓN' }}
                  </span>
                </div>
              </template>
            </Column>

            <Column header="Cliente">
              <template #body="{ data: exp }">
                <div class="client-cell">
                  <span class="client-name font-bold">{{ exp.clienteRazonSocial || '—' }}</span>
                  <span class="client-sub font-mono">Doc: {{ exp.clienteNumeroDocumento }}</span>
                </div>
              </template>
            </Column>

            <Column header="Embarque & Tránsito">
              <template #body="{ data: exp }">
                <div class="client-cell">
                  <span class="font-bold text-xs">{{ exp.naveViaje || (isExport(exp) ? 'Nave pendiente de asignación' : 'Sin nave confirmada') }}</span>
                  <span class="client-sub font-mono text-2xs">
                    Cont: {{ exp.numeroContenedor || 'S/N' }} | Prec: {{ exp.precintoAduanero || 'S/N' }}
                  </span>
                  <span v-if="exp.fechaZarpeEtd && isExport(exp)" class="client-sub text-2xs text-success">
                    Zarpe: {{ formatDateShort(exp.fechaZarpeEtd) }}
                  </span>
                  <span v-else-if="exp.fechaLlegadaEta" class="client-sub text-2xs text-pending">
                    ETA: {{ formatDateShort(exp.fechaLlegadaEta) }}
                  </span>
                </div>
              </template>
            </Column>

            <Column header="DAM & Canal SUNAT">
              <template #body="{ data: exp }">
                <div class="client-cell">
                  <span class="font-mono text-xs font-bold">{{ exp.numeroDam || 'Sin DAM numerada' }}</span>
                  <Tag
                    :value="exp.canalSunat || 'PENDIENTE'"
                    :severity="getCanalSeverity(exp.canalSunat)"
                    rounded
                  />
                </div>
              </template>
            </Column>

            <Column header="Estado Trámite">
              <template #body="{ data: exp }">
                <Tag
                  :value="exp.estadoExpediente || 'ASIGNADO'"
                  :severity="getExpedienteSeverity(exp.estadoExpediente)"
                  rounded
                />
              </template>
            </Column>

            <Column header="Tramo Terrestre">
              <template #body="{ data: exp }">
                <!-- EXPORTACIÓN -->
                <div v-if="isExport(exp)" class="client-cell">
                  <div v-if="exp.servicioTransporte">
                    <span class="text-xs font-bold text-info">
                      <i class="pi pi-truck text-xs"></i> Recojo en Planta SGA
                    </span>
                    <span v-if="exp.codigoTrackingTransporte" class="client-sub font-mono text-2xs">
                      {{ exp.codigoTrackingTransporte }} ({{ exp.estadoTransporte || 'PROGRAMADO' }})
                    </span>
                    <span v-else class="text-warning text-2xs font-bold">
                      Pendiente Orden Recojo
                    </span>
                  </div>
                  <div v-else>
                    <span class="text-xs text-muted-dash">
                      <i class="pi pi-building text-xs"></i> Ingreso por Exportador
                    </span>
                    <span class="client-sub text-2xs">Flete Terrestre Propio</span>
                  </div>
                </div>

                <!-- IMPORTACIÓN -->
                <div v-else class="client-cell">
                  <div v-if="exp.servicioTransporte">
                    <span class="text-xs font-bold text-info">
                      <i class="pi pi-truck text-xs"></i> Flota SGA Contratada
                    </span>
                    <span v-if="exp.codigoTrackingTransporte" class="client-sub font-mono text-2xs">
                      {{ exp.codigoTrackingTransporte }} ({{ exp.estadoTransporte || 'PROGRAMADO' }})
                    </span>
                    <span v-else-if="exp.estadoExpediente === 'LEVANTE_AUTORIZADO'" class="text-warning text-2xs font-bold">
                      Requiere Solicitar Envío
                    </span>
                    <span v-else class="client-sub text-2xs">
                      Pendiente de Levante
                    </span>
                  </div>
                  <div v-else>
                    <span class="text-xs text-muted-dash">
                      <i class="pi pi-building text-xs"></i> Retiro en Terminal
                    </span>
                    <span class="client-sub text-2xs">Transporte por Cliente</span>
                  </div>
                </div>
              </template>
            </Column>

            <Column header="Acciones de Campo" headerClass="text-right" bodyClass="text-right">
              <template #body="{ data: exp }">
                <div class="actions-wrapper">
                  <!-- ACCIONES ESPECÍFICAS DE EXPORTACIÓN -->
                  <template v-if="isExport(exp)">
                    <!-- Botón E1: Pedir Recojo en Planta (Si contrató transporte y no tiene orden) -->
                    <button
                      v-if="exp.servicioTransporte && !exp.idDespachoTransporte && (exp.estadoExpediente === 'ASIGNADO' || exp.estadoExpediente === 'EN_TRAMITE')"
                      type="button"
                      class="action-btn-outline approve"
                      title="Solicitar Recojo de Carga en Planta del Exportador"
                      @click="openRecojoExportacionModal(exp)"
                    >
                      <i class="pi pi-truck"></i>
                      <span>Pedir Recojo</span>
                    </button>

                    <!-- Botón E2: Confirmar Ingreso a Depósito Temporal / Zona Primaria (VGM) -->
                    <button
                      v-if="exp.estadoExpediente === 'ASIGNADO' || (exp.estadoExpediente === 'EN_TRAMITE' && !exp.fechaArriboAduana)"
                      type="button"
                      class="action-btn-outline"
                      title="Confirmar Ingreso a Depósito Temporal / Zona Primaria (VGM)"
                      @click="openIngresoOrigenModal(exp)"
                    >
                      <i class="pi pi-inbox"></i>
                      <span>Ingreso Almacén</span>
                    </button>

                    <!-- Botón E3: Diligencia / Aforo Físico (Canal Naranja / Rojo) -->
                    <button
                      v-if="exp.canalSunat === 'ROJO' || exp.canalSunat === 'NARANJA' || exp.estadoExpediente === 'INSPECCION'"
                      type="button"
                      class="action-btn-outline observe"
                      title="Registrar Acta de Diligencia / Inspección Física SUNAT"
                      @click="openDiligenciaModal(exp)"
                    >
                      <i class="pi pi-search-plus"></i>
                      <span>Diligencia</span>
                    </button>

                    <!-- Botón E4: Confirmar Embarque Internacional (Levante concedido y no embarcado) -->
                    <button
                      v-if="exp.estadoExpediente === 'LEVANTE_AUTORIZADO' && !isEmbarcado(exp)"
                      type="button"
                      class="action-btn-outline approve"
                      title="Confirmar Embarque a Bordo de la Nave / Buque"
                      @click="openEmbarqueModal(exp)"
                    >
                      <i class="pi pi-send"></i>
                      <span>Confirmar Embarque</span>
                    </button>

                    <!-- Botón E5: Confirmar Entrega en Destino Exterior (Embarcado y pendiente recepción) -->
                    <button
                      v-if="exp.estadoExpediente === 'LEVANTE_AUTORIZADO' && isEmbarcado(exp) && !isRecepcionado(exp)"
                      type="button"
                      class="action-btn-outline approve"
                      title="Confirmar Recepción por Consignatario Exterior"
                      @click="openRecepcionExteriorModal(exp)"
                    >
                      <i class="pi pi-check-circle"></i>
                      <span>Entrega Exterior</span>
                    </button>

                    <!-- Botón E6: Actualizar Datos de Nave / Contenedor / Precinto -->
                    <button
                      type="button"
                      class="action-btn-outline"
                      title="Actualizar Datos de Salida (Nave, Contenedor, Precinto)"
                      @click="openTransitoModal(exp)"
                    >
                      <i class="pi pi-globe"></i>
                      <span>Tránsito</span>
                    </button>
                  </template>

                  <!-- ACCIONES ESPECÍFICAS DE IMPORTACIÓN -->
                  <template v-else>
                    <!-- Botón I1: Actualizar Datos de Embarque / Tránsito en Origen -->
                    <button
                      type="button"
                      class="action-btn-outline"
                      title="Actualizar Datos de Embarque (Nave, Contenedor, Precinto, ETA)"
                      @click="openTransitoModal(exp)"
                    >
                      <i class="pi pi-globe"></i>
                      <span>Tránsito</span>
                    </button>

                    <!-- Botón I2: Registrar Diligencia / Intervención de Campo (Canal Naranja/Rojo) -->
                    <button
                      v-if="exp.canalSunat === 'ROJO' || exp.canalSunat === 'NARANJA' || exp.estadoExpediente === 'INSPECCION'"
                      type="button"
                      class="action-btn-outline observe"
                      title="Registrar Acta de Diligencia / Aforo Físico SUNAT"
                      @click="openDiligenciaModal(exp)"
                    >
                      <i class="pi pi-search-plus"></i>
                      <span>Diligencia</span>
                    </button>

                    <!-- Botón I3: Solicitar Envío a Destino (Si contrató transporte y tiene levante) -->
                    <button
                      v-if="exp.estadoExpediente === 'LEVANTE_AUTORIZADO' && exp.servicioTransporte && !exp.idDespachoTransporte"
                      type="button"
                      class="action-btn-outline approve"
                      title="Solicitar Traslado Terrestre a Transporte y Flota"
                      @click="openSolicitudTransporteModal(exp)"
                    >
                      <i class="pi pi-send"></i>
                      <span>Pedir Envío</span>
                    </button>

                    <!-- Botón I4: Acta de Retiro Directo (Si no contrató transporte y tiene levante) -->
                    <button
                      v-if="exp.estadoExpediente === 'LEVANTE_AUTORIZADO' && !exp.servicioTransporte"
                      type="button"
                      class="action-btn-outline"
                      title="Registrar Acta de Retiro en Terminal por el Cliente"
                      @click="openRetiroDirectoModal(exp)"
                    >
                      <i class="pi pi-building"></i>
                      <span>Acta Retiro</span>
                    </button>
                  </template>

                  <!-- COMUNES: Documentos Adjuntos y Detalle -->
                  <button
                    type="button"
                    class="action-btn-outline"
                    title="Gestionar Documentos Aduaneros (BL, Factura, Packing List, DAM)"
                    @click="openDocumentosExpediente(exp)"
                  >
                    <i class="pi pi-paperclip"></i>
                  </button>

                  <button
                    type="button"
                    class="action-btn-primary"
                    title="Ver Ficha y Auditoría Completa del Expediente"
                    @click="openDetalleExp(exp)"
                  >
                    <i class="pi pi-eye"></i>
                  </button>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- ============================================================== -->
    <!-- PESTAÑA 2: DEPÓSITOS TEMPORALES & CONTROL VGM                  -->
    <!-- ============================================================== -->
    <div v-else-if="currentTab === 'almacen'" class="fade-in">
      <!-- 2. Grid de Métricas KPI Técnicas (Warm Editorial) -->
      <div class="metrics-kpi-grid">
        <div class="kpi-card pending-card" @click="filterEstado = 'EN_CUSTODIA'">
          <div class="kpi-card-header">
            <span class="kpi-num">01</span>
            <span class="kpi-label">En Custodia / Patio</span>
            <i class="pi pi-box kpi-icon pending"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-pending">{{ enCustodiaCount }}</span>
            <span class="kpi-badge pending">En Depósito</span>
          </div>
          <div class="kpi-footer-note">
            <span>Contenedores en bahía y almacén temporal</span>
          </div>
        </div>

        <div class="kpi-card success-card" @click="filterEstado = 'SALIDA_AUTORIZADA'">
          <div class="kpi-card-header">
            <span class="kpi-num">02</span>
            <span class="kpi-label">Salidas Autorizadas</span>
            <i class="pi pi-check-circle kpi-icon success"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-success">{{ salidasCount }}</span>
            <span class="kpi-badge success">Con Levante</span>
          </div>
          <div class="kpi-footer-note">
            <span>Carga con pase de salida aduanero</span>
          </div>
        </div>

        <div class="kpi-card warning-card">
          <div class="kpi-card-header">
            <span class="kpi-num">03</span>
            <span class="kpi-label">Días Libres por Vencer</span>
            <i class="pi pi-exclamation-triangle kpi-icon warning"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono text-warning">{{ proximosAVencerCount }}</span>
            <span class="kpi-badge warning">Riesgo Sobrestadía</span>
          </div>
          <div class="kpi-footer-note">
            <span>Menos de 3 días sin penalidad</span>
          </div>
        </div>

        <div class="kpi-card accent-card" @click="filterEstado = ''">
          <div class="kpi-card-header">
            <span class="kpi-num">04</span>
            <span class="kpi-label">Total Registros VGM</span>
            <i class="pi pi-receipt kpi-icon accent"></i>
          </div>
          <div class="kpi-main-stat">
            <span class="kpi-value font-mono">{{ inventario.length }}</span>
            <span class="kpi-badge accent">Histórico</span>
          </div>
          <div class="kpi-footer-note">
            <span>Tickets de pesaje y balanza certificados</span>
          </div>
        </div>
      </div>

      <!-- 3. Contenedor de Listado de Almacén -->
      <div class="content-table-card">
        <div class="card-header-bar">
          <div>
            <span class="badge-kicker">INVENTARIO EN CUSTODIA</span>
            <h3 class="section-title">Contenedores y Carga en Depósitos Temporales</h3>
          </div>

          <div class="header-actions">
            <!-- Filtro por Estado -->
            <select v-model="filterEstado" class="filter-select">
              <option value="">Todos los Estados</option>
              <option value="EN_CUSTODIA">En Custodia</option>
              <option value="SALIDA_AUTORIZADA">Salida Autorizada</option>
              <option value="RETIRADO">Retirado del Depósito</option>
              <option value="ANULADO">Anulados</option>
            </select>

            <!-- Búsqueda Rápida -->
            <div class="search-input-wrap">
              <i class="pi pi-search search-icon"></i>
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="Buscar por ticket, expediente o bahía..."
              />
            </div>
          </div>
        </div>

        <!-- Tabla de Almacén con PrimeVue DataTable -->
        <div class="table-container">
          <DataTable
            :value="filteredInventario"
            :loading="loading"
            responsiveLayout="scroll"
            class="custom-datatable"
            dataKey="idControlAlmacen"
          >
            <template #empty>
              <div class="empty-state">
                <i class="pi pi-box empty-icon"></i>
                <p class="empty-title">No hay carga registrada en depósito</p>
                <p class="empty-desc">Registre el ingreso de contenedores y tickets de balanza VGM para su control aduanero.</p>
              </div>
            </template>

            <template #loading>
              <div class="loading-state">
                <i class="pi pi-spin pi-spinner"></i>
                <span>Cargando inventario de depósitos temporales...</span>
              </div>
            </template>

            <Column header="Ticket Balanza">
              <template #body="{ data: item }">
                <span class="ticket-badge font-mono">{{ item.ticketBalanzaVgm }}</span>
              </template>
            </Column>

            <Column header="Expediente Aduanero">
              <template #body="{ data: item }">
                <div class="client-cell">
                  <span class="font-mono text-xs font-bold">{{ item.codigoExpediente }}</span>
                  <span class="client-sub">{{ item.nombreSede || 'Sede Central' }}</span>
                </div>
              </template>
            </Column>

            <Column header="Pesaje VGM (kg)">
              <template #body="{ data: item }">
                <div class="numeric-cell">
                  <span class="font-mono font-bold">{{ formatNumber(item.pesoBrutoVgm) }}</span>
                  <span class="client-sub">Tara: {{ formatNumber(item.taraContenedor) }} kg</span>
                </div>
              </template>
            </Column>

            <Column header="Bultos & Volumen">
              <template #body="{ data: item }">
                <div class="numeric-cell">
                  <span class="font-mono">{{ item.cantidadBultosRecibidos }} bultos</span>
                  <span class="client-sub">{{ formatNumber(item.volumenM3) }} m³</span>
                </div>
              </template>
            </Column>

            <Column header="Ubicación Bahía">
              <template #body="{ data: item }">
                <span class="bahia-tag font-mono">{{ item.ubicacionBahia || 'S/A' }}</span>
              </template>
            </Column>

            <Column header="Días Libres">
              <template #body="{ data: item }">
                <span :class="['days-badge font-mono', getDaysBadgeClass(item.diasLibresAlmacenaje)]">
                  {{ item.diasLibresAlmacenaje }} días
                </span>
              </template>
            </Column>

            <Column header="Estado Depósito">
              <template #body="{ data: item }">
                <Tag
                  :value="formatEstado(item.estadoAlmacen)"
                  :severity="getAlmacenSeverity(item.estadoAlmacen)"
                  rounded
                />
              </template>
            </Column>

            <Column header="Acciones" headerClass="text-right" bodyClass="text-right">
              <template #body="{ data: item }">
                <div class="actions-wrapper">
                  <button
                    v-if="item.estadoAlmacen === 'EN_CUSTODIA'"
                    type="button"
                    class="action-btn-outline approve"
                    title="Autorizar Retiro / Salida Aduanera"
                    @click="handleAutorizarSalida(item)"
                  >
                    <i class="pi pi-check"></i>
                    <span>Autorizar Salida</span>
                  </button>

                  <button
                    v-if="item.estadoAlmacen === 'SALIDA_AUTORIZADA'"
                    type="button"
                    class="action-btn-outline"
                    title="Registrar Retiro Físico Efectivo"
                    @click="handleRegistrarRetiro(item)"
                  >
                    <i class="pi pi-sign-out"></i>
                    <span>Confirmar Retiro</span>
                  </button>

                  <button
                    v-if="item.estadoAlmacen !== 'ANULADO' && item.estadoAlmacen !== 'RETIRADO'"
                    type="button"
                    class="action-btn-outline reject"
                    title="Anular Registro de Almacén"
                    @click="handleDeleteAlmacen(item)"
                  >
                    <i class="pi pi-trash"></i>
                  </button>
                </div>
              </template>
            </Column>
          </DataTable>
        </div>
      </div>
    </div>

    <!-- ============================================================== -->
    <!-- MODALES OPERATIVOS AUXILIARES (BPMN IMPORTACIÓN)               -->
    <!-- ============================================================== -->
    <!-- Modal: Actualizar Tránsito y Embarque en Origen -->
    <ActualizarTransitoModal
      :visible="showTransitoModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showTransitoModal = false"
      @saved="onTransitoSaved"
    />

    <!-- Modal: Registrar Diligencia / Intervención de Campo SUNAT -->
    <DiligenciaCampoModal
      :visible="showDiligenciaModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showDiligenciaModal = false"
      @saved="onDiligenciaSaved"
    />

    <!-- Modal: Solicitar Despacho Terrestre a Transporte y Flota -->
    <SolicitudTransporteModal
      :visible="showSolicitudTransporteModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showSolicitudTransporteModal = false"
      @saved="onSolicitudTransporteSaved"
    />

    <!-- Modal: Acta de Retiro Directo en Terminal -->
    <RetiroDirectoModal
      :visible="showRetiroDirectoModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRetiroDirectoModal = false"
      @saved="onRetiroDirectoSaved"
    />

    <!-- Modal: Detalle Integral del Expediente -->
    <DetalleExpedienteModal
      :visible="showDetalleModal"
      :expediente="selectedExpedienteParaDetalle"
      @close="showDetalleModal = false"
      @open-docs="openDocumentosExpediente"
    />

    <!-- Modal: Gestión de Documentos Adjuntos -->
    <DocumentosManagerModal
      :visible="showDocumentosModal"
      entidad-tipo="EXPEDIENTE"
      :id-entidad="selectedDocEntidadId"
      :entidad-id="selectedDocEntidadId"
      :codigo-entidad="selectedDocCodigoEntidad"
      :titulo-entidad="selectedDocTituloEntidad"
      @close="showDocumentosModal = false"
    />

    <!-- ============================================================== -->
    <!-- MODALES OPERATIVOS ESPECÍFICOS DE EXPORTACIÓN (BPMN EXPORTACIÓN)-->
    <!-- ============================================================== -->
    <!-- Modal: Solicitar Recojo en Planta del Exportador a Flota -->
    <SolicitudRecojoExportacionModal
      :visible="showRecojoExportacionModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRecojoExportacionModal = false"
      @saved="onRecojoExportacionSaved"
    />

    <!-- Modal: Confirmar Ingreso a Depósito Temporal / Zona Primaria (VGM) -->
    <ConfirmarIngresoOrigenModal
      :visible="showIngresoOrigenModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showIngresoOrigenModal = false"
      @saved="onIngresoOrigenSaved"
    />

    <!-- Modal: Confirmar Embarque Internacional a Bordo de la Nave -->
    <ConfirmarEmbarqueExportacionModal
      :visible="showEmbarqueModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showEmbarqueModal = false"
      @saved="onEmbarqueSaved"
    />

    <!-- Modal: Confirmar Recepción Conforme en Destino Exterior -->
    <RecepcionDestinoExteriorModal
      :visible="showRecepcionExteriorModal"
      :expediente="selectedExpedienteParaAccion"
      @close="showRecepcionExteriorModal = false"
      @saved="onRecepcionExteriorSaved"
    />

    <!-- Modal de Ingreso a Almacén / Balanza VGM -->
    <IngresoAlmacenModal
      :visible="showIngresoModal"
      @close="showIngresoModal = false"
      @saved="onIngresoSaved"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import { almacenService } from '../services/almacenService'
import { expedienteService } from '@/features/aduanas/services/expedienteService'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { useNavigationStore } from '@/core/stores/navigationStore'
import { useNotification } from '@/shared/composables/useNotification'
import { useConfirmation } from '@/shared/composables/useConfirmation'
import IngresoAlmacenModal from '../components/IngresoAlmacenModal.vue'
import ActualizarTransitoModal from '@/features/aduanas/components/ActualizarTransitoModal.vue'
import DiligenciaCampoModal from '@/features/aduanas/components/DiligenciaCampoModal.vue'
import SolicitudTransporteModal from '@/features/aduanas/components/SolicitudTransporteModal.vue'
import RetiroDirectoModal from '@/features/aduanas/components/RetiroDirectoModal.vue'
import DetalleExpedienteModal from '@/features/aduanas/components/DetalleExpedienteModal.vue'
import DocumentosManagerModal from '@/shared/components/DocumentosManagerModal.vue'
import SolicitudRecojoExportacionModal from '@/features/aduanas/components/SolicitudRecojoExportacionModal.vue'
import ConfirmarIngresoOrigenModal from '@/features/aduanas/components/ConfirmarIngresoOrigenModal.vue'
import ConfirmarEmbarqueExportacionModal from '@/features/aduanas/components/ConfirmarEmbarqueExportacionModal.vue'
import RecepcionDestinoExteriorModal from '@/features/aduanas/components/RecepcionDestinoExteriorModal.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const navigationStore = useNavigationStore()
const { showSuccess, showError, showInfo } = useNotification()
const { confirmAction, confirmDelete } = useConfirmation()

const loading = ref(false)
const currentTab = ref('despachos') // 'despachos' | 'almacen'

// Datos de Almacén
const inventario = ref([])
const filterEstado = ref('')
const searchQuery = ref('')
const showIngresoModal = ref(false)

// Datos de Expedientes (Importación y Exportación)
const expedientes = ref([])
const filterRegimenExp = ref('')
const filterCanalExp = ref('')
const filterEstadoExp = ref('')
const searchExpQuery = ref('')

// Modales Operativos
const selectedExpedienteParaAccion = ref(null)
const selectedExpedienteParaDetalle = ref(null)
const showTransitoModal = ref(false)
const showDiligenciaModal = ref(false)
const showSolicitudTransporteModal = ref(false)
const showRetiroDirectoModal = ref(false)
const showDetalleModal = ref(false)

// Modales Operativos de Exportación
const showRecojoExportacionModal = ref(false)
const showIngresoOrigenModal = ref(false)
const showEmbarqueModal = ref(false)
const showRecepcionExteriorModal = ref(false)

// Modales de Documentos
const showDocumentosModal = ref(false)
const selectedDocEntidadId = ref(null)
const selectedDocCodigoEntidad = ref('')
const selectedDocTituloEntidad = ref('')

// Expedientes asignados al Agente autenticado
const misExpedientes = computed(() => {
  const currentUserId = authStore.user?.idUsuario
  // Si es admin o supervisor aduanero, ve todos; si es agente, filtra por su usuario
  if (authStore.user?.rol === 'ADMINISTRADOR_TI' || authStore.user?.rol === 'SUPERVISOR_ADUANERO' || authStore.user?.rol === 'GERENTE_GENERAL') {
    return expedientes.value
  }
  if (!currentUserId) return expedientes.value
  return expedientes.value.filter((e) => e.idAgenteAduanero === currentUserId)
})

// Métricas de Expedientes
const enTransitoCount = computed(() => {
  return misExpedientes.value.filter((e) => e.estadoExpediente === 'ASIGNADO' || e.estadoExpediente === 'EN_TRAMITE').length
})

const enInspeccionCount = computed(() => {
  return misExpedientes.value.filter((e) => e.estadoExpediente === 'INSPECCION' || e.canalSunat === 'ROJO' || e.canalSunat === 'NARANJA').length
})

const conLevanteCount = computed(() => {
  return misExpedientes.value.filter((e) => e.estadoExpediente === 'LEVANTE_AUTORIZADO' || !!e.fechaLevanteAutorizacion).length
})

// Filtrado reactivo de Expedientes
const filteredExpedientes = computed(() => {
  return misExpedientes.value.filter((e) => {
    if (filterRegimenExp.value) {
      const reg = (e.regimenAduanero || '').toUpperCase()
      if (filterRegimenExp.value === 'EXPORTACION' && !reg.includes('EXPORT')) return false
      if (filterRegimenExp.value === 'IMPORTACION' && !reg.includes('IMPORT')) return false
    }

    if (filterCanalExp.value && e.canalSunat !== filterCanalExp.value) {
      return false
    }

    if (filterEstadoExp.value && e.estadoExpediente !== filterEstadoExp.value) {
      return false
    }

    if (searchExpQuery.value) {
      const q = searchExpQuery.value.toLowerCase().trim()
      const match =
        e.codigoExpediente?.toLowerCase().includes(q) ||
        e.codigoCotizacion?.toLowerCase().includes(q) ||
        e.clienteRazonSocial?.toLowerCase().includes(q) ||
        e.clienteNumeroDocumento?.toLowerCase().includes(q) ||
        e.numeroDam?.toLowerCase().includes(q) ||
        e.numeroContenedor?.toLowerCase().includes(q) ||
        e.naveViaje?.toLowerCase().includes(q)
      if (!match) return false
    }

    return true
  })
})

// Métricas Almacén
const enCustodiaCount = computed(() => {
  return inventario.value.filter((i) => i.estadoAlmacen === 'EN_CUSTODIA').length
})

const salidasCount = computed(() => {
  return inventario.value.filter((i) => i.estadoAlmacen === 'SALIDA_AUTORIZADA' || i.estadoAlmacen === 'RETIRADO').length
})

const proximosAVencerCount = computed(() => {
  return inventario.value.filter((i) => i.estadoAlmacen === 'EN_CUSTODIA' && i.diasLibresAlmacenaje <= 3).length
})

// Filtrado Almacén
const filteredInventario = computed(() => {
  return inventario.value.filter((item) => {
    if (filterEstado.value && item.estadoAlmacen !== filterEstado.value) {
      return false
    }

    if (searchQuery.value) {
      const q = searchQuery.value.toLowerCase().trim()
      const match =
        item.ticketBalanzaVgm?.toLowerCase().includes(q) ||
        item.codigoExpediente?.toLowerCase().includes(q) ||
        item.ubicacionBahia?.toLowerCase().includes(q) ||
        item.nombreSede?.toLowerCase().includes(q)
      if (!match) return false
    }

    return true
  })
})

// Handlers de Modales de Expediente
const openTransitoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showTransitoModal.value = true
}

const onTransitoSaved = (actualizado) => {
  showTransitoModal.value = false
  showSuccess(`Datos de embarque y tránsito actualizados para ${actualizado.codigoExpediente}.`)
  loadData()
}

const openDiligenciaModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showDiligenciaModal.value = true
}

const onDiligenciaSaved = (actualizado) => {
  showDiligenciaModal.value = false
  showSuccess(`Diligencia aduanera registrada con éxito para ${actualizado.codigoExpediente}.`)
  loadData()
}

const openSolicitudTransporteModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showSolicitudTransporteModal.value = true
}

const onSolicitudTransporteSaved = () => {
  showSolicitudTransporteModal.value = false
  showSuccess('Solicitud de despacho terrestre enviada a Flota y Transporte exitosamente.')
  loadData()
}

const openRetiroDirectoModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRetiroDirectoModal.value = true
}

const onRetiroDirectoSaved = () => {
  showRetiroDirectoModal.value = false
  showSuccess('Acta de retiro directo de terminal registrada exitosamente.')
  loadData()
}

// Helpers y Handlers de Exportación
const isExport = (exp) => {
  return (exp?.regimenAduanero || '').toUpperCase().includes('EXPORT')
}

const isEmbarcado = (exp) => {
  const obs = exp?.observaciones || ''
  return Boolean(exp?.naveViaje || exp?.fechaZarpeEtd || obs.includes('EMBARQUE CONFIRMADO'))
}

const isRecepcionado = (exp) => {
  const obs = exp?.observaciones || ''
  return obs.includes('RECEPCIÓN DESTINO EXTERIOR')
}

const openRecojoExportacionModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRecojoExportacionModal.value = true
}

const onRecojoExportacionSaved = (act) => {
  showRecojoExportacionModal.value = false
  showSuccess(`Recojo en planta programado exitosamente para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openIngresoOrigenModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showIngresoOrigenModal.value = true
}

const onIngresoOrigenSaved = (act) => {
  showIngresoOrigenModal.value = false
  showSuccess(`Ingreso a terminal / zona primaria confirmado para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openEmbarqueModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showEmbarqueModal.value = true
}

const onEmbarqueSaved = (act) => {
  showEmbarqueModal.value = false
  showSuccess(`Embarque internacional confirmado para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openRecepcionExteriorModal = (exp) => {
  selectedExpedienteParaAccion.value = exp
  showRecepcionExteriorModal.value = true
}

const onRecepcionExteriorSaved = (act) => {
  showRecepcionExteriorModal.value = false
  showSuccess(`Recepción conforme en destino exterior confirmada para ${act?.codigoExpediente || 'el expediente'}.`)
  loadData()
}

const openDetalleExp = (exp) => {
  selectedExpedienteParaDetalle.value = exp
  showDetalleModal.value = true
}

const openDocumentosExpediente = (exp) => {
  if (!exp) return
  selectedDocEntidadId.value = exp.idExpediente
  selectedDocCodigoEntidad.value = exp.codigoExpediente || `EXP-${exp.idExpediente}`
  selectedDocTituloEntidad.value = `${exp.codigoExpediente} — ${exp.clienteRazonSocial || 'Cliente'}`
  showDocumentosModal.value = true
}

// Carga General
const loadData = async () => {
  loading.value = true
  try {
    const [invData, expData] = await Promise.all([
      almacenService.getAlmacen().catch(() => []),
      expedienteService.getExpedientes().catch(() => [])
    ])
    inventario.value = invData || []
    expedientes.value = expData || []
  } catch (err) {
    showError(err.response?.data?.message || 'Error al conectar con los servicios aduaneros.')
  } finally {
    loading.value = false
  }
}

const onIngresoSaved = (creado) => {
  showIngresoModal.value = false
  showSuccess(`Ingreso VGM registrado: ${creado.ticketBalanzaVgm}`)
  loadData()
}

const handleAutorizarSalida = async (item) => {
  confirmAction({
    title: 'Autorizar Salida de Almacén',
    message: `¿Confirma que la carga ${item.ticketBalanzaVgm} cuenta con Levante Aduanero y pase de salida válido?`,
    icon: 'pi pi-check-circle',
    acceptClass: 'p-button-success',
    accept: async () => {
      try {
        await almacenService.autorizarSalida(item.idControlAlmacen)
        showSuccess(`Salida autorizada para el ticket ${item.ticketBalanzaVgm}`)
        loadData()
      } catch (err) {
        showError(err.response?.data?.message || 'Error al autorizar salida de almacén.')
      }
    }
  })
}

const handleRegistrarRetiro = async (item) => {
  confirmAction({
    title: 'Confirmar Retiro Físico',
    message: `¿Confirma la entrega física de la carga ${item.ticketBalanzaVgm} a la unidad de transporte?`,
    icon: 'pi pi-sign-out',
    acceptClass: 'p-button-primary',
    accept: async () => {
      try {
        await almacenService.registrarRetiro(item.idControlAlmacen)
        showSuccess(`Carga ${item.ticketBalanzaVgm} retirada formalmente del depósito temporal`)
        loadData()
      } catch (err) {
        showError(err.response?.data?.message || 'Error al registrar retiro de almacén.')
      }
    }
  })
}

const handleDeleteAlmacen = (item) => {
  confirmDelete({
    title: 'Anular Registro de Almacén',
    message: `¿Está seguro de anular el control de almacén para el ticket ${item.ticketBalanzaVgm}?`,
    accept: async () => {
      try {
        await almacenService.deleteAlmacen(item.idControlAlmacen)
        showSuccess('Registro de almacén anulado exitosamente.')
        loadData()
      } catch (err) {
        showError(err.response?.data?.message || 'Error al anular el registro de almacén.')
      }
    }
  })
}

// Helpers
const formatNumber = (val) => {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const formatDateShort = (dateStr) => {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const formatEstado = (est) => {
  const map = {
    EN_CUSTODIA: 'En Custodia',
    SALIDA_AUTORIZADA: 'Salida Autorizada',
    RETIRADO: 'Retirado',
    ANULADO: 'Anulado'
  }
  return map[est] || est
}

const getAlmacenSeverity = (est) => {
  const map = {
    EN_CUSTODIA: 'info',
    SALIDA_AUTORIZADA: 'success',
    RETIRADO: 'secondary',
    ANULADO: 'danger'
  }
  return map[est] || 'info'
}

const getCanalSeverity = (canal) => {
  const map = {
    VERDE: 'success',
    NARANJA: 'warning',
    ROJO: 'danger',
    PENDIENTE: 'secondary'
  }
  return map[canal] || 'secondary'
}

const getExpedienteSeverity = (est) => {
  const map = {
    ASIGNADO: 'info',
    EN_TRAMITE: 'info',
    INSPECCION: 'warning',
    LEVANTE_AUTORIZADO: 'success',
    CERRADO: 'secondary',
    ANULADO: 'danger'
  }
  return map[est] || 'info'
}

const getDaysBadgeClass = (days) => {
  if (days <= 2) return 'days-danger'
  if (days <= 5) return 'days-warning'
  return 'days-ok'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.logistica-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding: var(--space-2, 0.5rem);
}

.fade-in {
  animation: fadeIn 0.25s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 1. Action Toolbar */
.agente-action-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4, 1rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.toolbar-left-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.badge-kicker {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-accent-kicker, #8c6a46);
}

.toolbar-title {
  font-size: var(--font-size-xl, 1.25rem);
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
  margin: 0;
  letter-spacing: -0.01em;
}

.toolbar-actions-group {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
}

.tabs-nav-bar {
  display: flex;
  align-items: center;
  background-color: #f2ebe1;
  padding: 0.25rem;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid var(--color-border, #e4dacb);
  gap: 0.25rem;
}

.tab-pill-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.85rem;
  background: transparent;
  border: none;
  border-radius: var(--radius-sm, 0.375rem);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted, #6f6356);
  cursor: pointer;
  transition: all 0.15s ease-in-out;
}

.tab-pill-btn:hover {
  color: var(--color-text-main, #2b2520);
}

.tab-pill-btn.active {
  background-color: #ffffff;
  color: #2b1d16;
  font-weight: 700;
  box-shadow: 0 1px 3px rgba(45, 35, 25, 0.08);
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.55rem 1.1rem;
  background-color: var(--color-primary, #322b25);
  color: #ffffff;
  border: 1px solid var(--color-primary-dark, #241e1a);
  border-radius: var(--radius-md, 0.5rem);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
}

.btn-espresso:hover:not(:disabled) {
  background-color: var(--color-primary-hover, #484039);
  border-color: var(--color-primary-hover, #484039);
  transform: translateY(-1px);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.55rem 0.9rem;
  background-color: var(--color-bg-surface, #ffffff);
  color: var(--color-text-main, #2b2520);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-md, 0.5rem);
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease-in-out;
}

.btn-outline-warm:hover:not(:disabled) {
  background-color: var(--color-bg-subtle, #f2ebe1);
  border-color: var(--color-border-strong, #d5c8b6);
  color: var(--color-text-main, #2b2520);
}

/* 2. Grid de Métricas KPI Técnicas */
.metrics-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4, 1rem);
  margin-bottom: var(--space-5, 1.25rem);
}

.kpi-card {
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  cursor: pointer;
  transition: all 0.2s ease-in-out;
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.kpi-card:hover {
  border-color: var(--color-border-strong, #d5c8b6);
  transform: translateY(-2px);
  box-shadow: var(--shadow-card, 0 4px 16px -2px rgba(50, 40, 30, 0.04));
}

.kpi-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-num {
  font-family: var(--font-mono, 'JetBrains Mono', monospace);
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  color: var(--color-text-subtle, #998c7e);
}

.kpi-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-muted, #6f6356);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-right: auto;
  margin-left: 0.5rem;
}

.kpi-icon {
  font-size: 1.1rem;
  color: var(--color-text-subtle, #998c7e);
}
.kpi-icon.pending { color: #21598f; }
.kpi-icon.success { color: #1e6638; }
.kpi-icon.warning { color: #8c5b16; }
.kpi-icon.accent { color: #9a7f60; }

.kpi-main-stat {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2, 0.5rem);
  margin-top: 0.15rem;
}

.kpi-value {
  font-size: var(--font-size-3xl, 1.875rem);
  font-weight: 800;
  line-height: 1;
  color: var(--color-text-main, #2b2520);
}

.kpi-badge {
  font-size: var(--font-size-2xs, 0.6875rem);
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.kpi-badge.success {
  background-color: #eaf6ed;
  color: #1e6638;
  border: 1px solid #bce5c6;
}

.kpi-badge.warning {
  background-color: #fdf5e6;
  color: #8c5b16;
  border: 1px solid #f8dab3;
}

.kpi-badge.pending {
  background-color: #eef4fb;
  color: #21598f;
  border: 1px solid #cadff4;
}

.kpi-badge.accent {
  background-color: #f2ebe1;
  color: #9a7f60;
  border: 1px solid #e4dacb;
}

.kpi-footer-note {
  font-size: var(--font-size-2xs, 0.6875rem);
  color: var(--color-text-subtle, #998c7e);
  border-top: 1px dashed var(--color-border-subtle, #ede5d8);
  padding-top: 0.4rem;
  margin-top: 0.2rem;
}

/* 3. Contenedores de Tabla */
.content-table-card {
  background-color: var(--color-bg-surface, #ffffff);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-5, 1.25rem);
  box-shadow: var(--shadow-subtle, 0 1px 3px rgba(45, 35, 25, 0.04));
}

.card-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4, 1rem);
  margin-bottom: var(--space-4, 1rem);
  padding-bottom: var(--space-3, 0.75rem);
  border-bottom: 1px solid var(--color-border-subtle, #ede5d8);
}

.section-title {
  font-size: var(--font-size-lg, 1.125rem);
  font-weight: 700;
  color: var(--color-text-main, #2b2520);
  margin: 0;
  letter-spacing: -0.01em;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
}

.filter-select {
  padding: 0.5rem 0.85rem;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main, #2b2520);
  background-color: var(--color-bg-subtle, #fcf9f5);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-md, 0.5rem);
  cursor: pointer;
  outline: none;
}

.filter-select:focus {
  border-color: var(--color-accent-kicker, #8c6a46);
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  color: var(--color-text-subtle, #998c7e);
  font-size: 0.85rem;
  pointer-events: none;
}

.search-input {
  padding: 0.5rem 0.85rem 0.5rem 2.25rem;
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main, #2b2520);
  background-color: var(--color-bg-subtle, #fcf9f5);
  border: 1px solid var(--color-border, #e4dacb);
  border-radius: var(--radius-md, 0.5rem);
  outline: none;
  min-width: 280px;
}

.search-input:focus {
  border-color: var(--color-accent-kicker, #8c6a46);
  background-color: #ffffff;
}

.table-container {
  overflow-x: auto;
}

.exp-code-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.65rem;
  background-color: #f7f2ea;
  border: 1px solid #e2d7cd;
  border-radius: 4px;
  color: #2b1d16;
  cursor: pointer;
  transition: all 0.15s;
}

.exp-code-badge:hover {
  background-color: #efe5d8;
  border-color: #8c6a46;
}

.badge-regimen {
  display: inline-block;
  font-size: 0.65rem;
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  border-radius: 4px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.badge-export {
  background-color: #ede9fe;
  color: #6d28d9;
  border: 1px solid #ddd6fe;
}
.badge-import {
  background-color: #e0f2fe;
  color: #0369a1;
  border: 1px solid #bae6fd;
}

.client-cell {
  display: flex;
  flex-direction: column;
}

.client-name {
  font-size: var(--font-size-sm, 0.8125rem);
  color: var(--color-text-main, #2b2520);
}

.client-sub {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted, #6f6356);
}

.numeric-cell {
  display: flex;
  flex-direction: column;
}

.ticket-badge {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: #2b1d16;
  background-color: #f4ede4;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  border: 1px solid #e0d4c5;
}

.bahia-tag {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: #554433;
  background-color: #fcf9f5;
  border: 1px solid #e5dbcf;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.days-badge {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}
.days-badge.days-ok { background-color: #eaf6ed; color: #1e6638; border: 1px solid #bce5c6; }
.days-badge.days-warning { background-color: #fdf5e6; color: #8c5b16; border: 1px solid #f8dab3; }
.days-badge.days-danger { background-color: #fdf0ed; color: #a12b18; border: 1px solid #f9ccc4; }

.actions-wrapper {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.4rem;
}

.action-btn-outline {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.65rem;
  font-size: 0.75rem;
  font-weight: 600;
  background: transparent;
  border: 1px solid #d4c5b9;
  border-radius: 4px;
  color: #4a3d33;
  cursor: pointer;
  transition: all 0.15s;
}

.action-btn-outline:hover {
  background: #f7f2eb;
  border-color: #b5a494;
}

.action-btn-outline.approve {
  border-color: #bce5c6;
  color: #1e6638;
}
.action-btn-outline.approve:hover {
  background: #eaf6ed;
}

.action-btn-outline.observe {
  border-color: #f8dab3;
  color: #8c5b16;
}
.action-btn-outline.observe:hover {
  background: #fdf5e6;
}

.action-btn-outline.reject {
  border-color: #f9ccc4;
  color: #a12b18;
}
.action-btn-outline.reject:hover {
  background: #fdf0ed;
}

.action-btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.65rem;
  font-size: 0.75rem;
  font-weight: 600;
  background: #2b1d16;
  border: 1px solid #1a100a;
  border-radius: 4px;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.15s;
}

.action-btn-primary:hover {
  background: #422d22;
}

.empty-state,
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  text-align: center;
}

.empty-icon {
  font-size: 2.5rem;
  color: #998c7e;
  margin-bottom: 0.75rem;
}

.empty-title {
  font-size: 1rem;
  font-weight: 700;
  color: #2b2520;
  margin: 0 0 0.25rem 0;
}

.empty-desc {
  font-size: 0.8125rem;
  color: #6f6356;
  margin: 0;
}

.text-pending { color: #21598f; }
.text-success { color: #1e6638; }
.text-warning { color: #8c5b16; }
.text-info { color: #1e3a5f; }
.text-muted-dash { color: #8c7e72; }
.text-2xs { font-size: 0.6875rem; }
</style>
