<template>
  <div class="operacion-transito-view fade-in">
    <!-- ============================================================== -->
    <!-- 1. BARRA SUPERIOR DE NAVEGACIÓN Y CONMUTADOR DE HITOS          -->
    <!-- ============================================================== -->
    <header class="form-header-bar">
      <div class="header-titles">
        <button
          type="button"
          class="btn-back-link"
          @click="$emit('back')"
          title="Regresar a la vista anterior"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver a la Bandeja</span>
        </button>

        <div class="breadcrumbs">
          <span class="crumb-muted">Agente Aduanero</span>
          <i class="pi pi-angle-right text-xs text-muted"></i>
          <span class="crumb-muted">Tránsito & Arribo</span>
          <i class="pi pi-angle-right text-xs text-muted"></i>
          <span class="crumb-active font-mono">{{ expediente?.codigoExpediente }}</span>
        </div>
      </div>

      <!-- Selector de Hito / Operación -->
      <div class="header-actions">
        <div class="hito-mode-switcher">
          <button
            type="button"
            :class="['switcher-btn', { active: activeMode === 'salida' }]"
            @click="activeMode = 'salida'"
          >
            <i class="pi pi-send"></i>
            <span>1. Salida de Origen</span>
          </button>

          <button
            type="button"
            :class="['switcher-btn', { active: activeMode === 'transito' }]"
            @click="activeMode = 'transito'"
          >
            <i class="pi pi-globe"></i>
            <span>2. Datos de Tránsito</span>
          </button>

          <button
            type="button"
            :class="['switcher-btn', { active: activeMode === 'arribo' }]"
            @click="activeMode = 'arribo'"
          >
            <i class="pi pi-compass"></i>
            <span>3. Confirmar Arribo</span>
          </button>
        </div>
      </div>
    </header>

    <!-- ============================================================== -->
    <!-- 2. HERO BANNER EJECUTIVO DEL DESPACHO                          -->
    <!-- ============================================================== -->
    <div class="operacion-hero-banner" v-if="expediente">
      <div class="hero-left-col">
        <div class="hero-kicker-row">
          <span class="hero-kicker-tag">OPERATORIA INTERNACIONAL // GESTIÓN DE TRAVESÍA</span>
          <span class="modalidad-tag">
            <i :class="modalidadIcon"></i>
            <span>{{ expediente.modalidad || 'TRANSPORTE INTERNACIONAL' }}</span>
          </span>
          <span class="regimen-tag">
            <i class="pi pi-arrow-down-left"></i>
            <span>{{ isExport ? 'EXPORTACIÓN DEFINITIVA' : 'IMPORTACIÓN PARA EL CONSUMO' }}</span>
          </span>
        </div>

        <h1 class="hero-main-title font-mono">
          {{ expediente.codigoExpediente }}
        </h1>

        <div class="hero-meta-row">
          <span class="meta-item">
            <i class="pi pi-building"></i>
            <span>Cliente: <strong>{{ expediente.clienteRazonSocial || 'Cliente no registrado' }}</strong></span>
            <span class="meta-sub font-mono" v-if="expediente.clienteRuc">(RUC {{ expediente.clienteRuc }})</span>
          </span>
          <span class="meta-divider">•</span>
          <span class="meta-item">
            <i class="pi pi-file"></i>
            <span>Cotización: <strong class="font-mono">{{ expediente.codigoCotizacion || '—' }}</strong></span>
          </span>
          <span class="meta-divider">•</span>
          <span class="meta-item">
            <i class="pi pi-user"></i>
            <span>Agente: <strong>{{ expediente.agenteAduaneroNombre || 'Agente Asignado' }}</strong></span>
          </span>
        </div>
      </div>

      <div class="hero-right-col">
        <div class="status-summary-card">
          <span class="status-kicker">ESTADO OPERATIVO</span>
          <div class="status-badge" :class="getEstadoBadgeClass(expediente.estadoExpediente)">
            <span class="status-dot"></span>
            <span>{{ formatEstado(expediente.estadoExpediente) }}</span>
          </div>
          <span class="dam-text font-mono" v-if="expediente.numeroDam">
            DAM: {{ expediente.numeroDam }}
          </span>
        </div>
      </div>
    </div>

    <!-- Alerta de Error de la Operación -->
    <div v-if="errorMessage" class="operacion-alert error" role="alert">
      <i class="pi pi-exclamation-triangle"></i>
      <span>{{ errorMessage }}</span>
    </div>

    <!-- ============================================================== -->
    <!-- 3. WORKSPACE PRINCIPAL A DOS COLUMNAS                          -->
    <!-- ============================================================== -->
    <div class="operacion-workspace-layout" v-if="expediente">
      <!-- ------------------------------------------------------------ -->
      <!-- COLUMNA IZQUIERDA: CONTEXTO LOGÍSTICO, RUTA & CARGA          -->
      <!-- ------------------------------------------------------------ -->
      <aside class="logistics-context-column">
        <!-- Tarjeta de Ruta POL -> POD -->
        <div class="context-card route-card">
          <div class="card-title-row">
            <i class="pi pi-map"></i>
            <span>Ruta Logística Internacional</span>
          </div>

          <div class="route-display-vertical">
            <!-- Origen -->
            <div class="route-station origin">
              <div class="station-icon-col">
                <div class="station-dot origin-dot"></div>
                <div class="station-line"></div>
              </div>
              <div class="station-info">
                <span class="station-role">{{ originRoleLabel }}</span>
                <span class="station-name">{{ getPuertoOrigen }}</span>
                <span class="station-country" v-if="expediente.puertoEnvioPais">{{ expediente.puertoEnvioPais }}</span>
              </div>
            </div>

            <!-- Travesía Central -->
            <div class="route-mid-journey">
              <div class="journey-badge">
                <i :class="modalidadIcon"></i>
                <span>{{ travesiaLabel }}</span>
              </div>
              <span class="journey-lead-time" v-if="expediente.fechaZarpeEtd || expediente.fechaLlegadaEta">
                {{ formatJourneyTime }}
              </span>
            </div>

            <!-- Destino -->
            <div class="route-station destination">
              <div class="station-icon-col">
                <div class="station-dot dest-dot"></div>
              </div>
              <div class="station-info">
                <span class="station-role">{{ destinationRoleLabel }}</span>
                <span class="station-name">{{ getPuertoDestino }}</span>
                <span class="station-country">Perú</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Tarjeta de Especificaciones de Carga -->
        <div class="context-card specs-card">
          <div class="card-title-row">
            <i class="pi pi-box"></i>
            <span>Detalles Técnicos de Carga</span>
          </div>

          <div class="specs-list">
            <div class="spec-row">
              <span class="spec-label">Mercancía:</span>
              <span class="spec-value strong">{{ expediente.mercanciaNombre || 'Carga Comercial' }}</span>
            </div>
            <div class="spec-row">
              <span class="spec-label">Tipo de Carga:</span>
              <span class="spec-value">{{ expediente.tipoCargaNombre || 'FCL / Carga General' }}</span>
            </div>
            <div class="spec-row">
              <span class="spec-label">Bultos / Cantidad:</span>
              <span class="spec-value">{{ expediente.cantidadBultos || 1 }} bulto(s)</span>
            </div>
            <div class="spec-row">
              <span class="spec-label">Peso Bruto:</span>
              <span class="spec-value">{{ expediente.pesoKg ? `${Number(expediente.pesoKg).toLocaleString()} kg` : '—' }}</span>
            </div>
            <div class="spec-row" v-if="expediente.naveViaje">
              <span class="spec-label">Carrier / Nave:</span>
              <span class="spec-value font-mono">{{ expediente.naveViaje }}</span>
            </div>
            <div class="spec-row" v-if="expediente.numeroContenedor">
              <span class="spec-label">Contenedor / AWB:</span>
              <span class="spec-value font-mono">{{ expediente.numeroContenedor }}</span>
            </div>
            <div class="spec-row" v-if="expediente.precintoAduanero">
              <span class="spec-label">Precinto de Origen:</span>
              <span class="spec-value font-mono">{{ expediente.precintoAduanero }}</span>
            </div>
          </div>
        </div>

        <!-- Tarjeta de Orientación Operativa -->
        <div class="context-card guidance-card">
          <div class="card-title-row">
            <i class="pi pi-info-circle"></i>
            <span>Guía de Hito Aduanero</span>
          </div>
          <p class="guidance-text">
            {{ currentGuidanceText }}
          </p>
        </div>
      </aside>

      <!-- ------------------------------------------------------------ -->
      <!-- COLUMNA DERECHA: FORMULARIO PRINCIPAL DE OPERACIÓN           -->
      <!-- ------------------------------------------------------------ -->
      <main class="operation-form-column">
        <!-- ========================================================== -->
        <!-- FORMA 1: REGISTRAR SALIDA DE ORIGEN                        -->
        <!-- ========================================================== -->
        <form
          v-if="activeMode === 'salida'"
          @submit.prevent="handleSalidaSubmit"
          class="op-form-container"
        >
          <div class="op-form-header">
            <div>
              <h2 class="form-title">
                <i class="pi pi-send"></i>
                <span>Registrar Salida de Origen & Embarque Internacional</span>
              </h2>
              <p class="form-subtitle">
                Certifique la partida de la carga en el puerto o aeropuerto de embarque extranjero.
              </p>
            </div>
            <span class="form-pill-step">Hito Operativo 1 / 3</span>
          </div>

          <div class="op-form-body">
            <!-- Bloque 1: Transporte -->
            <div class="op-form-group">
              <div class="group-kicker">
                <i class="pi pi-compass"></i>
                <span>Medio de Transporte & Compañía Carrier</span>
              </div>
              <div class="fields-grid two-cols">
                <div class="field-item">
                  <label for="salida-linea">{{ labelTransportista }} <span class="req">*</span></label>
                  <input
                    id="salida-linea"
                    v-model.trim="salidaForm.lineaTransporte"
                    type="text"
                    list="lineas-carrier"
                    :placeholder="placeholderTransportista"
                    class="op-input uppercase"
                    required
                    maxlength="60"
                  />
                  <datalist id="lineas-carrier">
                    <template v-if="isAereo">
                      <option value="LATAM CARGO" />
                      <option value="ATLAS AIR" />
                      <option value="KLM CARGO" />
                      <option value="AVIANCA CARGO" />
                      <option value="LUFTHANSA CARGO" />
                      <option value="QATAR AIRWAYS CARGO" />
                    </template>
                    <template v-else-if="isTerrestre">
                      <option value="RANSA LOGÍSTICA" />
                      <option value="CRUZ DEL SUR CARGO" />
                      <option value="TRANSPORTES HNOS. MEZA" />
                    </template>
                    <template v-else>
                      <option value="MSC (Mediterranean Shipping Company)" />
                      <option value="MAERSK LINE" />
                      <option value="CMA CGM" />
                      <option value="HAPAG-LLOYD" />
                      <option value="EVERGREEN MARINE" />
                      <option value="ONE (Ocean Network Express)" />
                      <option value="COSCO SHIPPING" />
                    </template>
                  </datalist>
                </div>

                <div class="field-item">
                  <label for="salida-nave">{{ labelNaveViaje }} <span class="req">*</span></label>
                  <input
                    id="salida-nave"
                    v-model.trim="salidaForm.nombreNaveViaje"
                    type="text"
                    :placeholder="placeholderNaveViaje"
                    class="op-input uppercase font-mono"
                    required
                    maxlength="70"
                  />
                </div>
              </div>
            </div>

            <!-- Bloque 2: Carga & Precinto -->
            <div class="op-form-group">
              <div class="group-kicker">
                <i class="pi pi-box"></i>
                <span>Unidad de Carga & Precinto de Seguridad</span>
              </div>
              <div class="fields-grid two-cols">
                <div class="field-item">
                  <label for="salida-cont">{{ labelUnidadCarga }} <span class="req">*</span></label>
                  <input
                    id="salida-cont"
                    v-model="salidaForm.numeroContenedor"
                    type="text"
                    :placeholder="placeholderUnidadCarga"
                    class="op-input uppercase font-mono"
                    required
                    maxlength="30"
                    @input="salidaForm.numeroContenedor = salidaForm.numeroContenedor.toUpperCase()"
                  />
                </div>

                <div class="field-item">
                  <label for="salida-precinto">{{ labelPrecinto }} <span class="req">*</span></label>
                  <input
                    id="salida-precinto"
                    v-model="salidaForm.precintoAduanero"
                    type="text"
                    :placeholder="placeholderPrecinto"
                    class="op-input uppercase font-mono"
                    required
                    maxlength="50"
                    @input="salidaForm.precintoAduanero = salidaForm.precintoAduanero.toUpperCase()"
                  />
                </div>
              </div>
            </div>

            <!-- Bloque 3: Fechas y Estimación -->
            <div class="op-form-group">
              <div class="group-kicker">
                <i class="pi pi-calendar"></i>
                <span>Cronograma de Salida & Arribo Estimado</span>
              </div>
              <div class="fields-grid two-cols">
                <div class="field-item">
                  <label for="salida-zarpe">{{ labelFechaZarpe }} <span class="req">*</span></label>
                  <input
                    id="salida-zarpe"
                    v-model="salidaForm.fechaZarpeEtd"
                    type="date"
                    class="op-input"
                    required
                  />
                  <div class="date-human-hint" v-if="salidaForm.fechaZarpeEtd">
                    <i class="pi pi-calendar-clock"></i>
                    <span>{{ formatHumanDate(salidaForm.fechaZarpeEtd) }}</span>
                  </div>
                </div>

                <div class="field-item">
                  <div class="field-label-row">
                    <label for="salida-eta">Fecha Estimada de Llegada (ETA)</label>
                    <div class="eta-presets" v-if="salidaForm.fechaZarpeEtd">
                      <span class="presets-lbl">Sugerencias:</span>
                      <button type="button" class="btn-eta-chip" @click="aplicarPresetSalidaEta(0)" title="Arribo el mismo día">Mismo día</button>
                      <button type="button" class="btn-eta-chip" @click="aplicarPresetSalidaEta(1)" title="Llegada al día siguiente">+1d</button>
                      <button type="button" class="btn-eta-chip" @click="aplicarPresetSalidaEta(3)" title="+3 días">+3d</button>
                      <button type="button" class="btn-eta-chip" @click="aplicarPresetSalidaEta(7)" title="+7 días">+7d</button>
                      <button type="button" class="btn-eta-chip" @click="aplicarPresetSalidaEta(15)" title="+15 días">+15d</button>
                      <button type="button" class="btn-eta-chip" @click="aplicarPresetSalidaEta(30)" title="+30 días">+30d</button>
                    </div>
                  </div>
                  <input
                    id="salida-eta"
                    v-model="salidaForm.fechaLlegadaEta"
                    type="date"
                    :min="salidaForm.fechaZarpeEtd || undefined"
                    class="op-input"
                  />
                  <div class="date-human-hint" v-if="salidaForm.fechaLlegadaEta">
                    <i class="pi pi-calendar-clock"></i>
                    <span>{{ formatHumanDate(salidaForm.fechaLlegadaEta) }}</span>
                  </div>
                </div>

                <!-- Indicador de Tránsito Calculado -->
                <div class="field-item full-width" v-if="tiempoTransitoCalculado">
                  <div :class="['lead-time-banner', tiempoTransitoCalculado.esValido ? 'valid' : 'invalid']">
                    <div class="lead-time-content">
                      <i :class="tiempoTransitoCalculado.esValido ? 'pi pi-clock' : 'pi pi-exclamation-triangle'"></i>
                      <span>{{ tiempoTransitoCalculado.texto }}</span>
                    </div>
                    <button
                      v-if="!tiempoTransitoCalculado.esValido && tiempoTransitoCalculado.puedeInvertir"
                      type="button"
                      class="btn-swap-dates-action"
                      @click="invertirMesDiaSalida"
                      title="Si tu navegador está en inglés y tomó DD como MM, haz clic aquí para corregirlo con un solo clic"
                    >
                      <i class="pi pi-arrows-h"></i>
                      <span>Corregir formato (Invertir Día y Mes)</span>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Bloque 4: Bitácora -->
            <div class="op-form-group">
              <div class="group-kicker">
                <i class="pi pi-file-edit"></i>
                <span>Bitácora de Embarque & Notas Operativas</span>
              </div>
              <div class="field-item full-width">
                <textarea
                  id="salida-obs"
                  v-model.trim="salidaForm.observaciones"
                  rows="3"
                  placeholder="Detalles sobre escalas técnicas (ej. Manzanillo, Balboa), notas de estiba, transbordos programados..."
                  class="op-textarea"
                ></textarea>
              </div>
            </div>
          </div>

          <!-- Pie de Acciones -->
          <footer class="op-form-footer">
            <button
              type="button"
              class="btn-outline-warm"
              @click="$emit('back')"
              :disabled="loading"
            >
              Cancelar
            </button>
            <button
              type="submit"
              class="btn-espresso"
              :disabled="loading || (tiempoTransitoCalculado && !tiempoTransitoCalculado.esValido)"
            >
              <i v-if="loading" class="pi pi-spin pi-spinner"></i>
              <i v-else class="pi pi-send"></i>
              <span>Confirmar Salida de Origen</span>
            </button>
          </footer>
        </form>

        <!-- ========================================================== -->
        <!-- FORMA 2: ACTUALIZAR DATOS DE TRÁNSITO                      -->
        <!-- ========================================================== -->
        <form
          v-else-if="activeMode === 'transito'"
          @submit.prevent="handleTransitoSubmit"
          class="op-form-container"
        >
          <div class="op-form-header">
            <div>
              <h2 class="form-title">
                <i class="pi pi-globe"></i>
                <span>Actualizar Seguimiento de Travesía Internacional</span>
              </h2>
              <p class="form-subtitle">
                Modifique los datos de transporte, transbordo o fechas de arribo en caso de novedades durante la navegación.
              </p>
            </div>
            <span class="form-pill-step">Hito Operativo 2 / 3</span>
          </div>

          <div class="op-form-body">
            <div class="op-form-group">
              <div class="group-kicker">
                <i class="pi pi-sliders-h"></i>
                <span>Parámetros de Navegación & Unidad</span>
              </div>
              <div class="fields-grid two-cols">
                <div class="field-item">
                  <label for="tr-nave">{{ labelNaveViaje }} <span class="req">*</span></label>
                  <input
                    id="tr-nave"
                    v-model.trim="transitoForm.naveViaje"
                    type="text"
                    :placeholder="placeholderNaveViaje"
                    class="op-input uppercase font-mono"
                    required
                    maxlength="100"
                  />
                </div>

                <div class="field-item">
                  <label for="tr-cont">{{ labelUnidadCarga }} <span class="req">*</span></label>
                  <input
                    id="tr-cont"
                    v-model="transitoForm.numeroContenedor"
                    type="text"
                    :placeholder="placeholderUnidadCarga"
                    class="op-input uppercase font-mono"
                    required
                    maxlength="30"
                    @input="transitoForm.numeroContenedor = transitoForm.numeroContenedor.toUpperCase()"
                  />
                </div>

                <div class="field-item">
                  <label for="tr-precinto">{{ labelPrecinto }} <span class="req">*</span></label>
                  <input
                    id="tr-precinto"
                    v-model="transitoForm.precintoAduanero"
                    type="text"
                    :placeholder="placeholderPrecinto"
                    class="op-input uppercase font-mono"
                    required
                    maxlength="50"
                    @input="transitoForm.precintoAduanero = transitoForm.precintoAduanero.toUpperCase()"
                  />
                </div>

                <div class="field-item">
                  <label for="tr-zarpe">{{ labelFechaZarpe }}</label>
                  <input
                    id="tr-zarpe"
                    v-model="transitoForm.fechaZarpeEtd"
                    type="date"
                    class="op-input"
                  />
                  <div class="date-human-hint" v-if="transitoForm.fechaZarpeEtd">
                    <i class="pi pi-calendar-clock"></i>
                    <span>{{ formatHumanDate(transitoForm.fechaZarpeEtd) }}</span>
                  </div>
                </div>

                <div class="field-item full-width">
                  <label for="tr-eta">Fecha Estimada de Llegada (ETA)</label>
                  <input
                    id="tr-eta"
                    v-model="transitoForm.fechaLlegadaEta"
                    type="date"
                    :min="transitoForm.fechaZarpeEtd || undefined"
                    class="op-input"
                  />
                  <div class="date-human-hint" v-if="transitoForm.fechaLlegadaEta">
                    <i class="pi pi-calendar-clock"></i>
                    <span>{{ formatHumanDate(transitoForm.fechaLlegadaEta) }}</span>
                  </div>
                </div>

                <div class="field-item full-width">
                  <label for="tr-obs">Bitácora & Observaciones de Navegación</label>
                  <textarea
                    id="tr-obs"
                    v-model.trim="transitoForm.observaciones"
                    rows="3"
                    placeholder="Novedades en alta mar, cambios de escala, alertas meteorológicas o avisos de recalada..."
                    class="op-textarea"
                  ></textarea>
                </div>
              </div>
            </div>
          </div>

          <footer class="op-form-footer">
            <button
              type="button"
              class="btn-outline-warm"
              @click="$emit('back')"
              :disabled="loading"
            >
              Cancelar
            </button>
            <button
              type="submit"
              class="btn-espresso"
              :disabled="loading"
            >
              <i v-if="loading" class="pi pi-spin pi-spinner"></i>
              <i v-else class="pi pi-save"></i>
              <span>Guardar Cambios de Tránsito</span>
            </button>
          </footer>
        </form>

        <!-- ========================================================== -->
        <!-- FORMA 3: CONFIRMAR ARRIBO A DESTINO                        -->
        <!-- ========================================================== -->
        <form
          v-else-if="activeMode === 'arribo'"
          @submit.prevent="handleArriboSubmit"
          class="op-form-container"
        >
          <div class="op-form-header">
            <div>
              <h2 class="form-title">
                <i class="pi pi-compass"></i>
                <span>Confirmar Arribo a Destino & Terminal de Descarga</span>
              </h2>
              <p class="form-subtitle">
                Registre la llegada oficial a territorio peruano para habilitar la inspección y el Canal SUNAT.
              </p>
            </div>
            <span class="form-pill-step">Hito Operativo 3 / 3</span>
          </div>

          <div class="op-form-body">
            <div class="op-form-group">
              <div class="group-kicker">
                <i class="pi pi-building"></i>
                <span>Terminal Portuario / Aeroportuario de Llegada</span>
              </div>
              <div class="fields-grid single-col">
                <div class="field-item">
                  <label for="arribo-terminal">{{ labelTerminal }} <span class="req">*</span></label>
                  <input
                    id="arribo-terminal"
                    v-model.trim="arriboForm.terminalAlmacenDestino"
                    type="text"
                    list="terminales-nacionales"
                    :placeholder="placeholderTerminal"
                    class="op-input"
                    required
                  />
                  <datalist id="terminales-nacionales">
                    <template v-if="isAereo">
                      <option value="Talma Servicios Aeroportuarios (Aeropuerto Jorge Chávez)" />
                      <option value="Shohin Depósito Temporal Aéreo" />
                      <option value="Swissport Cargo Perú" />
                      <option value="Servicios Aeroportuarios Andinos (SAASA)" />
                    </template>
                    <template v-else-if="isTerrestre">
                      <option value="Ransa Depósito Aduanero Terrestre" />
                      <option value="Terminal Terrestre Tacna / Puno" />
                      <option value="Neptunia Almacén Temporal" />
                    </template>
                    <template v-else>
                      <option value="DP World Callao (Muelle Sur)" />
                      <option value="APM Terminals Callao (Muelle Norte)" />
                      <option value="Imupesa Depósito Temporal" />
                      <option value="Ransa Depósito Aduanero" />
                      <option value="Neptunia Almacén Temporal" />
                    </template>
                  </datalist>
                </div>

                <div class="field-item">
                  <label for="arribo-fecha">{{ labelFechaArribo }} <span class="req">*</span></label>
                  <input
                    id="arribo-fecha"
                    v-model="arriboForm.fechaArriboAduana"
                    type="datetime-local"
                    class="op-input font-mono"
                    required
                  />
                </div>

                <div class="field-item">
                  <label for="arribo-obs">Observaciones de Descarga & Manifiesto Aduanero</label>
                  <textarea
                    id="arribo-obs"
                    v-model.trim="arriboForm.observaciones"
                    rows="3"
                    :placeholder="placeholderObservacionesArribo"
                    class="op-textarea"
                  ></textarea>
                </div>
              </div>
            </div>

            <!-- Aviso SUNAT -->
            <div class="notice-callout-box">
              <i class="pi pi-shield"></i>
              <div>
                <strong>Activación de Diligencia y Canal SUNAT</strong>
                <p>
                  Al confirmar la llegada al país, el expediente quedará habilitado para que el
                  <strong>Supervisor Aduanero</strong> valide el <strong>Canal SUNAT</strong> (Verde, Naranja o Rojo).
                </p>
              </div>
            </div>
          </div>

          <footer class="op-form-footer">
            <button
              type="button"
              class="btn-outline-warm"
              @click="$emit('back')"
              :disabled="loading"
            >
              Cancelar
            </button>
            <button
              type="submit"
              class="btn-espresso"
              :disabled="loading"
            >
              <i v-if="loading" class="pi pi-spin pi-spinner"></i>
              <i v-else class="pi pi-check"></i>
              <span>Confirmar Llegada a Destino</span>
            </button>
          </footer>
        </form>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { expedienteService } from '@/features/aduanas/services/expedienteService'
import {
  parseLocalDate,
  getLocalDateString,
  formatHumanDate,
  formatHumanShort,
  calculateTransitDays,
  canSwapMonthDay,
  swapMonthDay,
  addDaysToDate
} from '@/shared/utils/dateUtils'

const props = defineProps({
  expediente: {
    type: Object,
    required: true
  },
  initialMode: {
    type: String,
    default: 'salida' // 'salida' | 'transito' | 'arribo'
  }
})

const emit = defineEmits(['back', 'saved'])

const activeMode = ref(props.initialMode || 'salida')
const loading = ref(false)
const errorMessage = ref('')

// Detección de Modalidad
const isAereo = computed(() => {
  const m = (props.expediente?.modalidad || '').toUpperCase()
  return m.includes('AER')
})

const isTerrestre = computed(() => {
  const m = (props.expediente?.modalidad || '').toUpperCase()
  return m.includes('TERR')
})

const isExport = computed(() => {
  const reg = (props.expediente?.tipoRegimen || '').toUpperCase()
  return reg.includes('EXPORT')
})

const modalidadIcon = computed(() => {
  if (isAereo.value) return 'pi pi-send'
  if (isTerrestre.value) return 'pi pi-truck'
  return 'pi pi-compass'
})

// Textos adaptables
const originRoleLabel = computed(() => {
  if (isAereo.value) return 'AEROPUERTO ORIGEN'
  if (isTerrestre.value) return 'TERMINAL ORIGEN'
  return 'PUERTO ORIGEN (POL)'
})

const destinationRoleLabel = computed(() => {
  if (isAereo.value) return 'AEROPUERTO DESTINO'
  if (isTerrestre.value) return 'TERMINAL DESTINO'
  return 'PUERTO DESTINO (POD)'
})

const travesiaLabel = computed(() => {
  if (isAereo.value) return 'Vuelo Internacional'
  if (isTerrestre.value) return 'Tránsito Terrestre'
  return 'Travesía Internacional'
})

const labelTransportista = computed(() => {
  if (isAereo.value) return 'Aerolínea / Transportista'
  if (isTerrestre.value) return 'Empresa de Transporte'
  return 'Línea Naviera / Transportista'
})

const placeholderTransportista = computed(() => {
  if (isAereo.value) return 'Ej. LATAM Cargo, Atlas Air, KLM Cargo...'
  if (isTerrestre.value) return 'Ej. Ransa, Cruz del Sur Cargo...'
  return 'Ej. MSC, Maersk, Hapag-Lloyd, CMA CGM...'
})

const labelNaveViaje = computed(() => {
  if (isAereo.value) return 'N° de Vuelo / Guía AWB'
  if (isTerrestre.value) return 'Placa Tracto / Remolque'
  return 'Nave / Buque y N° de Viaje'
})

const placeholderNaveViaje = computed(() => {
  if (isAereo.value) return 'Ej. LA2401 - Vuelo Cargo'
  if (isTerrestre.value) return 'Ej. V1A-820 / Remolque R2B'
  return 'Ej. MSC INES - V. 402W'
})

const labelUnidadCarga = computed(() => {
  if (isAereo.value) return 'N° Pallet / Bulto / Contenedor Aéreo'
  if (isTerrestre.value) return 'N° Semirremolque / Furgón'
  return 'N° de Contenedor (ISO 6346)'
})

const placeholderUnidadCarga = computed(() => {
  if (isAereo.value) return 'Ej. PMC-48291-LA'
  if (isTerrestre.value) return 'Ej. FUR-90218'
  return 'Ej. MSKU-549281-0'
})

const labelPrecinto = computed(() => {
  if (isAereo.value) return 'Precinto de Seguridad / Sello'
  return 'Precinto Naviero / Sello de Origen'
})

const placeholderPrecinto = computed(() => {
  if (isAereo.value) return 'Ej. SL-892100'
  return 'Ej. ML-PE-892100'
})

const labelFechaZarpe = computed(() => {
  if (isAereo.value) return 'Fecha Real de Despegue / Salida (ETD)'
  if (isTerrestre.value) return 'Fecha de Salida de Terminal (ETD)'
  return 'Fecha Real de Salida / Zarpe (ETD)'
})

const labelTerminal = computed(() => {
  if (isAereo.value) return 'Terminal Aeroportuario / Depósito Temporal de Llegada'
  if (isTerrestre.value) return 'Terminal Terrestre / Depósito Temporal'
  return 'Terminal Portuario / Depósito Temporal de Llegada'
})

const placeholderTerminal = computed(() => {
  if (isAereo.value) return 'Ej. Talma Aeropuerto / Shohin / SAASA'
  if (isTerrestre.value) return 'Ej. Ransa Depósito / Terminal Terrestre'
  return 'Ej. DP World Callao / APM Terminals Callao'
})

const labelFechaArribo = computed(() => {
  if (isAereo.value) return 'Fecha y Hora Real de Aterrizaje en Destino'
  return 'Fecha y Hora Real de Arribo / Atraque a Puerto'
})

const placeholderObservacionesArribo = computed(() => {
  if (isAereo.value) return 'Vuelo aterrizado, bultos y guías AWB verificados sin daños ni faltantes...'
  return 'Nave atracada en muelle, precintos verificados, manifiesto aduanero conforme sin faltantes...'
})

const getPuertoOrigen = computed(() => {
  const exp = props.expediente
  if (!exp) return 'Punto de Origen'
  return exp.puertoEnvioNombre || (exp.puertoEnvioPais ? `Puerto (${exp.puertoEnvioPais})` : 'Punto de Origen')
})

const getPuertoDestino = computed(() => {
  const exp = props.expediente
  if (!exp) return 'Puerto del Callao'
  return exp.puertoDestinoNombre || 'Callao / Lima (Perú)'
})

const formatJourneyTime = computed(() => {
  const zarpe = props.expediente?.fechaZarpeEtd
  const eta = props.expediente?.fechaLlegadaEta
  if (zarpe && eta) {
    const diff = calculateTransitDays(zarpe, eta)
    if (diff === null) return 'Fechas registradas'
    if (diff === 0) return 'Mismo día (vuelo directo)'
    if (diff > 0) return `~${diff} día${diff === 1 ? '' : 's'} de travesía`
    return 'Fechas a verificar'
  }
  if (zarpe) return `Zarpe: ${formatHumanShort(zarpe)}`
  if (eta) return `ETA: ${formatHumanShort(eta)}`
  return 'En programación'
})

const currentGuidanceText = computed(() => {
  if (activeMode.value === 'salida') {
    return 'Al registrar la salida de origen, el despacho cambia formalmente a estado EN TRÁNSITO y se activa el seguimiento en tiempo real para el cliente en el portal B2B.'
  }
  if (activeMode.value === 'arribo') {
    return 'Al confirmar el arribo a territorio nacional, la carga queda lista para el desaduanaje y el Supervisor Aduanero procederá a registrar el Canal de Control SUNAT (Verde, Naranja o Rojo).'
  }
  return 'Permite actualizar datos de la nave, número de contenedor, precinto de seguridad o fechas estimadas en caso de retraso o escalas técnicas en puertos intermedios.'
})

// Formularios
const salidaForm = ref({
  lineaTransporte: '',
  nombreNaveViaje: '',
  numeroContenedor: '',
  precintoAduanero: '',
  fechaZarpeEtd: '',
  fechaLlegadaEta: '',
  observaciones: ''
})

const transitoForm = ref({
  naveViaje: '',
  numeroContenedor: '',
  precintoAduanero: '',
  fechaZarpeEtd: '',
  fechaLlegadaEta: '',
  observaciones: ''
})

const arriboForm = ref({
  terminalAlmacenDestino: '',
  fechaArriboAduana: '',
  observaciones: ''
})

const getLocalDateTimeString = (date = new Date()) => {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

// Inicializar formularios con datos del expediente
const initializeForms = (exp) => {
  if (!exp) return

  let linea = ''
  let nave = exp.naveViaje || ''
  if (nave.includes(' - ')) {
    const parts = nave.split(' - ')
    linea = parts[0]
    nave = parts.slice(1).join(' - ')
  }

  const todayStr = getLocalDateString(new Date())

  salidaForm.value = {
    lineaTransporte: linea,
    nombreNaveViaje: nave,
    numeroContenedor: (exp.numeroContenedor || '').toUpperCase(),
    precintoAduanero: (exp.precintoAduanero || '').toUpperCase(),
    fechaZarpeEtd: exp.fechaZarpeEtd || todayStr,
    fechaLlegadaEta: exp.fechaLlegadaEta || '',
    observaciones: ''
  }

  transitoForm.value = {
    naveViaje: exp.naveViaje || '',
    numeroContenedor: (exp.numeroContenedor || '').toUpperCase(),
    precintoAduanero: (exp.precintoAduanero || '').toUpperCase(),
    fechaZarpeEtd: exp.fechaZarpeEtd || '',
    fechaLlegadaEta: exp.fechaLlegadaEta || '',
    observaciones: exp.observaciones || ''
  }

  const defaultTerminal = isAereo.value
    ? 'Talma Servicios Aeroportuarios (Aeropuerto Jorge Chávez)'
    : 'DP World Callao - Muelle Sur'

  arriboForm.value = {
    terminalAlmacenDestino: exp.terminalAlmacenDestino || defaultTerminal,
    fechaArriboAduana: exp.fechaArriboAduana ? exp.fechaArriboAduana.slice(0, 16) : getLocalDateTimeString(),
    observaciones: ''
  }
}

watch(
  () => props.expediente,
  (newExp) => {
    initializeForms(newExp)
  },
  { immediate: true }
)

watch(
  () => props.initialMode,
  (mode) => {
    if (mode) activeMode.value = mode
  }
)

// Lead Time dinámico y validación de cronograma
const tiempoTransitoCalculado = computed(() => {
  const { fechaZarpeEtd, fechaLlegadaEta } = salidaForm.value
  if (!fechaZarpeEtd || !fechaLlegadaEta) return null

  const diffDays = calculateTransitDays(fechaZarpeEtd, fechaLlegadaEta)
  if (diffDays === null) return null

  if (diffDays < 0) {
    const invertible = canSwapMonthDay(fechaZarpeEtd) || canSwapMonthDay(fechaLlegadaEta)
    return {
      esValido: false,
      puedeInvertir: invertible,
      texto: 'La fecha estimada de arribo (ETA) debe ser igual o posterior a la fecha de salida.'
    }
  }

  if (diffDays === 0) {
    return {
      esValido: true,
      texto: isAereo.value
        ? 'Arribo el mismo día — Vuelo internacional directo / Courier Express.'
        : 'Arribo estimado el mismo día de la salida (tránsito local/directo).'
    }
  }

  if (diffDays === 1) {
    return {
      esValido: true,
      texto: 'Trayecto estimado: 1 día de viaje internacional.'
    }
  }

  return {
    esValido: true,
    texto: `Trayecto estimado: ${diffDays} días de viaje internacional.`
  }
})

// Acciones auxiliares de fechas
const aplicarPresetSalidaEta = (dias) => {
  if (!salidaForm.value.fechaZarpeEtd) return
  salidaForm.value.fechaLlegadaEta = addDaysToDate(salidaForm.value.fechaZarpeEtd, dias)
}

const invertirMesDiaSalida = () => {
  if (canSwapMonthDay(salidaForm.value.fechaZarpeEtd)) {
    salidaForm.value.fechaZarpeEtd = swapMonthDay(salidaForm.value.fechaZarpeEtd)
  }
  if (canSwapMonthDay(salidaForm.value.fechaLlegadaEta)) {
    salidaForm.value.fechaLlegadaEta = swapMonthDay(salidaForm.value.fechaLlegadaEta)
  }
}

// Handlers de Envío
const handleSalidaSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''

  if (!salidaForm.value.fechaZarpeEtd) {
    errorMessage.value = 'Debe indicar la fecha real de salida de origen.'
    loading.value = false
    return
  }

  const linea = salidaForm.value.lineaTransporte.trim()
  const nave = salidaForm.value.nombreNaveViaje.trim()
  const naveCompleta = linea && !nave.includes(linea) ? `${linea} - ${nave}` : (nave || linea)

  try {
    const updated = await expedienteService.confirmarSalidaOrigen(props.expediente.idExpediente, {
      fechaZarpeReal: salidaForm.value.fechaZarpeEtd,
      naveViaje: naveCompleta,
      numeroContenedor: salidaForm.value.numeroContenedor.trim(),
      precintoAduanero: salidaForm.value.precintoAduanero.trim(),
      fechaLlegadaEta: salidaForm.value.fechaLlegadaEta || null,
      observaciones: salidaForm.value.observaciones || null
    })

    emit('saved', updated)
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al registrar la salida de origen.'
  } finally {
    loading.value = false
  }
}

const handleTransitoSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''

  try {
    const payload = {
      naveViaje: transitoForm.value.naveViaje,
      numeroContenedor: transitoForm.value.numeroContenedor.trim(),
      precintoAduanero: transitoForm.value.precintoAduanero.trim(),
      fechaZarpeEtd: transitoForm.value.fechaZarpeEtd || null,
      fechaLlegadaEta: transitoForm.value.fechaLlegadaEta || null,
      observaciones: transitoForm.value.observaciones || null
    }

    const updated = await expedienteService.updateExpediente(props.expediente.idExpediente, payload)
    emit('saved', updated)
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al actualizar datos de travesía.'
  } finally {
    loading.value = false
  }
}

const handleArriboSubmit = async () => {
  if (!props.expediente?.idExpediente) return
  loading.value = true
  errorMessage.value = ''

  try {
    const updated = await expedienteService.confirmarArriboDestino(props.expediente.idExpediente, {
      fechaArriboAduana: arriboForm.value.fechaArriboAduana,
      terminalAlmacenDestino: arriboForm.value.terminalAlmacenDestino,
      observaciones: arriboForm.value.observaciones
    })

    emit('saved', updated)
  } catch (err) {
    errorMessage.value = err.response?.data?.message || 'Error al registrar el arribo de la carga a destino.'
  } finally {
    loading.value = false
  }
}

// Helpers de Formato
const formatEstado = (est) => {
  if (!est) return 'SIN ESTADO'
  const map = {
    REGISTRADO: 'REGISTRADO',
    APROBADO: 'APROBADO',
    ASIGNADO: 'ASIGNADO',
    EN_ORIGEN: 'EN ORIGEN',
    EN_TRANSITO: 'EN TRÁNSITO',
    ARRIBADO: 'ARRIBADO A PUERTO',
    EN_TRAMITE: 'EN TRÁMITE ADUANERO',
    NUMERADO: 'DAM NUMERADA',
    EN_INSPECCION: 'EN INSPECCIÓN',
    LEVANTE_AUTORIZADO: 'LEVANTE AUTORIZADO',
    EN_DISTRIBUCION: 'EN DISTRIBUCIÓN',
    ENTREGADO: 'ENTREGADO',
    FINALIZADO: 'FINALIZADO',
    CANCELADO: 'CANCELADO'
  }
  return map[est] || est.replace(/_/g, ' ')
}

const getEstadoBadgeClass = (est) => {
  if (!est) return 'badge-neutral'
  if (['ASIGNADO', 'EN_ORIGEN'].includes(est)) return 'badge-warning'
  if (['EN_TRANSITO', 'ARRIBADO'].includes(est)) return 'badge-info'
  if (['LEVANTE_AUTORIZADO', 'ENTREGADO', 'FINALIZADO'].includes(est)) return 'badge-success'
  return 'badge-neutral'
}
</script>

<style scoped>
.operacion-transito-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 1rem 1.5rem 3rem;
  width: 100%;
  color: var(--color-text-main);
  box-sizing: border-box;
}

.breadcrumbs {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.8125rem;
}

.crumb-muted {
  color: var(--color-text-subtle);
}

.crumb-active {
  color: var(--color-primary);
  font-weight: 700;
}

.hito-mode-switcher {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  background: var(--color-bg-subtle);
  padding: 0.25rem;
  border-radius: var(--radius-md, 8px);
  border: 1px solid var(--color-border);
}

.switcher-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.85rem;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.switcher-btn:hover {
  color: var(--color-text-main);
}

.switcher-btn.active {
  background: var(--color-bg-surface);
  color: var(--color-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

/* 2. Hero Banner */
.operacion-hero-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem 1.75rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  gap: 1.5rem;
  flex-wrap: wrap;
}

.hero-left-col {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.hero-kicker-row {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.hero-kicker-tag {
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker);
  text-transform: uppercase;
}

.modalidad-tag,
.regimen-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
  font-size: 0.6875rem;
  font-weight: 700;
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  color: var(--color-primary);
}

.hero-main-title {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 800;
  font-family: var(--font-mono);
  color: var(--color-text-main);
  letter-spacing: -0.02em;
}

.hero-meta-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.meta-item strong {
  color: var(--color-text-main);
}

.meta-sub {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
}

.meta-divider {
  color: var(--color-border-strong);
}

.hero-right-col {
  display: flex;
  align-items: center;
}

.status-summary-card {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.35rem;
  padding: 0.75rem 1.25rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
}

.status-kicker {
  font-size: 0.625rem;
  font-weight: 800;
  letter-spacing: 0.06em;
  color: var(--color-text-subtle);
  text-transform: uppercase;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.25rem 0.65rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 800;
}

.status-badge.badge-warning {
  background: var(--color-warning-bg);
  color: var(--color-warning-text);
  border: 1px solid var(--color-warning-border);
}

.status-badge.badge-info {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.status-badge.badge-success {
  background: var(--color-success-bg);
  color: var(--color-success-text);
  border: 1px solid var(--color-success-border);
}

.status-badge.badge-neutral {
  background: var(--color-bg-muted);
  color: var(--color-text-muted);
  border: 1px solid var(--color-border);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.dam-text {
  font-size: 0.7rem;
  color: var(--color-text-subtle);
}

/* Alerta de Error */
.operacion-alert.error {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.85rem 1.25rem;
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
  border-radius: var(--radius-md, 8px);
  font-size: 0.8125rem;
  font-weight: 600;
}

/* 3. Workspace Layout a Dos Columnas */
.operacion-workspace-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 1.5rem;
  align-items: start;
}

/* Columna Izquierda: Contexto Logístico */
.logistics-context-column {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.context-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 10px);
  padding: 1.15rem;
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.card-title-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8125rem;
  font-weight: 800;
  color: var(--color-text-main);
  padding-bottom: 0.45rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.card-title-row i {
  color: var(--color-accent-kicker);
}

/* Trazabilidad Vertical de Ruta */
.route-display-vertical {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0.5rem 0;
}

.route-station {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}

.station-icon-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 16px;
}

.station-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 3px solid var(--color-bg-surface);
  box-shadow: 0 0 0 1px var(--color-border-strong);
}

.station-dot.origin-dot {
  background: var(--color-primary);
}

.station-dot.dest-dot {
  background: var(--color-success-text);
}

.station-line {
  width: 2px;
  height: 48px;
  background: var(--color-border);
  margin: 3px 0;
}

.station-info {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.station-role {
  font-size: 0.625rem;
  font-weight: 800;
  color: var(--color-text-subtle);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.station-name {
  font-size: 0.875rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.station-country {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.route-mid-journey {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.25rem 0.5rem 0.25rem 2rem;
}

.journey-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.2rem 0.55rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: 9999px;
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-accent-kicker);
}

.journey-lead-time {
  font-size: 0.75rem;
  color: var(--color-text-subtle);
  font-style: italic;
}

/* Especificaciones */
.specs-list {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.spec-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8125rem;
  gap: 0.5rem;
}

.spec-label {
  color: var(--color-text-subtle);
}

.spec-value {
  color: var(--color-text-main);
  text-align: right;
}

.spec-value.strong {
  font-weight: 700;
}

/* Orientación */
.guidance-text {
  margin: 0;
  font-size: 0.78125rem;
  line-height: 1.5;
  color: var(--color-text-muted);
}

/* Columna Derecha: Formulario Operativo */
.operation-form-column {
  min-width: 0;
}

.op-form-container {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 10px);
  padding: 1.75rem;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.op-form-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border-subtle);
  gap: 1rem;
}

.form-title {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.form-title i {
  color: var(--color-accent-kicker);
}

.form-subtitle {
  margin: 0.25rem 0 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
}

.form-pill-step {
  display: inline-flex;
  padding: 0.25rem 0.65rem;
  border-radius: 9999px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  font-size: 0.6875rem;
  font-weight: 800;
  color: var(--color-text-subtle);
  white-space: nowrap;
}

.op-form-body {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.op-form-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.group-kicker {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding-bottom: 0.35rem;
  border-bottom: 1px dashed var(--color-border);
}

.group-kicker i {
  color: var(--color-accent-kicker);
}

.fields-grid {
  display: grid;
  gap: 1rem;
}

.fields-grid.two-cols {
  grid-template-columns: repeat(2, 1fr);
}

.fields-grid.single-col {
  grid-template-columns: 1fr;
}

.field-item {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.field-item.full-width {
  grid-column: span 2;
}

.field-item label {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.req {
  color: var(--color-danger-text);
  font-weight: bold;
}

.op-input,
.op-textarea {
  width: 100%;
  padding: 0.65rem 0.85rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 6px);
  font-size: 0.875rem;
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  box-sizing: border-box;
  transition: border-color var(--transition-fast, 0.15s ease), box-shadow var(--transition-fast, 0.15s ease);
}

.op-input:focus,
.op-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(180, 83, 9, 0.15);
}

.op-input.uppercase {
  text-transform: uppercase;
}

.font-mono {
  font-family: var(--font-mono);
}

.op-textarea {
  resize: vertical;
  min-height: 75px;
}

/* Banner de Lead Time */
.lead-time-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.65rem;
  padding: 0.65rem 0.85rem;
  border-radius: var(--radius-sm, 6px);
  font-size: 0.8125rem;
  font-weight: 700;
}

.lead-time-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.lead-time-banner.valid {
  background: var(--color-info-bg);
  color: var(--color-info-text);
  border: 1px solid var(--color-info-border);
}

.lead-time-banner.invalid {
  background: var(--color-danger-bg);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
}

.btn-swap-dates-action {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.7rem;
  border-radius: var(--radius-sm, 4px);
  background: var(--color-bg-surface);
  color: var(--color-danger-text);
  border: 1px solid var(--color-danger-border);
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-swap-dates-action:hover {
  background: var(--color-danger-text);
  color: #ffffff;
}

.date-human-hint {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin-top: 0.35rem;
  font-size: 0.75rem;
  color: var(--color-accent-kicker, #b8860b);
  font-weight: 600;
}

.field-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.25rem;
}

.eta-presets {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.presets-lbl {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
  font-weight: 500;
  margin-right: 0.15rem;
}

.btn-eta-chip {
  padding: 0.15rem 0.45rem;
  font-size: 0.6875rem;
  font-weight: 600;
  border-radius: 9999px;
  border: 1px solid var(--color-border);
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast, 0.15s ease);
}

.btn-eta-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-bg-surface);
}

/* Callout Box SUNAT */
.notice-callout-box {
  display: flex;
  align-items: flex-start;
  gap: 0.85rem;
  padding: 1rem 1.15rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
}

.notice-callout-box i {
  color: var(--color-accent-kicker);
  font-size: 1.25rem;
  margin-top: 0.1rem;
}

.notice-callout-box strong {
  display: block;
  font-size: 0.875rem;
  color: var(--color-text-main);
  margin-bottom: 0.2rem;
}

.notice-callout-box p {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  line-height: 1.45;
}

/* Pie del Formulario */
.op-form-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 1rem;
  padding-top: 1.25rem;
  border-top: 1px solid var(--color-border-subtle);
}

@media (max-width: 1024px) {
  .operacion-workspace-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .operacion-transito-view {
    padding: 0.75rem;
  }
  .fields-grid.two-cols {
    grid-template-columns: 1fr;
  }
  .field-item.full-width {
    grid-column: span 1;
  }
  .op-form-container {
    padding: 1.25rem;
  }
}
</style>
