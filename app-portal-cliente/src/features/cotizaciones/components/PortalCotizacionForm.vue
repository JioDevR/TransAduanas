<template>
  <div class="portal-cotizacion-form-view">
    <!-- Header de Acción y Título -->
    <PortalCotizacionFormHeader
      :submitting="submitting"
      :is-form-ready="isFormReady"
      @cancel="$emit('cancel')"
      @submit="handleSubmit"
    />

    <!-- Stepper de 3 Pasos B2B -->
    <PortalCotizacionStepperNav
      :current-step="currentStep"
      :form="form"
      :docs-count="queuedDocs.length"
      @update:current-step="currentStep = $event"
    />

    <!-- Layout Central de 2 Columnas: Pasos + Sidebar Proforma -->
    <div class="form-content-grid">
      <main class="form-main-column">
        <!-- ========================================================================= -->
        <!-- PASO 01: RÉGIMEN, LOGÍSTICA & MERCANCÍA -->
        <!-- ========================================================================= -->
        <section v-show="currentStep === 1" class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Régimen Aduanero, Logística & Mercancía</h3>
              <p class="block-sub">
                Configure el régimen oficial, modalidad de tráfico, ruta de terminales y mercancía para estructurar la cotización.
              </p>
            </div>
          </div>

          <div class="block-body">
            <!-- 1. Tarjetas de Régimen Aduanero -->
            <div class="field-group">
              <label class="field-label">Régimen Aduanero Oficial *</label>
              <div class="regimenes-grid two-cols">
                <div
                  v-for="reg in REGIMEN_OPTIONS"
                  :key="reg.value"
                  role="button"
                  tabindex="0"
                  :class="['regimen-card', { selected: form.regimenAduanero === reg.value }]"
                  @click="handleSelectRegimen(reg.value)"
                  @keydown.enter="handleSelectRegimen(reg.value)"
                  @keydown.space.prevent="handleSelectRegimen(reg.value)"
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

            <!-- 2. Tarjetas de Modalidad de Transporte -->
            <div class="field-group modalidad-selector-box">
              <div class="label-with-badge">
                <label class="field-label">Modalidad de Transporte Internacional *</label>
                <span class="kicker-mini-badge">
                  {{ modalidadTransporte === 'AEREO' ? 'VÍA AÉREA' : 'VÍA MARÍTIMA' }}
                </span>
              </div>
              <p class="field-helper-text">
                Seleccione el modo de tráfico para filtrar los terminales portuarios o aeropuertos disponibles.
              </p>

              <div class="modalidad-toggle-grid">
                <button
                  type="button"
                  :class="['btn-modalidad-card', { active: modalidadTransporte === 'MARITIMO' }]"
                  @click="setModalidad('MARITIMO')"
                >
                  <div class="modalidad-icon">
                    <i class="pi pi-compass"></i>
                  </div>
                  <div class="modalidad-info">
                    <span class="modalidad-title">Carga Marítima</span>
                    <span class="modalidad-sub">Puertos oceánicos, terminales y muelles de atraque</span>
                  </div>
                  <i v-if="modalidadTransporte === 'MARITIMO'" class="pi pi-check-circle modalidad-active-check"></i>
                </button>

                <button
                  type="button"
                  :class="['btn-modalidad-card', { active: modalidadTransporte === 'AEREO' }]"
                  @click="setModalidad('AEREO')"
                >
                  <div class="modalidad-icon">
                    <i class="pi pi-send"></i>
                  </div>
                  <div class="modalidad-info">
                    <span class="modalidad-title">Carga Aérea</span>
                    <span class="modalidad-sub">Aeropuertos internacionales y terminales de carga aérea</span>
                  </div>
                  <i v-if="modalidadTransporte === 'AEREO'" class="pi pi-check-circle modalidad-active-check"></i>
                </button>
              </div>
            </div>

            <!-- 3. Cascada País -> Puerto/Aeropuerto Origen y Destino -->
            <div class="ruta-logistica-grid">
              <!-- Bloque Origen -->
              <div class="ruta-card-block">
                <div class="ruta-block-header">
                  <span class="ruta-badge-badge">ORIGEN / SALIDA</span>
                  <span v-if="form.regimenAduanero === 'EXPORTACION_DEFINITIVA'" class="badge-peru-default">Default Perú</span>
                </div>

                <div class="ruta-fields-stack">
                  <div class="field-group">
                    <label for="portal-pais-envio" class="field-label">País de Origen *</label>
                    <Select
                      id="portal-pais-envio"
                      v-model="paisOrigen"
                      :options="paisesOrigenDisponibles"
                      filter
                      filterPlaceholder="Buscar país de salida..."
                      placeholder="Seleccione país de salida..."
                      class="sga-custom-select"
                      :disabled="submitting || loadingCatalogos"
                    />
                    <span class="field-hint-mini">País donde se ubica el terminal de despacho</span>
                  </div>

                  <div class="field-group">
                    <label for="portal-puerto-envio" class="field-label">
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
                      :filterPlaceholder="modalidadTransporte === 'AEREO' ? 'Buscar aeropuerto o código...' : 'Buscar puerto o código...'"
                      :placeholder="!paisOrigen ? 'Primero seleccione país de salida' : (modalidadTransporte === 'AEREO' ? 'Seleccione aeropuerto de salida...' : 'Seleccione puerto de salida...')"
                      class="sga-custom-select"
                      :disabled="submitting || loadingCatalogos || !paisOrigen"
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
                  </div>
                </div>
              </div>

              <!-- Bloque Destino -->
              <div class="ruta-card-block destination">
                <div class="ruta-block-header">
                  <span class="ruta-badge-badge destination">DESTINO / ARRIBO</span>
                  <span v-if="form.regimenAduanero === 'IMPORTACION_DEFINITIVA'" class="badge-peru-default">Default Perú</span>
                </div>

                <div class="ruta-fields-stack">
                  <div class="field-group">
                    <label for="portal-pais-destino" class="field-label">País de Destino *</label>
                    <Select
                      id="portal-pais-destino"
                      v-model="paisDestino"
                      :options="paisesDestinoDisponibles"
                      filter
                      filterPlaceholder="Buscar país de arribo..."
                      placeholder="Seleccione país de arribo..."
                      class="sga-custom-select"
                      :disabled="submitting || loadingCatalogos"
                    />
                    <span class="field-hint-mini">País de la aduana o terminal de llegada</span>
                  </div>

                  <div class="field-group">
                    <label for="portal-puerto-destino" class="field-label">
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
                      :filterPlaceholder="modalidadTransporte === 'AEREO' ? 'Buscar aeropuerto o código...' : 'Buscar puerto o código...'"
                      :placeholder="!paisDestino ? 'Primero seleccione país de arribo' : (modalidadTransporte === 'AEREO' ? 'Seleccione aeropuerto de arribo...' : 'Seleccione puerto de arribo...')"
                      class="sga-custom-select"
                      :disabled="submitting || loadingCatalogos || !paisDestino"
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
                  </div>
                </div>
              </div>
            </div>

            <!-- 4. Tipo de Carga y Mercancía -->
            <div class="field-row-grid two-cols">
              <div class="field-group">
                <label for="portal-tipo-carga" class="field-label">Tipo de Carga / Unidad Logística</label>
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
                      <div class="opt-left">
                        <span class="badge-code-select">{{ slotProps.option.codigo }}</span>
                        <div class="opt-text-col">
                          <span class="opt-title-text">{{ slotProps.option.nombre }}</span>
                          <span v-if="slotProps.option.descripcion" class="opt-category-sub">
                            {{ slotProps.option.descripcion }}
                          </span>
                        </div>
                      </div>
                    </div>
                  </template>
                </Select>
                <span class="field-hint-mini">Modalidad de estiba y transporte de la mercancía</span>

                <!-- Callout descriptivo si se selecciona tipo de carga -->
                <transition name="fade">
                  <div v-if="selectedTipoCarga?.descripcion" class="tipo-carga-desc-callout">
                    <div class="callout-icon-box">
                      <i class="pi pi-box"></i>
                    </div>
                    <div class="callout-body">
                      <div class="callout-header-row">
                        <span class="callout-tag font-mono">{{ selectedTipoCarga.codigo }}</span>
                        <span class="callout-kicker">ESPECIFICACIÓN OPERATIVA</span>
                      </div>
                      <p class="callout-desc">{{ selectedTipoCarga.descripcion }}</p>
                    </div>
                  </div>
                </transition>
              </div>

              <div class="field-group">
                <label for="portal-mercancia" class="field-label">Mercancía / Partida Arancelaria</label>
                <Select
                  id="portal-mercancia"
                  v-model="form.idMercancia"
                  :options="mercancias"
                  optionLabel="nombre"
                  optionValue="idMercancia"
                  filter
                  filterMatchMode="smart"
                  :filterFields="['searchCorpus', 'codigo', 'nombre', 'categoria', 'entidadReguladora']"
                  filterPlaceholder="Buscar por código, nombre o sector..."
                  placeholder="Seleccione mercancía clasificada..."
                  class="sga-custom-select"
                  :disabled="submitting || loadingCatalogos"
                >
                  <template #value="slotProps">
                    <div v-if="selectedMercancia" class="select-val-row">
                      <span class="badge-code-select">{{ selectedMercancia.codigo }}</span>
                      <span class="select-label-text">{{ selectedMercancia.nombre }}</span>
                      <Tag
                        v-if="selectedMercancia.requierePermiso"
                        :value="'Req: ' + (selectedMercancia.entidadReguladora || 'Permiso')"
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
                    <div class="select-opt-row">
                      <div class="opt-left">
                        <span class="badge-code-select">{{ slotProps.option.codigo }}</span>
                        <div class="opt-text-col">
                          <span class="opt-title-text">{{ slotProps.option.nombre }}</span>
                          <span class="opt-category-sub">{{ slotProps.option.categoria }}</span>
                        </div>
                      </div>
                      <Tag
                        v-if="slotProps.option.requierePermiso"
                        :value="'Req: ' + (slotProps.option.entidadReguladora || 'Permiso')"
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
                <span class="field-hint-mini">Partida arancelaria correspondiente según SUNAT</span>

                <!-- Banner Informativo de Mercancía: Libre vs Controlada -->
                <transition name="fade">
                  <div v-if="selectedMercancia" class="mercancia-status-panel">
                    <div v-if="selectedMercancia.requierePermiso" class="regulatory-warning-banner">
                      <div class="warning-icon-box">
                        <i class="pi pi-exclamation-triangle"></i>
                      </div>
                      <div class="warning-content">
                        <div class="warning-kicker">MERCANCÍA CON CONTROL REGULATORIO SECTORIAL</div>
                        <h4 class="warning-title">
                          Requiere Permiso / Visto Bueno de: {{ selectedMercancia.entidadReguladora || 'Entidad Reguladora' }}
                        </h4>
                        <p class="warning-desc">
                          La subpartida <strong>{{ selectedMercancia.codigo }}</strong> exige autorización previa.
                          Recuerde adjuntar la resolución o permiso correspondiente en el <strong>Paso 02</strong>.
                        </p>
                      </div>
                    </div>
                    <div v-else class="free-goods-banner">
                      <i class="pi pi-check-circle"></i>
                      <div class="free-content">
                        <strong>Mercancía de Libre Trámite:</strong>
                        <span>La partida {{ selectedMercancia.codigo }} no está sujeta a permisos sectoriales previos para su despacho.</span>
                      </div>
                    </div>
                  </div>
                </transition>
              </div>
            </div>

            <!-- 5. Tarjetas de Transporte Terrestre Local SGA -->
            <div class="field-group transport-option-box">
              <div class="label-with-badge">
                <label class="field-label">Servicio de Transporte Terrestre Local SGA *</label>
                <span class="kicker-mini-badge">FLOTA SGA</span>
              </div>
              <p class="field-helper-text">
                ¿Requiere traslado terrestre con flota monitoreada de TransAduanas hasta almacén o planta?
              </p>

              <div class="transport-toggle-grid">
                <div
                  role="button"
                  tabindex="0"
                  :class="['transport-card-opt', { selected: form.servicioTransporte === true }]"
                  @click="form.servicioTransporte = true"
                  @keydown.enter="form.servicioTransporte = true"
                  @keydown.space.prevent="form.servicioTransporte = true"
                >
                  <div class="opt-icon"><i class="pi pi-truck"></i></div>
                  <div class="opt-info">
                    <h4 class="opt-title">Sí, Transporte Interno SGA</h4>
                    <p class="opt-desc">Traslado desde terminal portuario/almacén hasta su destino final en Perú.</p>
                  </div>
                  <i v-if="form.servicioTransporte === true" class="pi pi-check-circle check-active"></i>
                </div>

                <div
                  role="button"
                  tabindex="0"
                  :class="['transport-card-opt', { selected: form.servicioTransporte === false }]"
                  @click="form.servicioTransporte = false"
                  @keydown.enter="form.servicioTransporte = false"
                  @keydown.space.prevent="form.servicioTransporte = false"
                >
                  <div class="opt-icon"><i class="pi pi-building"></i></div>
                  <div class="opt-info">
                    <h4 class="opt-title">No requerido</h4>
                    <p class="opt-desc">El retiro y flete terrestre será gestionado por transportistas propios del cliente.</p>
                  </div>
                  <i v-if="form.servicioTransporte === false" class="pi pi-check-circle check-active"></i>
                </div>
              </div>

              <!-- Selectores Ubigeo si servicioTransporte es true (Sin campo de tarifa) -->
              <transition name="fade-slide">
                <div v-if="form.servicioTransporte" class="transport-route-config-panel">
                  <div class="route-panel-header">
                    <div class="route-badge-icon">
                      <i class="pi pi-map"></i>
                    </div>
                    <div>
                      <h4 class="route-panel-title">Ruta Nacional de Transporte Terrestre</h4>
                      <p class="route-panel-desc">
                        Especifique el origen y destino del traslado. La cotización del flete terrestre local será asignada por su asesor comercial tras evaluar la ruta.
                      </p>
                    </div>
                  </div>

                  <div class="route-inputs-grid">
                    <!-- ORIGEN LOCAL -->
                    <div class="route-location-card">
                      <div class="loc-card-tag loc-origin">
                        <i class="pi pi-compass"></i>
                        <span>PUNTO DE RECOJO (ORIGEN)</span>
                      </div>
                      <div class="loc-fields-row">
                        <div class="field-subgroup">
                          <label for="portal-orig-dep" class="loc-label">Departamento *</label>
                          <select
                            id="portal-orig-dep"
                            v-model="form.origenDepartamento"
                            class="form-select"
                            :disabled="submitting"
                          >
                            <option v-for="dep in departamentosPeru" :key="'orig-dep-' + dep" :value="dep">
                              {{ dep }}
                            </option>
                          </select>
                        </div>
                        <div class="field-subgroup">
                          <label for="portal-orig-prov" class="loc-label">Provincia *</label>
                          <select
                            id="portal-orig-prov"
                            v-model="form.origenProvincia"
                            class="form-select"
                            :disabled="submitting || !form.origenDepartamento"
                          >
                            <option v-for="prov in provinciasOrigen" :key="'orig-prov-' + prov" :value="prov">
                              {{ prov }}
                            </option>
                          </select>
                        </div>
                      </div>
                    </div>

                    <!-- DESTINO LOCAL -->
                    <div class="route-location-card">
                      <div class="loc-card-tag loc-dest">
                        <i class="pi pi-map-marker"></i>
                        <span>PUNTO DE ENTREGA (DESTINO)</span>
                      </div>
                      <div class="loc-fields-row">
                        <div class="field-subgroup">
                          <label for="portal-dest-dep" class="loc-label">Departamento *</label>
                          <select
                            id="portal-dest-dep"
                            v-model="form.destinoDepartamento"
                            class="form-select"
                            :disabled="submitting"
                          >
                            <option v-for="dep in departamentosPeru" :key="'dest-dep-' + dep" :value="dep">
                              {{ dep }}
                            </option>
                          </select>
                        </div>
                        <div class="field-subgroup">
                          <label for="portal-dest-prov" class="loc-label">Provincia *</label>
                          <select
                            id="portal-dest-prov"
                            v-model="form.destinoProvincia"
                            class="form-select"
                            :disabled="submitting || !form.destinoDepartamento"
                          >
                            <option v-for="prov in provinciasDestino" :key="'dest-prov-' + prov" :value="prov">
                              {{ prov }}
                            </option>
                          </select>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </transition>
            </div>

            <!-- 6. Validez de la Solicitud (Días Calendario con Chips) -->
            <div class="field-group highlight-group">
              <div class="label-with-badge">
                <label for="portal-validez" class="field-label">Validez Deseada de la Propuesta (Días Calendario) *</label>
                <span class="kicker-mini-badge">PLAZO VIGENCIA</span>
              </div>
              <div class="input-icon-box">
                <i class="pi pi-calendar input-icon"></i>
                <input
                  id="portal-validez"
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
                Tiempo de vigencia requerido para evaluar y autorizar la propuesta técnica cotizada.
              </p>

              <div class="quick-shortcuts-row">
                <span class="shortcuts-label">Plazos frecuentes:</span>
                <button
                  type="button"
                  :class="['shortcut-chip', { active: form.validezDias === 15 }]"
                  @click="form.validezDias = 15"
                >
                  15 Días
                </button>
                <button
                  type="button"
                  :class="['shortcut-chip', { active: form.validezDias === 30 }]"
                  @click="form.validezDias = 30"
                >
                  30 Días
                </button>
                <button
                  type="button"
                  :class="['shortcut-chip', { active: form.validezDias === 45 }]"
                  @click="form.validezDias = 45"
                >
                  45 Días
                </button>
                <button
                  type="button"
                  :class="['shortcut-chip', { active: form.validezDias === 60 }]"
                  @click="form.validezDias = 60"
                >
                  60 Días
                </button>
              </div>
            </div>

            <!-- 7. Observaciones del Cliente -->
            <div class="field-group">
              <label for="portal-obs" class="field-label">Observaciones, Instrucciones o Requerimientos Especiales</label>
              <textarea
                id="portal-obs"
                v-model="form.observaciones"
                rows="3"
                placeholder="Indique requerimientos específicos de estiba, dimensiones especiales, fechas estimadas de arribo..."
                class="form-textarea"
                :disabled="submitting"
              ></textarea>
            </div>

            <!-- Botones de Navegación del Paso 1 -->
            <div class="step-footer-actions">
              <div></div>
              <button
                type="button"
                class="btn-espresso"
                @click="currentStep = 2"
              >
                <span>Siguiente: Documentación Sustentatoria</span>
                <i class="pi pi-arrow-right"></i>
              </button>
            </div>
          </div>
        </section>

        <!-- ========================================================================= -->
        <!-- PASO 02: DOCUMENTACIÓN SUSTENTATORIA -->
        <!-- ========================================================================= -->
        <section v-show="currentStep === 2" class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Documentación Técnica Sustentatoria</h3>
              <p class="block-sub">
                Adjunte facturas comerciales, listas de empaque, permisos o borradores de BL/AWB para que el asesor liquide los tributos y costos operativos.
              </p>
            </div>
          </div>

          <div class="block-body">
            <PortalDocumentosUploadPanel
              mode="deferred"
              v-model="queuedDocs"
              :permiso-requerido="selectedMercancia?.requierePermiso || false"
              :entidad-reguladora="selectedMercancia?.entidadReguladora || ''"
            />

            <!-- Botones de Navegación del Paso 2 -->
            <div class="step-footer-actions">
              <button
                type="button"
                class="btn-outline-warm"
                @click="currentStep = 1"
              >
                <i class="pi pi-arrow-left"></i>
                <span>Anterior: Régimen & Logística</span>
              </button>
              <button
                type="button"
                class="btn-espresso"
                @click="currentStep = 3"
              >
                <span>Siguiente: Valores Declarados</span>
                <i class="pi pi-arrow-right"></i>
              </button>
            </div>
          </div>
        </section>

        <!-- ========================================================================= -->
        <!-- PASO 03: VALORES DECLARADOS Y LIQUIDACIÓN PENDIENTE -->
        <!-- ========================================================================= -->
        <section v-show="currentStep === 3" class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Valores Declarados & Estructura de Liquidación (USD)</h3>
              <p class="block-sub">
                Declare el valor comercial de la mercancía. Los costos de seguro, agenciamiento y tributos aduaneros serán determinados por su asesor comercial.
              </p>
            </div>
          </div>

          <div class="block-body">
            <!-- Sección 1: Inputs editables por el cliente -->
            <div class="costs-two-cols">
              <!-- Valor FOB Mercancía (Obligatorio) -->
              <div class="field-group highlight-group">
                <div class="label-with-badge">
                  <label for="portal-fob" class="field-label">Valor de Mercancía (FOB en Factura Comercial) *</label>
                  <span class="kicker-mini-badge">BASE COMERCIAL</span>
                </div>
                <div class="input-icon-box">
                  <i class="pi pi-money-bill input-icon"></i>
                  <input
                    id="portal-fob"
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
                <span class="field-hint-mini">Monto de la factura comercial o proforma del proveedor. Base para la valoración técnica.</span>
              </div>

              <!-- Flete Internacional Estimado (Opcional) -->
              <div class="field-group">
                <div class="label-with-badge">
                  <label for="portal-flete" class="field-label">Flete Internacional Estimado (USD)</label>
                  <span class="kicker-mini-badge">OPCIONAL</span>
                </div>
                <div class="input-icon-box">
                  <i class="pi pi-compass input-icon"></i>
                  <input
                    id="portal-flete"
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
                <span class="field-hint-mini">Flete marítimo o aéreo acordado o estimado según cotización previa de naviera/aerolínea.</span>
              </div>
            </div>

            <!-- Sección 2: Tarjetas informativas de liquidación en solo lectura ($0.00) -->
            <div class="readonly-advisory-section">
              <div class="section-micro-header">
                <i class="pi pi-lock"></i>
                <span>CONCEPTOS LIQUIDADOS POR ASESOR COMERCIAL SGA</span>
              </div>
              <p class="section-micro-desc">
                Los siguientes rubros serán determinados conforme a la tabla de tarifas corporativas, subpartida nacional y pólizas aplicables una vez recibida su solicitud.
              </p>

              <div class="advisory-cards-grid">
                <!-- Seguro Estimado -->
                <div class="advisory-card">
                  <div class="advisory-card-header">
                    <span class="advisory-card-title">Seguro de Transporte</span>
                    <span class="advisory-status-badge">Asignado por asesor</span>
                  </div>
                  <div class="advisory-amount font-mono">$ 0.00</div>
                  <span class="advisory-desc">Conforme a póliza flotante corporativa o tabla de seguro aduanero SUNAT.</span>
                </div>

                <!-- Gastos Operativos & Agencia -->
                <div class="advisory-card">
                  <div class="advisory-card-header">
                    <span class="advisory-card-title">Gastos Operativos & Agencia</span>
                    <span class="advisory-status-badge">Asignado por asesor</span>
                  </div>
                  <div class="advisory-amount font-mono">$ 0.00</div>
                  <span class="advisory-desc">Comisión de despacho aduanero y gastos portuarios/aeroportuarios aplicables.</span>
                </div>

                <!-- Flete Terrestre Local (si servicioTransporte) -->
                <div v-if="form.servicioTransporte" class="advisory-card">
                  <div class="advisory-card-header">
                    <span class="advisory-card-title">Flete Terrestre Local (SGA)</span>
                    <span class="advisory-status-badge">Asignado por asesor</span>
                  </div>
                  <div class="advisory-amount font-mono">$ 0.00</div>
                  <span class="advisory-desc">Tarifa de ruta terrestre {{ form.origenProvincia }} &rarr; {{ form.destinoProvincia }}.</span>
                </div>

                <!-- Tributos Aduaneros SUNAT -->
                <div class="advisory-card">
                  <div class="advisory-card-header">
                    <span class="advisory-card-title">Tributos Aduaneros SUNAT</span>
                    <span class="advisory-status-badge">
                      {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Inafecto' : 'Asignado por asesor' }}
                    </span>
                  </div>
                  <div class="advisory-amount font-mono">$ 0.00</div>
                  <span class="advisory-desc">
                    {{ form.regimenAduanero === 'EXPORTACION_DEFINITIVA' ? 'Régimen de exportación definitivo inafecto de tributos.' : 'Ad-Valórem arancelario, IGV, IPM y Percepción según clasificación arancelaria.' }}
                  </span>
                </div>

                <!-- Total Liquidado B2B -->
                <div class="advisory-card total-card">
                  <div class="advisory-card-header">
                    <span class="advisory-card-title">TOTAL COTIZADO ESTIMADO</span>
                    <span class="advisory-status-badge badge-pending">Pendiente de revisión comercial</span>
                  </div>
                  <div class="advisory-amount font-mono total-amount">$ 0.00</div>
                  <span class="advisory-desc">El monto total liquidado se reflejará en su panel una vez evaluada su solicitud.</span>
                </div>
              </div>

              <!-- Banner de compromiso de respuesta comercial -->
              <div class="advisory-final-note">
                <div class="note-icon"><i class="pi pi-shield"></i></div>
                <div class="note-text">
                  <strong>Evaluación Comercial Garantizada:</strong>
                  <span>TransAduanas revisará su solicitud en un plazo máximo de 24 horas hábiles y asignará un Asesor Comercial para liquidar los conceptos arancelarios y operativos correspondientes.</span>
                </div>
              </div>
            </div>

            <!-- Botones de Navegación del Paso 3 -->
            <div class="step-footer-actions">
              <button
                type="button"
                class="btn-outline-warm"
                @click="currentStep = 2"
                :disabled="submitting"
              >
                <i class="pi pi-arrow-left"></i>
                <span>Anterior: Documentación</span>
              </button>
            </div>
          </div>
        </section>
      </main>

      <!-- Sidebar Proforma Preview -->
      <PortalCotizacionProformaPreview
        :form="form"
        :empresa="empresaData"
        :selected-puerto-envio="selectedPuertoEnvio"
        :selected-puerto-destino="selectedPuertoDestino"
        :selected-tipo-carga="selectedTipoCarga"
        :selected-mercancia="selectedMercancia"
        :docs-count="queuedDocs.length"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { FilterService } from '@primevue/core/api'
import { useAuthStore } from '@/core/stores/authStore'
import { useNotification } from '@/shared/composables/useNotification'
import { portalCotizacionService } from '../services/portalCotizacionService'
import { useModalidadPuertos } from '../composables/useModalidadPuertos'
import { getDepartamentosPeru, getProvinciasPorDepartamento } from '../data/peruUbigeo'
import {
  REGIMEN_OPTIONS,
  DESCRIPCIONES_TIPO_CARGA_FALLBACK,
  matchesSmartFilter,
  createDefaultPortalSolicitudForm,
  validatePortalCotizacionForm,
  buildPortalSolicitudPayload
} from '../utils/portalCotizacionFormUtils'
import PortalCotizacionFormHeader from './PortalCotizacionFormHeader.vue'
import PortalCotizacionStepperNav from './PortalCotizacionStepperNav.vue'
import PortalDocumentosUploadPanel from './PortalDocumentosUploadPanel.vue'
import PortalCotizacionProformaPreview from './PortalCotizacionProformaPreview.vue'

// Registrar filtro inteligente en FilterService para los componentes Select de PrimeVue
FilterService.register('smart', matchesSmartFilter)

const emit = defineEmits(['cancel', 'saved'])

const authStore = useAuthStore()
const { showError, showWarn } = useNotification()

const currentStep = ref(1)
const submitting = ref(false)
const loadingCatalogos = ref(false)
const queuedDocs = ref([])
const form = ref(createDefaultPortalSolicitudForm())

const puertos = ref([])
const tiposCarga = ref([])
const mercancias = ref([])

const regimen = computed(() => form.value.regimenAduanero)

const {
  modalidadTransporte,
  paisOrigen,
  paisDestino,
  paisesOrigenDisponibles,
  paisesDestinoDisponibles,
  puertosOrigenFiltrados,
  puertosDestinoFiltrados,
  setModalidad,
  aplicarReglasRegimen
} = useModalidadPuertos({
  puertos,
  regimen,
  form
})

const departamentosPeru = getDepartamentosPeru()

const provinciasOrigen = computed(() => {
  return getProvinciasPorDepartamento(form.value.origenDepartamento)
})

const provinciasDestino = computed(() => {
  return getProvinciasPorDepartamento(form.value.destinoDepartamento)
})

watch(
  () => form.value.origenDepartamento,
  (newDep) => {
    const validProvs = getProvinciasPorDepartamento(newDep)
    if (!validProvs.includes(form.value.origenProvincia)) {
      form.value.origenProvincia = validProvs[0] || ''
    }
  }
)

watch(
  () => form.value.destinoDepartamento,
  (newDep) => {
    const validProvs = getProvinciasPorDepartamento(newDep)
    if (!validProvs.includes(form.value.destinoProvincia)) {
      form.value.destinoProvincia = validProvs[0] || ''
    }
  }
)

const selectedPuertoEnvio = computed(() => {
  return puertos.value.find((p) => p.idPuerto === form.value.idPuertoEnvio) || null
})

const selectedPuertoDestino = computed(() => {
  return puertos.value.find((p) => p.idPuerto === form.value.idPuertoDestino) || null
})

const selectedTipoCarga = computed(() => {
  return tiposCarga.value.find((t) => t.idTipoCarga === form.value.idTipoCarga) || null
})

const selectedMercancia = computed(() => {
  return mercancias.value.find((m) => m.idMercancia === form.value.idMercancia) || null
})

const empresaData = computed(() => ({
  razonSocial: authStore.empresaNombre,
  ruc: authStore.ruc,
  usuarioNombre: authStore.usuarioNombre
}))

const isFormReady = computed(() => {
  return validatePortalCotizacionForm(form.value).valid
})

const handleSelectRegimen = (regValue) => {
  if (form.value.regimenAduanero === regValue) return
  form.value.regimenAduanero = regValue
  aplicarReglasRegimen(regValue)
}

onMounted(async () => {
  loadingCatalogos.value = true
  try {
    const [pRes, tcRes, mRes] = await Promise.allSettled([
      portalCotizacionService.obtenerPuertos(true),
      portalCotizacionService.obtenerTiposCarga(true),
      portalCotizacionService.obtenerMercancias(true)
    ])

    const unwrap = (r) => (r.status === 'fulfilled' && Array.isArray(r.value) ? r.value : [])

    puertos.value = unwrap(pRes).map((p) => ({
      ...p,
      searchCorpus: `${p.codigoPuerto || ''} ${p.nombre || ''} ${p.pais || ''} ${p.tipo || ''}`
    }))

    tiposCarga.value = unwrap(tcRes).map((t) => {
      const desc = t.descripcion || DESCRIPCIONES_TIPO_CARGA_FALLBACK[t.codigo] || ''
      return {
        ...t,
        descripcion: desc,
        searchCorpus: `${t.codigo || ''} ${t.nombre || ''} ${desc}`
      }
    })

    mercancias.value = unwrap(mRes).map((m) => ({
      ...m,
      searchCorpus: `${m.codigo || ''} ${m.nombre || ''} ${m.categoria || ''} ${m.entidadReguladora || ''}`
    }))

    aplicarReglasRegimen()
  } catch (err) {
    console.error('Error cargando catálogos para portal:', err)
  } finally {
    loadingCatalogos.value = false
  }
})

const handleSubmit = async () => {
  const valResult = validatePortalCotizacionForm(form.value)
  if (!valResult.valid) {
    if (valResult.step) {
      currentStep.value = valResult.step
    }
    showWarn(valResult.error || 'Por favor verifique los datos del formulario.')
    return
  }

  submitting.value = true
  try {
    const payload = buildPortalSolicitudPayload(form.value)
    const created = await portalCotizacionService.solicitarCotizacion(payload)

    let docsResult = { uploaded: [], failed: [] }
    if (created?.idCotizacion && queuedDocs.value && queuedDocs.value.length > 0) {
      docsResult = await portalCotizacionService.subirDocumentosCotizacion(
        created.idCotizacion,
        queuedDocs.value
      )
    }

    emit('saved', { cotizacion: created, docsResult })
  } catch (err) {
    showError(err.response?.data?.message || err.message || 'Error al enviar la solicitud')
  } finally {
    submitting.value = false
  }
}

defineExpose({
  form,
  currentStep,
  queuedDocs,
  handleSubmit,
  isFormReady
})
</script>

<style scoped>
.portal-cotizacion-form-view {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  animation: fadeIn 0.2s ease-out;
}

.form-content-grid {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: var(--space-6, 1.5rem);
  align-items: start;
}

.form-main-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

.form-block-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  padding: var(--space-6, 1.5rem) var(--space-8, 2rem);
  box-shadow: var(--shadow-subtle);
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
  font-family: var(--font-mono, monospace);
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
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
  font-size: var(--font-size-md, 0.9375rem);
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: -0.01em;
  margin: 0;
}

.block-sub {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.35;
}

.block-body {
  display: flex;
  flex-direction: column;
  gap: var(--space-5, 1.25rem);
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.field-label {
  font-size: var(--font-size-xs, 0.75rem);
  font-weight: 700;
  color: var(--color-text-main);
  letter-spacing: 0.02em;
}

.field-helper-text {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0 0 0.5rem 0;
  line-height: 1.35;
}

.field-hint-mini {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  margin-top: 0.2rem;
}

.label-with-badge {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.kicker-mini-badge {
  font-size: 0.625rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: var(--color-primary);
  background: var(--color-bg-subtle);
  padding: 0.15rem 0.5rem;
  border-radius: var(--radius-xs, 4px);
  border: 1px solid var(--color-border);
}

/* Regímenes Grid */
.regimenes-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4, 1rem);
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
  transition: all var(--transition-fast, 0.15s ease);
}

.regimen-card:hover {
  border-color: var(--color-border-subtle);
  background: var(--color-bg-surface);
}

.regimen-card.selected {
  border-color: var(--color-primary);
  background: var(--color-bg-surface);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.regimen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.regimen-code-badge {
  font-family: var(--font-mono, monospace);
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
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid var(--color-border);
}

.regimen-title {
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0.2rem 0 0 0;
}

.regimen-desc {
  font-size: var(--font-size-xs, 0.75rem);
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.35;
}

/* Modalidad de Transporte */
.modalidad-toggle-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4, 1rem);
}

.btn-modalidad-card {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-4, 1rem);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  cursor: pointer;
  text-align: left;
  transition: all var(--transition-fast, 0.15s ease);
  position: relative;
}

.btn-modalidad-card:hover {
  background: var(--color-bg-surface);
  border-color: var(--color-border-subtle);
}

.btn-modalidad-card.active {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.modalidad-icon {
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 1.1rem;
  flex-shrink: 0;
}

.modalidad-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  flex-grow: 1;
}

.modalidad-title {
  font-size: var(--font-size-sm, 0.8125rem);
  font-weight: 700;
  color: var(--color-text-main);
}

.modalidad-sub {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

.modalidad-active-check {
  color: var(--color-primary);
  font-size: 1.1rem;
}

/* Rutas Logísticas Grid */
.ruta-logistica-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4, 1rem);
}

.ruta-card-block {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: var(--space-4, 1rem);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ruta-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ruta-badge-badge {
  font-size: 0.625rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  padding: 0.15rem 0.5rem;
  border-radius: var(--radius-xs, 4px);
}

.badge-peru-default {
  font-size: 0.625rem;
  font-weight: 700;
  color: var(--color-success-text, #1e7e34);
  background: var(--color-success-bg, #e8f5e9);
  border: 1px solid var(--color-success-border, #c8e6c9);
  padding: 0.1rem 0.45rem;
  border-radius: var(--radius-pill, 9999px);
}

.ruta-fields-stack {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

/* Custom Select & PrimeVue Select override */
.sga-custom-select {
  width: 100%;
}

.select-val-row,
.select-opt-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  width: 100%;
}

.opt-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.opt-text-col {
  display: flex;
  flex-direction: column;
}

.opt-title-text {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.opt-category-sub {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

.badge-code-select {
  font-family: var(--font-mono, monospace);
  font-size: 0.65rem;
  font-weight: 700;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 0.1rem 0.4rem;
  border-radius: var(--radius-xs, 4px);
  color: var(--color-primary);
}

.badge-type-pill {
  font-size: 0.625rem;
  font-weight: 700;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  padding: 0.1rem 0.35rem;
  border-radius: var(--radius-xs, 4px);
}

.field-row-grid.two-cols {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4, 1rem);
}

/* Callout Tipo de Carga */
.tipo-carga-desc-callout {
  margin-top: 0.5rem;
  padding: 0.75rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-left: 3px solid var(--color-primary);
  border-radius: var(--radius-md, 0.5rem);
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
}

.callout-icon-box {
  color: var(--color-primary);
  font-size: 1rem;
  margin-top: 0.15rem;
}

.callout-body {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.callout-header-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.callout-tag {
  font-size: 0.65rem;
  font-weight: 800;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  padding: 0.05rem 0.35rem;
  border-radius: 4px;
}

.callout-kicker {
  font-size: 0.6rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-text-muted);
}

.callout-desc {
  font-size: 0.75rem;
  color: var(--color-text-main);
  margin: 0;
  line-height: 1.35;
}

/* Banners Mercancía */
.mercancia-status-panel {
  margin-top: 0.5rem;
}

.regulatory-warning-banner {
  padding: 0.75rem 0.85rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-radius: var(--radius-md, 0.5rem);
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
}

.warning-icon-box {
  color: var(--color-warning-text);
  font-size: 1.15rem;
  margin-top: 0.1rem;
}

.warning-kicker {
  font-size: 0.6rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: var(--color-warning-text);
}

.warning-title {
  font-size: 0.8rem;
  font-weight: 700;
  margin: 0.15rem 0;
  color: var(--color-text-main);
}

.warning-desc {
  font-size: 0.725rem;
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.35;
}

.free-goods-banner {
  padding: 0.6rem 0.85rem;
  background: var(--color-success-bg, #e8f5e9);
  border: 1px solid var(--color-success-border, #c8e6c9);
  border-radius: var(--radius-md, 0.5rem);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-main);
}

.free-goods-banner i {
  color: var(--color-success-text, #2e7d32);
}

/* Transporte Terrestre Local */
.transport-option-box {
  border-top: 1px solid var(--color-border-subtle);
  padding-top: var(--space-4, 1rem);
}

.transport-toggle-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4, 1rem);
}

.transport-card-opt {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.transport-card-opt:hover {
  background: var(--color-bg-surface);
}

.transport-card-opt.selected {
  background: var(--color-bg-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 1px var(--color-primary);
}

.opt-icon {
  width: 2rem;
  height: 2rem;
  border-radius: var(--radius-md, 0.5rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 1rem;
  flex-shrink: 0;
}

.opt-info {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  flex-grow: 1;
}

.opt-title {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.opt-desc {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.3;
}

.check-active {
  color: var(--color-primary);
  font-size: 1rem;
}

/* Panel Ubigeo Transporte */
.transport-route-config-panel {
  margin-top: 0.75rem;
  padding: 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.route-panel-header {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
}

.route-badge-icon {
  color: var(--color-primary);
  font-size: 1.1rem;
  margin-top: 0.15rem;
}

.route-panel-title {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.route-panel-desc {
  font-size: 0.725rem;
  color: var(--color-text-muted);
  margin: 0.15rem 0 0 0;
}

.route-inputs-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4, 1rem);
}

.route-location-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  padding: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.loc-card-tag {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.625rem;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.loc-origin {
  color: var(--color-primary);
}

.loc-dest {
  color: var(--color-accent-kicker);
}

.loc-fields-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.5rem;
}

.field-subgroup {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.loc-label {
  font-size: 0.6875rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.form-select {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  padding: 0.45rem 2.25rem 0.45rem 0.65rem;
  font-size: 0.75rem;
  background-color: var(--color-bg-surface);
  color: var(--color-text-main);
  outline: none;
}

/* Quick shortcuts chips */
.quick-shortcuts-row {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  margin-top: 0.4rem;
  flex-wrap: wrap;
}

.shortcuts-label {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

.shortcut-chip {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 9999px);
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.shortcut-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.shortcut-chip.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-text);
}

.form-textarea {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  padding: 0.65rem;
  font-family: inherit;
  font-size: 0.8125rem;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  outline: none;
  resize: vertical;
}

/* Step Footer Navigation */
.step-footer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--space-4, 1rem);
  border-top: 1px solid var(--color-border-subtle);
  margin-top: var(--space-4, 1rem);
}

/* Inputs con Icono */
.costs-two-cols {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4, 1rem);
}

.input-icon-box {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 0.75rem;
  color: var(--color-text-muted);
  font-size: 0.875rem;
}

.form-input {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  padding: 0.55rem 3.5rem 0.55rem 2.25rem;
  font-size: 0.875rem;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  outline: none;
  transition: border-color var(--transition-fast, 0.15s ease);
}

.form-input.number-input {
  font-family: var(--font-mono, monospace);
  font-weight: 600;
}

.form-input:focus {
  border-color: var(--color-primary);
}

.input-suffix {
  position: absolute;
  right: 0.75rem;
  font-size: 0.7rem;
  font-weight: 700;
  color: var(--color-text-muted);
  letter-spacing: 0.05em;
}

/* Readonly Advisory Section */
.readonly-advisory-section {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  border-top: 1px solid var(--color-border-subtle);
  padding-top: 1.25rem;
  margin-top: 0.5rem;
}

.section-micro-header {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.725rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-primary);
}

.section-micro-desc {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.35;
}

.advisory-cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.85rem;
}

.advisory-card {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 0.85rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.advisory-card.total-card {
  grid-column: span 2;
  background: var(--color-bg-surface);
  border: 2px dashed var(--color-primary);
}

.advisory-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.advisory-card-title {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.advisory-status-badge {
  font-size: 0.625rem;
  font-weight: 700;
  color: var(--color-text-muted);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  padding: 0.1rem 0.45rem;
  border-radius: var(--radius-pill, 9999px);
}

.advisory-status-badge.badge-pending {
  color: var(--color-warning-text);
  background: var(--color-warning-bg);
  border-color: var(--color-warning-border);
}

.advisory-amount {
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-text-muted);
}

.advisory-amount.total-amount {
  font-size: 1.35rem;
  color: var(--color-primary);
}

.advisory-desc {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  line-height: 1.3;
}

.advisory-final-note {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  font-size: 0.75rem;
  color: var(--color-text-main);
  line-height: 1.4;
}

.note-icon {
  color: var(--color-primary);
  font-size: 1.25rem;
  margin-top: 0.1rem;
}

.note-text {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

/* Modo Oscuro adaptado */
.dark-theme .form-block-card,
.dark-theme .advisory-card.total-card,
.dark-theme .transport-card-opt.selected {
  background: var(--color-bg-surface);
}

.dark-theme .block-idx,
.dark-theme .regimen-card,
.dark-theme .btn-modalidad-card,
.dark-theme .ruta-card-block,
.dark-theme .transport-card-opt,
.dark-theme .transport-route-config-panel,
.dark-theme .advisory-card,
.dark-theme .advisory-final-note {
  background: var(--color-bg-subtle);
}

@media (max-width: 1080px) {
  .form-content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .regimenes-grid,
  .modalidad-toggle-grid,
  .ruta-logistica-grid,
  .field-row-grid.two-cols,
  .transport-toggle-grid,
  .route-inputs-grid,
  .costs-two-cols,
  .advisory-cards-grid {
    grid-template-columns: 1fr;
  }

  .advisory-card.total-card {
    grid-column: span 1;
  }
}
</style>
