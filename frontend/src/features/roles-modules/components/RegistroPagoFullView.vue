<template>
  <div class="registro-pago-full-view">
    <!-- 1. Barra Superior de Navegación & Acciones de la Ficha -->
    <div class="form-header-bar">
      <div class="header-breadcrumb-area">
        <button
          type="button"
          class="btn-back-link"
          @click="$emit('cancel')"
          :disabled="loading"
          title="Regresar a la lista de cotizaciones"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver a Pagos Pre-Expedientes</span>
        </button>

        <div class="header-titles">
          <div class="kicker-row">
            <span class="badge-kicker">FINANZAS & TESORERÍA // ABONO BANCARIO</span>
            <span class="mode-pill mode-create">
              <i class="pi pi-wallet"></i>
              REGISTRO DE PAGO / ANTICIPO
            </span>
          </div>
          <h2 class="form-main-title">
            {{
              activeCotizacion
                ? `Abono de Fondos: ${activeCotizacion.codigoCotizacion} — ${activeCotizacion.clienteRazonSocial || 'Cliente'}`
                : 'Registro de Abono / Anticipo de Cotización'
            }}
          </h2>
          <p class="form-main-desc">
            Ingrese los detalles del depósito bancario para revisión contable. La validación del abono (mínimo 30%)
            desbloqueará automáticamente la creación del Expediente Aduanero.
          </p>
        </div>
      </div>

      <!-- Acciones de Cabecera -->
      <div class="header-actions">
        <button
          type="button"
          class="btn-outline-warm"
          @click="$emit('cancel')"
          :disabled="loading"
        >
          <i class="pi pi-times"></i>
          <span>Descartar</span>
        </button>

        <button
          form="registro-pago-form"
          type="submit"
          class="btn-espresso"
          :disabled="loading || !isFormValid"
        >
          <i v-if="loading" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-send"></i>
          <span>Registrar y Enviar a Finanzas</span>
        </button>
      </div>
    </div>

    <!-- Error Alert Banner -->
    <transition name="slide-fade">
      <div v-if="errorMessage" class="error-banner">
        <i class="pi pi-exclamation-triangle"></i>
        <div class="error-content">
          <strong>Error de Registro:</strong>
          <span>{{ errorMessage }}</span>
        </div>
        <button type="button" class="error-close-btn" @click="errorMessage = ''">
          <i class="pi pi-times"></i>
        </button>
      </div>
    </transition>

    <!-- Banner Informativo de Validación Financiera -->
    <div class="info-alert-editorial">
      <div class="info-icon-badge">
        <i class="pi pi-shield"></i>
      </div>
      <div class="info-content-editorial">
        <h4 class="info-title">Validación Previa Requerida por Finanzas</h4>
        <p class="info-desc">
          El abono ingresado pasará inmediatamente a la bandeja de Finanzas. Una vez que el analista contable corrobore los fondos
          en la cuenta bancaria de SGA y apruebe el comprobante, el sistema <strong>desbloqueará automáticamente la creación del Expediente Aduanero</strong>.
        </p>
      </div>
    </div>

    <!-- 2. Grid Principal: Formulario Estructurado (68%) + Ficha Resumen Live (32%) -->
    <div class="form-content-grid">
      <!-- COLUMNA PRINCIPAL: BLOQUES DE CAMPOS -->
      <form id="registro-pago-form" @submit.prevent="handleSubmit" class="form-blocks-column">
        <!-- ======================================================== -->
        <!-- BLOQUE 01: COTIZACIÓN COMERCIAL VINCULADA                -->
        <!-- ======================================================== -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Cotización Comercial Vinculada</h3>
              <p class="block-sub">
                Identificación de la orden comercial aprobada y verificación del saldo pendiente de cobranza.
              </p>
            </div>
          </div>

          <div class="block-body">
            <!-- Si no se proporcionó cotización o el usuario desea cambiarla -->
            <div v-if="!activeCotizacion || cambiarCotizacionActiva" class="field-item full-width cotizacion-picker-wrap">
              <label for="f-cotizacion-select" class="field-label">
                <i class="pi pi-list"></i>
                <span>Seleccionar Cotización Aprobada por Cobrar *</span>
              </label>
              <select
                id="f-cotizacion-select"
                v-model="selectedCotizacionId"
                class="form-select picker-select"
                @change="onCotizacionSelected"
                required
                :disabled="loadingCotizaciones"
              >
                <option :value="null">-- Seleccione una Cotización Aprobada pendiente de pago --</option>
                <option
                  v-for="c in cotizacionesDisponibles"
                  :key="c.idCotizacion"
                  :value="c.idCotizacion"
                >
                  {{ c.codigoCotizacion }} — {{ c.clienteRazonSocial || 'Cliente' }} (Total: ${{ Number(c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }} | Saldo: ${{ Number(c.saldoPendiente != null ? c.saldoPendiente : c.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }})
                </option>
              </select>
              <div v-if="loadingCotizaciones" class="picker-status text-muted">
                <i class="pi pi-spin pi-spinner"></i>
                <span>Cargando cotizaciones aprobadas desde el servidor...</span>
              </div>
              <div v-else-if="cotizacionesDisponibles.length === 0" class="picker-status text-warning">
                <i class="pi pi-exclamation-circle"></i>
                <span>No se encontraron cotizaciones aprobadas pendientes de pago.</span>
              </div>
            </div>

            <!-- Ficha Detallada de la Cotización Activa -->
            <div v-if="activeCotizacion" class="cotizacion-selected-hero">
              <div class="hero-header-row">
                <div class="hero-code-box">
                  <span class="hero-kicker">COTIZACIÓN APROBADA</span>
                  <strong class="hero-code font-mono">{{ activeCotizacion.codigoCotizacion }}</strong>
                  <span class="hero-client-name">{{ activeCotizacion.clienteRazonSocial || 'Cliente Importador' }}</span>
                  <span class="hero-client-doc font-mono">RUC / Doc: {{ activeCotizacion.clienteNumeroDocumento || '—' }}</span>
                </div>

                <button
                  type="button"
                  class="btn-change-cot"
                  @click="toggleCambiarCotizacion"
                  title="Seleccionar otra cotización"
                >
                  <i class="pi pi-sync"></i>
                  <span>{{ cambiarCotizacionActiva ? 'Mantener Seleccionada' : 'Cambiar Cotización' }}</span>
                </button>
              </div>

              <div class="hero-metrics-grid">
                <div class="hero-metric-cell">
                  <span class="metric-cell-lbl">Régimen Aduanero</span>
                  <strong class="metric-cell-val">{{ activeCotizacion.regimenAduanero || 'IMPORTACIÓN DEFINITIVA' }}</strong>
                  <span class="metric-cell-sub">{{ activeCotizacion.mercanciaNombre || activeCotizacion.tipoCargaNombre || 'Carga General' }}</span>
                </div>

                <div class="hero-metric-cell">
                  <span class="metric-cell-lbl">Total Cotizado</span>
                  <strong class="metric-cell-val font-mono text-primary">
                    $ {{ Number(activeCotizacion.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                  </strong>
                  <span class="metric-cell-sub">Monto comercial neto</span>
                </div>

                <div class="hero-metric-cell">
                  <span class="metric-cell-lbl">Abonado a la Fecha</span>
                  <strong class="metric-cell-val font-mono text-success">
                    $ {{ Number(activeCotizacion.totalAbonado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                  </strong>
                  <span class="metric-cell-sub">{{ Math.round(((Number(activeCotizacion.totalAbonado || 0)) / (Number(activeCotizacion.totalCotizado || 1))) * 100) }}% cubierto</span>
                </div>

                <div class="hero-metric-cell hero-pending-cell">
                  <span class="metric-cell-lbl">Saldo Pendiente de Cobro</span>
                  <strong class="metric-cell-val font-mono text-pending">
                    $ {{ Number(activeCotizacion.saldoPendiente != null ? activeCotizacion.saldoPendiente : activeCotizacion.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
                  </strong>
                  <span class="metric-cell-sub">
                    ≈ S/ {{ (Number(activeCotizacion.saldoPendiente != null ? activeCotizacion.saldoPendiente : activeCotizacion.totalCotizado || 0) * (form.tipoCambio || 3.75)).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- ======================================================== -->
        <!-- BLOQUE 02: MODALIDAD DE ABONO & MONTO                    -->
        <!-- ======================================================== -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Modalidad de Abono y Monto Transferido</h3>
              <p class="block-sub">
                Especifique la condición de cobro (anticipo de apertura, pago íntegro o afianzamiento) y el importe.
              </p>
            </div>
          </div>

          <div class="block-body">
            <!-- Selector de Modalidad de Abono -->
            <div class="field-group">
              <label class="field-label">Modalidad de Abono *</label>
              <div class="modalidad-cards-grid">
                <button
                  type="button"
                  :class="['modalidad-card-btn', { active: form.tipoAbono === 'ADELANTO' }]"
                  @click="setTipoAbono('ADELANTO')"
                >
                  <div class="modalidad-icon-wrap">
                    <i class="pi pi-percentage"></i>
                  </div>
                  <div class="modalidad-info">
                    <strong class="modalidad-title">Adelanto / Anticipo Inicial</strong>
                    <span class="modalidad-desc">Abono parcial para cobertura de gastos y apertura de expediente.</span>
                  </div>
                  <div class="modalidad-badge font-mono">30% MÍNIMO</div>
                </button>

                <button
                  type="button"
                  :class="['modalidad-card-btn', { active: form.tipoAbono === 'PAGO_TOTAL' }]"
                  @click="setTipoAbono('PAGO_TOTAL')"
                >
                  <div class="modalidad-icon-wrap">
                    <i class="pi pi-check-circle"></i>
                  </div>
                  <div class="modalidad-info">
                    <strong class="modalidad-title">Pago Total (100%)</strong>
                    <span class="modalidad-desc">Cancelación total del saldo de la cotización comercial.</span>
                  </div>
                  <div class="modalidad-badge font-mono">100% SALDO</div>
                </button>

                <button
                  type="button"
                  :class="['modalidad-card-btn', { active: form.tipoAbono === 'GARANTIA_CREDITO' }]"
                  @click="setTipoAbono('GARANTIA_CREDITO')"
                >
                  <div class="modalidad-icon-wrap">
                    <i class="pi pi-briefcase"></i>
                  </div>
                  <div class="modalidad-info">
                    <strong class="modalidad-title">Línea de Crédito / Fianza</strong>
                    <span class="modalidad-desc">Garantía bancaria, carta fianza o crédito comercial otorgado.</span>
                  </div>
                  <div class="modalidad-badge font-mono">AFIANZADO</div>
                </button>
              </div>
            </div>

            <!-- Botones de Cálculo Rápido (Sugeridos) -->
            <div v-if="form.tipoAbono === 'ADELANTO'" class="quick-calculator-row">
              <div class="calc-label-wrap">
                <i class="pi pi-calculator text-primary"></i>
                <span class="calc-label">Calcular sugerido para apertura de expediente:</span>
              </div>
              <div class="calc-buttons-group">
                <button
                  type="button"
                  class="btn-calc-pill btn-calc-recommended"
                  @click="aplicarPorcentaje(0.30)"
                  title="30% requerido para habilitar expediente aduanero"
                >
                  <span class="pct-chip">30% MÍN</span>
                  <span class="pct-val font-mono">$ {{ calcularMontoSugerido(0.30) }}</span>
                </button>

                <button
                  type="button"
                  class="btn-calc-pill"
                  @click="aplicarPorcentaje(0.50)"
                  title="50% de anticipo inicial estándar"
                >
                  <span class="pct-chip">50%</span>
                  <span class="pct-val font-mono">$ {{ calcularMontoSugerido(0.50) }}</span>
                </button>

                <button
                  type="button"
                  class="btn-calc-pill"
                  @click="aplicarPorcentaje(1.00)"
                  title="100% del saldo pendiente"
                >
                  <span class="pct-chip">100%</span>
                  <span class="pct-val font-mono">Saldo Total</span>
                </button>
              </div>
            </div>

            <!-- Fila de Importes y Monedas -->
            <div class="fields-2col">
              <!-- Moneda de Pago -->
              <div class="field-group">
                <label for="f-pago-moneda" class="field-label">Moneda de Pago *</label>
                <div class="input-icon-box">
                  <i class="pi pi-money-bill input-icon"></i>
                  <select
                    id="f-pago-moneda"
                    v-model="form.moneda"
                    class="form-select"
                    required
                    :disabled="loading"
                  >
                    <option value="USD">Dólares Americanos (USD $)</option>
                    <option value="PEN">Soles Peruanos (PEN S/)</option>
                  </select>
                </div>
                <span class="field-hint">Moneda en la que se efectuó el depósito bancario.</span>
              </div>

              <!-- Monto Abonado -->
              <div class="field-group">
                <label for="f-pago-monto" class="field-label">Monto Abonado *</label>
                <div class="input-with-currency-box">
                  <span class="currency-tag font-mono">{{ form.moneda === 'PEN' ? 'S/' : '$' }}</span>
                  <input
                    id="f-pago-monto"
                    v-model.number="form.montoAbonado"
                    type="number"
                    step="0.01"
                    min="0.01"
                    placeholder="0.00"
                    class="form-input font-mono font-bold"
                    required
                    :disabled="loading"
                  />
                </div>
                <span class="field-hint">Importe exacto que figura en el voucher bancario.</span>
              </div>
            </div>

            <!-- Tipo de Cambio y Conversión Equivalente -->
            <div class="fields-2col">
              <!-- Tipo de Cambio Oficial -->
              <div class="field-group">
                <label for="f-pago-tc" class="field-label">Tipo de Cambio Oficial (S/)</label>
                <div class="input-icon-box">
                  <i class="pi pi-chart-line input-icon"></i>
                  <input
                    id="f-pago-tc"
                    v-model.number="form.tipoCambio"
                    type="number"
                    step="0.0001"
                    min="1.0"
                    class="form-input font-mono"
                    :disabled="loading"
                  />
                </div>
                <span class="field-hint">Tipo de cambio referencial SUNAT / SBS para contabilidad.</span>
              </div>

              <!-- Monto Equivalente en Soles Display -->
              <div class="field-group">
                <label class="field-label">Monto Equivalente en Soles</label>
                <div class="equivalent-display-box">
                  <div class="equiv-main">
                    <span class="equiv-symbol font-mono">S/</span>
                    <strong class="equiv-val font-mono">{{ montoEquivalentePenDisplay }}</strong>
                  </div>
                  <span class="equiv-sub">Convertido al T.C. {{ form.tipoCambio || 3.75 }}</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- ======================================================== -->
        <!-- BLOQUE 03: DETALLE DE LA OPERACIÓN BANCARIA              -->
        <!-- ======================================================== -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Datos de la Transacción Bancaria</h3>
              <p class="block-sub">
                Trazabilidad oficial del abono en cuentas recaudadoras autorizadas de TransAduanas SGA.
              </p>
            </div>
          </div>

          <div class="block-body">
            <div class="fields-2col">
              <!-- Método de Pago -->
              <div class="field-group">
                <label for="f-pago-metodo" class="field-label">Método de Pago *</label>
                <div class="input-icon-box">
                  <i class="pi pi-credit-card input-icon"></i>
                  <select
                    id="f-pago-metodo"
                    v-model="form.metodoPago"
                    class="form-select"
                    required
                    :disabled="loading"
                  >
                    <option value="TRANSFERENCIA_BANCARIA">Transferencia Bancaria Directa</option>
                    <option value="DEPOSITO_VENTANILLA">Depósito en Ventanilla / Agente</option>
                    <option value="TRANSFERENCIA_CCI">Transferencia Interbancaria (CCI)</option>
                    <option value="CHEQUE_GERENCIA">Cheque de Gerencia</option>
                    <option value="PAGO_LINK">Pago con Tarjeta / Link Digital</option>
                    <option value="CARTA_FIANZA">Carta Fianza Bancaria</option>
                  </select>
                </div>
              </div>

              <!-- Cuenta Bancaria Receptora SGA -->
              <div class="field-group">
                <label for="f-pago-banco-destino" class="field-label">Cuenta Bancaria Receptora (SGA) *</label>
                <div class="input-icon-box">
                  <i class="pi pi-building-columns input-icon"></i>
                  <select
                    id="f-pago-banco-destino"
                    v-model="form.bancoDestino"
                    class="form-select font-mono"
                    required
                    :disabled="loading"
                  >
                    <option value="BCP Dólares Cta Cte 191-0023412-1-44">BCP Cta Cte USD (191-0023412-1-44)</option>
                    <option value="BCP Soles Cta Cte 191-0023412-0-43">BCP Cta Cte Soles (191-0023412-0-43)</option>
                    <option value="BBVA Dólares Cta Cte 0011-0182-0100034567">BBVA Cta Cte USD (0011-0182-0100034567)</option>
                    <option value="BBVA Soles Cta Cte 0011-0182-0100034568">BBVA Cta Cte Soles (0011-0182-0100034568)</option>
                    <option value="Interbank Dólares Cta Cte 200-3001245678">Interbank Cta Cte USD (200-3001245678)</option>
                    <option value="Scotiabank Soles Cta Cte 000-4567890">Scotiabank Soles (000-4567890)</option>
                  </select>
                </div>
              </div>
            </div>

            <div class="fields-2col">
              <!-- Banco Emisor del Cliente -->
              <div class="field-group">
                <label for="f-pago-banco-origen" class="field-label">Banco Emisor del Cliente</label>
                <div class="input-icon-box">
                  <i class="pi pi-arrow-right-arrow-left input-icon"></i>
                  <input
                    id="f-pago-banco-origen"
                    v-model.trim="form.bancoOrigen"
                    type="text"
                    placeholder="Ej. BCP, BBVA, Interbank, Banco del Exterior..."
                    class="form-input"
                    maxlength="80"
                    :disabled="loading"
                  />
                </div>
                <span class="field-hint">Entidad financiera desde la cual el cliente emitió los fondos.</span>
              </div>

              <!-- N° de Operación Bancaria -->
              <div class="field-group">
                <label for="f-pago-operacion" class="field-label">N° de Operación Bancaria *</label>
                <div class="input-icon-box">
                  <i class="pi pi-hashtag input-icon"></i>
                  <input
                    id="f-pago-operacion"
                    v-model.trim="form.numeroOperacion"
                    type="text"
                    placeholder="Ej. OP-4829104 / Ref 99283"
                    class="form-input font-mono font-bold"
                    required
                    maxlength="60"
                    :disabled="loading"
                  />
                </div>
                <span class="field-hint">Código único de transacción emitido por la entidad bancaria.</span>
              </div>
            </div>

            <!-- Fecha de Operación -->
            <div class="fields-2col">
              <div class="field-group">
                <label for="f-pago-fecha" class="field-label">Fecha del Depósito / Transferencia *</label>
                <div class="input-icon-box">
                  <i class="pi pi-calendar input-icon"></i>
                  <input
                    id="f-pago-fecha"
                    v-model="form.fechaOperacion"
                    type="date"
                    class="form-input font-mono"
                    required
                    :disabled="loading"
                  />
                </div>
                <span class="field-hint">Fecha en la que se efectuó la acreditación bancaria.</span>
              </div>

              <div class="field-group">
                <label class="field-label">Estado Inicial en el Flujo</label>
                <div class="status-placeholder-box">
                  <span class="status-pill-badge badge-pending">
                    <span class="status-dot"></span>
                    PENDIENTE DE REVISIÓN EN FINANZAS
                  </span>
                  <span class="status-box-desc">
                    El abono se registrará en estado "En Revisión" hasta que el analista corrobore el estado de cuenta.
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- ======================================================== -->
        <!-- BLOQUE 04: COMPROBANTE DIGITAL & OBSERVACIONES          -->
        <!-- ======================================================== -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">04</div>
            <div class="block-header-info">
              <h3 class="block-title">Comprobante Digital y Observaciones Contables</h3>
              <p class="block-sub">
                Adjunte el soporte documental (voucher o boleta) y registre observaciones de tesorería.
              </p>
            </div>
          </div>

          <div class="block-body">
            <!-- Dropzone para Subida de Voucher -->
            <div class="field-group">
              <label class="field-label">
                <i class="pi pi-paperclip"></i>
                <span>Adjuntar Comprobante Bancario (Voucher / Boleta)</span>
              </label>

              <div
                class="file-dropzone-editorial"
                :class="{ 'has-file': !!selectedFile, 'is-dragging': isDragging }"
                @click="triggerFileInput"
                @dragover.prevent="isDragging = true"
                @dragleave.prevent="isDragging = false"
                @drop.prevent="onFileDrop"
              >
                <input
                  ref="fileInputRef"
                  type="file"
                  accept=".pdf,image/png,image/jpeg,image/jpg"
                  class="hidden-file-input"
                  @change="onFileSelected"
                />

                <div v-if="!selectedFile" class="dropzone-content-empty">
                  <div class="upload-icon-circle">
                    <i class="pi pi-cloud-upload"></i>
                  </div>
                  <strong class="upload-title">Haga clic o arrastre aquí para subir el voucher o comprobante</strong>
                  <span class="upload-sub">Formatos soportados: PDF, PNG, JPG (Límite máximo 10MB)</span>
                  <span class="upload-hint">El archivo se indexará en el repositorio digital del expediente.</span>
                </div>

                <div v-else class="dropzone-content-selected" @click.stop>
                  <div class="file-icon-badge">
                    <i :class="selectedFile.type.includes('pdf') ? 'pi pi-file-pdf' : 'pi pi-image'"></i>
                  </div>
                  <div class="file-details">
                    <strong class="file-name font-mono">{{ selectedFile.name }}</strong>
                    <div class="file-meta-row">
                      <span class="file-size font-mono">{{ formatFileSize(selectedFile.size) }}</span>
                      <span class="file-type-pill">{{ selectedFile.type || 'Documento' }}</span>
                      <span class="file-ready-tag"><i class="pi pi-check"></i> Listo para subir</span>
                    </div>
                  </div>
                  <div class="file-actions">
                    <button
                      type="button"
                      class="btn-change-file"
                      @click.stop="triggerFileInput"
                      title="Cambiar archivo"
                    >
                      <i class="pi pi-pencil"></i>
                      <span>Cambiar</span>
                    </button>
                    <button
                      type="button"
                      class="btn-remove-file"
                      @click.stop="clearSelectedFile"
                      title="Quitar archivo adjunto"
                    >
                      <i class="pi pi-trash"></i>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Observaciones / Glosa Contable -->
            <div class="field-group">
              <label for="f-pago-observaciones" class="field-label">Observaciones y Glosa Contable</label>
              <textarea
                id="f-pago-observaciones"
                v-model="form.observaciones"
                rows="3"
                placeholder="Ingrese detalles adicionales de la transacción, número de cheque, observaciones de cuenta o instrucciones especiales para el conciliador contable..."
                class="form-textarea"
                :disabled="loading"
              ></textarea>
              <span class="field-hint">Información visible para el equipo de finanzas durante la validación del voucher.</span>
            </div>
          </div>
        </section>
      </form>

      <!-- ======================================================== -->
      <!-- COLUMNA LATERAL: FICHA RESUMEN EN VIVO & CHECKLIST       -->
      <!-- ======================================================== -->
      <aside class="form-sidebar-column">
        <!-- Tarjeta Ficha Financiera Live -->
        <div class="financial-preview-card">
          <div class="preview-card-header">
            <span class="card-kicker-pill font-mono">BALANCE PROYECTADO</span>
            <div class="preview-cot-title font-mono">
              {{ activeCotizacion ? activeCotizacion.codigoCotizacion : 'COTIZACIÓN —' }}
            </div>
            <div class="preview-cot-client">
              {{ activeCotizacion ? (activeCotizacion.clienteRazonSocial || 'Cliente Importador') : 'Seleccione una cotización' }}
            </div>
          </div>

          <div class="preview-card-body">
            <!-- Fila: Total Cotizado -->
            <div class="balance-row">
              <span class="b-lbl">Total Cotizado:</span>
              <strong class="b-val font-mono">
                $ {{ Number(activeCotizacion?.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
              </strong>
            </div>

            <!-- Fila: Pagos Previos Conformes -->
            <div class="balance-row">
              <span class="b-lbl">Abonos Aprobados:</span>
              <span class="b-val font-mono text-muted">
                $ {{ Number(activeCotizacion?.totalAbonado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
              </span>
            </div>

            <!-- Fila: Saldo Actual Pendiente -->
            <div class="balance-row">
              <span class="b-lbl">Saldo Actual Pendiente:</span>
              <span class="b-val font-mono text-pending font-bold">
                $ {{ Number(activeCotizacion?.saldoPendiente != null ? activeCotizacion.saldoPendiente : activeCotizacion?.totalCotizado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
              </span>
            </div>

            <div class="balance-divider"></div>

            <!-- Fila Destacada: Nuevo Abono a Registrar -->
            <div class="balance-row highlight-abono-row">
              <div class="b-lbl-highlight">
                <i class="pi pi-plus-circle text-primary"></i>
                <span>Nuevo Abono:</span>
              </div>
              <strong class="b-val-highlight font-mono">
                + {{ form.moneda === 'PEN' ? 'S/' : '$' }} {{ Number(form.montoAbonado || 0).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
              </strong>
            </div>

            <!-- Equivalente USD si fue en PEN -->
            <div v-if="form.moneda === 'PEN' && form.montoAbonado" class="balance-row sub-row font-mono">
              <span class="b-lbl text-muted">Equivalente USD:</span>
              <span class="b-val text-muted">$ {{ montoEnUsdCalculado.toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</span>
            </div>

            <!-- Saldo Resultante Proyectado -->
            <div class="balance-row resulting-balance-row">
              <span class="b-lbl">Saldo Proyectado:</span>
              <strong
                class="b-val font-mono"
                :class="saldoProyectadoUsd <= 0 ? 'text-success' : 'text-pending'"
              >
                $ {{ Math.max(0, saldoProyectadoUsd).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}
              </strong>
            </div>

            <!-- Barra de Cobertura Porcentual -->
            <div class="coverage-progress-wrap">
              <div class="coverage-labels">
                <span class="cov-lbl">Cobertura Proyectada:</span>
                <strong class="cov-pct font-mono" :class="porcentajeProyectado >= 30 ? 'text-success' : 'text-warning'">
                  {{ Math.min(100, Math.round(porcentajeProyectado)) }}%
                </strong>
              </div>
              <div class="coverage-bar">
                <div
                  class="coverage-fill"
                  :class="{ 'fill-success': porcentajeProyectado >= 30, 'fill-complete': porcentajeProyectado >= 100 }"
                  :style="{ width: Math.min(100, Math.round(porcentajeProyectado)) + '%' }"
                ></div>
                <!-- Marcador de umbral de apertura (30%) -->
                <div class="threshold-marker-30" title="Umbral mínimo para apertura de expediente (30%)">
                  <span class="marker-line"></span>
                  <span class="marker-label">30%</span>
                </div>
              </div>
            </div>

            <!-- Dictamen de Apertura Automática -->
            <div
              class="apertura-verdict-box"
              :class="porcentajeProyectado >= 30 ? 'verdict-ready' : 'verdict-insufficient'"
            >
              <i :class="porcentajeProyectado >= 30 ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle'"></i>
              <div class="verdict-text">
                <strong v-if="porcentajeProyectado >= 30">Cumple Umbral de Apertura (30%)</strong>
                <strong v-else>Anticipo Menor al 30% Mínimo</strong>
                <p v-if="porcentajeProyectado >= 30">
                  Una vez validado este comprobante, el sistema <strong>desbloqueará la creación del Expediente Aduanero</strong>.
                </p>
                <p v-else>
                  El abono cubrirá parte del saldo pero requerirá autorización de gerencia para abrir el expediente sin el 30%.
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Checklist de Calidad del Registro -->
        <div class="quality-checklist-card">
          <h4 class="checklist-title">
            <i class="pi pi-list-check"></i>
            <span>Requisitos para Envío a Finanzas</span>
          </h4>
          <ul class="checklist-items">
            <li :class="{ complete: !!activeCotizacion }">
              <i :class="activeCotizacion ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Cotización aprobada seleccionada</span>
            </li>
            <li :class="{ complete: !!form.tipoAbono }">
              <i :class="form.tipoAbono ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Modalidad de abono definida</span>
            </li>
            <li :class="{ complete: Number(form.montoAbonado) > 0 }">
              <i :class="Number(form.montoAbonado) > 0 ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Monto abonado válido (> 0)</span>
            </li>
            <li :class="{ complete: !!form.numeroOperacion }">
              <i :class="form.numeroOperacion ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>N° de Operación Bancaria ingresado</span>
            </li>
            <li :class="{ complete: !!form.bancoDestino }">
              <i :class="form.bancoDestino ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Cuenta bancaria receptora SGA</span>
            </li>
            <li :class="{ complete: !!form.fechaOperacion }">
              <i :class="form.fechaOperacion ? 'pi pi-check-circle text-success' : 'pi pi-circle text-muted'"></i>
              <span>Fecha del depósito / transferencia</span>
            </li>
            <li :class="{ complete: !!selectedFile }">
              <i :class="selectedFile ? 'pi pi-check-circle text-success' : 'pi pi-info-circle text-info'"></i>
              <span>Voucher adjunto (Recomendado)</span>
            </li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { comercialService } from '../services/comercialService'
import { documentoService } from '@/features/documentos/services/documentoService'
import { useAuthStore } from '@/features/auth/stores/authStore'

const props = defineProps({
  cotizacion: {
    type: Object,
    default: null
  },
  cotizaciones: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['cancel', 'saved'])
const authStore = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')
const fileInputRef = ref(null)
const selectedFile = ref(null)
const isDragging = ref(false)

const selectedCotizacionId = ref(props.cotizacion?.idCotizacion || null)
const cotizacionesDisponibles = ref([])
const loadingCotizaciones = ref(false)
const cambiarCotizacionActiva = ref(false)

const defaultForm = () => ({
  tipoAbono: 'ADELANTO',
  moneda: 'USD',
  montoAbonado: null,
  tipoCambio: 3.75,
  metodoPago: 'TRANSFERENCIA_BANCARIA',
  bancoOrigen: '',
  bancoDestino: 'BCP Dólares Cta Cte 191-0023412-1-44',
  numeroOperacion: '',
  fechaOperacion: new Date().toISOString().substring(0, 10),
  observaciones: '',
  idDocumentoComprobante: null
})

const form = ref(defaultForm())

const activeCotizacion = computed(() => {
  if (props.cotizacion && !cambiarCotizacionActiva.value && !selectedCotizacionId.value) {
    return props.cotizacion
  }
  if (selectedCotizacionId.value) {
    return cotizacionesDisponibles.value.find(c => Number(c.idCotizacion) === Number(selectedCotizacionId.value)) || props.cotizacion || null
  }
  return props.cotizacion || null
})

const montoEnUsdCalculado = computed(() => {
  const monto = Number(form.value.montoAbonado || 0)
  const tc = Number(form.value.tipoCambio || 3.75)
  if (form.value.moneda === 'USD') return monto
  return tc > 0 ? monto / tc : 0
})

const montoEquivalentePenDisplay = computed(() => {
  const monto = Number(form.value.montoAbonado || 0)
  const tc = Number(form.value.tipoCambio || 3.75)
  if (form.value.moneda === 'PEN') {
    return monto.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  }
  return (monto * tc).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
})

const saldoProyectadoUsd = computed(() => {
  if (!activeCotizacion.value) return 0
  const saldoActual = Number(
    activeCotizacion.value.saldoPendiente != null
      ? activeCotizacion.value.saldoPendiente
      : activeCotizacion.value.totalCotizado || 0
  )
  return saldoActual - montoEnUsdCalculado.value
})

const porcentajeProyectado = computed(() => {
  if (!activeCotizacion.value) return 0
  const totalCot = Number(activeCotizacion.value.totalCotizado || 1)
  const totalAbonadoPrevio = Number(activeCotizacion.value.totalAbonado || 0)
  const nuevoTotal = totalAbonadoPrevio + montoEnUsdCalculado.value
  return (nuevoTotal / totalCot) * 100
})

const isFormValid = computed(() => {
  const hasCotizacion = !!activeCotizacion.value
  const hasMonto = Number(form.value.montoAbonado) > 0
  const hasOperacion = !!form.value.numeroOperacion?.trim()
  const hasBancoDestino = !!form.value.bancoDestino
  const hasFecha = !!form.value.fechaOperacion
  return hasCotizacion && hasMonto && hasOperacion && hasBancoDestino && hasFecha
})

onMounted(async () => {
  if (props.cotizaciones && props.cotizaciones.length > 0) {
    cotizacionesDisponibles.value = props.cotizaciones.filter(
      c => c.estado === 'APROBADA' && c.estadoPago !== 'PAGADO_TOTAL'
    )
  } else {
    await loadCotizacionesAprobadas()
  }

  if (props.cotizacion) {
    selectedCotizacionId.value = props.cotizacion.idCotizacion
    aplicarMontoInicial()
  }
})

watch(
  () => props.cotizacion,
  (c) => {
    if (c) {
      selectedCotizacionId.value = c.idCotizacion
      cambiarCotizacionActiva.value = false
      aplicarMontoInicial()
    }
  },
  { immediate: true }
)

async function loadCotizacionesAprobadas() {
  loadingCotizaciones.value = true
  try {
    const list = await comercialService.getCotizaciones()
    cotizacionesDisponibles.value = (list || []).filter(
      c => c.estado === 'APROBADA' && c.estadoPago !== 'PAGADO_TOTAL'
    )
  } catch (err) {
    console.error('Error cargando cotizaciones aprobadas:', err)
  } finally {
    loadingCotizaciones.value = false
  }
}

function toggleCambiarCotizacion() {
  cambiarCotizacionActiva.value = !cambiarCotizacionActiva.value
  if (cambiarCotizacionActiva.value && cotizacionesDisponibles.value.length === 0) {
    loadCotizacionesAprobadas()
  }
}

function onCotizacionSelected() {
  if (activeCotizacion.value) {
    aplicarMontoInicial()
  }
}

function aplicarMontoInicial() {
  const target = activeCotizacion.value
  if (target && !form.value.montoAbonado) {
    const total = Number(target.totalCotizado || 0)
    const saldo = Number(target.saldoPendiente != null ? target.saldoPendiente : total)
    // Sugerir por defecto 30% del total o el saldo pendiente si es menor
    const sugerido30 = Math.round(total * 0.3 * 100) / 100
    form.value.montoAbonado = Math.min(sugerido30, saldo)
  }
}

function setTipoAbono(tipo) {
  form.value.tipoAbono = tipo
  if (tipo === 'PAGO_TOTAL' && activeCotizacion.value) {
    const total = Number(
      activeCotizacion.value.saldoPendiente != null
        ? activeCotizacion.value.saldoPendiente
        : activeCotizacion.value.totalCotizado || 0
    )
    form.value.montoAbonado = total
  } else if (tipo === 'ADELANTO' && activeCotizacion.value) {
    aplicarPorcentaje(0.30)
  }
}

function calcularMontoSugerido(pct) {
  if (!activeCotizacion.value) return '0.00'
  const total = Number(activeCotizacion.value.totalCotizado || 0)
  return (total * pct).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function aplicarPorcentaje(pct) {
  if (!activeCotizacion.value) return
  const total = Number(activeCotizacion.value.totalCotizado || 0)
  form.value.montoAbonado = Math.round(total * pct * 100) / 100
}

function triggerFileInput() {
  if (fileInputRef.value) {
    fileInputRef.value.click()
  }
}

function onFileSelected(event) {
  const file = event.target.files?.[0]
  processSelectedFile(file)
}

function onFileDrop(event) {
  isDragging.value = false
  const file = event.dataTransfer?.files?.[0]
  processSelectedFile(file)
}

function processSelectedFile(file) {
  if (!file) return
  if (file.size > 10 * 1024 * 1024) {
    errorMessage.value = 'El archivo supera el límite máximo permitido de 10 MB.'
    return
  }
  selectedFile.value = file
  errorMessage.value = ''
}

function clearSelectedFile() {
  selectedFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function formatFileSize(bytes) {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

async function handleSubmit() {
  const targetCotizacion = activeCotizacion.value
  if (!targetCotizacion?.idCotizacion) {
    errorMessage.value = 'Debe seleccionar una cotización aprobada para registrar el pago.'
    return
  }
  if (!form.value.montoAbonado || form.value.montoAbonado <= 0) {
    errorMessage.value = 'Ingrese un monto abonado válido superior a cero.'
    return
  }
  if (!form.value.numeroOperacion?.trim()) {
    errorMessage.value = 'El número de operación bancaria es obligatorio.'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    let idDocComprobante = null

    // 1. Si adjuntó archivo, subirlo primero
    if (selectedFile.value) {
      try {
        const uploadResult = await documentoService.upload([selectedFile.value], {
          categoria: 'COMPROBANTE_PAGO',
          descripcion: `Voucher depósito cotización ${targetCotizacion.codigoCotizacion} - Op: ${form.value.numeroOperacion}`,
          entidadTipo: 'COTIZACION',
          idEntidad: targetCotizacion.idCotizacion,
          idUsuario: authStore.user?.idUsuario || 1
        })
        if (Array.isArray(uploadResult) && uploadResult.length > 0) {
          idDocComprobante = uploadResult[0].idDocumento
        }
      } catch (err) {
        console.warn('No se pudo subir comprobante, se registrará el pago sin adjunto:', err)
      }
    }

    // 2. Registrar el pago en el backend
    const payload = {
      idCotizacion: targetCotizacion.idCotizacion,
      tipoAbono: form.value.tipoAbono,
      montoAbonado: Number(form.value.montoAbonado),
      moneda: form.value.moneda,
      tipoCambio: Number(form.value.tipoCambio || 3.75),
      metodoPago: form.value.metodoPago,
      bancoOrigen: form.value.bancoOrigen || '',
      bancoDestino: form.value.bancoDestino,
      numeroOperacion: form.value.numeroOperacion.trim(),
      fechaOperacion: form.value.fechaOperacion,
      observaciones: form.value.observaciones ? form.value.observaciones.trim() : null,
      idDocumentoComprobante: idDocComprobante
    }

    const nuevoPago = await comercialService.registrarPagoCotizacion(targetCotizacion.idCotizacion, payload)

    emit('saved', { pago: nuevoPago, cotizacion: targetCotizacion })
  } catch (error) {
    console.error('Error registrando pago:', error)
    errorMessage.value = error.response?.data?.message || 'Error al registrar el comprobante de pago. Verifique los datos bancarios.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ==========================================================================
   WARM EDITORIAL 2.0 - REGISTRO DE PAGO FULL VIEW
   ========================================================================== */
.registro-pago-full-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 0.5rem 0 3rem;
  color: var(--color-text-main);
  animation: fadeIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 1. Header Bar */
.form-header-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 1.5rem 1.75rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.header-breadcrumb-area {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.btn-back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: transparent;
  border: none;
  color: var(--color-text-muted);
  font-size: 0.825rem;
  font-weight: 600;
  cursor: pointer;
  padding: 0;
  margin-bottom: 0.25rem;
  transition: color var(--transition-fast);
}

.btn-back-link:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.25rem;
  flex-wrap: wrap;
}

.mode-pill {
  font-size: 0.65rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  padding: 0.2rem 0.55rem;
  border-radius: var(--radius-pill, 999px);
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.mode-create {
  background: var(--color-success-bg, rgba(46, 125, 50, 0.1));
  color: var(--color-success-text, #2e7d32);
  border: 1px solid var(--color-success-border, rgba(46, 125, 50, 0.25));
}

.form-main-title {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-text-main);
  letter-spacing: -0.01em;
  margin: 0 0 0.35rem;
}

.form-main-desc {
  font-size: 0.875rem;
  color: var(--color-text-subtle);
  margin: 0;
  max-width: 760px;
  line-height: 1.45;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.25rem;
  font-size: 0.85rem;
  font-weight: 600;
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-outline-warm:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background: var(--color-bg-subtle);
}

.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.35rem;
  font-size: 0.85rem;
  font-weight: 700;
  border-radius: var(--radius-md, 0.5rem);
  border: none;
  background: var(--color-primary);
  color: var(--color-primary-text);
  cursor: pointer;
  transition: all var(--transition-fast);
  box-shadow: 0 2px 6px rgba(53, 47, 42, 0.2);
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.btn-espresso:disabled,
.btn-outline-warm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* Error Banner */
.error-banner {
  background: var(--color-danger-bg, rgba(211, 47, 47, 0.1));
  border: 1px solid var(--color-danger-border, rgba(211, 47, 47, 0.3));
  color: var(--color-danger-text, #d32f2f);
  padding: 0.85rem 1.25rem;
  border-radius: var(--radius-md, 0.5rem);
  display: flex;
  align-items: center;
  gap: 0.85rem;
  font-size: 0.875rem;
}

.error-content {
  flex: 1;
  display: flex;
  gap: 0.4rem;
}

.error-close-btn {
  background: transparent;
  border: none;
  color: var(--color-danger-text, #d32f2f);
  cursor: pointer;
  padding: 0.25rem;
  display: flex;
  align-items: center;
}

/* Banner Editorial Verde de Validación */
.info-alert-editorial {
  display: flex;
  align-items: flex-start;
  gap: 1.15rem;
  padding: 1.15rem 1.35rem;
  background: rgba(46, 125, 50, 0.06);
  border: 1px solid rgba(46, 125, 50, 0.25);
  border-radius: var(--radius-lg, 0.75rem);
}

.info-icon-badge {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: rgba(46, 125, 50, 0.12);
  color: #2e7d32;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.info-content-editorial {
  flex: 1;
}

.info-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: #2e7d32;
  margin: 0 0 0.25rem;
}

.info-desc {
  font-size: 0.85rem;
  color: var(--color-text-main);
  margin: 0;
  line-height: 1.45;
}

/* 2. Grid Principal */
.form-content-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 1.5rem;
  align-items: start;
}

@media (max-width: 1180px) {
  .form-content-grid {
    grid-template-columns: 1fr;
  }
}

.form-blocks-column {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Tarjetas de Bloques Indexados */
.form-block-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.5rem 1.75rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.block-header {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  margin-bottom: 1.35rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.block-idx {
  font-family: var(--font-mono, monospace);
  font-size: 1rem;
  font-weight: 800;
  color: var(--color-accent-kicker, #b85d19);
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  width: 38px;
  height: 38px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.block-header-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.block-title {
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.block-sub {
  font-size: 0.8rem;
  color: var(--color-text-muted);
  margin: 0;
}

.block-body {
  display: flex;
  flex-direction: column;
  gap: 1.35rem;
}

/* Campos y Layouts */
.field-group {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.field-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: 0.4rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.field-hint {
  font-size: 0.725rem;
  color: var(--color-text-muted);
}

.fields-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.15rem;
}

@media (max-width: 640px) {
  .fields-2col {
    grid-template-columns: 1fr;
  }
}

/* Inputs con Icono */
.input-icon-box {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon-box .input-icon {
  position: absolute;
  left: 0.9rem;
  color: var(--color-text-muted);
  font-size: 0.85rem;
  pointer-events: none;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: 0.65rem 0.9rem;
  font-size: 0.875rem;
  color: var(--color-text-main);
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  outline: none;
  transition: all var(--transition-fast);
}

.input-icon-box .form-input,
.input-icon-box .form-select {
  padding-left: 2.35rem;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(184, 93, 25, 0.12);
}

.form-textarea {
  resize: vertical;
  line-height: 1.45;
}

/* Input con Prefijo de Moneda */
.input-with-currency-box {
  display: flex;
  align-items: center;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  overflow: hidden;
  transition: border-color var(--transition-fast);
}

.input-with-currency-box:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(184, 93, 25, 0.12);
}

.currency-tag {
  padding: 0 0.85rem;
  font-weight: 700;
  font-size: 0.9rem;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  border-right: 1px solid var(--color-border);
  height: 100%;
  display: flex;
  align-items: center;
}

.input-with-currency-box .form-input {
  border: none;
  border-radius: 0;
  box-shadow: none;
}

/* Cuadro de Cotización Seleccionada Hero */
.cotizacion-selected-hero {
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 1.15rem;
}

.hero-header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.hero-code-box {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
}

.hero-kicker {
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #b85d19);
}

.hero-code {
  font-size: 1.15rem;
  color: var(--color-primary);
}

.hero-client-name {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.hero-client-doc {
  font-size: 0.775rem;
  color: var(--color-text-muted);
}

.btn-change-cot {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 0.375rem);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-change-cot:hover {
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.hero-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.75rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-border-subtle);
}

@media (max-width: 768px) {
  .hero-metrics-grid {
    grid-template-columns: 1fr 1fr;
  }
}

.hero-metric-cell {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  padding: 0.65rem 0.85rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm, 0.375rem);
}

.hero-pending-cell {
  background: rgba(184, 93, 25, 0.05);
  border-color: rgba(184, 93, 25, 0.25);
}

.metric-cell-lbl {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.metric-cell-val {
  font-size: 0.925rem;
}

.metric-cell-sub {
  font-size: 0.7rem;
  color: var(--color-text-muted);
}

/* Selector Modalidad Cards */
.modalidad-cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.85rem;
}

@media (max-width: 768px) {
  .modalidad-cards-grid {
    grid-template-columns: 1fr;
  }
}

.modalidad-card-btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.65rem;
  padding: 1rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  text-align: left;
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

.modalidad-card-btn:hover {
  border-color: var(--color-border-strong);
  background: var(--color-bg-subtle);
  transform: translateY(-1px);
}

.modalidad-card-btn.active {
  background: rgba(184, 93, 25, 0.08);
  border-color: var(--color-accent-kicker, #b85d19);
  box-shadow: 0 0 0 1px var(--color-accent-kicker, #b85d19);
}

.modalidad-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--color-bg-subtle);
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.95rem;
  transition: all var(--transition-fast);
}

.modalidad-card-btn.active .modalidad-icon-wrap {
  background: var(--color-accent-kicker, #b85d19);
  color: #ffffff;
}

.modalidad-title {
  font-size: 0.85rem;
  color: var(--color-text-main);
  display: block;
}

.modalidad-desc {
  font-size: 0.725rem;
  color: var(--color-text-muted);
  line-height: 1.35;
  display: block;
  margin-top: 0.15rem;
}

.modalidad-badge {
  font-size: 0.65rem;
  font-weight: 700;
  color: var(--color-text-muted);
  background: var(--color-bg-subtle);
  padding: 0.15rem 0.45rem;
  border-radius: var(--radius-sm, 0.25rem);
  border: 1px solid var(--color-border-subtle);
}

.modalidad-card-btn.active .modalidad-badge {
  color: var(--color-accent-kicker, #b85d19);
  border-color: var(--color-accent-kicker, #b85d19);
  background: rgba(184, 93, 25, 0.12);
}

/* Fila de Cálculo Rápido */
.quick-calculator-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1.15rem;
  background: var(--color-bg-subtle);
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  flex-wrap: wrap;
}

.calc-label-wrap {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-text-main);
}

.calc-buttons-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-calc-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.35rem 0.75rem;
  font-size: 0.8rem;
  font-weight: 600;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-pill, 999px);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.btn-calc-pill:hover {
  border-color: var(--color-accent-kicker);
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
}

.btn-calc-recommended {
  border-color: rgba(184, 93, 25, 0.4);
  background: rgba(184, 93, 25, 0.08);
}

.pct-chip {
  font-size: 0.65rem;
  font-weight: 800;
  background: var(--color-bg-subtle);
  padding: 0.1rem 0.35rem;
  border-radius: 4px;
}

/* Display Equivalente Soles */
.equivalent-display-box {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0.55rem 0.9rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
  height: 42px;
}

.equiv-main {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.equiv-symbol {
  font-size: 0.85rem;
  color: var(--color-text-muted);
}

.equiv-val {
  font-size: 1rem;
  color: var(--color-text-main);
}

.equiv-sub {
  font-size: 0.675rem;
  color: var(--color-text-muted);
}

/* Status Placeholder Box */
.status-placeholder-box {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  padding: 0.65rem 0.85rem;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 0.5rem);
}

.status-pill-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  padding: 0.2rem 0.5rem;
  border-radius: var(--radius-pill, 999px);
  width: fit-content;
}

.badge-pending {
  background: rgba(230, 81, 0, 0.1);
  color: #e65100;
  border: 1px solid rgba(230, 81, 0, 0.25);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-box-desc {
  font-size: 0.725rem;
  color: var(--color-text-muted);
}

/* Dropzone de Archivo */
.file-dropzone-editorial {
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.5rem;
  text-align: center;
  background: var(--color-bg-subtle);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

.file-dropzone-editorial:hover,
.file-dropzone-editorial.is-dragging {
  border-color: var(--color-primary);
  background: rgba(184, 93, 25, 0.04);
}

.file-dropzone-editorial.has-file {
  border-style: solid;
  border-color: var(--color-border);
  background: var(--color-bg-surface);
  cursor: default;
  text-align: left;
}

.hidden-file-input {
  display: none;
}

.dropzone-content-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
}

.upload-icon-circle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: rgba(184, 93, 25, 0.1);
  color: var(--color-accent-kicker, #b85d19);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  margin-bottom: 0.35rem;
}

.upload-title {
  font-size: 0.95rem;
  color: var(--color-text-main);
}

.upload-sub {
  font-size: 0.785rem;
  color: var(--color-text-muted);
}

.upload-hint {
  font-size: 0.7rem;
  color: var(--color-text-muted);
  font-style: italic;
  margin-top: 0.25rem;
}

.dropzone-content-selected {
  display: flex;
  align-items: center;
  gap: 1.15rem;
}

.file-icon-badge {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(211, 47, 47, 0.1);
  color: #d32f2f;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  flex-shrink: 0;
}

.file-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.file-name {
  font-size: 0.9rem;
  color: var(--color-text-main);
  word-break: break-all;
}

.file-meta-row {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  flex-wrap: wrap;
}

.file-size {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.file-type-pill {
  font-size: 0.65rem;
  font-weight: 700;
  text-transform: uppercase;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  color: var(--color-text-muted);
}

.file-ready-tag {
  font-size: 0.725rem;
  font-weight: 600;
  color: #2e7d32;
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.btn-change-file {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.45rem 0.85rem;
  font-size: 0.75rem;
  font-weight: 600;
  border: 1px solid var(--color-border);
  background: var(--color-bg-surface);
  border-radius: var(--radius-sm, 0.375rem);
  cursor: pointer;
  color: var(--color-text-main);
}

.btn-change-file:hover {
  background: var(--color-bg-subtle);
}

.btn-remove-file {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm, 0.375rem);
  border: 1px solid var(--color-danger-border, rgba(211, 47, 47, 0.3));
  background: var(--color-danger-bg, rgba(211, 47, 47, 0.08));
  color: var(--color-danger-text, #d32f2f);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-remove-file:hover {
  background: rgba(211, 47, 47, 0.18);
}

/* ==========================================================================
   COLUMNA LATERAL (SIDEBAR)
   ========================================================================== */
.form-sidebar-column {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

/* Tarjeta Ficha Financiera Live */
.financial-preview-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.preview-card-header {
  margin-bottom: 1.25rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid var(--color-border-subtle);
}

.card-kicker-pill {
  font-size: 0.65rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #b85d19);
  display: inline-block;
  margin-bottom: 0.35rem;
}

.preview-cot-title {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--color-text-main);
}

.preview-cot-client {
  font-size: 0.85rem;
  color: var(--color-text-muted);
  margin-top: 0.15rem;
}

.preview-card-body {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.balance-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  font-size: 0.85rem;
}

.balance-row .b-lbl {
  color: var(--color-text-muted);
}

.balance-row.sub-row {
  font-size: 0.75rem;
  padding-left: 1.25rem;
}

.balance-divider {
  height: 1px;
  background: var(--color-border-subtle);
  margin: 0.35rem 0;
}

.highlight-abono-row {
  padding: 0.75rem 0.85rem;
  background: rgba(184, 93, 25, 0.08);
  border-radius: var(--radius-md, 0.5rem);
  border: 1px solid rgba(184, 93, 25, 0.25);
}

.b-lbl-highlight {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.b-val-highlight {
  font-size: 1.05rem;
  color: var(--color-accent-kicker, #b85d19);
}

.resulting-balance-row {
  padding-top: 0.35rem;
}

.resulting-balance-row .b-lbl {
  font-weight: 700;
  color: var(--color-text-main);
}

.resulting-balance-row .b-val {
  font-size: 1.1rem;
}

/* Barra de Cobertura */
.coverage-progress-wrap {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  margin-top: 0.5rem;
}

.coverage-labels {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cov-lbl {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.cov-pct {
  font-size: 0.95rem;
}

.coverage-bar {
  position: relative;
  width: 100%;
  height: 10px;
  background: var(--color-bg-subtle);
  border-radius: var(--radius-pill, 999px);
  overflow: visible;
  border: 1px solid var(--color-border-subtle);
}

.coverage-fill {
  height: 100%;
  background: var(--color-warning, #f59e0b);
  border-radius: var(--radius-pill, 999px);
  transition: width 0.3s ease;
}

.coverage-fill.fill-success {
  background: #2e7d32;
}

.coverage-fill.fill-complete {
  background: #15803d;
}

.threshold-marker-30 {
  position: absolute;
  left: 30%;
  top: -3px;
  bottom: -3px;
  display: flex;
  flex-direction: column;
  align-items: center;
  pointer-events: none;
}

.marker-line {
  width: 2px;
  height: 16px;
  background: #000000;
  opacity: 0.35;
}

.marker-label {
  position: absolute;
  bottom: -15px;
  font-size: 0.55rem;
  font-weight: 800;
  color: var(--color-text-muted);
  font-family: var(--font-mono, monospace);
}

/* Veredicto de Apertura */
.apertura-verdict-box {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.85rem;
  border-radius: var(--radius-md, 0.5rem);
  font-size: 0.775rem;
  margin-top: 0.75rem;
  line-height: 1.4;
}

.verdict-ready {
  background: rgba(46, 125, 50, 0.08);
  border: 1px solid rgba(46, 125, 50, 0.25);
  color: #2e7d32;
}

.verdict-ready p {
  color: var(--color-text-main);
  margin: 0.2rem 0 0;
}

.verdict-insufficient {
  background: rgba(230, 81, 0, 0.08);
  border: 1px solid rgba(230, 81, 0, 0.25);
  color: #e65100;
}

.verdict-insufficient p {
  color: var(--color-text-main);
  margin: 0.2rem 0 0;
}

.verdict-text strong {
  display: block;
  font-size: 0.825rem;
}

/* Checklist de Calidad */
.quality-checklist-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 0.75rem);
  padding: 1.35rem 1.5rem;
}

.checklist-title {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0 0 1rem;
}

.checklist-items {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}

.checklist-items li {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  font-size: 0.8rem;
  color: var(--color-text-muted);
  transition: color var(--transition-fast);
}

.checklist-items li.complete {
  color: var(--color-text-main);
  font-weight: 600;
}

/* Sidebar Action Buttons */
.sidebar-actions-card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.btn-block {
  width: 100%;
  justify-content: center;
  padding: 0.75rem 1rem;
}

/* Transiciones */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.25s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-8px);
  opacity: 0;
}

/* Tipografía Mono */
.font-mono {
  font-family: var(--font-mono, monospace);
}

.text-pending {
  color: #c2410c;
}

.text-success {
  color: #15803d;
}

.text-primary {
  color: var(--color-primary);
}

.text-warning {
  color: #d97706;
}

.text-muted {
  color: var(--color-text-muted);
}
</style>
