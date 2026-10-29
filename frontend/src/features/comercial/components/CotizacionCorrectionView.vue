<template>
  <div class="cotizacion-correction-view fade-in">
    <!-- 1. Header Fijo de Control y Acciones -->
    <header class="correction-header-bar">
      <div class="header-left">
        <button type="button" class="btn-back-action" @click="$emit('cancel')">
          <i class="pi pi-arrow-left"></i>
          <span>Volver</span>
        </button>
        <div class="header-titles">
          <div class="badges-row">
            <span class="badge-code">{{ form.codigoCotizacion || 'COT-SIN-CODIGO' }}</span>
            <span class="badge-status-observed">
              <i class="pi pi-exclamation-circle"></i>
              <span>OBSERVADA / EN REVISIÓN</span>
            </span>
          </div>
          <h2 class="header-main-title">Corrección de Propuesta Comercial</h2>
          <p class="header-subtitle">
            Ajuste los parámetros técnicos y económicos observados por Supervisión Aduanera en una sola vista.
          </p>
        </div>
      </div>

      <div class="header-right-actions">
        <button
          type="button"
          class="btn-cancel-flat"
          :disabled="submitting"
          @click="$emit('cancel')"
        >
          Cancelar
        </button>
        <button
          type="button"
          class="btn-submit-correction"
          :disabled="submitting"
          @click="handleSubmitCorrection"
        >
          <i v-if="submitting" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-send"></i>
          <span>{{ submitting ? 'Guardando...' : 'Guardar y Reenviar a Supervisión' }}</span>
        </button>
      </div>
    </header>

    <!-- Banner de Error si la validación falla -->
    <div v-if="validationError" class="error-banner">
      <i class="pi pi-exclamation-triangle"></i>
      <div class="error-content">
        <strong>Error de validación:</strong>
        <span>{{ validationError }}</span>
      </div>
      <button type="button" class="error-close-btn" @click="validationError = ''">
        <i class="pi pi-times"></i>
      </button>
    </div>

    <!-- 2. Banner Destacado de Observación Técnica -->
    <section class="observation-master-banner">
      <div class="obs-banner-icon-col">
        <div class="obs-icon-circle">
          <i class="pi pi-exclamation-triangle"></i>
        </div>
      </div>
      <div class="obs-banner-content">
        <div class="obs-kicker-row">
          <span class="obs-kicker">OBSERVACIÓN TÉCNICA DE SUPERVISIÓN ADUANERA</span>
          <span class="obs-cliente-tag">{{ form.clienteRazonSocial || 'Cliente no especificado' }}</span>
        </div>
        <h3 class="obs-title">Motivo de Devolución para Subsanación:</h3>
        <p class="obs-reason-text">
          "{{ motivoObservacion || 'Se requieren correcciones y precisiones técnicas antes de la aprobación definitiva.' }}"
        </p>
        <div class="obs-instruction-hint">
          <i class="pi pi-info-circle"></i>
          <span>Modifique los campos correspondientes a continuación. Al presionar "Guardar y Reenviar", la cotización regresará automáticamente a la bandeja de Supervisión Aduanera como <strong>ENVIADA</strong>.</span>
        </div>
      </div>
    </section>

    <!-- 3. Formulario Unificado en Pantalla Continua (2 Columnas) -->
    <div class="correction-main-grid">
      <!-- ======================================================== -->
      <!-- COLUMNA 1: LOGÍSTICA, RUTAS Y PARÁMETROS ADUANEROS       -->
      <!-- ======================================================== -->
      <div class="grid-column">
        <!-- Tarjeta A: Identificación de Cliente y Régimen -->
        <div class="correction-card">
          <div class="card-header">
            <div class="card-icon-tag"><i class="pi pi-id-card"></i></div>
            <div>
              <h4 class="card-title">1. Cliente y Régimen Aduanero</h4>
              <p class="card-subtitle">Datos de la empresa importadora/exportadora y tratamiento legal</p>
            </div>
          </div>

          <div class="card-body">
            <!-- Info de Cliente -->
            <div class="client-summary-box">
              <div class="client-meta">
                <span class="client-label">Cliente Asignado:</span>
                <strong class="client-name">{{ form.clienteRazonSocial || 'Sin Razón Social' }}</strong>
                <span class="client-doc">Doc: {{ form.clienteNumeroDocumento || 'S/D' }}</span>
              </div>
              <div v-if="clientes && clientes.length > 0" class="field-group">
                <label class="form-label-sub">Reasignar Cliente (Opcional):</label>
                <Select
                  v-model="form.idCliente"
                  :options="clientes"
                  option-label="razonSocial"
                  option-value="idCliente"
                  filter
                  placeholder="Seleccionar cliente registrado"
                  class="w-full sga-select"
                  @change="handleClientReassign"
                />
              </div>
            </div>

            <!-- Régimen Aduanero -->
            <div class="field-group mt-3">
              <label class="form-label">Régimen Aduanero SUNAT *</label>
              <div class="regimen-selector-pills">
                <button
                  type="button"
                  :class="['pill-opt', { active: form.regimenAduanero === 'IMPORTACION_DEFINITIVA' }]"
                  @click="handleSelectRegimen('IMPORTACION_DEFINITIVA')"
                >
                  <i class="pi pi-download"></i>
                  <span>Importación Definitiva (REG-10)</span>
                </button>
                <button
                  type="button"
                  :class="['pill-opt', { active: form.regimenAduanero === 'EXPORTACION_DEFINITIVA' }]"
                  @click="handleSelectRegimen('EXPORTACION_DEFINITIVA')"
                >
                  <i class="pi pi-upload"></i>
                  <span>Exportación Definitiva (REG-40)</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Tarjeta B: Modalidad de Transporte y Terminales en Cascada -->
        <div class="correction-card">
          <div class="card-header">
            <div class="card-icon-tag"><i class="pi pi-compass"></i></div>
            <div>
              <h4 class="card-title">2. Modalidad y Ruta Internacional</h4>
              <p class="card-subtitle">Terminales de origen y arribo con filtrado reactivo por país</p>
            </div>
          </div>

          <div class="card-body">
            <!-- Botones segmentados de Modalidad -->
            <div class="field-group">
              <label class="form-label">Modalidad de Transporte *</label>
              <div class="segmented-mode-selector">
                <button
                  type="button"
                  :class="['mode-btn', { active: modalidadTransporte === 'MARITIMO' }]"
                  @click="setModalidad('MARITIMO')"
                >
                  <i class="pi pi-compass"></i>
                  <span>Carga Marítima (FCL / LCL)</span>
                </button>
                <button
                  type="button"
                  :class="['mode-btn', { active: modalidadTransporte === 'AEREO' }]"
                  @click="setModalidad('AEREO')"
                >
                  <i class="pi pi-send"></i>
                  <span>Carga Aérea (Air Cargo)</span>
                </button>
              </div>
            </div>

            <!-- Cascada Origen: País -> Puerto/Aeropuerto -->
            <div class="two-fields-row">
              <div class="field-group">
                <label class="form-label">País de Origen / Envío</label>
                <Select
                  v-model="paisOrigen"
                  :options="paisesOrigenDisponibles"
                  filter
                  placeholder="Seleccione país origen"
                  class="w-full sga-select"
                  @change="handlePaisOrigenChange"
                />
              </div>

              <div class="field-group">
                <label class="form-label">Terminal / Puerto de Origen *</label>
                <Select
                  v-model="form.idPuertoEnvio"
                  :options="puertosOrigenFiltrados"
                  option-label="nombre"
                  option-value="idPuerto"
                  filter
                  :disabled="!paisOrigen"
                  placeholder="Seleccione terminal"
                  class="w-full sga-select"
                >
                  <template #option="{ option }">
                    <div class="port-option-row">
                      <span class="port-code">{{ option.codigoPuerto }}</span>
                      <span class="port-name">{{ option.nombre }}</span>
                      <span class="port-type">{{ option.tipo }}</span>
                    </div>
                  </template>
                </Select>
              </div>
            </div>

            <!-- Cascada Destino: País -> Puerto/Aeropuerto -->
            <div class="two-fields-row">
              <div class="field-group">
                <label class="form-label">País de Destino / Llegada</label>
                <Select
                  v-model="paisDestino"
                  :options="paisesDestinoDisponibles"
                  filter
                  placeholder="Seleccione país destino"
                  class="w-full sga-select"
                  @change="handlePaisDestinoChange"
                />
              </div>

              <div class="field-group">
                <label class="form-label">Terminal / Puerto de Destino *</label>
                <Select
                  v-model="form.idPuertoDestino"
                  :options="puertosDestinoFiltrados"
                  option-label="nombre"
                  option-value="idPuerto"
                  filter
                  :disabled="!paisDestino"
                  placeholder="Seleccione terminal"
                  class="w-full sga-select"
                >
                  <template #option="{ option }">
                    <div class="port-option-row">
                      <span class="port-code">{{ option.codigoPuerto }}</span>
                      <span class="port-name">{{ option.nombre }}</span>
                      <span class="port-type">{{ option.tipo }}</span>
                    </div>
                  </template>
                </Select>
              </div>
            </div>

            <!-- Tipo de Carga y Mercancía -->
            <div class="two-fields-row mt-2">
              <div class="field-group">
                <label class="form-label">Tipo de Carga</label>
                <Select
                  v-model="form.idTipoCarga"
                  :options="tiposCarga"
                  option-label="nombre"
                  option-value="idTipoCarga"
                  filter
                  placeholder="Tipo de carga"
                  class="w-full sga-select"
                />
              </div>

              <div class="field-group">
                <label class="form-label">Clasificación Arancelaria / Mercancía</label>
                <Select
                  v-model="form.idMercancia"
                  :options="mercancias"
                  option-label="nombre"
                  option-value="idMercancia"
                  filter
                  placeholder="Seleccione mercancía"
                  class="w-full sga-select"
                  @change="handleMercanciaChange"
                >
                  <template #option="{ option }">
                    <div class="merc-option-row">
                      <strong>{{ option.nombre }}</strong>
                      <span v-if="option.arancelBasePct !== undefined" class="merc-arancel">
                        Arancel: {{ option.arancelBasePct }}%
                      </span>
                    </div>
                  </template>
                </Select>
              </div>
            </div>

            <!-- Panel de Alerta Regulatoria de Mercancía -->
            <div v-if="selectedMercancia" class="mercancia-status-panel mt-3">
              <div v-if="selectedMercancia.requierePermiso" class="regulatory-warning-banner">
                <i class="pi pi-exclamation-triangle reg-icon"></i>
                <div class="reg-content">
                  <div class="reg-kicker">MERCANCÍA RESTRINGIDA / SUJETA A CONTROL SECTORIAL</div>
                  <strong class="reg-title">Requiere Permiso y Visto Bueno previo de: {{ selectedMercancia.entidadReguladora || 'Entidad Competente' }}</strong>
                  <p class="reg-desc">Sector: {{ selectedMercancia.categoria || 'N/A' }} • Arancel Ad-Valórem Base: {{ selectedMercancia.arancelBasePct }}%</p>
                </div>
              </div>
              <div v-else class="free-goods-banner">
                <i class="pi pi-check-circle free-icon"></i>
                <span><strong>Libre Trámite:</strong> No requiere autorizaciones sectoriales previas para numeración aduanera. Arancel Base: {{ selectedMercancia.arancelBasePct }}%.</span>
              </div>
            </div>

            <!-- Servicio de Transporte Terrestre Local SGA -->
            <div class="field-group mt-4 transport-config-section">
              <div class="label-with-badge">
                <label class="form-label">Servicio de Transporte Terrestre Local SGA *</label>
                <span class="kicker-mini-badge">FLOTA TERRESTRE</span>
              </div>
              <p class="transport-helper">¿El cliente contratará el servicio de transporte terrestre interno de SGA o retirará con transporte externo?</p>

              <div class="transport-toggle-grid">
                <div
                  :class="['transport-card-opt', { selected: form.servicioTransporte === true }]"
                  @click="form.servicioTransporte = true"
                >
                  <div class="opt-icon"><i class="pi pi-truck"></i></div>
                  <div class="opt-info">
                    <h4 class="opt-title">Sí, Transporte Interno SGA</h4>
                    <p class="opt-desc">Traslado desde puerto/aeropuerto hasta almacén del cliente con unidades monitoreadas por GPS de SGA.</p>
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
              <div v-if="form.servicioTransporte" class="transport-route-config-panel mt-3">
                <div class="route-panel-header">
                  <div class="route-badge-icon"><i class="pi pi-map"></i></div>
                  <div>
                    <h4 class="route-panel-title">Ruta Nacional & Tarifa de Flete Local SGA</h4>
                    <p class="route-panel-desc">Especifique el origen y destino departamental/provincial en Perú y la tarifa cotizada.</p>
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
                          class="sga-select-native"
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
                          class="sga-select-native"
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
                          class="sga-select-native"
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
                          class="sga-select-native"
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
                <div class="route-cost-row mt-3">
                  <div class="route-cost-info">
                    <div class="cost-title-box">
                      <i class="pi pi-dollar"></i>
                      <label class="route-cost-label">Tarifa Flete Transporte Terrestre SGA (USD) *</label>
                    </div>
                    <span class="route-cost-sub">Importe que se sumará a los conceptos a liquidar de esta cotización comercial.</span>
                  </div>
                  <div class="input-money-wrapper route-cost-input-wrap">
                    <span class="currency-prefix">$</span>
                    <input
                      v-model.number="form.costoTransporteInterno"
                      type="number"
                      step="0.01"
                      min="0"
                      class="sga-input money-input"
                      placeholder="0.00"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- Validez de la Propuesta (Días) con Atajos -->
            <div class="field-group mt-4">
              <label class="form-label">Validez de la Oferta Comercial (Días Calendario) *</label>
              <div class="input-icon-box">
                <input
                  v-model.number="form.validezDias"
                  type="number"
                  min="1"
                  max="90"
                  class="sga-input"
                  style="max-width: 140px;"
                />
                <span class="input-suffix-tag">DÍAS</span>
              </div>
              <div class="quick-shortcuts-row mt-2">
                <span class="shortcuts-label">Plazos estándar:</span>
                <button type="button" :class="['shortcut-chip', { active: form.validezDias === 15 }]" @click="form.validezDias = 15">15 Días</button>
                <button type="button" :class="['shortcut-chip', { active: form.validezDias === 30 }]" @click="form.validezDias = 30">30 Días</button>
                <button type="button" :class="['shortcut-chip', { active: form.validezDias === 45 }]" @click="form.validezDias = 45">45 Días</button>
                <button type="button" :class="['shortcut-chip', { active: form.validezDias === 60 }]" @click="form.validezDias = 60">60 Días</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- COLUMNA 2: ESTRUCTURA ECONÓMICA Y LIQUIDACIÓN TRIBUTARIA  -->
      <!-- ======================================================== -->
      <div class="grid-column">
        <div class="correction-card economics-card">
          <div class="card-header">
            <div class="card-icon-tag"><i class="pi pi-dollar"></i></div>
            <div>
              <h4 class="card-title">3. Valoración Económica y Tributos</h4>
              <p class="card-subtitle">Ajuste de montos en USD con liquidación SUNAT en tiempo real</p>
            </div>
          </div>

          <div class="card-body">
            <!-- Entradas Económicas Principales -->
            <div class="economic-inputs-grid">
              <div class="field-group">
                <label class="form-label">Valor Mercancía (FOB) USD *</label>
                <div class="input-money-wrapper">
                  <span class="currency-prefix">$</span>
                  <input
                    v-model.number="form.valorFob"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="sga-input money-input"
                  />
                </div>
              </div>

              <div class="field-group">
                <label class="form-label">Flete Internacional Estimado (USD)</label>
                <div class="input-money-wrapper">
                  <span class="currency-prefix">$</span>
                  <input
                    v-model.number="form.montoFleteEstimado"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="sga-input money-input"
                  />
                </div>
              </div>

              <div class="field-group">
                <div class="label-with-toggle">
                  <label class="form-label">Seguro de Transporte (USD)</label>
                  <button
                    v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'"
                    type="button"
                    class="btn-toggle-seguro"
                    @click="toggleSeguroManual"
                  >
                    {{ seguroManual ? 'Usar Tabla SUNAT' : 'Editar Manual' }}
                  </button>
                </div>
                <div class="input-money-wrapper">
                  <span class="currency-prefix">$</span>
                  <input
                    v-model.number="form.montoSeguroEstimado"
                    type="number"
                    step="0.01"
                    min="0"
                    :readonly="!seguroManual && form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'"
                    class="sga-input money-input"
                  />
                </div>
                <span class="input-caption">
                  {{ seguroManual ? 'Modo de cálculo manual' : 'Calculado según tabla de seguros SUNAT' }}
                </span>
              </div>

              <div class="field-group">
                <label class="form-label">Comisión Agenciamiento / Honorarios (USD)</label>
                <div class="input-money-wrapper">
                  <span class="currency-prefix">$</span>
                  <input
                    v-model.number="form.montoHonorariosEstimado"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="sga-input money-input"
                  />
                </div>
              </div>

              <!-- Si incluye transporte local, mostrar también el campo aquí para acceso directo -->
              <div v-if="form.servicioTransporte" class="field-group">
                <label class="form-label">Flete Terrestre Local SGA (USD)</label>
                <div class="input-money-wrapper">
                  <span class="currency-prefix">$</span>
                  <input
                    v-model.number="form.costoTransporteInterno"
                    type="number"
                    step="0.01"
                    min="0"
                    placeholder="0.00"
                    class="sga-input money-input"
                  />
                </div>
              </div>
            </div>

            <!-- Tasas Arancelarias -->
            <div v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'" class="two-fields-row mt-3">
              <div class="field-group">
                <label class="form-label">Tasa Ad-Valórem (%)</label>
                <Select
                  v-model="form.tasaAdValorem"
                  :options="tasasAdValoremOptions"
                  option-label="label"
                  option-value="value"
                  class="w-full sga-select"
                />
              </div>

              <div class="field-group">
                <label class="form-label">Tasa Percepción SUNAT (%)</label>
                <Select
                  v-model="form.tasaPercepcion"
                  :options="tasasPercepcionOptions"
                  option-label="label"
                  option-value="value"
                  class="w-full sga-select"
                />
              </div>
            </div>

            <!-- Tarjeta de Liquidación y Desglose en Vivo -->
            <div class="liquidation-summary-box mt-4">
              <div class="liq-header">
                <span class="liq-kicker">LIQUIDACIÓN ESTIMADA ADUANERA</span>
                <strong class="liq-title">Cálculo Reactivo SUNAT</strong>
              </div>

              <div class="liq-rows-list">
                <div class="liq-row">
                  <span class="liq-label">Valor CIF Calculado (FOB + Flete + Seguro):</span>
                  <strong class="liq-val">${{ formatMoney(valorCif) }} USD</strong>
                </div>

                <template v-if="form.regimenAduanero !== 'EXPORTACION_DEFINITIVA'">
                  <div class="liq-row">
                    <span class="liq-label">Ad-Valórem (Arancel {{ form.tasaAdValorem }}%):</span>
                    <span class="liq-val">${{ formatMoney(montoAdValorem) }} USD</span>
                  </div>

                  <div class="liq-row">
                    <span class="liq-label">IGV (16%) + IPM (2%) = 18%:</span>
                    <span class="liq-val">${{ formatMoney(montoIgv) }} USD</span>
                  </div>

                  <div class="liq-row">
                    <span class="liq-label">Percepción SUNAT ({{ form.tasaPercepcion }}%):</span>
                    <span class="liq-val">${{ formatMoney(montoPercepcion) }} USD</span>
                  </div>

                  <div class="liq-row liq-subtotal-tributos">
                    <span class="liq-label">Total Tributos de Importación:</span>
                    <strong class="liq-val">${{ formatMoney(form.montoTributosEstimado) }} USD</strong>
                  </div>

                  <div v-if="form.servicioTransporte && form.costoTransporteInterno > 0" class="liq-row">
                    <span class="liq-label">Transporte Terrestre Local (SGA):</span>
                    <span class="liq-val">${{ formatMoney(form.costoTransporteInterno) }} USD</span>
                  </div>

                  <div v-if="form.montoHonorariosEstimado > 0" class="liq-row">
                    <span class="liq-label">Comisión Agenciamiento / Honorarios:</span>
                    <span class="liq-val">${{ formatMoney(form.montoHonorariosEstimado) }} USD</span>
                  </div>
                </template>
                <div v-else class="liq-row export-exempt-badge">
                  <i class="pi pi-check-circle"></i>
                  <span>Régimen de Exportación Definitiva: Inafecto de Tributos Arancelarios</span>
                </div>
              </div>

              <div class="liq-grand-total">
                <div class="total-label-box">
                  <span class="total-kicker">IMPORTE TOTAL DE LA PROPUESTA</span>
                  <span class="total-caption">(Incluye tributos, flete, logística y honorarios)</span>
                </div>
                <div class="total-amount-box">
                  <span class="total-currency">USD</span>
                  <span class="total-number">${{ formatMoney(form.totalCotizado) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ======================================================== -->
    <!-- SECCIÓN ANCHA INFERIOR: DOCUMENTACIÓN Y SUBSANACIÓN      -->
    <!-- ======================================================== -->
    <div class="correction-bottom-grid">
      <!-- Tarjeta C: Documentación Sustentatoria -->
      <div class="correction-card">
        <div class="card-header">
          <div class="card-icon-tag"><i class="pi pi-paperclip"></i></div>
          <div>
            <h4 class="card-title">4. Documentación y Expediente Digital</h4>
            <p class="card-subtitle">Visualice los adjuntos actuales y cargue nuevos sustentos solicitados</p>
          </div>
        </div>

        <div class="card-body">
          <!-- Documentos Existentes -->
          <div class="docs-section-title">Documentos Adjuntos Actuales:</div>
          <div v-if="loadingDocs" class="docs-loading">
            <i class="pi pi-spin pi-spinner"></i>
            <span>Cargando expediente digital...</span>
          </div>
          <div v-else-if="existingDocs.length === 0" class="docs-empty-state">
            <i class="pi pi-folder-open"></i>
            <span>No hay documentos adjuntos previos registrados para esta cotización.</span>
          </div>
          <div v-else class="existing-docs-list">
            <div v-for="doc in existingDocs" :key="doc.idDocumento" class="existing-doc-chip">
              <i class="pi pi-file-pdf doc-icon"></i>
              <div class="doc-info">
                <strong class="doc-name">{{ doc.nombreOriginal || doc.nombreArchivo }}</strong>
                <span class="doc-meta">{{ doc.categoria || 'SUSTENTO' }} • {{ doc.tamanoBytes ? (doc.tamanoBytes / 1024).toFixed(1) + ' KB' : '' }}</span>
              </div>
              <button
                type="button"
                class="btn-doc-download"
                title="Descargar archivo"
                @click="handleDownloadDoc(doc)"
              >
                <i class="pi pi-download"></i>
              </button>
            </div>
          </div>

          <!-- Cola de Nuevos Documentos a Subir -->
          <div class="docs-section-title mt-4">Adjuntar Nuevos Documentos para Subsanación:</div>
          <div class="doc-upload-dropzone">
            <input
              id="file-upload-correction"
              type="file"
              multiple
              class="hidden-file-input"
              @change="handleFilesSelected"
            />
            <label for="file-upload-correction" class="upload-dropzone-label">
              <i class="pi pi-cloud-upload upload-icon"></i>
              <span class="upload-main-text">Haga clic o arrastre aquí archivos PDF, XML o imágenes</span>
              <span class="upload-sub-text">Factura Comercial, Packing List, BL / Guía Aérea rectificada</span>
            </label>
          </div>

          <!-- Lista de archivos en cola -->
          <div v-if="queuedDocs.length > 0" class="queued-docs-list mt-3">
            <div v-for="(qDoc, idx) in queuedDocs" :key="idx" class="queued-doc-item">
              <i class="pi pi-file"></i>
              <span class="qdoc-name">{{ qDoc.file.name }}</span>
              <Select
                v-model="qDoc.categoria"
                :options="categoriasDocOptions"
                class="qdoc-category-select"
              />
              <button type="button" class="btn-remove-qdoc" @click="removeQueuedDoc(idx)">
                <i class="pi pi-trash"></i>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Tarjeta D: Nota de Subsanación Comercial -->
      <div class="correction-card">
        <div class="card-header">
          <div class="card-icon-tag"><i class="pi pi-comment"></i></div>
          <div>
            <h4 class="card-title">5. Nota de Subsanación Comercial</h4>
            <p class="card-subtitle">Detalle al Supervisor Aduanero las modificaciones y correcciones efectuadas</p>
          </div>
        </div>

        <div class="card-body">
          <div class="field-group">
            <label class="form-label">Explicación de las Correcciones Realizadas *</label>
            <textarea
              v-model="notaSubsanacion"
              rows="5"
              placeholder="Ejemplo: Se corrigió el flete marítimo según cotización de naviera, se actualizó la subpartida arancelaria al 6% y se adjuntó el Packing List firmado por el consignatario."
              class="sga-textarea"
            ></textarea>
            <span class="input-caption">
              Esta nota se añadirá al historial de la cotización para que el supervisor valide rápidamente los puntos subsanados.
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, toRef } from 'vue'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import { useCotizacionCorrection } from '../composables/useCotizacionCorrection'
import { catalogoService } from '@/features/admin/catalogos/services/catalogoService'
import { documentoService } from '@/features/documentos/services/documentoService'
import { formatMoney } from '../composables/useCotizacionCalculator'
import { getDepartamentosPeru, getProvinciasPorDepartamento } from '../data/peruUbigeo'

const props = defineProps({
  cotizacion: {
    type: Object,
    required: true
  },
  clientes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['cancel', 'saved'])

// Catálogos
const puertos = ref([])
const tiposCarga = ref([])
const mercancias = ref([])
const loadingCatalogos = ref(false)
const submitting = ref(false)
const validationError = ref('')

// Documentación
const existingDocs = ref([])
const loadingDocs = ref(false)
const queuedDocs = ref([])

// Opciones arancelarias fijas
const tasasAdValoremOptions = [
  { label: '0% (Exonerado / TLC)', value: 0 },
  { label: '6% (Tasa Estándar)', value: 6 },
  { label: '11% (Bienes Sensibles)', value: 11 }
]

const tasasPercepcionOptions = [
  { label: '0% (Exportación / Inafecto)', value: 0 },
  { label: '3.5% (Importador Habitual)', value: 3.5 },
  { label: '5.0% (Bienes Usados)', value: 5.0 },
  { label: '10.0% (Primera Importación)', value: 10.0 }
]

const categoriasDocOptions = [
  'FACTURA_COMERCIAL',
  'PACKING_LIST',
  'DOCUMENTO_TRANSPORTE',
  'CERTIFICADO_ORIGEN',
  'SUSTENTO_ADUANERO',
  'OTRO'
]

// Instanciar composable de corrección
const cotizacionRef = toRef(props, 'cotizacion')
const {
  form,
  motivoObservacion,
  notaSubsanacion,
  seguroManual,
  modalidadTransporte,
  paisOrigen,
  paisDestino,
  paisesOrigenDisponibles,
  paisesDestinoDisponibles,
  puertosOrigenFiltrados,
  puertosDestinoFiltrados,
  setModalidad,
  aplicarReglasRegimen,
  valorCif,
  montoAdValorem,
  montoIgv,
  montoPercepcion,
  recalcularLiquidacion,
  validateCorrection,
  buildSubmissionPayload
} = useCotizacionCorrection({
  cotizacion: cotizacionRef,
  puertos
})

// Catálogo de Ubigeo y Cascada de Provincias
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

const selectedMercancia = computed(() => {
  if (!form.value.idMercancia || !mercancias.value) return null
  return mercancias.value.find((m) => m.idMercancia === form.value.idMercancia) || null
})

// Desenvolver respuestas de API
const unwrapList = (res) => {
  if (!res) return []
  if (Array.isArray(res)) return res
  if (Array.isArray(res.data)) return res.data
  return []
}

// Cargar catálogos auxiliares
const loadCatalogos = async () => {
  loadingCatalogos.value = true
  try {
    const [puertosRes, tiposRes, mercanciasRes] = await Promise.allSettled([
      catalogoService.getPuertos(true),
      catalogoService.getTiposCarga(true),
      catalogoService.getMercancias(true)
    ])

    if (puertosRes.status === 'fulfilled') {
      puertos.value = unwrapList(puertosRes.value)
    }
    if (tiposRes.status === 'fulfilled') {
      tiposCarga.value = unwrapList(tiposRes.value)
    }
    if (mercanciasRes.status === 'fulfilled') {
      mercancias.value = unwrapList(mercanciasRes.value)
    }
  } catch (err) {
    console.error('Error cargando catálogos en CotizacionCorrectionView:', err)
  } finally {
    loadingCatalogos.value = false
  }
}

// Cargar documentos existentes
const loadExistingDocuments = async () => {
  if (!props.cotizacion?.idCotizacion) return
  loadingDocs.value = true
  try {
    const docs = await documentoService.getDocumentos({
      entidadTipo: 'COTIZACION',
      idCotizacion: props.cotizacion.idCotizacion
    })
    existingDocs.value = Array.isArray(docs) ? docs : unwrapList(docs)
  } catch (err) {
    console.error('Error al cargar documentos adjuntos:', err)
    existingDocs.value = []
  } finally {
    loadingDocs.value = false
  }
}

const handleDownloadDoc = async (doc) => {
  try {
    const blob = await documentoService.download(doc.idDocumento)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = doc.nombreOriginal || doc.nombreArchivo || 'documento.pdf'
    document.body.appendChild(a)
    a.click()
    a.remove()
    window.URL.revokeObjectURL(url)
  } catch (err) {
    console.error('Error al descargar archivo:', err)
  }
}

// Manejadores de cascada
const handleSelectRegimen = (regimen) => {
  form.value.regimenAduanero = regimen
  aplicarReglasRegimen(regimen)
  recalcularLiquidacion()
}

const handlePaisOrigenChange = () => {
  if (form.value.idPuertoEnvio) {
    const valid = puertosOrigenFiltrados.value.some((p) => p.idPuerto === form.value.idPuertoEnvio)
    if (!valid) form.value.idPuertoEnvio = null
  }
}

const handlePaisDestinoChange = () => {
  if (form.value.idPuertoDestino) {
    const valid = puertosDestinoFiltrados.value.some((p) => p.idPuerto === form.value.idPuertoDestino)
    if (!valid) form.value.idPuertoDestino = null
  }
}

const handleMercanciaChange = () => {
  if (form.value.idMercancia) {
    const found = mercancias.value.find((m) => m.idMercancia === form.value.idMercancia)
    if (found && found.arancelBasePct !== undefined && found.arancelBasePct !== null) {
      const arancel = Number(found.arancelBasePct)
      if ([0, 6, 11].includes(arancel)) {
        form.value.tasaAdValorem = arancel
      }
    }
    recalcularLiquidacion()
  }
}

const handleClientReassign = () => {
  if (form.value.idCliente && props.clientes) {
    const c = props.clientes.find((cl) => cl.idCliente === form.value.idCliente)
    if (c) {
      form.value.clienteRazonSocial = c.razonSocial || ''
      form.value.clienteNumeroDocumento = c.numeroDocumento || ''
    }
  }
}

const toggleSeguroManual = () => {
  seguroManual.value = !seguroManual.value
  recalcularLiquidacion()
}

// Manejo de carga de archivos en cola
const handleFilesSelected = (event) => {
  const files = Array.from(event.target.files || [])
  for (const f of files) {
    queuedDocs.value.push({
      file: f,
      categoria: 'SUSTENTO_ADUANERO',
      descripcion: ''
    })
  }
  event.target.value = ''
}

const removeQueuedDoc = (idx) => {
  queuedDocs.value.splice(idx, 1)
}

// Envío de la corrección
const handleSubmitCorrection = () => {
  validationError.value = ''
  const valResult = validateCorrection()
  if (!valResult.valid) {
    validationError.value = valResult.error
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }

  submitting.value = true
  const payload = buildSubmissionPayload()

  emit('saved', {
    isEdit: true,
    cotizacionId: form.value.idCotizacion,
    payload,
    documentos: queuedDocs.value,
    done: () => {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadCatalogos()
  loadExistingDocuments()
})
</script>

<style scoped>
.cotizacion-correction-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
  padding-bottom: var(--space-10, 2.5rem);
  width: 100%;
}

/* 1. Header Bar */
.correction-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-6, 1.5rem);
  padding: var(--space-5, 1.25rem) var(--space-6, 1.5rem);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  box-shadow: var(--shadow-subtle);
  flex-wrap: wrap;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-4, 1rem);
}

.btn-back-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 0.5rem;
  color: var(--color-text-main);
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-back-action:hover {
  background: var(--color-bg-muted);
  border-color: var(--color-border-strong);
}

.badges-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.badge-code {
  font-family: var(--font-mono, monospace);
  font-size: 0.85rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  border-radius: 0.35rem;
  letter-spacing: 0.04em;
}

.badge-status-observed {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
  border-radius: 0.35rem;
}

.header-main-title {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.header-subtitle {
  margin: 0.15rem 0 0 0;
  font-size: 0.88rem;
  color: var(--color-text-muted);
}

.header-right-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-cancel-flat {
  padding: 0.65rem 1.2rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 0.6rem;
  color: var(--color-text-main);
  font-weight: 600;
  cursor: pointer;
}

.btn-cancel-flat:hover {
  background: var(--color-bg-muted);
}

.btn-submit-correction {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1.4rem;
  background: var(--color-primary);
  border: 1px solid var(--color-primary);
  color: var(--color-primary-text);
  border-radius: 0.6rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: var(--shadow-subtle);
  transition: all 0.2s ease;
}

.btn-submit-correction:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-card);
}

.btn-submit-correction:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Error Banner */
.error-banner {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.85rem 1.25rem;
  background: var(--color-danger-bg);
  border: 1px solid var(--color-danger-border);
  color: var(--color-danger-text);
  border-radius: 0.75rem;
}

.error-content {
  flex: 1;
  font-size: 0.9rem;
}

.error-close-btn {
  background: transparent;
  border: none;
  color: var(--color-danger-text);
  cursor: pointer;
}

/* 2. Banner de Observación */
.observation-master-banner {
  display: flex;
  align-items: flex-start;
  gap: 1.25rem;
  padding: 1.25rem 1.5rem;
  background: var(--color-warning-bg);
  border: 2px solid var(--color-warning-border);
  border-radius: 1rem;
  box-shadow: var(--shadow-subtle);
}

.obs-icon-circle {
  width: 2.75rem;
  height: 2.75rem;
  border-radius: 50%;
  background: var(--color-warning-text);
  color: var(--color-bg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.3rem;
  box-shadow: var(--shadow-subtle);
}

.obs-banner-content {
  flex: 1;
}

.obs-kicker-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.35rem;
  flex-wrap: wrap;
}

.obs-kicker {
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-warning-text);
}

.obs-cliente-tag {
  font-size: 0.8rem;
  font-weight: 700;
  padding: 0.15rem 0.6rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-warning-border);
  color: var(--color-warning-text);
  border-radius: 1rem;
}

.obs-title {
  margin: 0 0 0.4rem 0;
  font-size: 1.05rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.obs-reason-text {
  margin: 0 0 0.65rem 0;
  font-size: 1rem;
  line-height: 1.5;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  border-left: 4px solid var(--color-warning-text);
  font-style: italic;
}

.obs-instruction-hint {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.82rem;
  color: var(--color-warning-text);
}

/* 3. Grid Principal (2 Columnas) */
.correction-main-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: var(--space-6, 1.5rem);
}

@media (max-width: 1024px) {
  .correction-main-grid {
    grid-template-columns: 1fr;
  }
}

.grid-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

/* Tarjetas */
.correction-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl, 1rem);
  box-shadow: var(--shadow-subtle);
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 1rem 1.25rem;
  background: var(--color-bg-subtle);
  border-bottom: 1px solid var(--color-border);
}

.card-icon-tag {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 0.5rem;
  background: var(--color-primary);
  color: var(--color-primary-text);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.05rem;
}

.card-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.card-subtitle {
  margin: 0.1rem 0 0 0;
  font-size: 0.78rem;
  color: var(--color-text-muted);
}

.card-body {
  padding: 1.25rem;
}

/* Client summary */
.client-summary-box {
  padding: 0.85rem 1rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 0.75rem;
}

.client-meta {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
  font-size: 0.9rem;
}

.client-label {
  color: var(--color-text-muted);
  font-size: 0.82rem;
}

.client-name {
  color: var(--color-text-main);
}

.client-doc {
  font-size: 0.78rem;
  background: var(--color-bg-muted);
  padding: 0.1rem 0.45rem;
  border-radius: 0.35rem;
  color: var(--color-text-muted);
}

/* Campos */
.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.form-label {
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-text-main);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.form-label-sub {
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--color-text-muted);
}

.two-fields-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

@media (max-width: 640px) {
  .two-fields-row {
    grid-template-columns: 1fr;
  }
}

.sga-input,
.sga-textarea {
  width: 100%;
  padding: 0.65rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: 0.5rem;
  font-size: 0.92rem;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  outline: none;
  transition: border-color 0.2s;
}

.sga-input:focus,
.sga-textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-bg-subtle);
}

.sga-textarea {
  resize: vertical;
}

/* Regimen Selector Pills */
.regimen-selector-pills {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.6rem;
}

.pill-opt {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  padding: 0.7rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: 0.6rem;
  background: var(--color-bg-surface);
  color: var(--color-text-muted);
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.pill-opt.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-text);
}

/* Segmented Mode Selector */
.segmented-mode-selector {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
  background: var(--color-bg-subtle);
  padding: 0.35rem;
  border-radius: 0.65rem;
}

.mode-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.65rem 0.75rem;
  border: none;
  border-radius: 0.5rem;
  background: transparent;
  color: var(--color-text-muted);
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.mode-btn.active {
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  box-shadow: var(--shadow-subtle);
}

/* Ports Option Row */
.port-option-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.port-code {
  font-family: monospace;
  font-weight: 700;
  font-size: 0.8rem;
  background: var(--color-bg-subtle);
  padding: 0.1rem 0.35rem;
  border-radius: 0.25rem;
}

.port-name {
  flex: 1;
}

.port-type {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

/* Mercancía Option Row */
.merc-option-row {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.merc-arancel {
  font-size: 0.75rem;
  color: var(--color-success-text);
  font-weight: 700;
}

/* Money Input */
.economic-inputs-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.85rem;
}

.input-money-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.currency-prefix {
  position: absolute;
  left: 0.85rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.money-input {
  padding-left: 2rem;
  font-family: var(--font-mono, monospace);
  font-weight: 700;
  font-size: 1rem;
}

.label-with-toggle {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-toggle-seguro {
  background: none;
  border: none;
  color: var(--color-info-text);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  text-decoration: underline;
}

.input-caption {
  font-size: 0.74rem;
  color: var(--color-text-muted);
}

.checkbox-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 0;
}

.chk-label {
  font-size: 0.85rem;
  color: var(--color-text-main);
  cursor: pointer;
}

/* Liquidación Box */
.liquidation-summary-box {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 0.85rem;
  padding: 1.25rem;
}

.liq-header {
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 0.6rem;
  margin-bottom: 0.75rem;
}

.liq-kicker {
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
}

.liq-title {
  display: block;
  font-size: 1.05rem;
  color: var(--color-text-main);
}

.liq-rows-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.liq-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.88rem;
  color: var(--color-text-muted);
}

.liq-subtotal-tributos {
  margin-top: 0.4rem;
  padding-top: 0.5rem;
  border-top: 1px dashed var(--color-border-strong);
  color: var(--color-text-main);
}

.export-exempt-badge {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.6rem 0.85rem;
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
  border-radius: 0.5rem;
  font-weight: 600;
  font-size: 0.82rem;
}

.liq-grand-total {
  margin-top: 1rem;
  padding: 1rem 1.25rem;
  background: var(--color-bg-hero);
  border: 1px solid var(--color-border-strong);
  border-radius: 0.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--color-text-main);
}

.total-kicker {
  font-size: 0.75rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: var(--color-accent-kicker);
  display: block;
}

.total-caption {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.total-currency {
  font-size: 0.85rem;
  color: var(--color-accent-kicker);
  font-weight: 700;
  margin-right: 0.35rem;
}

.total-number {
  font-size: 1.55rem;
  font-weight: 900;
  color: var(--color-text-main);
  font-family: var(--font-mono, monospace);
}

/* 4. Bottom Grid (Documentos y Subsanación) */
.correction-bottom-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-6, 1.5rem);
}

@media (max-width: 1024px) {
  .correction-bottom-grid {
    grid-template-columns: 1fr;
  }
}

.docs-section-title {
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-text-muted);
  margin-bottom: 0.5rem;
}

.existing-docs-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.existing-doc-chip {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.65rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 0.5rem;
}

.doc-icon {
  font-size: 1.4rem;
  color: var(--color-danger-text);
}

.doc-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.doc-name {
  font-size: 0.88rem;
  color: var(--color-text-main);
}

.doc-meta {
  font-size: 0.72rem;
  color: var(--color-text-muted);
}

.btn-doc-download {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 0.35rem;
  padding: 0.35rem 0.6rem;
  color: var(--color-text-main);
  cursor: pointer;
}

.btn-doc-download:hover {
  background: var(--color-bg-muted);
}

.docs-empty-state {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background: var(--color-bg-subtle);
  border: 1px dashed var(--color-border-strong);
  border-radius: 0.5rem;
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.doc-upload-dropzone {
  position: relative;
  border: 2px dashed var(--color-border-strong);
  border-radius: 0.75rem;
  padding: 1.25rem 1rem;
  text-align: center;
  background: var(--color-bg-subtle);
  transition: border-color 0.2s;
}

.hidden-file-input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.upload-dropzone-label {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  cursor: pointer;
}

.upload-icon {
  font-size: 1.8rem;
  color: var(--color-primary);
}

.upload-main-text {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.upload-sub-text {
  font-size: 0.76rem;
  color: var(--color-text-muted);
}

.queued-docs-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.queued-doc-item {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.5rem 0.75rem;
  background: var(--color-bg-subtle);
  border-radius: 0.5rem;
  font-size: 0.85rem;
}

.qdoc-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.qdoc-category-select {
  width: 180px;
  font-size: 0.8rem;
}

.btn-remove-qdoc {
  background: none;
  border: none;
  color: var(--color-danger-text);
  cursor: pointer;
}

/* ======================================================== */
/* ESTILOS DE TRANSPORTE LOCAL, UBIGEO Y MERCANCÍA          */
/* ======================================================== */
.label-with-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.25rem;
}

.kicker-mini-badge {
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  padding: 0.15rem 0.45rem;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  border-radius: 0.35rem;
}

.transport-helper {
  margin: 0 0 0.75rem 0;
  font-size: 0.8rem;
  color: var(--color-text-muted);
  line-height: 1.4;
}

.transport-toggle-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

@media (max-width: 640px) {
  .transport-toggle-grid {
    grid-template-columns: 1fr;
  }
}

.transport-card-opt {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  padding: 0.9rem 1rem;
  background: var(--color-bg-surface);
  border: 2px solid var(--color-border);
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.transport-card-opt:hover {
  border-color: var(--color-border-strong);
  background: var(--color-bg-subtle);
}

.transport-card-opt.selected {
  border-color: var(--color-primary);
  background: var(--color-bg-subtle);
  box-shadow: var(--shadow-subtle);
}

.transport-card-opt .opt-icon {
  width: 2.3rem;
  height: 2.3rem;
  border-radius: 0.5rem;
  background: var(--color-bg-subtle);
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.15rem;
  flex-shrink: 0;
}

.transport-card-opt.selected .opt-icon {
  background: var(--color-primary);
  color: var(--color-primary-text);
}

.transport-card-opt .opt-info {
  flex: 1;
}

.transport-card-opt .opt-title {
  margin: 0;
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.transport-card-opt.selected .opt-title {
  color: var(--color-primary);
}

.transport-card-opt .opt-desc {
  margin: 0.2rem 0 0 0;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  line-height: 1.35;
}

.transport-card-opt .check-active {
  color: var(--color-success-text);
  font-size: 1.15rem;
  margin-top: 0.15rem;
}

/* Panel de Ruta de Transporte */
.transport-route-config-panel {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 0.75rem;
  padding: 1.1rem;
}

.route-panel-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding-bottom: 0.85rem;
  margin-bottom: 0.85rem;
  border-bottom: 1px solid var(--color-border);
}

.route-badge-icon {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 0.5rem;
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
}

.route-panel-title {
  margin: 0;
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.route-panel-desc {
  margin: 0.1rem 0 0 0;
  font-size: 0.76rem;
  color: var(--color-text-muted);
}

.route-inputs-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.85rem;
}

@media (max-width: 640px) {
  .route-inputs-grid {
    grid-template-columns: 1fr;
  }
}

.route-location-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: 0.6rem;
  padding: 0.85rem;
}

.loc-card-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.05em;
  margin-bottom: 0.65rem;
}

.loc-origin {
  color: var(--color-info-text);
}

.loc-dest {
  color: var(--color-warning-text);
}

.loc-fields-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.6rem;
}

.field-subgroup {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.loc-label {
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.sga-select-native {
  width: 100%;
  padding: 0.55rem 0.65rem;
  border: 1px solid var(--color-border);
  border-radius: 0.45rem;
  font-size: 0.85rem;
  background-color: var(--color-bg-surface);
  color: var(--color-text-main);
  outline: none;
}

.sga-select-native:focus {
  border-color: var(--color-primary);
}

.route-cost-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  padding: 0.85rem;
  background: var(--color-bg-surface);
  border: 1px dashed var(--color-border-strong);
  border-radius: 0.6rem;
  flex-wrap: wrap;
}

.cost-title-box {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.route-cost-label {
  font-size: 0.84rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.route-cost-sub {
  font-size: 0.74rem;
  color: var(--color-text-muted);
  display: block;
}

.route-cost-input-wrap {
  width: 170px;
}

/* Alerta de Mercancía Restringida / Libre */
.mercancia-status-panel {
  width: 100%;
}

.regulatory-warning-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  padding: 0.85rem 1rem;
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning-border);
  border-left: 4px solid var(--color-warning-text);
  border-radius: 0.5rem;
}

.reg-icon {
  font-size: 1.35rem;
  color: var(--color-warning-text);
  margin-top: 0.15rem;
}

.reg-kicker {
  font-size: 0.68rem;
  font-weight: 800;
  color: var(--color-warning-text);
  letter-spacing: 0.05em;
  margin-bottom: 0.15rem;
}

.reg-title {
  font-size: 0.88rem;
  color: var(--color-text-main);
}

.reg-desc {
  margin: 0.2rem 0 0 0;
  font-size: 0.78rem;
  color: var(--color-warning-text);
}

.free-goods-banner {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.65rem 0.85rem;
  background: var(--color-success-bg);
  border: 1px solid var(--color-success-border);
  border-radius: 0.5rem;
  font-size: 0.82rem;
  color: var(--color-success-text);
}

.free-icon {
  font-size: 1.1rem;
  color: var(--color-success-text);
}

/* Atajos de Validez */
.input-icon-box {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.input-suffix-tag {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.quick-shortcuts-row {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.shortcuts-label {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  font-weight: 600;
}

.shortcut-chip {
  padding: 0.25rem 0.6rem;
  font-size: 0.76rem;
  font-weight: 600;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 0.4rem;
  color: var(--color-text-main);
  cursor: pointer;
  transition: all 0.2s;
}

.shortcut-chip:hover {
  background: var(--color-bg-muted);
}

.shortcut-chip.active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-primary-text);
}

.mt-2 { margin-top: 0.5rem; }
.mt-3 { margin-top: 0.75rem; }
.mt-4 { margin-top: 1rem; }
.w-full { width: 100%; }
.fade-in {
  animation: fadeIn 0.25s ease-in-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
