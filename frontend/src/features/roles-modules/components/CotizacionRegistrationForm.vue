<template>
  <div class="cotizacion-registration-form-view">
    <!-- 1. Header Bar Modular de Navegación y Acciones -->
    <CotizacionFormHeader
      :is-edit-mode="isEditMode"
      :codigo-cotizacion="form.codigoCotizacion"
      :submitting="submitting"
      :is-form-ready="isFormReady"
      @cancel="$emit('cancel')"
      @submit="handleSubmit"
    />

    <!-- Banner de Error si la validación falla -->
    <div v-if="errorMessage" class="error-banner">
      <i class="pi pi-exclamation-triangle"></i>
      <div class="error-content">
        <strong>Error:</strong>
        <span>{{ errorMessage }}</span>
      </div>
      <button type="button" class="error-close-btn" @click="errorMessage = ''">
        <i class="pi pi-times"></i>
      </button>
    </div>

    <!-- Banner de Observación de Supervisión Aduanera si está en REVISION u OBSERVADA -->
    <div v-if="props.cotizacionToEdit?.estado === 'REVISION' || props.cotizacionToEdit?.estado === 'OBSERVADA' || props.cotizacionToEdit?.estado === 'EN_REVISION'" class="revision-alert-banner">
      <i class="pi pi-exclamation-circle revision-icon"></i>
      <div class="revision-body">
        <span class="revision-kicker">OBSERVADA POR SUPERVISIÓN ADUANERA</span>
        <strong class="revision-title">Esta cotización fue devuelta para corrección técnica:</strong>
        <p class="revision-note">{{ props.cotizacionToEdit?.motivoObservacion || 'Se requieren correcciones antes de la aprobación definitiva.' }}</p>
        <span class="revision-hint">Realice los ajustes requeridos y presione "Guardar Cambios". La cotización retornará a la bandeja de Supervisión como Enviada.</span>
      </div>
    </div>

    <!-- Barra de Pasos / Stepper Interactivo Modular -->
    <CotizacionStepperNav
      v-model:current-step="currentStep"
      :form="form"
      :docs-count="isEditMode ? existingDocsCount : queuedDocs.length"
    />

    <!-- 2. Grid Principal -->
    <div class="form-content-grid">
      <!-- Columna Principal: Pasos del Formulario -->
      <div class="form-blocks-column">

        <!-- ========================================================================= -->
        <!-- PASO 1: CLIENTE SOLICITANTE -->
        <!-- ========================================================================= -->
        <section v-show="currentStep === 1" class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Identificación y Asignación del Cliente o Prospecto</h3>
              <p class="block-sub">Busque en el directorio de clientes registrados o cree un nuevo cliente inmediatamente para esta propuesta.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- Barra de Búsqueda y Creación Rápida de Cliente -->
            <div class="client-search-actions-bar">
              <div class="search-input-box">
                <i class="pi pi-search search-icon"></i>
                <input
                  v-model.trim="clientSearchQuery"
                  type="text"
                  placeholder="Buscar cliente por RUC, DNI o razón social..."
                  class="client-search-input"
                />
                <button
                  v-if="clientSearchQuery"
                  type="button"
                  class="clear-query-btn"
                  @click="clientSearchQuery = ''"
                >
                  <i class="pi pi-times"></i>
                </button>
              </div>

              <button
                type="button"
                :class="['btn-toggle-quick-client', { active: showQuickClientForm }]"
                @click="showQuickClientForm = !showQuickClientForm"
              >
                <i :class="showQuickClientForm ? 'pi pi-times' : 'pi pi-user-plus'"></i>
                <span>{{ showQuickClientForm ? 'Cerrar Registro' : '+ Crear Nuevo Cliente' }}</span>
              </button>
            </div>

            <!-- Subformulario de Creación Rápida de Cliente (Inline) -->
            <div v-if="showQuickClientForm" class="quick-client-panel">
              <div class="quick-panel-header">
                <div class="panel-header-title">
                  <i class="pi pi-user-plus"></i>
                  <span>Alta Rápida de Cliente Comercial</span>
                </div>
                <span class="panel-header-badge">REGISTRO INMEDIATO</span>
              </div>

              <div class="quick-panel-body">
                <div class="quick-form-grid">
                  <div class="field-group">
                    <label class="field-label">Tipo Documento *</label>
                    <select v-model="quickClient.tipoDocumento" class="form-select">
                      <option value="RUC">RUC</option>
                      <option value="DNI">DNI</option>
                      <option value="CE">Carnet Extranjería</option>
                      <option value="PASAPORTE">Pasaporte</option>
                    </select>
                  </div>

                  <div class="field-group">
                    <label class="field-label">Número de Documento *</label>
                    <input
                      v-model.trim="quickClient.numeroDocumento"
                      type="text"
                      placeholder="Ej. 20601234567"
                      class="form-input"
                      maxlength="15"
                    />
                  </div>

                  <div class="field-group full-width">
                    <label class="field-label">Razón Social o Nombre Completo *</label>
                    <input
                      v-model.trim="quickClient.razonSocial"
                      type="text"
                      placeholder="Ej. COMERCIALIZADORA DEL PACIFICO S.A.C."
                      class="form-input"
                      maxlength="150"
                    />
                  </div>

                  <div class="field-group">
                    <label class="field-label">Correo de Contacto *</label>
                    <input
                      v-model.trim="quickClient.correoContacto"
                      type="email"
                      placeholder="Ej. importaciones@empresa.com"
                      class="form-input"
                      maxlength="100"
                    />
                  </div>

                  <div class="field-group">
                    <label class="field-label">Teléfono Móvil</label>
                    <input
                      v-model.trim="quickClient.telefonoContacto"
                      type="text"
                      placeholder="Ej. +51 999 888 777"
                      class="form-input"
                      maxlength="20"
                    />
                  </div>
                </div>

                <div v-if="quickClientError" class="quick-error-msg">
                  <i class="pi pi-exclamation-circle"></i>
                  <span>{{ quickClientError }}</span>
                </div>

                <div class="quick-panel-actions">
                  <button
                    type="button"
                    class="btn-outline-warm"
                    @click="showQuickClientForm = false"
                    :disabled="savingQuickClient"
                  >
                    Cancelar
                  </button>
                  <button
                    type="button"
                    class="btn-espresso"
                    @click="handleSaveQuickClient"
                    :disabled="savingQuickClient"
                  >
                    <i v-if="savingQuickClient" class="pi pi-spin pi-spinner"></i>
                    <i v-else class="pi pi-check"></i>
                    <span>Registrar y Seleccionar Cliente</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Si ya hay un cliente seleccionado: Tarjeta de Cliente Activo -->
            <div v-if="selectedClientObject" class="selected-client-card">
              <div class="client-card-left">
                <div class="client-avatar-circle">
                  <i class="pi pi-building"></i>
                </div>
                <div class="client-card-texts">
                  <div class="client-kicker-row">
                    <span class="doc-badge">{{ selectedClientObject.tipoDocumento }}: {{ selectedClientObject.numeroDocumento }}</span>
                    <span :class="['classification-pill', selectedClientObject.esProspecto ? 'pill-prospect' : 'pill-client']">
                      {{ selectedClientObject.esProspecto ? 'Prospecto Comercial' : 'Cliente Efectivo' }}
                    </span>
                  </div>
                  <h4 class="client-name">{{ selectedClientObject.razonSocial }}</h4>
                  <div class="client-meta-row">
                    <span v-if="selectedClientObject.contactoRepresentante" class="meta-item">
                      <i class="pi pi-user"></i> {{ selectedClientObject.contactoRepresentante }}
                    </span>
                    <span class="meta-item">
                      <i class="pi pi-envelope"></i> {{ selectedClientObject.correoContacto }}
                    </span>
                    <span v-if="selectedClientObject.telefonoContacto" class="meta-item">
                      <i class="pi pi-phone"></i> {{ selectedClientObject.telefonoContacto }}
                    </span>
                  </div>
                </div>
              </div>

              <button
                type="button"
                class="btn-change-client"
                @click="clearSelectedClient"
                title="Cambiar cliente seleccionado"
              >
                <i class="pi pi-pencil"></i>
                <span>Cambiar</span>
              </button>
            </div>

            <!-- Lista de Resultados de Búsqueda de Clientes si no hay ninguno seleccionado -->
            <div v-else class="clients-selection-list">
              <label class="field-label">Seleccione un cliente de la lista ({{ filteredAvailableClients.length }} disponibles):</label>
              
              <div v-if="filteredAvailableClients.length === 0" class="empty-clients-box">
                <i class="pi pi-users empty-icon"></i>
                <p>No se encontraron clientes coincidentes con "{{ clientSearchQuery }}".</p>
                <button
                  type="button"
                  class="btn-link-action"
                  @click="showQuickClientForm = true"
                >
                  ¿Desea registrar a este cliente ahora?
                </button>
              </div>

              <div v-else class="clients-grid">
                <div
                  v-for="c in filteredAvailableClients.slice(0, 8)"
                  :key="c.idCliente"
                  class="client-select-card"
                  @click="selectClient(c)"
                >
                  <div class="client-top-row">
                    <span class="doc-badge">{{ c.tipoDocumento }}: {{ c.numeroDocumento }}</span>
                    <span :class="['classification-pill-mini', c.esProspecto ? 'pill-prospect' : 'pill-client']">
                      {{ c.esProspecto ? 'Prospecto' : 'Cliente' }}
                    </span>
                  </div>
                  <span class="client-card-name">{{ c.razonSocial }}</span>
                  <span class="client-card-email">{{ c.correoContacto }}</span>
                </div>
              </div>
            </div>

            <!-- Botón de Siguiente Paso -->
            <div class="step-footer-actions">
              <div></div>
              <button
                type="button"
                class="btn-espresso"
                :disabled="!form.idCliente"
                @click="currentStep = 2"
              >
                <span>Continuar a Régimen y Plazos</span>
                <i class="pi pi-arrow-right"></i>
              </button>
            </div>
          </div>
        </section>

        <!-- ========================================================================= -->
        <!-- PASO 2: RÉGIMEN ADUANERO Y VALIDEZ -->
        <!-- ========================================================================= -->
        <!-- ========================================================================= -->
        <!-- PASO 2: RÉGIMEN, LOGÍSTICA & PLAZOS -->
        <!-- ========================================================================= -->
        <section v-show="currentStep === 2" class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Régimen Aduanero, Logística Portuaria & Mercancía</h3>
              <p class="block-sub">Determine el régimen oficial (Importación / Exportación), puertos de tráfico, tipo de carga, partida arancelaria y servicio de transporte.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- 1. Régimen Aduanero: Solo 2 Oficiales (Importación 10 y Exportación 40) -->
            <div class="field-group">
              <label class="field-label">Régimen Aduanero Oficial *</label>
              <div class="regimenes-grid two-cols">
                <div
                  v-for="reg in regimenOptions"
                  :key="reg.value"
                  :class="['regimen-card', { selected: form.regimenAduanero === reg.value }]"
                  @click="form.regimenAduanero = reg.value"
                >
                  <div class="regimen-header">
                    <span class="regimen-code-badge">{{ reg.codigo }}</span>
                    <div class="regimen-radio-indicator">
                      <i v-if="form.regimenAduanero === reg.value" class="pi pi-check-circle"></i>
                      <span v-else class="radio-circle"></span>
                    </div>
                  </div>
                  <h4 class="regimen-title">{{ reg.label }}</h4>
                  <p class="regimen-desc">{{ reg.desc }}</p>
                </div>
              </div>
            </div>

            <!-- 2. Ruta y Puertos (Origen & Destino desde API) -->
            <div class="field-row-grid">
              <div class="field-group">
                <label for="cot-puerto-envio" class="field-label">Puerto de Origen / Envío</label>
                <Select
                  id="cot-puerto-envio"
                  v-model="form.idPuertoEnvio"
                  :options="puertos"
                  optionLabel="nombre"
                  optionValue="idPuerto"
                  filter
                  filterPlaceholder="Buscar puerto o país..."
                  placeholder="Seleccione puerto de embarque..."
                  class="sga-custom-select"
                  :disabled="submitting || loadingCatalogos"
                >
                  <template #value="slotProps">
                    <div v-if="selectedPuertoEnvio" class="select-val-row">
                      <span class="badge-code-select">{{ selectedPuertoEnvio.codigoPuerto }}</span>
                      <span class="select-label-text">{{ selectedPuertoEnvio.nombre }} ({{ selectedPuertoEnvio.pais }})</span>
                      <span class="badge-type-pill">{{ selectedPuertoEnvio.tipo }}</span>
                    </div>
                    <span v-else class="select-placeholder-text">{{ slotProps.placeholder }}</span>
                  </template>
                  <template #option="slotProps">
                    <div class="select-opt-row">
                      <div class="opt-left">
                        <span class="badge-code-select">{{ slotProps.option.codigoPuerto }}</span>
                        <span class="opt-text">{{ slotProps.option.nombre }} ({{ slotProps.option.pais }})</span>
                      </div>
                      <span class="badge-type-pill">{{ slotProps.option.tipo }}</span>
                    </div>
                  </template>
                </Select>
                <span class="field-hint-mini">Terminal o puerto internacional de salida</span>
              </div>

              <div class="field-group">
                <label for="cot-puerto-destino" class="field-label">Puerto de Destino / Arribo</label>
                <Select
                  id="cot-puerto-destino"
                  v-model="form.idPuertoDestino"
                  :options="puertos"
                  optionLabel="nombre"
                  optionValue="idPuerto"
                  filter
                  filterPlaceholder="Buscar puerto o aduana..."
                  placeholder="Seleccione puerto de arribo..."
                  class="sga-custom-select"
                  :disabled="submitting || loadingCatalogos"
                >
                  <template #value="slotProps">
                    <div v-if="selectedPuertoDestino" class="select-val-row">
                      <span class="badge-code-select">{{ selectedPuertoDestino.codigoPuerto }}</span>
                      <span class="select-label-text">{{ selectedPuertoDestino.nombre }} ({{ selectedPuertoDestino.pais }})</span>
                      <span class="badge-type-pill">{{ selectedPuertoDestino.tipo }}</span>
                    </div>
                    <span v-else class="select-placeholder-text">{{ slotProps.placeholder }}</span>
                  </template>
                  <template #option="slotProps">
                    <div class="select-opt-row">
                      <div class="opt-left">
                        <span class="badge-code-select">{{ slotProps.option.codigoPuerto }}</span>
                        <span class="opt-text">{{ slotProps.option.nombre }} ({{ slotProps.option.pais }})</span>
                      </div>
                      <span class="badge-type-pill">{{ slotProps.option.tipo }}</span>
                    </div>
                  </template>
                </Select>
                <span class="field-hint-mini">Aduana o terminal de arribo nacional</span>
              </div>
            </div>

            <!-- 3. Tipo de Carga y Selección de Mercancía del Catálogo -->
            <div class="field-row-grid">
              <div class="field-group">
                <label for="cot-tipo-carga" class="field-label">Tipo de Carga / Unidad Logística</label>
                <Select
                  id="cot-tipo-carga"
                  v-model="form.idTipoCarga"
                  :options="tiposCarga"
                  optionLabel="nombre"
                  optionValue="idTipoCarga"
                  placeholder="Seleccione tipo de carga..."
                  class="sga-custom-select"
                  :disabled="submitting || loadingCatalogos"
                >
                  <template #value="slotProps">
                    <div v-if="selectedTipoCarga" class="select-val-row">
                      <span class="badge-code-select">{{ selectedTipoCarga.codigo }}</span>
                      <span class="select-label-text">{{ selectedTipoCarga.nombre }}</span>
                    </div>
                    <span v-else class="select-placeholder-text">{{ slotProps.placeholder }}</span>
                  </template>
                  <template #option="slotProps">
                    <div class="select-opt-row">
                      <span class="badge-code-select">{{ slotProps.option.codigo }}</span>
                      <span class="opt-text">{{ slotProps.option.nombre }}</span>
                    </div>
                  </template>
                </Select>
                <span class="field-hint-mini">Modalidad de estiba y transporte de la mercancía</span>
              </div>

              <div class="field-group">
                <label for="cot-mercancia" class="field-label">Catálogo de Mercancía (Partida Arancelaria)</label>
                <Select
                  id="cot-mercancia"
                  v-model="form.idMercancia"
                  :options="mercancias"
                  optionLabel="nombre"
                  optionValue="idMercancia"
                  filter
                  filterPlaceholder="Buscar por código, nombre o sector..."
                  placeholder="Seleccione mercancía clasificada..."
                  class="sga-custom-select"
                  :disabled="submitting || loadingCatalogos"
                  @change="handleMercanciaChange"
                >
                  <template #value="slotProps">
                    <div v-if="selectedMercancia" class="select-val-row">
                      <span class="badge-code-select">{{ selectedMercancia.codigo }}</span>
                      <span class="select-label-text">{{ selectedMercancia.nombre }}</span>
                      <Tag
                        v-if="selectedMercancia.requierePermiso"
                        :value="'Req: ' + selectedMercancia.entidadReguladora"
                        severity="warn"
                        rounded
                      />
                      <Tag
                        v-else
                        value="Libre Trámite"
                        severity="success"
                        rounded
                      />
                    </div>
                    <span v-else class="select-placeholder-text">{{ slotProps.placeholder }}</span>
                  </template>
                  <template #option="slotProps">
                    <div class="select-opt-row merc-opt-row">
                      <div class="opt-left">
                        <span class="badge-code-select">{{ slotProps.option.codigo }}</span>
                        <div class="opt-text-col">
                          <span class="opt-title-text">{{ slotProps.option.nombre }}</span>
                          <span class="opt-category-sub">{{ slotProps.option.categoria }} • Arancel Base: {{ slotProps.option.arancelBasePct }}%</span>
                        </div>
                      </div>
                      <Tag
                        v-if="slotProps.option.requierePermiso"
                        :value="'Req: ' + slotProps.option.entidadReguladora"
                        severity="warn"
                        rounded
                      />
                      <Tag
                        v-else
                        value="Libre"
                        severity="success"
                        rounded
                      />
                    </div>
                  </template>
                </Select>
                <span class="field-hint-mini">Partida del Arancel de Aduanas SUNAT</span>
              </div>
            </div>

            <!-- 4. ALERTA / VALIDACIÓN DE MERCANCÍA: LIBRE vs RESTRINGIDA (PERMISOS) -->
            <transition name="fade">
              <div v-if="selectedMercancia" class="mercancia-status-panel">
                <!-- Alerta cuando requiere permiso -->
                <div v-if="selectedMercancia.requierePermiso" class="regulatory-warning-banner">
                  <div class="warning-icon-box">
                    <i class="pi pi-exclamation-triangle"></i>
                  </div>
                  <div class="warning-content">
                    <div class="warning-kicker">MERCANCÍA RESTRINGIDA / REGULADA POR EL ESTADO</div>
                    <h4 class="warning-title">Requiere Permiso y Visto Bueno previo de: {{ selectedMercancia.entidadReguladora }}</h4>
                    <p class="warning-desc">
                      La partida <strong>{{ selectedMercancia.codigo }}</strong> ({{ selectedMercancia.nombre }}) está sujeta a control sectorial. Se debe gestionar y adjuntar la autorización oficial emitida por <strong>{{ selectedMercancia.entidadReguladora }}</strong> antes de la numeración aduanera.
                    </p>
                    <div class="warning-meta-pills">
                      <span class="meta-pill">Sector: {{ selectedMercancia.categoria }}</span>
                      <span class="meta-pill">Entidad: {{ selectedMercancia.entidadReguladora }}</span>
                      <span class="meta-pill">Arancel Ad-Valorem Base: {{ selectedMercancia.arancelBasePct }}%</span>
                    </div>
                  </div>
                </div>

                <!-- Aviso cuando es producto libre -->
                <div v-else class="free-goods-banner">
                  <i class="pi pi-check-circle"></i>
                  <div class="free-content">
                    <strong>Producto de Libre Trámite:</strong>
                    <span>La partida {{ selectedMercancia.codigo }} no está sujeta a regulaciones sectoriales ni permisos especiales previos. Tasa Ad-Valorem base: {{ selectedMercancia.arancelBasePct }}%.</span>
                  </div>
                </div>
              </div>
            </transition>

            <!-- 5. Contratación de Servicio de Transporte Local SGA -->
            <div class="field-group transport-option-box">
              <div class="label-with-badge">
                <label class="field-label">Servicio de Transporte Terrestre Local SGA *</label>
                <span class="kicker-mini-badge">FLOTA SGA</span>
              </div>
              <p class="transport-helper">¿El cliente contratará el servicio de transporte terrestre interno de SGA o utilizará transporte externo?</p>

              <div class="transport-toggle-grid">
                <div
                  :class="['transport-card-opt', { selected: form.servicioTransporte === true }]"
                  @click="form.servicioTransporte = true"
                >
                  <div class="opt-icon"><i class="pi pi-truck"></i></div>
                  <div class="opt-info">
                    <h4 class="opt-title">Sí, Transporte Interno SGA</h4>
                    <p class="opt-desc">Traslado desde terminal portuario/almacén hasta almacén del cliente con unidades monitoreadas por GPS de SGA.</p>
                  </div>
                  <i v-if="form.servicioTransporte === true" class="pi pi-check-circle check-active"></i>
                </div>

                <div
                  :class="['transport-card-opt', { selected: form.servicioTransporte === false }]"
                  @click="form.servicioTransporte = false"
                >
                  <div class="opt-icon"><i class="pi pi-building"></i></div>
                  <div class="opt-info">
                    <h4 class="opt-title">No, Transporte Externo por Cliente</h4>
                    <p class="opt-desc">El cliente gestionará el retiro y flete terrestre con sus propios transportistas o unidades de terceros.</p>
                  </div>
                  <i v-if="form.servicioTransporte === false" class="pi pi-check-circle check-active"></i>
                </div>
              </div>

              <!-- Configuración Condicional de Ruta de Transporte Interno SGA -->
              <transition name="fade-slide">
                <div v-if="form.servicioTransporte" class="transport-route-config-panel">
                  <div class="route-panel-header">
                    <div class="route-badge-icon">
                      <i class="pi pi-map"></i>
                    </div>
                    <div>
                      <h4 class="route-panel-title">Ruta Nacional & Tarifa de Flete Local SGA</h4>
                      <p class="route-panel-desc">Especifique el origen y destino departamental/provincial en Perú y la tarifa de flete local cotizada.</p>
                    </div>
                  </div>

                  <div class="route-inputs-grid">
                    <!-- ORIGEN -->
                    <div class="route-location-card">
                      <div class="loc-card-tag loc-origin">
                        <i class="pi pi-compass"></i>
                        <span>PUNTO DE PARTIDA (ORIGEN)</span>
                      </div>
                      <div class="loc-fields-row">
                        <div class="field-subgroup">
                          <label class="loc-label">Departamento *</label>
                          <select
                            v-model="form.origenDepartamento"
                            class="form-select-styled"
                            @change="onOrigenDepartamentoChange"
                          >
                            <option v-for="dep in departamentosList" :key="'orig-' + dep" :value="dep">
                              {{ dep }}
                            </option>
                          </select>
                        </div>

                        <div class="field-subgroup">
                          <label class="loc-label">Provincia *</label>
                          <select
                            v-model="form.origenProvincia"
                            class="form-select-styled"
                            :disabled="!form.origenDepartamento"
                          >
                            <option v-for="prov in provinciasOrigenList" :key="'orig-prov-' + prov" :value="prov">
                              {{ prov }}
                            </option>
                          </select>
                        </div>
                      </div>
                    </div>

                    <!-- DESTINO -->
                    <div class="route-location-card">
                      <div class="loc-card-tag loc-dest">
                        <i class="pi pi-map-marker"></i>
                        <span>PUNTO DE LLEGADA (DESTINO)</span>
                      </div>
                      <div class="loc-fields-row">
                        <div class="field-subgroup">
                          <label class="loc-label">Departamento *</label>
                          <select
                            v-model="form.destinoDepartamento"
                            class="form-select-styled"
                            @change="onDestinoDepartamentoChange"
                          >
                            <option v-for="dep in departamentosList" :key="'dest-' + dep" :value="dep">
                              {{ dep }}
                            </option>
                          </select>
                        </div>

                        <div class="field-subgroup">
                          <label class="loc-label">Provincia *</label>
                          <select
                            v-model="form.destinoProvincia"
                            class="form-select-styled"
                            :disabled="!form.destinoDepartamento"
                          >
                            <option v-for="prov in provinciasDestinoList" :key="'dest-prov-' + prov" :value="prov">
                              {{ prov }}
                            </option>
                          </select>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- TARIFA DE TRANSPORTE LOCAL -->
                  <div class="route-cost-row">
                    <div class="route-cost-info">
                      <div class="cost-title-box">
                        <i class="pi pi-dollar"></i>
                        <label class="route-cost-label">Tarifa Flete Transporte Terrestre SGA (USD) *</label>
                      </div>
                      <span class="route-cost-sub">Importe que se sumará a los conceptos a liquidar de esta cotización comercial.</span>
                    </div>
                    <div class="route-cost-input-wrap">
                      <span class="currency-tag">$ USD</span>
                      <input
                        v-model.number="form.costoTransporteInterno"
                        type="number"
                        step="0.01"
                        min="0"
                        class="form-input cost-input"
                        placeholder="0.00"
                        :disabled="submitting"
                      />
                    </div>
                  </div>
                </div>
              </transition>
            </div>

            <!-- 6. Validez de la Propuesta (Días) -->
            <div class="field-group highlight-group">
              <div class="label-with-badge">
                <label for="cot-validez" class="field-label">Validez de la Oferta Comercial (Días Calendario) *</label>
                <span class="kicker-mini-badge">PLAZO VIGENCIA</span>
              </div>
              <div class="input-icon-box">
                <i class="pi pi-calendar input-icon"></i>
                <input
                  id="cot-validez"
                  v-model.number="form.validezDias"
                  type="number"
                  min="1"
                  max="90"
                  class="form-input number-input"
                  required
                  :disabled="submitting"
                />
                <span class="input-suffix">DÍAS</span>
              </div>
              <p class="field-helper-text">
                Transcurrido este plazo, los fletes marítimos/aéreos y tarifas de honorarios quedarán sujetos a re-cotización.
              </p>

              <!-- Atajos de días -->
              <div class="quick-shortcuts-row">
                <span class="shortcuts-label">Plazos estándar:</span>
                <button type="button" class="shortcut-chip" @click="form.validezDias = 15">15 Días</button>
                <button type="button" class="shortcut-chip" @click="form.validezDias = 30">30 Días</button>
                <button type="button" class="shortcut-chip" @click="form.validezDias = 45">45 Días</button>
                <button type="button" class="shortcut-chip" @click="form.validezDias = 60">60 Días</button>
              </div>
            </div>

            <!-- 7. Observaciones y Términos Comerciales (Trasladado al Paso 2) -->
            <div class="field-group">
              <label for="cot-obs" class="field-label">Observaciones, Términos Comerciales y Requerimientos Especiales</label>
              <textarea
                id="cot-obs"
                v-model="form.observaciones"
                rows="3"
                placeholder="Indique detalles de tipo de bultos, requerimientos de cuadrilla en puerto/aeropuerto, almacenaje temporal contratado, etc."
                class="form-textarea"
                :disabled="submitting"
              ></textarea>
            </div>

            <!-- Botones de Navegación del Paso 2 -->
            <div class="step-footer-actions">
              <button
                type="button"
                class="btn-outline-warm"
                @click="currentStep = 1"
              >
                <i class="pi pi-arrow-left"></i>
                <span>Regresar a Cliente</span>
              </button>
              <button
                type="button"
                class="btn-espresso"
                @click="currentStep = 3"
              >
                <span>Continuar a Documentación</span>
                <i class="pi pi-arrow-right"></i>
              </button>
            </div>
          </div>
        </section>

        <!-- ========================================================================= -->
        <!-- PASO 3: DOCUMENTACIÓN SUSTENTATORIA -->
        <!-- ========================================================================= -->
        <section v-show="currentStep === 3" class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Documentación Sustentatoria de la Propuesta</h3>
              <p class="block-sub">Adjunte facturas proforma, listas de empaque, certificados de origen o resoluciones sectoriales para respaldar técnica y documentalmente la cotización.</p>
            </div>
          </div>

          <div class="block-body">
            <DocumentosUploadPanel
              :mode="isEditMode ? 'live' : 'deferred'"
              v-model="queuedDocs"
              :entidad-tipo="'COTIZACION'"
              :id-entidad="props.cotizacionToEdit?.idCotizacion"
              :entidad-id="props.cotizacionToEdit?.idCotizacion"
              :codigo-entidad="form.codigoCotizacion || 'NUEVA'"
              :titulo-entidad="selectedClientObject?.razonSocial || 'Propuesta Comercial'"
              :permiso-requerido="selectedMercancia?.requierePermiso"
              :entidad-reguladora="selectedMercancia?.entidadReguladora"
              :hide-header-banner="true"
            />

            <!-- Botones de Navegación del Paso 3 -->
            <div class="step-footer-actions">
              <button
                type="button"
                class="btn-outline-warm"
                @click="currentStep = 2"
              >
                <i class="pi pi-arrow-left"></i>
                <span>Regresar a Régimen y Mercancía</span>
              </button>
              <button
                type="button"
                class="btn-espresso"
                @click="currentStep = 4"
              >
                <span>Continuar a Costos y Liquidación</span>
                <i class="pi pi-arrow-right"></i>
              </button>
            </div>
          </div>
        </section>

        <!-- ========================================================================= -->
        <!-- PASO 4: ESTRUCTURA ECONÓMICA Y LIQUIDACIÓN ESTIMADA -->
        <!-- ========================================================================= -->
        <section v-show="currentStep === 4" class="form-block-card">
          <div class="block-header">
            <div class="block-idx">04</div>
            <div class="block-header-info">
              <h3 class="block-title">Estructura Económica y Liquidación Estimada (USD)</h3>
              <p class="block-sub">Determine el valor comercial de la mercancía (FOB), fletes, agenciamiento aduanero y la liquidación tributaria arancelaria oficial.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- 1. Valor de Mercancía, Flete y Seguro -->
            <div class="costs-two-cols">
              <!-- Valor FOB Obligatorio -->
              <div class="field-group highlight-group">
                <div class="label-with-badge">
                  <label for="cot-fob" class="field-label">Valor de Mercancía (FOB en Factura Comercial) *</label>
                  <span class="kicker-mini-badge">BASE DE CÁLCULO</span>
                </div>
                <div class="input-icon-box">
                  <i class="pi pi-money-bill input-icon"></i>
                  <input
                    id="cot-fob"
                    v-model.number="form.valorFob"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="form-input number-input"
                    required
                    :disabled="submitting"
                  />
                  <span class="input-suffix">USD</span>
                </div>
                <span class="field-hint-mini">Monto de la factura comercial del proveedor. Sin este dato no existe base de cálculo.</span>
              </div>

              <!-- Flete Internacional -->
              <div class="field-group">
                <label for="cot-flete" class="field-label">Flete Internacional Estimado (USD)</label>
                <div class="input-icon-box">
                  <i class="pi pi-compass input-icon"></i>
                  <input
                    id="cot-flete"
                    v-model.number="form.montoFleteEstimado"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="form-input number-input"
                    :disabled="submitting"
                  />
                  <span class="input-suffix">USD</span>
                </div>
                <span class="field-hint-mini">Costo del flete marítimo o aéreo internacional (viene cotizado o acordado).</span>
              </div>
            </div>

            <!-- Fila: Seguro Estimado y Badge CIF / FOB -->
            <div class="costs-two-cols">
              <!-- Seguro Estimado (Automático editable en Import / Opcional en Export) -->
              <div class="field-group">
                <div class="label-with-badge">
                  <label for="cot-seguro" class="field-label">Seguro Estimado (USD)</label>
                  <span v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'" class="kicker-mini-badge">
                    {{ seguroManual ? 'MANUAL' : 'AUTO (0.75% SUNAT)' }}
                  </span>
                </div>
                <div class="input-icon-box">
                  <i class="pi pi-shield input-icon"></i>
                  <input
                    id="cot-seguro"
                    v-model.number="form.montoSeguroEstimado"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="form-input number-input"
                    @input="handleSeguroManualInput"
                    :disabled="submitting"
                  />
                  <span class="input-suffix">USD</span>
                </div>
                <div class="seguro-helper-row">
                  <span class="field-hint-mini">
                    {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Opcional si el Incoterm convenido incluye póliza propia.' : 'Por defecto: FOB * 0.0075 (tabla referencial SUNAT ~0.75%), o editable si el cliente contrató póliza.' }}
                  </span>
                  <button
                    v-if="seguroManual && form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'"
                    type="button"
                    class="btn-reset-seguro"
                    @click="resetearSeguroAutomatico"
                  >
                    Restablecer 0.75%
                  </button>
                </div>
              </div>

              <!-- Base de Valoración Visible (Badge CIF en Importación / FOB en Exportación) -->
              <div class="field-group">
                <label class="field-label">
                  {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Valor Oficial de Exportación (DAM 40)' : 'Valor CIF Calculado (Base Imponible Tributos)' }}
                </label>
                <div class="cif-display-box">
                  <div class="cif-info-left">
                    <span class="cif-badge-label">
                      {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'BASE DECLARADA' : 'CIF = FOB + FLETE + SEGURO' }}
                    </span>
                    <span class="cif-amount">
                      ${{ formatNumber(form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? form.valorFob : valorCif) }}
                      <span class="cif-currency">USD</span>
                    </span>
                  </div>
                  <i :class="form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'pi pi-send cif-icon' : 'pi pi-calculator cif-icon'"></i>
                </div>
                <span class="field-hint-mini">
                  {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Monto de referencia para la aduana de salida.' : 'Sirve de base visible oficial para la liquidación de tributos SUNAT.' }}
                </span>
              </div>
            </div>

            <!-- 2. Gastos Operativos y Agencia de Aduanas -->
            <div class="field-group">
              <div class="label-with-badge">
                <label for="cot-honorarios" class="field-label">Gastos Operativos y Agencia de Aduanas (USD) *</label>
                <span class="kicker-mini-badge">TARIFA PLANA SGA</span>
              </div>
              <div class="input-icon-box">
                <i class="pi pi-briefcase input-icon"></i>
                <input
                  id="cot-honorarios"
                  v-model.number="form.montoHonorariosEstimado"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  class="form-input number-input"
                  :disabled="submitting"
                />
                <span class="input-suffix">USD</span>
              </div>
              <span class="field-hint-mini">Agrupa honorarios aduaneros + visto bueno + almacén temporal.</span>
            </div>

            <!-- 2.1 Flete Transporte Terrestre Local SGA (Si se contrató servicio) -->
            <div v-if="form.servicioTransporte" class="field-group transport-summary-row-box">
              <div class="label-with-badge">
                <label class="field-label">Flete Transporte Terrestre Local SGA (USD) *</label>
                <span class="kicker-mini-badge" style="background: rgba(194, 65, 12, 0.15); color: #c2410c;">
                  RUTA: {{ form.origenProvincia || 'Origen' }} → {{ form.destinoProvincia || 'Destino' }}
                </span>
              </div>
              <div class="input-icon-box">
                <i class="pi pi-truck input-icon"></i>
                <input
                  v-model.number="form.costoTransporteInterno"
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  class="form-input number-input"
                  :disabled="submitting"
                />
                <span class="input-suffix">USD</span>
              </div>
              <span class="field-hint-mini">Costo del traslado terrestre desde {{ form.origenProvincia }}, {{ form.origenDepartamento }} hacia {{ form.destinoProvincia }}, {{ form.destinoDepartamento }}.</span>
            </div>

            <!-- 3. TRIBUTOS Y GRAVÁMENES SUNAT -->
            <!-- Caso Exportación Definitiva: Inafecto de Tributos -->
            <div v-if="form.regimenAduanero === 'EXPORTACION_DEFINITIVA'" class="export-inafecto-banner">
              <div class="inafecto-icon-box">
                <i class="pi pi-info-circle"></i>
              </div>
              <div class="inafecto-body">
                <h4 class="inafecto-title">Régimen de Exportación Definitiva (REG-40): Inafecto de Tributos SUNAT</h4>
                <p class="inafecto-desc">
                  Conforme al Art. 60 de la Ley General de Aduanas (D.L. 1053) y el TUO de la Ley del IGV, la exportación definitiva de mercancías no se encuentra afecta a ningún derecho arancelario Ad-Valórem, IGV/IPM ni percepción aduanera.
                </p>
                <div class="inafecto-badge-row">
                  <span class="inafecto-pill">Tasa Ad-Valórem: 0%</span>
                  <span class="inafecto-pill">Tasa IGV/IPM: 0%</span>
                  <span class="inafecto-pill">Percepción: 0%</span>
                  <span class="inafecto-pill total-zero">TRIBUTOS SUNAT: $0.00 USD</span>
                </div>
              </div>
            </div>

            <!-- Caso Importación Definitiva: Desglose Completo según cuadro -->
            <div v-else class="tributos-breakdown-card">
              <div class="tributos-card-header">
                <div class="tributos-header-left">
                  <i class="pi pi-percentage header-icon"></i>
                  <div>
                    <h4 class="tributos-card-title">Desglose de Tributos y Gravámenes SUNAT (USD)</h4>
                    <p class="tributos-card-desc">Cálculo automático sobre el Valor CIF calculado (${{ formatNumber(valorCif) }} USD) según la tasa ad-valórem y percepción seleccionada.</p>
                  </div>
                </div>
                <div class="tributos-total-badge">
                  <span class="badge-label">TRIBUTO ESTIMADO TOTAL</span>
                  <span class="badge-amount">${{ formatNumber(form.montoTributosEstimado) }} USD</span>
                </div>
              </div>

              <div class="tributos-inputs-grid">
                <!-- Tasa y Monto Ad-Valórem -->
                <div class="tributo-item">
                  <div class="tributo-item-top">
                    <label for="cot-tasa-advalorem" class="tributo-label">Tasa Ad-Valórem *</label>
                    <span class="tributo-rate-pill">SELECTOR</span>
                  </div>
                  <select
                    id="cot-tasa-advalorem"
                    v-model.number="form.tasaAdValorem"
                    class="form-select-styled"
                    :disabled="submitting"
                  >
                    <option :value="0">0% (Insumos / Libre arancel)</option>
                    <option :value="6">6% (Bienes intermedios / Maquinaria)</option>
                    <option :value="11">11% (Bienes de consumo final)</option>
                  </select>
                  <div class="read-only-amount-box">
                    <span class="read-only-label">Monto Ad-Valórem:</span>
                    <span class="read-only-val">${{ formatNumber(montoAdValorem) }} USD</span>
                  </div>
                  <span class="tributo-helper">advalorem = cif * tasa_advalorem</span>
                </div>

                <!-- IGV (16%) + IPM (2%) -->
                <div class="tributo-item">
                  <div class="tributo-item-top">
                    <label class="tributo-label">IGV (16%) + IPM (2%)</label>
                    <span class="tributo-rate-pill">Tasa: 18%</span>
                  </div>
                  <div class="read-only-fixed-rate">
                    <span class="fixed-rate-text">18.00% (Fija SUNAT)</span>
                  </div>
                  <div class="read-only-amount-box">
                    <span class="read-only-label">Monto IGV + IPM:</span>
                    <span class="read-only-val">${{ formatNumber(montoIgv) }} USD</span>
                  </div>
                  <span class="tributo-helper">igv_ipm = (cif + advalorem) * 0.18</span>
                </div>

                <!-- Percepción SUNAT (Selector Opcional) -->
                <div class="tributo-item">
                  <div class="tributo-item-top">
                    <label for="cot-tasa-percepcion" class="tributo-label">Percepción SUNAT</label>
                    <span class="tributo-rate-pill-optional">SELECTOR + AUTO</span>
                  </div>
                  <select
                    id="cot-tasa-percepcion"
                    v-model.number="form.tasaPercepcion"
                    class="form-select-styled"
                    :disabled="submitting"
                  >
                    <option :value="0">0% (Sin percepción / Exonerado)</option>
                    <option :value="3.5">3.5% (Frecuente / Importador habitual)</option>
                    <option :value="5">5% (Bienes usados)</option>
                    <option :value="10">10% (Primera importación / No habido)</option>
                  </select>
                  <div class="read-only-amount-box">
                    <span class="read-only-label">Monto Percepción:</span>
                    <span class="read-only-val">${{ formatNumber(montoPercepcion) }} USD</span>
                  </div>
                  <span class="tributo-helper">percepcion = (cif + adv + igv) * tasa</span>
                </div>
              </div>
            </div>

            <!-- 4. Total Cotizado Liquidado -->
            <div class="total-summary-banner">
              <div class="total-banner-info">
                <span class="total-badge">IMPORTE TOTAL LIQUIDADO</span>
                <span class="total-sub">
                  {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Suma consolidada de flete y gastos operativos de agencia (Flete + Gastos Op.)' : 'Suma consolidada de flete, gastos operativos y tributos estimados (Flete + Gastos Op. + Tributos)' }}
                </span>
              </div>
              <div class="total-val-box">
                <span class="currency-symbol">$</span>
                <span class="total-number">{{ formatNumber(form.totalCotizado) }}</span>
                <span class="currency-code">USD</span>
              </div>
            </div>

            <!-- Botones de Navegación del Paso 4 -->
            <div class="step-footer-actions">
              <button
                type="button"
                class="btn-outline-warm"
                @click="currentStep = 3"
              >
                <i class="pi pi-arrow-left"></i>
                <span>Regresar a Documentación</span>
              </button>

              <button
                type="button"
                class="btn-espresso"
                @click="handleSubmit"
                :disabled="submitting || !isFormReady"
              >
                <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
                <i v-else class="pi pi-check"></i>
                <span>{{ isEditMode ? 'Guardar Cambios' : 'Emitir Cotización' }}</span>
              </button>
            </div>
          </div>
        </section>
      </div>

      <!-- Columna Lateral Sticky: Previsualización en Vivo de la Proforma Modular -->
      <CotizacionProformaPreview
        :form="form"
        :selected-client-object="selectedClientObject"
        :selected-puerto-envio="selectedPuertoEnvio"
        :selected-puerto-destino="selectedPuertoDestino"
        :selected-tipo-carga="selectedTipoCarga"
        :selected-mercancia="selectedMercancia"
        :is-edit-mode="isEditMode"
        :submitting="submitting"
        :is-form-ready="isFormReady"
        :docs-count="isEditMode ? existingDocsCount : queuedDocs.length"
        :get-regimen-label="getRegimenLabel"
        :format-number="formatNumber"
        @submit="handleSubmit"
        @cancel="$emit('cancel')"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { useAuthStore } from '@/features/auth/stores/authStore'
import { comercialService } from '../services/comercialService'
import { catalogoService } from '@/features/admin/catalogos/services/catalogoService'
import { documentoService } from '@/features/documentos/services/documentoService'
import CotizacionFormHeader from './CotizacionFormHeader.vue'
import CotizacionStepperNav from './CotizacionStepperNav.vue'
import CotizacionProformaPreview from './CotizacionProformaPreview.vue'
import DocumentosUploadPanel from '@/shared/components/DocumentosUploadPanel.vue'
import { getDepartamentosPeru, getProvinciasPorDepartamento } from '../data/peruUbigeo'

const props = defineProps({
  cotizacionToEdit: {
    type: Object,
    default: null
  },
  clientes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['cancel', 'saved', 'client-created'])
const authStore = useAuthStore()

const currentStep = ref(1)
const submitting = ref(false)
const errorMessage = ref('')
const loadingCatalogos = ref(false)

// Estado para gestión documental en el Paso 03
const queuedDocs = ref([])
const existingDocsCount = ref(0)

// Bandera para indicar si el usuario editó manualmente el Seguro en importación
const seguroManual = ref(false)

// Catálogos auxiliares traídos desde API
const puertos = ref([])
const tiposCarga = ref([])
const mercancias = ref([])

// Función utilitaria para desenvolver listas del envoltorio ApiResponse
const unwrapList = (res) => {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.data)) return res.data
  return []
}

// Lista local reactiva de clientes para soportar alta inmediata
const localClientsList = ref([...props.clientes])

watch(
  () => props.clientes,
  (newClients) => {
    localClientsList.value = [...(newClients || [])]
  },
  { deep: true }
)

// Búsqueda de clientes
const clientSearchQuery = ref('')

// Formulario Rápido de Nuevo Cliente Inline
const showQuickClientForm = ref(false)
const savingQuickClient = ref(false)
const quickClientError = ref('')
const quickClient = ref({
  tipoDocumento: 'RUC',
  numeroDocumento: '',
  razonSocial: '',
  correoContacto: '',
  telefonoContacto: '',
  esProspecto: true
})

// Opciones de Régimen Aduanero Oficial
const regimenOptions = [
  {
    value: 'IMPORTACION_DEFINITIVA',
    codigo: 'REG-10',
    label: 'Importación para el Consumo',
    desc: 'Ingreso legal de mercancías para consumo definitivo en el país previo pago de tributos arancelarios.'
  },
  {
    value: 'EXPORTACION_DEFINITIVA',
    codigo: 'REG-40',
    label: 'Exportación Definitiva',
    desc: 'Salida de mercancías nacionales o nacionalizadas del territorio aduanero para uso o consumo en el exterior.'
  }
]

// Estado del Formulario
const defaultForm = () => ({
  codigoCotizacion: '',
  idCliente: '',
  idUsuarioComercial: authStore.user?.idUsuario || 1,
  validezDias: 15,
  regimenAduanero: 'IMPORTACION_DEFINITIVA',
  idPuertoEnvio: null,
  idPuertoDestino: null,
  idTipoCarga: null,
  idMercancia: null,
  servicioTransporte: false,
  origenDepartamento: 'CALLAO',
  origenProvincia: 'Callao',
  destinoDepartamento: 'LIMA',
  destinoProvincia: 'Lima (Metropolitana)',
  costoTransporteInterno: 0,
  valorFob: 0,
  montoFleteEstimado: 0,
  montoSeguroEstimado: 0,
  montoHonorariosEstimado: 0,
  tasaAdValorem: 0,
  tasaPercepcion: 0,
  montoTributosEstimado: 0,
  totalCotizado: 0,
  observaciones: ''
})

// Catálogo de Ubigeo y Helpers
const departamentosList = computed(() => getDepartamentosPeru())
const provinciasOrigenList = computed(() => getProvinciasPorDepartamento(form.value.origenDepartamento))
const provinciasDestinoList = computed(() => getProvinciasPorDepartamento(form.value.destinoDepartamento))

const onOrigenDepartamentoChange = () => {
  const provs = getProvinciasPorDepartamento(form.value.origenDepartamento)
  form.value.origenProvincia = provs.length > 0 ? provs[0] : ''
}

const onDestinoDepartamentoChange = () => {
  const provs = getProvinciasPorDepartamento(form.value.destinoDepartamento)
  form.value.destinoProvincia = provs.length > 0 ? provs[0] : ''
}

const form = ref(defaultForm())

const isEditMode = computed(() => !!props.cotizacionToEdit && !!props.cotizacionToEdit.idCotizacion)

// Computed de Entidades Seleccionadas
const selectedClientObject = computed(() => {
  if (!form.value.idCliente) return null
  return localClientsList.value.find((c) => c.idCliente === form.value.idCliente) || null
})

const selectedMercancia = computed(() => {
  if (!form.value.idMercancia) return null
  return mercancias.value.find((m) => m.idMercancia === form.value.idMercancia) || null
})

const selectedPuertoEnvio = computed(() => {
  if (!form.value.idPuertoEnvio) return null
  return puertos.value.find((p) => p.idPuerto === form.value.idPuertoEnvio) || null
})

const selectedPuertoDestino = computed(() => {
  if (!form.value.idPuertoDestino) return null
  return puertos.value.find((p) => p.idPuerto === form.value.idPuertoDestino) || null
})

const selectedTipoCarga = computed(() => {
  if (!form.value.idTipoCarga) return null
  return tiposCarga.value.find((tc) => tc.idTipoCarga === form.value.idTipoCarga) || null
})

// Cargar catálogos desde backend con desenvolvimiento seguro
const loadCatalogos = async () => {
  loadingCatalogos.value = true
  try {
    const [puertosRes, tiposCargaRes, mercanciasRes] = await Promise.allSettled([
      catalogoService.getPuertos(true),
      catalogoService.getTiposCarga(true),
      catalogoService.getMercancias(true)
    ])
    if (puertosRes.status === 'fulfilled') {
      puertos.value = unwrapList(puertosRes.value)
    }
    if (tiposCargaRes.status === 'fulfilled') {
      tiposCarga.value = unwrapList(tiposCargaRes.value)
    }
    if (mercanciasRes.status === 'fulfilled') {
      mercancias.value = unwrapList(mercanciasRes.value)
    }
  } catch (err) {
    console.error('Error cargando catálogos para cotización:', err)
  } finally {
    loadingCatalogos.value = false
  }
}

onMounted(() => {
  loadCatalogos()
})

watch(
  () => props.cotizacionToEdit,
  async (val) => {
    errorMessage.value = ''
    queuedDocs.value = []
    seguroManual.value = false

    if (val && val.idCotizacion) {
      form.value = {
        codigoCotizacion: val.codigoCotizacion || '',
        idCliente: val.idCliente || '',
        idUsuarioComercial: val.idUsuarioComercial || authStore.user?.idUsuario || 1,
        validezDias: val.validezDias || 15,
        regimenAduanero: val.regimenAduanero || 'IMPORTACION_DEFINITIVA',
        idPuertoEnvio: val.idPuertoEnvio || null,
        idPuertoDestino: val.idPuertoDestino || null,
        idTipoCarga: val.idTipoCarga || null,
        idMercancia: val.idMercancia || null,
        servicioTransporte: val.servicioTransporte === true,
        origenDepartamento: val.origenDepartamento || 'CALLAO',
        origenProvincia: val.origenProvincia || 'Callao',
        destinoDepartamento: val.destinoDepartamento || 'LIMA',
        destinoProvincia: val.destinoProvincia || 'Lima (Metropolitana)',
        costoTransporteInterno: Number(val.costoTransporteInterno || 0),
        valorFob: Number(val.valorFob || 0),
        montoFleteEstimado: Number(val.montoFleteEstimado || 0),
        montoSeguroEstimado: Number(val.montoSeguroEstimado || 0),
        montoHonorariosEstimado: Number(val.montoHonorariosEstimado || 0),
        tasaAdValorem: Number(val.tasaAdValorem || 0),
        tasaPercepcion: Number(val.tasaPercepcion || 0),
        montoTributosEstimado: Number(val.montoTributosEstimado || 0),
        totalCotizado: Number(val.totalCotizado || 0),
        observaciones: val.observaciones || ''
      }

      if (form.value.montoSeguroEstimado > 0) {
        seguroManual.value = true
      }

      try {
        const docs = await documentoService.getDocumentos({
          entidadTipo: 'COTIZACION',
          idCotizacion: val.idCotizacion
        })
        existingDocsCount.value = Array.isArray(docs) ? docs.length : 0
      } catch {
        existingDocsCount.value = 0
      }
    } else {
      form.value = defaultForm()
      existingDocsCount.value = 0
    }
  },
  { immediate: true }
)

// Filtrado de Clientes para el Selector
const filteredAvailableClients = computed(() => {
  const query = clientSearchQuery.value.toLowerCase().trim()
  if (!query) return localClientsList.value

  return localClientsList.value.filter((c) => {
    return (
      c.razonSocial?.toLowerCase().includes(query) ||
      c.numeroDocumento?.includes(query) ||
      c.correoContacto?.toLowerCase().includes(query)
    )
  })
})

const selectClient = (client) => {
  form.value.idCliente = client.idCliente
  clientSearchQuery.value = ''
}

const clearSelectedClient = () => {
  form.value.idCliente = ''
}

// Guardar nuevo cliente desde formulario rápido inline
const handleSaveQuickClient = async () => {
  quickClientError.value = ''
  if (!quickClient.value.numeroDocumento || !quickClient.value.razonSocial || !quickClient.value.correoContacto) {
    quickClientError.value = 'Complete los campos obligatorios: Documento, Razón Social y Correo.'
    return
  }

  savingQuickClient.value = true
  try {
    const createdClient = await comercialService.createCliente(quickClient.value)
    localClientsList.value.unshift(createdClient)
    form.value.idCliente = createdClient.idCliente
    showQuickClientForm.value = false
    quickClient.value = {
      tipoDocumento: 'RUC',
      numeroDocumento: '',
      razonSocial: '',
      correoContacto: '',
      telefonoContacto: '',
      esProspecto: true
    }
    emit('client-created', createdClient)
  } catch (err) {
    quickClientError.value = err.response?.data?.message || 'Error al registrar nuevo cliente.'
  } finally {
    savingQuickClient.value = false
  }
}

// Acción al cambiar mercancía: sincronizar arancel si corresponde
const handleMercanciaChange = () => {
  if (selectedMercancia.value) {
    if (selectedMercancia.value.arancelBasePct !== undefined && selectedMercancia.value.arancelBasePct !== null) {
      const pct = Number(selectedMercancia.value.arancelBasePct)
      if ([0, 6, 11].includes(pct)) {
        form.value.tasaAdValorem = pct
      }
    }
    recalcularLiquidacion()
  }
}

// =========================================================================
// MOTOR DE VALORACIÓN ADUANERA Y LIQUIDACIÓN ECONÓMICA (SUNAT)
// =========================================================================

// Valor CIF Calculado = FOB + Flete + Seguro
const valorCif = computed(() => {
  const fob = Number(form.value.valorFob) || 0
  const flete = Number(form.value.montoFleteEstimado) || 0
  const seguro = Number(form.value.montoSeguroEstimado) || 0
  return Number((fob + flete + seguro).toFixed(2))
})

// Monto Ad-Valórem = CIF * tasa_advalorem
const montoAdValorem = computed(() => {
  if (form.value.regimenAduanero === 'EXPORTACION_DEFINITIVA') return 0
  const tasa = Number(form.value.tasaAdValorem) || 0
  return Number((valorCif.value * (tasa / 100)).toFixed(2))
})

// IGV (16%) + IPM (2%) = (CIF + Ad-Valórem) * 0.18
const montoIgv = computed(() => {
  if (form.value.regimenAduanero === 'EXPORTACION_DEFINITIVA') return 0
  const base = valorCif.value + montoAdValorem.value
  return Number((base * 0.18).toFixed(2))
})

// Percepción SUNAT = (CIF + Ad-Valórem + IGV/IPM) * tasa_percepcion
const montoPercepcion = computed(() => {
  if (form.value.regimenAduanero === 'EXPORTACION_DEFINITIVA') return 0
  const tasa = Number(form.value.tasaPercepcion) || 0
  if (tasa <= 0) return 0
  const base = valorCif.value + montoAdValorem.value + montoIgv.value
  return Number((base * (tasa / 100)).toFixed(2))
})

// Recálculo integral de liquidación
const recalcularLiquidacion = () => {
  const fob = Number(form.value.valorFob) || 0
  const flete = Number(form.value.montoFleteEstimado) || 0
  const honorarios = Number(form.value.montoHonorariosEstimado) || 0
  const costoTransp = form.value.servicioTransporte ? (Number(form.value.costoTransporteInterno) || 0) : 0

  if (form.value.regimenAduanero === 'EXPORTACION_DEFINITIVA') {
    form.value.tasaAdValorem = 0
    form.value.tasaPercepcion = 0
    form.value.montoTributosEstimado = 0
    form.value.totalCotizado = Number((flete + honorarios + costoTransp).toFixed(2))
  } else {
    // Si no fue editado manualmente, seguro = fob * 0.0075 (~0.75% SUNAT)
    if (!seguroManual.value) {
      form.value.montoSeguroEstimado = Number((fob * 0.0075).toFixed(2))
    }
    const tributos = Number((montoAdValorem.value + montoIgv.value + montoPercepcion.value).toFixed(2))
    form.value.montoTributosEstimado = tributos
    form.value.totalCotizado = Number((flete + honorarios + tributos + costoTransp).toFixed(2))
  }
}

const handleSeguroManualInput = () => {
  seguroManual.value = true
  recalcularLiquidacion()
}

const resetearSeguroAutomatico = () => {
  seguroManual.value = false
  recalcularLiquidacion()
}

// Watch dinámico de insumos para recálculo automático y sincronización en vivo
watch(
  () => [
    form.value.valorFob,
    form.value.montoFleteEstimado,
    form.value.montoSeguroEstimado,
    form.value.montoHonorariosEstimado,
    form.value.tasaAdValorem,
    form.value.tasaPercepcion,
    form.value.regimenAduanero,
    form.value.servicioTransporte,
    form.value.costoTransporteInterno
  ],
  () => {
    recalcularLiquidacion()
  }
)

const formatNumber = (num) => {
  if (num === null || num === undefined || isNaN(num)) return '0.00'
  return Number(num).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const getRegimenLabel = (val) => {
  const match = regimenOptions.find((r) => r.value === val)
  return match ? `${match.label} (${match.codigo})` : val
}

const isFormReady = computed(() => {
  return (
    !!form.value.idCliente &&
    !!form.value.regimenAduanero &&
    form.value.validezDias > 0 &&
    Number(form.value.valorFob) > 0 &&
    Number(form.value.totalCotizado) > 0
  )
})

const handleSubmit = () => {
  errorMessage.value = ''
  if (!form.value.idCliente) {
    errorMessage.value = 'Debe seleccionar o registrar un cliente en el Paso 01.'
    currentStep.value = 1
    return
  }

  if (!form.value.valorFob || form.value.valorFob <= 0) {
    errorMessage.value = 'El Valor de Mercancía (FOB) es obligatorio y debe ser mayor a $0.00 USD. Configure los montos en el Paso 04.'
    currentStep.value = 4
    return
  }

  if (form.value.totalCotizado <= 0) {
    errorMessage.value = 'El total cotizado debe ser mayor a $0.00 USD. Configure los montos en el Paso 04.'
    currentStep.value = 4
    return
  }

  submitting.value = true
  emit('saved', {
    isEdit: isEditMode.value,
    cotizacionId: props.cotizacionToEdit?.idCotizacion,
    payload: {
      ...form.value,
      idUsuarioComercial: authStore.user?.idUsuario || form.value.idUsuarioComercial || 1
    },
    documentos: queuedDocs.value
  })
}
</script>

<style scoped>
.cotizacion-registration-form-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

/* 1. Header Bar */
.form-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-6, 1.5rem);
  padding: var(--space-6, 1.5rem) var(--space-8, 2rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  box-shadow: var(--shadow-sm);
  flex-wrap: wrap;
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  max-width: 800px;
}

.btn-back-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  cursor: pointer;
  padding: 0;
  margin-bottom: var(--space-1, 0.25rem);
  transition: color var(--transition-fast);
}

.btn-back-link:hover {
  color: var(--color-primary);
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.badge-kicker {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--color-primary);
  background: var(--color-primary-subtle);
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-sm, 0.25rem);
}

.mode-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  padding: 0.2rem 0.6rem;
  border-radius: var(--radius-full, 9999px);
}

.mode-create {
  background: var(--color-success-subtle);
  color: var(--color-success);
}

.mode-edit {
  background: var(--color-warning-subtle);
  color: var(--color-warning);
}

.form-main-title {
  font-family: var(--font-sans);
  font-size: var(--font-size-2xl, 1.75rem);
  font-weight: 800;
  color: var(--color-text-main);
  line-height: 1.2;
  margin: 0;
}

.form-main-desc {
  font-size: var(--font-size-sm, 0.875rem);
  color: var(--color-text-muted);
  line-height: 1.5;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.65rem 1.25rem;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-outline-warm:hover:not(:disabled) {
  background: var(--color-bg-subtle);
  border-color: var(--color-border-subtle);
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.65rem 1.4rem;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  border-radius: var(--radius-md, 0.5rem);
  border: none;
  background: var(--color-primary);
  color: var(--color-primary-contrast);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-sm);
}

.btn-espresso:hover:not(:disabled) {
  opacity: 0.92;
  transform: translateY(-1px);
}

.btn-espresso:disabled,
.btn-outline-warm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* Banner de Error */
.error-banner {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background: var(--color-danger-subtle);
  border: 1px solid var(--color-danger);
  border-radius: var(--radius-lg, 0.75rem);
  color: var(--color-danger);
  font-size: var(--font-size-sm, 0.875rem);
}

.error-content {
  flex: 1;
}

.error-close-btn {
  background: none;
  border: none;
  color: var(--color-danger);
  cursor: pointer;
  padding: 0.25rem;
}

/* Banner de Observación de Supervisión */
.revision-alert-banner {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4, 1rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-lg, 0.75rem);
  color: var(--color-warning-text);
}

.revision-icon {
  font-size: 1.5rem;
  margin-top: 0.15rem;
  flex-shrink: 0;
}

.revision-body {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.revision-kicker {
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  opacity: 0.9;
}

.revision-title {
  font-size: 0.9375rem;
  font-weight: 700;
}

.revision-note {
  margin: 0.25rem 0;
  font-size: 0.875rem;
  line-height: 1.45;
  font-weight: 500;
}

.revision-hint {
  font-size: 0.75rem;
  opacity: 0.85;
}

/* Stepper Navigation Bar */
.stepper-nav-bar {
  display: flex;
  align-items: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-3, 0.75rem) var(--space-6, 1.5rem);
  box-shadow: var(--shadow-sm);
  gap: var(--space-4, 1rem);
  overflow-x: auto;
}

.stepper-step-btn {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
  border-radius: var(--radius-lg, 0.75rem);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.stepper-step-btn:hover {
  background: var(--color-bg-subtle);
}

.step-badge {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 50%;
  border: 2px solid var(--color-border);
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  font-weight: 800;
  font-size: var(--font-size-xs, 0.75rem);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.stepper-step-btn.active .step-badge {
  background: var(--color-primary);
  color: var(--color-primary-contrast);
  border-color: var(--color-primary);
}

.stepper-step-btn.completed .step-badge {
  background: var(--color-success-subtle);
  color: var(--color-success);
  border-color: var(--color-success);
}

.step-info {
  display: flex;
  flex-direction: column;
  text-align: left;
}

.step-label {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-text-muted);
}

.step-title {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.stepper-step-btn.active .step-title {
  color: var(--color-primary);
}

.stepper-divider {
  flex: 1;
  height: 2px;
  background: var(--color-border-subtle);
  min-width: 2rem;
}

/* 2. Grid de Contenido */
.form-content-grid {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: var(--space-6, 1.5rem);
  align-items: start;
}

@media (max-width: 1120px) {
  .form-content-grid {
    grid-template-columns: 1fr;
  }
}

/* Bloques de Formulario */
.form-blocks-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

.form-block-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-6, 1.5rem) var(--space-8, 2rem);
  box-shadow: var(--shadow-sm);
}

.block-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4, 1rem);
  margin-bottom: var(--space-6, 1.5rem);
  padding-bottom: var(--space-4, 1rem);
  border-bottom: 1px solid var(--color-border-subtle);
}

.block-idx {
  font-family: var(--font-mono);
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-muted);
  border: 1px solid var(--color-border-subtle);
  width: 2.25rem;
  height: 2.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md, 0.5rem);
  flex-shrink: 0;
}

.block-header-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.block-title {
  font-size: var(--font-size-md, 1.05rem);
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0;
}

.block-sub {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0;
}

.block-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
}

/* Acciones de Búsqueda y Creación de Cliente */
.client-search-actions-bar {
  display: flex;
  gap: var(--space-3, 0.75rem);
  align-items: center;
  flex-wrap: wrap;
}

.search-input-box {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  min-width: 260px;
}

.search-icon {
  position: absolute;
  left: 0.9rem;
  color: var(--color-text-muted);
  font-size: 0.95rem;
  pointer-events: none;
}

.client-search-input {
  width: 100%;
  padding: 0.7rem 2.2rem 0.7rem 2.5rem;
  font-size: var(--font-size-sm, 0.875rem);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  transition: all var(--transition-fast);
}

.client-search-input:focus {
  outline: none;
  border-color: var(--color-primary);
  background: var(--color-bg-surface);
  box-shadow: 0 0 0 2px var(--color-primary-subtle);
}

.clear-query-btn {
  position: absolute;
  right: 0.75rem;
  background: none;
  border: none;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 0.25rem;
}

.btn-toggle-quick-client {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.7rem 1.15rem;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px dashed var(--color-primary);
  background: var(--color-primary-subtle);
  color: var(--color-primary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-toggle-quick-client:hover {
  background: var(--color-primary);
  color: var(--color-primary-contrast);
}

/* Panel de Alta Rápida Inline */
.quick-client-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  background: var(--color-bg-subtle);
  padding: var(--space-5, 1.25rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
}

.quick-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-border-subtle);
  padding-bottom: var(--space-3, 0.75rem);
}

.panel-header-title {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 800;
  color: var(--color-text-main);
}

.panel-header-badge {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  background: var(--color-primary-subtle);
  color: var(--color-primary);
  padding: 0.15rem 0.45rem;
  border-radius: var(--radius-sm, 0.25rem);
}

.quick-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3, 0.75rem);
}

.quick-error-msg {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  color: var(--color-danger);
  font-size: var(--font-size-xs, 0.75rem);
}

.quick-panel-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3, 0.75rem);
  margin-top: var(--space-2, 0.5rem);
}

/* Tarjeta de Cliente Seleccionado */
.selected-client-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4, 1rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background: var(--color-primary-subtle);
  border: 1.5px solid var(--color-primary);
  border-radius: var(--radius-lg, 0.75rem);
}

.client-card-left {
  display: flex;
  align-items: center;
  gap: var(--space-4, 1rem);
}

.client-avatar-circle {
  width: 3rem;
  height: 3rem;
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-primary);
  color: var(--color-primary-contrast);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  flex-shrink: 0;
}

.client-card-texts {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.client-kicker-row {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
}

.doc-badge {
  font-family: monospace;
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  padding: 0.15rem 0.45rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 0.25rem);
  color: var(--color-text-main);
}

.classification-pill {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  padding: 0.15rem 0.5rem;
  border-radius: var(--radius-full, 9999px);
}

.pill-client {
  background: var(--color-success-subtle);
  color: var(--color-success);
}

.pill-prospect {
  background: var(--color-warning-subtle);
  color: var(--color-warning);
}

.client-name {
  font-size: var(--font-size-md, 1rem);
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0;
}

.client-meta-row {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
}

.btn-change-client {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: 0.5rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-change-client:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

/* Lista de Selección de Clientes */
.clients-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--space-3, 0.75rem);
}

.client-select-card {
  padding: var(--space-3, 0.75rem) var(--space-4, 1rem);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  background: var(--color-bg-subtle);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  transition: all var(--transition-fast);
}

.client-select-card:hover {
  border-color: var(--color-primary);
  background: var(--color-bg-surface);
  transform: translateY(-1px);
}

.client-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.classification-pill-mini {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-full, 9999px);
}

.client-card-name {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  color: var(--color-text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.client-card-email {
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
}

.empty-clients-box {
  padding: var(--space-6, 1.5rem);
  text-align: center;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-lg, 0.75rem);
  border: 1px dashed var(--color-border);
}

.btn-link-action {
  background: none;
  border: none;
  color: var(--color-primary);
  font-weight: 700;
  text-decoration: underline;
  cursor: pointer;
}

/* Regímenes Grid */
.regimenes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-3, 0.75rem);
}

.regimen-card {
  padding: var(--space-4, 1rem);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  background: var(--color-bg-subtle);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  transition: all var(--transition-fast);
}

.regimen-card:hover {
  border-color: var(--color-border-subtle);
  background: var(--color-bg-surface);
}

.regimen-card.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-subtle);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.regimen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.regimen-code-badge {
  font-family: monospace;
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  padding: 0.15rem 0.5rem;
  border-radius: var(--radius-sm, 0.25rem);
  color: var(--color-primary);
}

.regimen-radio-indicator {
  color: var(--color-primary);
  font-size: 1rem;
}

.radio-circle {
  display: inline-block;
  width: 0.9rem;
  height: 0.9rem;
  border-radius: 50%;
  border: 1.5px solid var(--color-border);
}

.regimen-title {
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0;
}

.regimen-desc {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  line-height: 1.35;
  margin: 0;
}

/* Desglose de Costos */
.costs-three-cols {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4, 1rem);
}

@media (max-width: 768px) {
  .costs-three-cols {
    grid-template-columns: 1fr;
  }
}

.field-hint-mini {
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
}

.total-summary-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-5, 1.25rem) var(--space-6, 1.5rem);
  background: var(--color-primary-subtle);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-xl, 1rem);
  gap: var(--space-4, 1rem);
  flex-wrap: wrap;
}

.total-banner-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.total-badge {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.12em;
  color: var(--color-primary);
}

.total-sub {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
}

.total-val-box {
  display: flex;
  align-items: baseline;
  gap: 0.35rem;
  font-family: var(--font-mono);
  color: var(--color-primary);
}

.currency-symbol {
  font-size: var(--font-size-xl, 1.5rem);
  font-weight: 800;
}

.total-number {
  font-size: var(--font-size-3xl, 2.25rem);
  font-weight: 900;
  line-height: 1;
}

.currency-code {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  letter-spacing: 0.05em;
}

/* Campos Generales */
.field-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}

.full-width {
  grid-column: 1 / -1;
}

.label-with-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.field-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-main);
}

.kicker-mini-badge {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  padding: 0.15rem 0.45rem;
  border-radius: var(--radius-sm, 0.25rem);
}

.input-icon-box {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 0.9rem;
  color: var(--color-text-muted);
  font-size: 0.95rem;
  pointer-events: none;
}

.input-suffix {
  position: absolute;
  right: 0.9rem;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  color: var(--color-text-muted);
  pointer-events: none;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 0.7rem 0.9rem 0.7rem 2.5rem;
  font-size: var(--font-size-sm, 0.875rem);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  background-color: var(--color-bg-subtle);
  color: var(--color-text-main);
  transition: all var(--transition-fast);
}

.form-select {
  width: 100%;
  font-size: var(--font-size-sm, 0.875rem);
}

.form-textarea {
  padding: 0.75rem;
  resize: vertical;
}

.number-input {
  padding-right: 3rem;
  font-weight: 700;
  font-family: monospace;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  background-color: var(--color-bg-surface);
  box-shadow: 0 0 0 2px var(--color-primary-subtle);
}

.field-helper-text {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.4;
}

.quick-shortcuts-row {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  flex-wrap: wrap;
  margin-top: 0.25rem;
}

.shortcuts-label {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.shortcut-chip {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full, 9999px);
  padding: 0.2rem 0.55rem;
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.shortcut-chip:hover {
  background: var(--color-primary-subtle);
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.step-footer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--color-border-subtle);
  padding-top: var(--space-5, 1.25rem);
  margin-top: var(--space-2, 0.5rem);
}

/* 3. Columna Lateral Sticky */
.form-sidebar-column {
  position: sticky;
  top: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
}

/* Tarjeta Proforma Comercial */
.preview-proforma-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-6, 1.5rem);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.card-ribbon {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.1em;
  color: var(--color-primary);
  background: var(--color-primary-subtle);
  padding: 0.25rem 0.65rem;
  border-radius: var(--radius-full, 9999px);
  margin-bottom: var(--space-3, 0.75rem);
  align-self: flex-start;
}

.proforma-code-row {
  margin-bottom: var(--space-3, 0.75rem);
}

.doc-code-lg {
  font-family: monospace;
  font-size: var(--font-size-md, 1rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  color: var(--color-text-main);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 0.3rem 0.75rem;
  border-radius: var(--radius-md, 0.5rem);
  display: inline-block;
}

.proforma-client-snippet {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  margin-bottom: var(--space-4, 1rem);
  padding-bottom: var(--space-3, 0.75rem);
  border-bottom: 1px solid var(--color-border-subtle);
}

.proforma-sub-label {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--color-text-muted);
}

.proforma-client-name {
  font-family: var(--font-sans);
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
  line-height: 1.3;
}

.proforma-client-doc {
  font-family: var(--font-mono);
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-primary);
  font-weight: 700;
}

.proforma-details-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  margin-bottom: var(--space-5, 1.25rem);
}

.proforma-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-xs, 0.75rem);
  gap: var(--space-2, 0.5rem);
}

.p-label {
  color: var(--color-text-muted);
  font-weight: 600;
}

.p-val {
  color: var(--color-text-main);
  font-weight: 700;
  text-align: right;
}

.proforma-divider {
  height: 1px;
  background: var(--color-border-subtle);
  margin: 0.25rem 0;
}

.total-row {
  padding-top: var(--space-2, 0.5rem);
}

.p-label-total {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  color: var(--color-text-main);
}

.p-val-total {
  font-family: var(--font-mono);
  font-size: var(--font-size-md, 1.15rem);
  font-weight: 800;
  color: var(--color-primary);
}

.card-footer-stamp {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
  font-weight: 700;
}

/* Tarjeta Checklist */
.checklist-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-5, 1.25rem);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
}

.checklist-title {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: var(--color-text-main);
  margin: 0;
}

.checklist-items {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}

.checklist-row {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  transition: color var(--transition-fast);
}

.checklist-row.valid {
  color: var(--color-success);
  font-weight: 600;
}

.sidebar-action-buttons {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
  margin-top: var(--space-2, 0.5rem);
}

.full-width {
  width: 100%;
}

/* Layout y Campos Agrupados */
.two-cols {
  grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
}

.field-row-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4, 1rem);
}

.costs-two-cols {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4, 1rem);
}

.field-hint-mini {
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-subtle);
  margin-top: 0.35rem;
  display: block;
}

/* =========================================================================
   ESTILOS PERSONALIZADOS PARA PRIMEVUE SELECT (Conformidad reglaArq.md)
   ========================================================================= */
.sga-custom-select {
  width: 100%;
  background: var(--color-bg-surface) !important;
  border: 1px solid var(--color-border) !important;
  border-radius: var(--radius-lg, 0.75rem) !important;
  color: var(--color-text-main) !important;
  transition: all var(--transition-fast);
}

.sga-custom-select:hover {
  border-color: var(--color-border-strong) !important;
}

.sga-custom-select:focus-within {
  border-color: var(--color-primary) !important;
  box-shadow: 0 0 0 3px var(--color-border-subtle) !important;
}

.select-val-row {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.select-placeholder-text {
  color: var(--color-text-subtle);
  font-size: var(--font-size-sm, 0.875rem);
}

.select-label-text {
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 600;
  color: var(--color-text-main);
}

.badge-code-select {
  font-family: var(--font-family-mono, monospace);
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  padding: 0.15rem 0.45rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  border-radius: var(--radius-sm, 0.25rem);
}

.badge-type-pill {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 600;
  padding: 0.1rem 0.35rem;
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  border-radius: var(--radius-sm, 0.25rem);
}

.select-opt-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0.25rem 0;
  gap: var(--space-3, 0.75rem);
}

.select-opt-row .opt-left {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  overflow: hidden;
}

.select-opt-row .opt-text {
  font-size: var(--font-size-sm, 0.875rem);
  color: inherit;
}

.merc-opt-row .opt-text-col {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.opt-title-text {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: inherit;
}

.opt-category-sub {
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
}

/* =========================================================================
   ALERTAS DE MERCANCÍA: RESTRINGIDA vs LIBRE (Regla 1 & Regla 4 reglaArq.md)
   ========================================================================= */
.mercancia-status-panel {
  margin: var(--space-2, 0.5rem) 0 var(--space-4, 1rem);
}

.regulatory-warning-banner {
  display: flex;
  gap: var(--space-4, 1rem);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-left: 5px solid var(--color-warning-text);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-4, 1rem);
  color: var(--color-warning-text);
}

.warning-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-surface);
  color: var(--color-warning-text);
  font-size: 1.25rem;
  flex-shrink: 0;
  border: 1px solid var(--color-warning-border);
}

.warning-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.warning-kicker {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-warning-text);
  text-transform: uppercase;
}

.warning-title {
  margin: 0;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.warning-desc {
  margin: 0;
  font-size: var(--font-size-xs, 0.75rem);
  line-height: 1.45;
  color: var(--color-text-muted);
}

.warning-meta-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.meta-pill {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 600;
  background: var(--color-bg-surface);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
  padding: 0.2rem 0.5rem;
  border-radius: 9999px;
}

.free-goods-banner {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  border-left: 4px solid var(--color-success-text);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 0.75rem 1rem;
  color: var(--color-success-text);
  font-size: var(--font-size-xs, 0.75rem);
}

.free-content {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

/* =========================================================================
   SELECTOR DE TRANSPORTE TERRESTRE SGA (Regla 3 reglaArq.md)
   ========================================================================= */
.transport-option-box {
  margin-top: var(--space-2, 0.5rem);
}

.transport-helper {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin-bottom: var(--space-3, 0.75rem);
}

.transport-toggle-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4, 1rem);
}

.transport-card-opt {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-4, 1rem);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  background: var(--color-bg-surface);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

.transport-card-opt:hover {
  border-color: var(--color-border-strong);
  background: var(--color-bg-subtle);
  transform: translateY(-1px);
}

.transport-card-opt.selected {
  border-color: var(--color-primary);
  background: var(--color-bg-subtle);
  box-shadow: var(--shadow-sm);
}

.opt-icon {
  font-size: 1.25rem;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-muted);
  flex-shrink: 0;
}

.opt-info {
  flex: 1;
}

.opt-title {
  margin: 0 0 0.25rem 0;
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.opt-desc {
  margin: 0;
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
  line-height: 1.35;
}

.check-active {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  color: var(--color-primary);
  font-size: 1rem;
}

/* =========================================================================
   PANEL CONFIGURACIÓN DE RUTA DE TRANSPORTE INTERNO SGA
   ========================================================================= */
.transport-route-config-panel {
  margin-top: var(--space-4, 1rem);
  padding: var(--space-5, 1.25rem);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-xl, 1rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
  box-shadow: var(--shadow-sm);
}

.route-panel-header {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
}

.route-badge-icon {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-md, 0.5rem);
  background: rgba(194, 65, 12, 0.12);
  color: #c2410c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.15rem;
  flex-shrink: 0;
}

.route-panel-title {
  margin: 0;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 800;
  color: var(--color-text-main);
}

.route-panel-desc {
  margin: 0.15rem 0 0 0;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
}

.route-inputs-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4, 1rem);
}

@media (max-width: 768px) {
  .route-inputs-grid {
    grid-template-columns: 1fr;
  }
}

.route-location-card {
  padding: var(--space-4, 1rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-3, 0.75rem);
}

.loc-card-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  letter-spacing: 0.05em;
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-full, 9999px);
  width: fit-content;
}

.loc-origin {
  background: rgba(14, 165, 233, 0.1);
  color: #0284c7;
  border: 1px solid rgba(14, 165, 233, 0.25);
}

.loc-dest {
  background: rgba(234, 88, 12, 0.1);
  color: #ea580c;
  border: 1px solid rgba(234, 88, 12, 0.25);
}

.loc-fields-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3, 0.75rem);
}

@media (max-width: 480px) {
  .loc-fields-row {
    grid-template-columns: 1fr;
  }
}

.field-subgroup {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.loc-label {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.route-cost-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4, 1rem);
  padding: var(--space-4, 1rem) var(--space-5, 1.25rem);
  background: var(--color-bg-surface);
  border: 1px dashed var(--color-border-strong);
  border-radius: var(--radius-lg, 0.75rem);
  flex-wrap: wrap;
}

.cost-title-box {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  color: var(--color-text-main);
}

.cost-title-box i {
  color: var(--color-success);
  font-size: 1.1rem;
}

.route-cost-label {
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 800;
  color: var(--color-text-main);
}

.route-cost-sub {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  display: block;
  margin-top: 0.15rem;
}

.route-cost-input-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  min-width: 220px;
}

.currency-tag {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-muted);
}

.cost-input {
  font-weight: 800;
  text-align: right;
  font-size: var(--font-size-base, 1rem);
}

.transport-summary-row-box {
  background: var(--color-bg-surface);
  border: 1px solid rgba(194, 65, 12, 0.25);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-4, 1rem);
}

/* =========================================================================
   TARJETA DESGLOSE DE TRIBUTOS SUNAT (Regla 1 & Regla 3 reglaArq.md)
   ========================================================================= */
.tributos-breakdown-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-5, 1.25rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-4, 1rem);
  box-shadow: var(--shadow-sm);
}

.tributos-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-3, 0.75rem);
  border-bottom: 1px solid var(--color-border-subtle);
  padding-bottom: var(--space-3, 0.75rem);
}

.tributos-header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
}

.header-icon {
  font-size: 1.35rem;
  color: var(--color-primary);
}

.tributos-card-title {
  margin: 0;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.tributos-card-desc {
  margin: 0.15rem 0 0 0;
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
}

.tributos-total-badge {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  padding: 0.4rem 0.75rem;
}

.badge-label {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  color: var(--color-text-muted);
}

.badge-amount {
  font-family: var(--font-mono);
  font-size: var(--font-size-md, 1.1rem);
  font-weight: 800;
  color: var(--color-primary);
}

.tributos-inputs-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4, 1rem);
}

.tributo-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.tributo-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tributo-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 600;
  color: var(--color-text-main);
}

.tributo-rate-pill {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  color: var(--color-primary);
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-sm, 0.25rem);
}

.tributo-rate-pill-optional {
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-subtle);
}

.tributo-helper {
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
  line-height: 1.25;
}

/* Styled Select and Read-only boxes for Customs calculation */
.form-select-styled {
  width: 100%;
}

.read-only-amount-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.35rem 0.5rem;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-sm, 0.25rem);
  border: 1px dashed var(--color-border);
  margin-top: 0.25rem;
}

.read-only-label {
  font-size: var(--font-size-2xs, 0.65rem);
  color: var(--color-text-muted);
}

.read-only-val {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  font-family: var(--font-mono);
  color: var(--color-text-main);
}

.read-only-fixed-rate {
  padding: 0.55rem 0.75rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  display: flex;
  align-items: center;
}

.fixed-rate-text {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
}

/* Seguro Helper Row & Reset */
.seguro-helper-row {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-top: 0.25rem;
}

.btn-reset-seguro {
  align-self: flex-start;
  background: transparent;
  border: none;
  color: var(--color-primary);
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  cursor: pointer;
  text-decoration: underline;
  padding: 0;
  margin-top: 0.15rem;
}

.btn-reset-seguro:hover {
  opacity: 0.85;
}

/* CIF / Base Valuation Box */
.cif-display-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.55rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  min-height: 44px;
}

.cif-info-left {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.cif-badge-label {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-primary);
}

.cif-amount {
  font-size: var(--font-size-base, 1rem);
  font-weight: 800;
  font-family: var(--font-mono);
  color: var(--color-text-main);
}

.cif-currency {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 600;
  color: var(--color-text-muted);
  margin-left: 0.2rem;
}

.cif-icon {
  font-size: 1.35rem;
  color: var(--color-primary);
}

/* Export Inafecto Banner */
.export-inafecto-banner {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4, 1rem);
  padding: var(--space-5, 1.25rem);
  background: rgba(59, 130, 246, 0.06);
  border: 1.5px solid rgba(59, 130, 246, 0.25);
  border-radius: var(--radius-xl, 1rem);
}

.inafecto-icon-box {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-md, 0.5rem);
  background: rgba(59, 130, 246, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  font-size: 1.25rem;
  flex-shrink: 0;
}

.inafecto-body {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  flex: 1;
}

.inafecto-title {
  margin: 0;
  font-size: var(--font-size-sm, 0.875rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.inafecto-desc {
  margin: 0;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  line-height: 1.4;
}

.inafecto-badge-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.4rem;
}

.inafecto-pill {
  font-size: var(--font-size-2xs, 0.65rem);
  font-weight: 700;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text-main);
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-sm, 0.25rem);
}

.inafecto-pill.total-zero {
  background: #ecfdf5;
  border-color: #10b981;
  color: #047857;
}

/* Sidebar Live Badges */
.p-val-col {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.15rem;
}

.merc-code-text {
  font-weight: 700;
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-main);
}

.badge-restringida-mini {
  font-size: var(--font-size-2xs, 0.65rem);
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
  padding: 0.1rem 0.35rem;
  border-radius: var(--radius-sm, 0.25rem);
  font-weight: 700;
}

.badge-libre-mini {
  font-size: var(--font-size-2xs, 0.65rem);
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
  padding: 0.1rem 0.35rem;
  border-radius: var(--radius-sm, 0.25rem);
  font-weight: 700;
}

.text-accent {
  color: var(--color-primary) !important;
  font-weight: 700;
}

@media (max-width: 768px) {
  .two-cols,
  .field-row-grid,
  .costs-two-cols,
  .transport-toggle-grid,
  .tributos-inputs-grid {
    grid-template-columns: 1fr !important;
  }
}
</style>
