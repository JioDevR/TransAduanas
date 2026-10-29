<template>
  <div class="auditoria-expediente-view fade-in">
    <!-- 1. Header Bar de Auditoría y Acciones de Supervisión -->
    <div class="form-header-bar">
      <div class="header-titles">
        <button
          type="button"
          class="btn-back-link"
          @click="$emit('back')"
          title="Regresar a la bandeja de supervisión"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver a Gestión Aduanera</span>
        </button>

        <div class="kicker-row">
          <span class="badge-kicker">SUPERVISIÓN TÉCNICA // AUDITORÍA INTEGRAL</span>

          <!-- Badge de Modalidad Aérea / Marítima -->
          <span :class="['modalidad-pill', isAirModal ? 'aereo' : 'maritimo']">
            <i :class="isAirModal ? 'pi pi-send' : 'pi pi-compass'"></i>
            <span>{{ isAirModal ? 'MODALIDAD AÉREA' : 'MODALIDAD MARÍTIMA' }}</span>
          </span>

          <span :class="['status-badge', getStatusBadgeClass(activeData.estado || activeData.estadoExpediente)]">
            <i :class="getStatusIcon(activeData.estado || activeData.estadoExpediente)"></i>
            <span>{{ getEstadoLabel(activeData.estado || activeData.estadoExpediente) }}</span>
          </span>

          <span v-if="activeData.canalSunat" :class="['canal-pill', `canal-${String(activeData.canalSunat).toLowerCase()}`]">
            <i class="pi pi-shield"></i>
            <span>CANAL SUNAT: {{ activeData.canalSunat }}</span>
          </span>

          <span v-if="activeData.codigoExpediente" class="exp-linked-badge">
            <i class="pi pi-folder"></i>
            <span>EXPEDIENTE: {{ activeData.codigoExpediente }}</span>
          </span>
        </div>

        <h1 class="form-main-title">
          {{ isExpedienteView ? `Auditoría Integral de Expediente: ${activeData.codigoExpediente}` : `Auditoría Técnica de Cotización: ${activeData.codigoCotizacion || `COT-${activeData.idCotizacion}`}` }}
        </h1>
        <p class="form-main-desc">
          Inspección técnica integral, valoración arancelaria, verificación de partidas NANDINA, descripción de carga y trazabilidad documentaria ante SUNAT.
        </p>
      </div>

      <!-- Acciones de Cabecera -->
      <div class="header-actions">
        <!-- Acciones cuando la cotización está en revisión técnica (sin aperturar) -->
        <template v-if="isAuditableState && !isExpedienteView">
          <button
            type="button"
            class="btn-danger-action"
            @click="toggleRejectBox"
            :disabled="processing"
            title="Rechazar definitivamente la propuesta comercial"
          >
            <i class="pi pi-times"></i>
            <span>Rechazar</span>
          </button>

          <button
            type="button"
            class="btn-outline-warm"
            @click="toggleObservationBox"
            :disabled="processing"
            title="Devolver a Ventas con observaciones técnicas para corrección"
          >
            <i class="pi pi-file-edit"></i>
            <span>Solicitar Corrección</span>
          </button>

          <button
            type="button"
            class="btn-espresso"
            @click="$emit('approve', activeData)"
            :disabled="processing"
            title="Aprobar técnicamente para apertura de Expediente Aduanero"
          >
            <i v-if="processing" class="pi pi-spin pi-spinner"></i>
            <i v-else class="pi pi-check"></i>
            <span>Aprobar Propuesta</span>
          </button>
        </template>

        <!-- Acciones cuando la cotización ya fue APROBADA (pero aún sin expediente) -->
        <template v-else-if="activeData.estado === 'APROBADA' && !activeData.tieneExpediente && !isExpedienteView">
          <button
            v-if="activeData.tienePagoAprobado"
            type="button"
            class="btn-espresso"
            @click="$emit('open-apertura', activeData)"
            title="Generar expediente aduanero formal para esta cotización"
          >
            <i class="pi pi-folder-open"></i>
            <span>+ Aperturar Expediente</span>
          </button>

          <span
            v-else
            class="status-lock-pill"
            title="Se requiere aprobación de pago en Finanzas antes de aperturar el expediente"
          >
            <i class="pi pi-lock"></i>
            <span>Requiere Pago Finanzas</span>
          </span>
        </template>

        <!-- Botón de retorno -->
        <button
          type="button"
          class="btn-outline-warm"
          @click="$emit('back')"
          title="Regresar a la vista principal"
        >
          <i class="pi pi-arrow-left"></i>
          <span>Volver al Directorio</span>
        </button>
      </div>
    </div>

    <!-- 2. Panel Expansible para Solicitar Corrección / Observación -->
    <div v-if="showObservationBox" class="inline-action-panel observation-panel fade-in">
      <div class="panel-header">
        <div class="panel-title">
          <i class="pi pi-file-edit"></i>
          <span>Solicitar Corrección Técnica a Ventas (Marcar como Observada)</span>
        </div>
        <button type="button" class="btn-close-panel" @click="showObservationBox = false">
          <i class="pi pi-times"></i>
        </button>
      </div>
      <p class="panel-desc">
        Especifique con precisión las observaciones técnicas o arancelarias para que el asesor comercial rectifique la propuesta <strong>{{ activeData.codigoCotizacion }}</strong>. Una vez corregida, volverá a supervisión.
      </p>
      <div class="panel-input-box">
        <label class="panel-label">Detalle de Observación / Motivo de Revisión *</label>
        <textarea
          v-model="observationReason"
          rows="3"
          class="form-textarea"
          placeholder="Ej. Rectificar partida arancelaria NANDINA para régimen definitivo, ajustar flete o adjuntar packing list legalizado..."
        ></textarea>
      </div>
      <div class="panel-actions-row">
        <button type="button" class="btn-outline-warm" @click="showObservationBox = false">
          <span>Cancelar</span>
        </button>
        <button
          type="button"
          class="btn-warm-action"
          :disabled="!observationReason.trim() || processing"
          @click="submitObservation"
        >
          <i v-if="processing" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-send"></i>
          <span>Confirmar y Enviar a Ventas</span>
        </button>
      </div>
    </div>

    <!-- 3. Panel Expansible para Rechazo Definitivo -->
    <div v-if="showRejectBox" class="inline-action-panel reject-panel fade-in">
      <div class="panel-header">
        <div class="panel-title">
          <i class="pi pi-times-circle"></i>
          <span>Rechazar Propuesta Comercial Definitivamente</span>
        </div>
        <button type="button" class="btn-close-panel" @click="showRejectBox = false">
          <i class="pi pi-times"></i>
        </button>
      </div>
      <p class="panel-desc">
        Indique el motivo técnico del rechazo definitivo para <strong>{{ activeData.codigoCotizacion }}</strong>. La cotización pasará a estado RECHAZADA.
      </p>
      <div class="panel-input-box">
        <label class="panel-label">Motivo de Rechazo Inapelable *</label>
        <textarea
          v-model="rejectReason"
          rows="3"
          class="form-textarea"
          placeholder="Ej. Mercancía prohibida según listado SUNAT/DIGESA, restricciones legales insubsanables..."
        ></textarea>
      </div>
      <div class="panel-actions-row">
        <button type="button" class="btn-outline-warm" @click="showRejectBox = false">
          <span>Cancelar</span>
        </button>
        <button
          type="button"
          class="btn-danger-action"
          :disabled="!rejectReason.trim() || processing"
          @click="submitReject"
        >
          <i v-if="processing" class="pi pi-spin pi-spinner"></i>
          <i v-else class="pi pi-times"></i>
          <span>Confirmar Rechazo Definitivo</span>
        </button>
      </div>
    </div>

    <!-- 4. Grid Principal de Contenido (Resumen Técnico + Panel Lateral) -->
    <div class="form-content-grid">
      <!-- Columna Principal: Ficha Técnica y Documentos -->
      <div class="form-blocks-column">
        <!-- Bloque 01: Identificación y Ficha Comercial -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">01</div>
            <div class="block-header-info">
              <h3 class="block-title">Identificación del Solicitante & Ficha Comercial</h3>
              <p class="block-sub">Información tributaria y canales comerciales del cliente solicitante.</p>
            </div>
          </div>

          <div class="block-body">
            <div class="details-grid-3">
              <div class="detail-item">
                <span class="detail-label">Cliente / Razón Social:</span>
                <span class="detail-val strong">{{ activeData.clienteRazonSocial || '—' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">RUC / Documento:</span>
                <span class="detail-val font-mono">{{ activeData.clienteNumeroDocumento || '—' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Tipo de Cartera:</span>
                <span class="detail-val">
                  <span :class="['badge-type', activeData.clienteTipo === 'CLIENTE' ? 'type-cliente' : 'type-prospecto']">
                    {{ activeData.clienteTipo || 'CLIENTE' }}
                  </span>
                </span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Asesor Comercial Emisor:</span>
                <span class="detail-val">{{ activeData.usuarioComercialNombre || 'Asesor Comercial Asignado' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Agente Aduanero Responsable:</span>
                <span class="detail-val strong" style="color: var(--color-accent-kicker);">
                  {{ activeData.agenteAduaneroNombre || 'Agente Asignado' }}
                </span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Fecha de Apertura / Emisión:</span>
                <span class="detail-val font-mono">{{ formatDate(activeData.fechaApertura || activeData.fechaEmision) }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- Bloque 02: Parámetros Técnicos, Puertos, Mercancía & Tipo de Carga -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">02</div>
            <div class="block-header-info">
              <h3 class="block-title">Parámetros Técnicos, Logística Internacional & Mercancía</h3>
              <p class="block-sub">Régimen aduanero, puertos origen/destino, partida NANDINA y especificación técnica de la carga.</p>
            </div>
          </div>

          <div class="block-body">
            <div class="details-grid-3">
              <div class="detail-item full-width-sm">
                <span class="detail-label">Régimen Aduanero SUNAT:</span>
                <div class="regimen-tag-box">
                  <span class="regimen-code-pill font-mono">{{ getRegimenCode(activeData.regimenAduanero) }}</span>
                  <span class="regimen-name-text">{{ formatRegimen(activeData.regimenAduanero) }}</span>
                </div>
              </div>

              <div class="detail-item">
                <span class="detail-label">Modalidad de Transporte:</span>
                <div class="modalidad-tag">
                  <i :class="isAirModal ? 'pi pi-send' : 'pi pi-compass'"></i>
                  <span class="font-bold">{{ isAirModal ? 'Transporte Aéreo' : 'Transporte Marítimo' }}</span>
                </div>
              </div>

              <div class="detail-item">
                <span class="detail-label">Modalidad de Despacho:</span>
                <span class="detail-val">{{ formatTipoDespacho(activeData.tipoDespacho) }}</span>
              </div>

              <!-- Cascada de Puertos Origen y Destino -->
              <div class="detail-item full-width-sm">
                <span class="detail-label">Ruta y Puertos / Terminales Internacionales:</span>
                <div class="ports-cascade-box">
                  <div class="port-cascade-card">
                    <span class="port-role-tag">ORIGEN / EMBARQUE</span>
                    <strong class="port-name">{{ activeData.puertoEnvioNombre || 'No especificado' }}</strong>
                    <div class="port-meta font-mono">
                      <span>Cód: {{ activeData.puertoEnvioCodigo || 'S/C' }}</span>
                      <span class="dot">•</span>
                      <span>{{ activeData.puertoEnvioPais || 'Exterior' }}</span>
                    </div>
                  </div>

                  <div class="route-divider-icon">
                    <i class="pi pi-arrow-right"></i>
                  </div>

                  <div class="port-cascade-card highlight">
                    <span class="port-role-tag">DESTINO / ARRIBO</span>
                    <strong class="port-name">{{ activeData.puertoDestinoNombre || 'Sede Central Callao' }}</strong>
                    <div class="port-meta font-mono">
                      <span>Cód: {{ activeData.puertoDestinoCodigo || 'PE-CALLAO' }}</span>
                      <span class="dot">•</span>
                      <span>{{ activeData.puertoDestinoPais || 'Perú' }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Ficha de la Mercancía y Partida Arancelaria NANDINA -->
              <div class="detail-item full-width-sm">
                <span class="detail-label">Mercancía & Clasificación Arancelaria NANDINA:</span>
                <div class="mercancia-card">
                  <div class="mercancia-code-pill font-mono">
                    <i class="pi pi-tag"></i>
                    <span>PARTIDA: {{ activeData.mercanciaCodigo || 'PENDIENTE CLASIFICACIÓN' }}</span>
                  </div>
                  <strong class="mercancia-title">{{ activeData.mercanciaNombre || 'Mercancía general' }}</strong>
                </div>
              </div>

              <!-- Tipo de Carga y Tarjeta Callout con Descripción Técnica -->
              <div class="detail-item full-width-sm">
                <span class="detail-label">Tipo de Carga & Naturaleza:</span>
                <div class="tipo-carga-badge">
                  <i class="pi pi-box"></i>
                  <span>{{ activeData.tipoCargaNombre || 'Carga General / Contenedorizada' }}</span>
                </div>

                <!-- Tarjeta Callout de Descripción Técnica (Agregada en BD recientemente) -->
                <div v-if="activeData.tipoCargaDescripcion" class="tipo-carga-callout-card">
                  <div class="callout-icon-wrap">
                    <i class="pi pi-info-circle"></i>
                  </div>
                  <div class="callout-content">
                    <span class="callout-kicker">ESPECIFICACIÓN TÉCNICA DE LA CARGA:</span>
                    <p class="callout-desc">{{ activeData.tipoCargaDescripcion }}</p>
                  </div>
                </div>
              </div>

              <!-- Tramo Nacional Terrestre -->
              <div v-if="activeData.servicioTransporte || activeData.origenDepartamento" class="detail-item full-width-sm">
                <span class="detail-label">Ruta Nacional de Transporte Local (SGA Flota):</span>
                <div class="route-badge-box">
                  <i class="pi pi-truck"></i>
                  <span>{{ activeData.origenDepartamento || 'Callao' }} ({{ activeData.origenProvincia || 'Callao' }})</span>
                  <i class="pi pi-arrow-right route-arrow"></i>
                  <span>{{ activeData.destinoDepartamento || 'Lima' }} ({{ activeData.destinoProvincia || 'Lima' }})</span>
                  <span v-if="Number(activeData.costoTransporteInterno || 0) > 0" class="flete-cost-tag font-mono">
                    Flete: ${{ formatCurrency(activeData.costoTransporteInterno) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- Bloque Operativo SUNAT & DAM (Si ya es Expediente o tiene numeración) -->
        <section v-if="isExpedienteView || activeData.numeroDam" class="form-block-card highlight-border">
          <div class="block-header">
            <div class="block-idx sunat-idx">
              <i class="pi pi-shield"></i>
            </div>
            <div class="block-header-info">
              <h3 class="block-title">Control Aduanero SUNAT, Declaración DAM & Levante</h3>
              <p class="block-sub">Trazabilidad de despacho aduanero, canal de aforo asignado y datos de manifiesto.</p>
            </div>
            <span v-if="activeData.canalSunat" :class="['canal-badge-lg', `canal-${String(activeData.canalSunat).toLowerCase()}`]">
              CANAL {{ activeData.canalSunat }}
            </span>
          </div>

          <div class="block-body">
            <div class="details-grid-3">
              <div class="detail-item">
                <span class="detail-label">Declaración Aduanera (DAM):</span>
                <span class="detail-val font-mono font-bold" style="color: var(--color-accent-copper);">
                  {{ activeData.numeroDam || 'Pendiente de numeración' }}
                </span>
              </div>

              <div class="detail-item">
                <span class="detail-label">Estado de Levante Aduanero:</span>
                <span v-if="activeData.fechaLevanteAutorizacion" class="text-success font-bold">
                  <i class="pi pi-check-circle"></i> Levante Autorizado ({{ formatDate(activeData.fechaLevanteAutorizacion) }})
                </span>
                <span v-else class="text-warning font-bold">
                  <i class="pi pi-clock"></i> Pendiente de Levante
                </span>
              </div>

              <div class="detail-item">
                <span class="detail-label">Nave / Embarcación / N° Vuelo:</span>
                <span class="detail-val">{{ activeData.naveViaje || 'Sin nave asignada' }}</span>
              </div>

              <div class="detail-item">
                <span class="detail-label">N° Contenedor:</span>
                <span class="detail-val font-mono strong">{{ activeData.numeroContenedor || 'Sin contenedor asignado' }}</span>
              </div>

              <div class="detail-item">
                <span class="detail-label">Precinto Aduanero:</span>
                <span class="detail-val font-mono">{{ activeData.precintoAduanero || 'Pendiente' }}</span>
              </div>

              <div class="detail-item">
                <span class="detail-label">Fechas Estimadas Tránsito:</span>
                <span class="detail-val font-mono">
                  ETD: {{ formatDate(activeData.fechaZarpeEtd) }} | ETA: {{ formatDate(activeData.fechaLlegadaEta) }}
                </span>
              </div>

              <div v-if="activeData.detalleInspeccion" class="detail-item full-width-sm">
                <span class="detail-label">Diligencia / Incidencias de Inspección Técnica:</span>
                <p class="obs-full-text">{{ activeData.detalleInspeccion }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- Bloque 03: Valoración Aduanera & Liquidación Arancelaria -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">03</div>
            <div class="block-header-info">
              <h3 class="block-title">Liquidación Económica & Valoración Aduanera</h3>
              <p class="block-sub">Base imponible FOB/CIF y liquidación de tributos, aranceles y servicios aduaneros.</p>
            </div>
          </div>

          <div class="block-body">
            <!-- Tarjetas de Base Imponible (Valor CIF) -->
            <div class="cif-cards-row">
              <div class="cif-card">
                <span class="cif-card-label">VALOR FOB</span>
                <span class="cif-card-val font-mono">${{ formatCurrency(activeData.valorFob || activeData.montoFobEstimado) }}</span>
                <span class="cif-card-sub">Mercancía declarada</span>
              </div>
              <div class="cif-card">
                <span class="cif-card-label">FLETE INTERNACIONAL</span>
                <span class="cif-card-val font-mono">${{ formatCurrency(activeData.fleteInternacional || activeData.montoFleteEstimado) }}</span>
                <span class="cif-card-sub">Flete naviero / aéreo</span>
              </div>
              <div class="cif-card">
                <span class="cif-card-label">SEGURO ESTIMADO</span>
                <span class="cif-card-val font-mono">${{ formatCurrency(activeData.seguroEstimado || activeData.montoSeguroEstimado) }}</span>
                <span class="cif-card-sub">Póliza o tabla SUNAT</span>
              </div>
              <div class="cif-card highlight">
                <span class="cif-card-label">VALOR CIF (BASE SUNAT)</span>
                <span class="cif-card-val font-mono">${{ formatCurrency(calculatedCif) }}</span>
                <span class="cif-card-sub">Base imponible tributaria</span>
              </div>
            </div>

            <!-- Tabla de Tributos y Servicios a Liquidar -->
            <div class="financial-table-wrapper">
              <table class="financial-breakdown-table">
                <thead>
                  <tr>
                    <th>Concepto Arancelario / Operativo</th>
                    <th class="text-center">Tasa Aplicada</th>
                    <th class="text-right">Monto Liquidado ($ USD)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>
                      <strong>Arancel Ad-Valórem</strong>
                      <span class="concept-sub">Tasa arancelaria según partida NANDINA</span>
                    </td>
                    <td class="text-center font-mono font-bold">{{ activeData.tasaAdValorem ?? 0 }}%</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(calculatedAdValorem) }}</td>
                  </tr>
                  <tr>
                    <td>
                      <strong>Impuesto General a las Ventas (IGV 16% + IPM 2%)</strong>
                      <span class="concept-sub">Tasa oficial SUNAT sobre base CIF + Ad-Valórem</span>
                    </td>
                    <td class="text-center font-mono">18.00%</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(calculatedIgv) }}</td>
                  </tr>
                  <tr v-if="Number(activeData.tasaPercepcion || 0) > 0 || Number(activeData.montoPercepcion || 0) > 0">
                    <td>
                      <strong>Percepción SUNAT</strong>
                      <span class="concept-sub">Régimen de percepción para importaciones</span>
                    </td>
                    <td class="text-center font-mono">{{ activeData.tasaPercepcion || 0 }}%</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(calculatedPercepcion) }}</td>
                  </tr>
                  <tr>
                    <td>
                      <strong>Honorarios de Agencia Aduanera (SGA)</strong>
                      <span class="concept-sub">Servicios integrales de agenciamiento aduanero</span>
                    </td>
                    <td class="text-center text-muted">—</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(activeData.montoHonorariosEstimado) }}</td>
                  </tr>
                  <tr v-if="Number(activeData.costoTransporteInterno || 0) > 0">
                    <td>
                      <strong>Flete Terrestre Nacional</strong>
                      <span class="concept-sub">Transporte local de carga (Ruta nacional)</span>
                    </td>
                    <td class="text-center text-muted">—</td>
                    <td class="text-right font-mono font-bold">${{ formatCurrency(activeData.costoTransporteInterno) }}</td>
                  </tr>
                </tbody>
                <tfoot>
                  <tr class="total-row">
                    <td colspan="2">
                      <span class="total-label">TOTAL ESTIMADO A LIQUIDAR (EXCLUYE FOB)</span>
                    </td>
                    <td class="text-right">
                      <span class="total-amount font-mono">${{ formatCurrency(activeData.totalCotizado) }} USD</span>
                    </td>
                  </tr>
                </tfoot>
              </table>
            </div>

            <!-- Resumen Financiero de Abonos en Finanzas -->
            <div class="payment-clearance-strip">
              <div class="strip-col">
                <span class="strip-label">ESTADO DEL PAGO</span>
                <span class="strip-val font-bold" :style="{ color: activeData.tienePagoAprobado ? '#15803d' : '#8c5b16' }">
                  <i :class="activeData.tienePagoAprobado ? 'pi pi-check-circle' : 'pi pi-clock'"></i>
                  {{ activeData.estadoPago || (activeData.tienePagoAprobado ? 'PAGO_VALIDADO' : 'PENDIENTE') }}
                </span>
              </div>
              <div class="strip-col">
                <span class="strip-label">TOTAL ABONADO / ANTICIPO</span>
                <span class="strip-val font-mono text-success font-bold">${{ formatCurrency(activeData.totalAbonado) }}</span>
              </div>
              <div class="strip-col">
                <span class="strip-label">SALDO PENDIENTE</span>
                <span class="strip-val font-mono font-bold" :class="Number(activeData.saldoPendiente || 0) > 0 ? 'text-warning' : 'text-muted'">
                  ${{ formatCurrency(activeData.saldoPendiente) }}
                </span>
              </div>
            </div>
          </div>
        </section>

        <!-- Bloque 04: Observaciones Registradas -->
        <section v-if="activeData.observaciones" class="form-block-card">
          <div class="block-header">
            <div class="block-idx">04</div>
            <div class="block-header-info">
              <h3 class="block-title">Observaciones Operativas & Anotaciones de Supervisión</h3>
              <p class="block-sub">Instrucciones registradas para la operación aduanera.</p>
            </div>
          </div>
          <div class="block-body">
            <p class="obs-full-text">{{ activeData.observaciones }}</p>
          </div>
        </section>

        <!-- Bloque 05: DOCUMENTOS SUSTENTATORIOS ADJUNTOS -->
        <section class="form-block-card">
          <div class="block-header">
            <div class="block-idx">05</div>
            <div class="block-header-info">
              <h3 class="block-title">Documentos Sustentatorios y Anexos Aduaneros</h3>
              <p class="block-sub">Facturas comerciales, listas de empaque (packing list), conocimientos de embarque (BL/Air Waybill) y resoluciones SUNAT.</p>
            </div>
            <span class="panel-header-badge">GESTIÓN INTEGRADA</span>
          </div>

          <div class="block-body">
            <DocumentosUploadPanel
              mode="live"
              :entidad-tipo="isExpedienteView ? 'EXPEDIENTE' : 'COTIZACION'"
              :id-entidad="isExpedienteView ? (activeData.idExpediente || activeData.idCotizacion) : activeData.idCotizacion"
              :entidad-id="isExpedienteView ? (activeData.idExpediente || activeData.idCotizacion) : activeData.idCotizacion"
              :codigo-entidad="isExpedienteView ? activeData.codigoExpediente : (activeData.codigoCotizacion || `COT-${activeData.idCotizacion}`)"
              :titulo-entidad="`${isExpedienteView ? activeData.codigoExpediente : (activeData.codigoCotizacion || 'Cotización')} — ${activeData.clienteRazonSocial || 'Cliente'}`"
              @updated="$emit('doc-updated')"
            />
          </div>
        </section>
      </div>

      <!-- Columna Lateral: Resumen Proforma & Checklist de Auditoría -->
      <aside class="form-summary-column">
        <!-- Tarjeta Proforma Oficial Resumida -->
        <div class="proforma-summary-card">
          <div class="card-kicker-row">
            <span class="proforma-badge-kicker">PROFORMA AUDITADA</span>
            <span class="proforma-badge-number font-mono">{{ activeData.codigoCotizacion || activeData.codigoExpediente }}</span>
          </div>

          <div class="proforma-total-box">
            <span class="proforma-total-label">TOTAL COTIZADO OPERACIÓN</span>
            <span class="proforma-total-amount font-mono">${{ formatCurrency(activeData.totalCotizado) }}</span>
            <span class="proforma-total-currency">Dólares Americanos (USD)</span>
          </div>

          <div class="proforma-divider"></div>

          <div class="proforma-quick-stats">
            <div class="quick-stat-row">
              <span class="stat-lbl">Valor CIF Estimado:</span>
              <span class="stat-val font-mono">${{ formatCurrency(calculatedCif) }}</span>
            </div>
            <div class="quick-stat-row">
              <span class="stat-lbl">Tributos SUNAT (Est.):</span>
              <span class="stat-val font-mono">${{ formatCurrency(calculatedTributosTotal) }}</span>
            </div>
            <div class="quick-stat-row">
              <span class="stat-lbl">Honorarios SGA:</span>
              <span class="stat-val font-mono">${{ formatCurrency(activeData.montoHonorariosEstimado) }}</span>
            </div>
            <div class="quick-stat-row">
              <span class="stat-lbl">Canal SUNAT:</span>
              <span class="stat-val font-bold" :style="{ color: getCanalColor(activeData.canalSunat) }">
                {{ activeData.canalSunat || 'PENDIENTE' }}
              </span>
            </div>
          </div>
        </div>

        <!-- Checklist de Auditoría Técnica -->
        <div class="audit-checklist-card">
          <div class="checklist-header">
            <i class="pi pi-check-square"></i>
            <span class="checklist-title">Checklist de Auditoría Técnica</span>
          </div>
          <p class="checklist-desc">
            Criterios normativos validados para el despacho aduanero:
          </p>

          <div class="checklist-list">
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>RUC del importador/exportador activo y habido</span>
            </div>
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>Régimen aduanero coherente con la operación</span>
            </div>
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>Base imponible CIF sustentada técnicamente</span>
            </div>
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>Partida arancelaria NANDINA identificada</span>
            </div>
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>Tipo de carga y especificación descriptiva verificada</span>
            </div>
            <div class="checklist-row valid">
              <i class="pi pi-check-circle"></i>
              <span>Documentos comerciales mínimos cargados</span>
            </div>
          </div>

          <div class="checklist-audit-stamp">
            <i class="pi pi-verified"></i>
            <div>
              <strong>Supervisión Aduanera SGA</strong>
              <small>Validación de conformidad normativa SUNAT</small>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import DocumentosUploadPanel from '@/shared/components/DocumentosUploadPanel.vue'

const props = defineProps({
  cotizacion: {
    type: Object,
    default: null
  },
  expediente: {
    type: Object,
    default: null
  },
  processing: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['back', 'approve', 'observe', 'reject', 'open-apertura', 'doc-updated'])

const showObservationBox = ref(false)
const observationReason = ref('')

const showRejectBox = ref(false)
const rejectReason = ref('')

// Unificación de datos de entrada (Expediente o Cotización)
const activeData = computed(() => {
  return props.expediente || props.cotizacion || {}
})

const isExpedienteView = computed(() => {
  return !!props.expediente || !!props.cotizacion?.codigoExpediente
})

const isAirModal = computed(() => {
  const mod = activeData.value.modalidad || activeData.value.puertoEnvioTipo || activeData.value.puertoDestinoTipo
  return String(mod).toUpperCase() === 'AEREO'
})

const isAuditableState = computed(() => {
  const st = activeData.value?.estado
  return ['PENDIENTE', 'EN_REVISION', 'ENVIADA', 'REVISION', 'OBSERVADA'].includes(st)
})

const toggleObservationBox = () => {
  showRejectBox.value = false
  showObservationBox.value = !showObservationBox.value
}

const toggleRejectBox = () => {
  showObservationBox.value = false
  showRejectBox.value = !showRejectBox.value
}

const submitObservation = () => {
  if (!observationReason.value.trim()) return
  emit('observe', {
    cotizacion: activeData.value,
    reason: observationReason.value.trim()
  })
}

const submitReject = () => {
  if (!rejectReason.value.trim()) return
  emit('reject', {
    cotizacion: activeData.value,
    reason: rejectReason.value.trim()
  })
}

// Cálculos financieros reactivos
const calculatedCif = computed(() => {
  if (activeData.value.valorCif) return Number(activeData.value.valorCif)
  const fob = Number(activeData.value.valorFob || activeData.value.montoFobEstimado || 0)
  const flete = Number(activeData.value.fleteInternacional || activeData.value.montoFleteEstimado || 0)
  const seguro = Number(activeData.value.seguroEstimado || activeData.value.montoSeguroEstimado || 0)
  return fob + flete + seguro
})

const calculatedAdValorem = computed(() => {
  if (activeData.value.montoAdValorem) return Number(activeData.value.montoAdValorem)
  const tasa = Number(activeData.value.tasaAdValorem || 0)
  return (calculatedCif.value * tasa) / 100
})

const calculatedIgv = computed(() => {
  if (activeData.value.montoIgv) return Number(activeData.value.montoIgv)
  const baseIgv = calculatedCif.value + calculatedAdValorem.value
  return (baseIgv * 18) / 100
})

const calculatedPercepcion = computed(() => {
  if (activeData.value.montoPercepcion) return Number(activeData.value.montoPercepcion)
  const tasa = Number(activeData.value.tasaPercepcion || 0)
  if (tasa <= 0) return 0
  const baseTotal = calculatedCif.value + calculatedAdValorem.value + calculatedIgv.value
  return (baseTotal * tasa) / 100
})

const calculatedTributosTotal = computed(() => {
  return calculatedAdValorem.value + calculatedIgv.value + calculatedPercepcion.value
})

// Formateadores
const formatCurrency = (val) => {
  return Number(val || 0).toLocaleString('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
}

const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  return String(dateStr).split('T')[0]
}

const formatRegimen = (regimen) => {
  if (!regimen) return 'Importación para el Consumo (REG-10)'
  const map = {
    IMPORTACION_DEFINITIVA: 'Importación para el Consumo (REG-10)',
    EXPORTACION_DEFINITIVA: 'Exportación Definitiva (REG-40)',
    DEPOSITO_ADUANERO: 'Depósito Aduanero (REG-70)',
    TRANSITO_ADUANERO: 'Tránsito Aduanero (REG-80)',
    ADMISION_TEMPORAL: 'Admisión Temporal (REG-20)'
  }
  return map[regimen] || regimen.replace(/_/g, ' ')
}

const getRegimenCode = (regimen) => {
  if (!regimen) return 'REG-10'
  if (regimen.includes('10') || regimen.includes('CONSUMO') || regimen.includes('IMPORTACION_DEFINITIVA')) return 'REG-10'
  if (regimen.includes('TEMPORAL') || regimen.includes('20')) return 'REG-20'
  if (regimen.includes('EXPORTACION') || regimen.includes('40')) return 'REG-40'
  if (regimen.includes('TRANSITO') || regimen.includes('80')) return 'REG-80'
  if (regimen.includes('DEPOSITO') || regimen.includes('70')) return 'REG-70'
  return 'REG-10'
}

const formatTipoDespacho = (despacho) => {
  if (!despacho) return 'Despacho Anticipado'
  return String(despacho).replace(/_/g, ' ')
}

const getEstadoLabel = (estado) => {
  const map = {
    ENVIADA: 'Por Revisar',
    PENDIENTE: 'Por Revisar',
    EN_REVISION: 'En Evaluación Técnica',
    REVISION: 'Observada',
    OBSERVADA: 'Observada',
    APROBADA: 'Aprobada',
    RECHAZADA: 'Rechazada',
    ANULADA: 'Anulada',
    ASIGNADO: 'Expediente Asignado',
    EN_TRAMITE: 'En Trámite SUNAT',
    CON_CANAL: 'Con Canal Asignado',
    LEVANTE_AUTORIZADO: 'Levante Autorizado',
    CERRADO: 'Expediente Cerrado'
  }
  return map[estado] || estado || 'Pendiente'
}

const getStatusBadgeClass = (estado) => {
  if (['APROBADA', 'LEVANTE_AUTORIZADO', 'CERRADO'].includes(estado)) return 'status-approved'
  if (['RECHAZADA', 'ANULADA'].includes(estado)) return 'status-rejected'
  if (['OBSERVADA', 'REVISION'].includes(estado)) return 'status-pending'
  return 'status-progress'
}

const getStatusIcon = (estado) => {
  if (['APROBADA', 'LEVANTE_AUTORIZADO', 'CERRADO'].includes(estado)) return 'pi pi-check-circle'
  if (['RECHAZADA', 'ANULADA'].includes(estado)) return 'pi pi-times-circle'
  if (['OBSERVADA', 'REVISION'].includes(estado)) return 'pi pi-exclamation-triangle'
  return 'pi pi-clock'
}

const getCanalColor = (canal) => {
  if (canal === 'VERDE') return '#15803d'
  if (canal === 'NARANJA') return '#ea580c'
  if (canal === 'ROJO') return '#dc2626'
  return '#64748b'
}
</script>

<style scoped>
.auditoria-expediente-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding-bottom: 2.5rem;
  width: 100%;
}

/* Header Bar */
.form-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1.25rem;
  padding: 1.5rem 1.75rem;
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  box-shadow: var(--shadow-subtle);
  flex-wrap: wrap;
}

.header-titles {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  max-width: 820px;
}

.btn-back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  background: none;
  border: none;
  color: var(--color-text-muted);
  font-size: 0.725rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  cursor: pointer;
  padding: 0;
  margin-bottom: 0.15rem;
  transition: color 0.15s ease;
}

.btn-back-link:hover {
  color: var(--color-accent-copper, #c2410c);
}

.kicker-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-top: 0.25rem;
}

.badge-kicker {
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: var(--color-accent-kicker, #9a7f60);
  background: var(--color-bg-subtle, #f5f2eb);
  padding: 0.25rem 0.625rem;
  border-radius: 4px;
}

/* Modalidad Pill */
.modalidad-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.6875rem;
  font-weight: 800;
  letter-spacing: 0.04em;
  padding: 0.25rem 0.625rem;
  border-radius: 4px;
}

.modalidad-pill.aereo {
  background: #e0f2fe;
  color: #0369a1;
  border: 1px solid #bae6fd;
}

.modalidad-pill.maritimo {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fde68a;
}

.canal-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.6875rem;
  font-weight: 800;
  padding: 0.25rem 0.625rem;
  border-radius: 4px;
}

.canal-pill.canal-verde {
  background: #dcfce7;
  color: #15803d;
  border: 1px solid #bbf7d0;
}

.canal-pill.canal-naranja {
  background: #ffedd5;
  color: #c2410c;
  border: 1px solid #fed7aa;
}

.canal-pill.canal-rojo {
  background: #fee2e2;
  color: #b91c1c;
  border: 1px solid #fecaca;
}

.canal-pill.canal-pendiente {
  background: #f1f5f9;
  color: #475569;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.25rem 0.625rem;
  border-radius: 4px;
}

.status-approved { background: #dcfce7; color: #15803d; }
.status-rejected { background: #fee2e2; color: #b91c1c; }
.status-pending { background: #ffedd5; color: #c2410c; }
.status-progress { background: #dbeafe; color: #1d4ed8; }

.exp-linked-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.75rem;
  font-weight: 700;
  background: #f1f5f9;
  color: #1e293b;
  border: 1px solid #cbd5e1;
  padding: 0.25rem 0.625rem;
  border-radius: 4px;
}

.form-main-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0.25rem 0 0.25rem 0;
  letter-spacing: -0.02em;
}

.form-main-desc {
  font-size: 0.875rem;
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.5;
}

/* Header Actions */
.header-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.status-lock-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: #8c5b16;
  background: #fdf5e6;
  border: 1px solid #f8dab3;
  padding: 0.5rem 0.875rem;
  border-radius: 6px;
}

/* Content Grid */
.form-content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 1.5rem;
}

.form-blocks-column {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-block-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.5rem 1.75rem;
  box-shadow: var(--shadow-subtle);
}

.form-block-card.highlight-border {
  border-color: #fbd38d;
  background: #fffcf8;
}

.block-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 1rem;
  margin-bottom: 1.25rem;
}

.block-idx {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: var(--color-accent-kicker, #9a7f60);
  color: #fff;
  font-weight: 800;
  font-size: 0.875rem;
}

.block-idx.sunat-idx {
  background: #c2410c;
}

.block-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin: 0;
}

.block-sub {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0.125rem 0 0 0;
}

.canal-badge-lg {
  margin-left: auto;
  font-size: 0.8125rem;
  font-weight: 800;
  padding: 0.35rem 0.875rem;
  border-radius: 9999px;
}

.canal-badge-lg.canal-verde { background: #dcfce7; color: #15803d; }
.canal-badge-lg.canal-naranja { background: #ffedd5; color: #c2410c; }
.canal-badge-lg.canal-rojo { background: #fee2e2; color: #b91c1c; }

.panel-header-badge {
  margin-left: auto;
  font-size: 0.6875rem;
  font-weight: 800;
  color: var(--color-accent-kicker);
  background: var(--color-bg-subtle);
  padding: 0.25rem 0.625rem;
  border-radius: 4px;
}

/* Grids & Details */
.details-grid-3 {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1.25rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.detail-item.full-width-sm {
  grid-column: 1 / -1;
}

.detail-label {
  font-size: 0.6875rem;
  font-weight: 800;
  color: var(--color-text-subtle);
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.detail-val {
  font-size: 0.9375rem;
  color: var(--color-text-main);
}

.detail-val.strong {
  font-weight: 700;
}

.badge-type {
  font-size: 0.6875rem;
  font-weight: 800;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}
.type-cliente { background: #dbeafe; color: #1e40af; }
.type-prospecto { background: #fef3c7; color: #92400e; }

.regimen-tag-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.regimen-code-pill {
  font-size: 0.75rem;
  font-weight: 800;
  background: #f1f5f9;
  color: #0f172a;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
  border: 1px solid #cbd5e1;
}

.regimen-name-text {
  font-size: 0.875rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.modalidad-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
  color: var(--color-text-main);
}

/* Cascada de Puertos */
.ports-cascade-box {
  display: flex;
  align-items: center;
  gap: 1rem;
  background: var(--color-bg-subtle, #f5f2eb);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 1rem 1.25rem;
  margin-top: 0.25rem;
  flex-wrap: wrap;
}

.port-cascade-card {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
  flex: 1;
  min-width: 180px;
}

.port-role-tag {
  font-size: 0.625rem;
  font-weight: 800;
  color: var(--color-text-subtle);
  letter-spacing: 0.06em;
}

.port-name {
  font-size: 1rem;
  color: var(--color-text-main);
}

.port-meta {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.port-meta .dot {
  margin: 0 0.35rem;
}

.route-divider-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background: #fff;
  border: 1px solid var(--color-border);
  color: var(--color-accent-kicker);
}

/* Mercancía Card */
.mercancia-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.75rem 1rem;
  margin-top: 0.25rem;
  flex-wrap: wrap;
}

.mercancia-code-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  font-weight: 800;
  background: #eff6ff;
  color: #1d4ed8;
  border: 1px solid #bfdbfe;
  padding: 0.25rem 0.625rem;
  border-radius: 4px;
}

.mercancia-title {
  font-size: 0.9375rem;
  color: var(--color-text-main);
}

.tipo-carga-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin-top: 0.25rem;
}

/* Tarjeta Callout de Descripción Técnica de Carga */
.tipo-carga-callout-card {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  background: #fdfaf6;
  border: 1px solid #fed7aa;
  border-left: 4px solid var(--color-accent-copper, #c2410c);
  border-radius: var(--radius-md, 8px);
  padding: 0.875rem 1rem;
  margin-top: 0.5rem;
}

.callout-icon-wrap {
  color: var(--color-accent-copper, #c2410c);
  font-size: 1.125rem;
  margin-top: 0.125rem;
}

.callout-kicker {
  display: block;
  font-size: 0.6875rem;
  font-weight: 800;
  color: var(--color-accent-copper, #c2410c);
  letter-spacing: 0.05em;
  margin-bottom: 0.25rem;
}

.callout-desc {
  font-size: 0.8125rem;
  color: #431407;
  margin: 0;
  line-height: 1.45;
}

/* Ruta Nacional */
.route-badge-box {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  padding: 0.625rem 1rem;
  border-radius: var(--radius-md, 8px);
  font-size: 0.8125rem;
  color: var(--color-text-main);
  flex-wrap: wrap;
}

.flete-cost-tag {
  margin-left: auto;
  font-weight: 700;
  color: var(--color-accent-copper);
  background: #fff;
  border: 1px solid #cbd5e1;
  padding: 0.2rem 0.5rem;
  border-radius: 4px;
}

/* CIF Cards */
.cif-cards-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.cif-card {
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.875rem 1rem;
}

.cif-card.highlight {
  background: #f0fdf4;
  border-color: #86efac;
}

.cif-card-label {
  font-size: 0.6875rem;
  font-weight: 800;
  color: var(--color-text-subtle);
  letter-spacing: 0.05em;
}

.cif-card-val {
  font-size: 1.125rem;
  font-weight: 800;
  color: var(--color-text-main);
  margin: 0.25rem 0;
}

.cif-card.highlight .cif-card-val {
  color: #15803d;
}

.cif-card-sub {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

/* Financial Table */
.financial-table-wrapper {
  overflow-x: auto;
  margin-bottom: 1.5rem;
}

.financial-breakdown-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.financial-breakdown-table th {
  background: var(--color-bg-subtle, #f5f2eb);
  padding: 0.75rem 1rem;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-text-main);
  border-bottom: 1px solid var(--color-border);
}

.financial-breakdown-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--color-border);
  color: var(--color-text-main);
}

.concept-sub {
  display: block;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  font-weight: normal;
}

.financial-breakdown-table .total-row td {
  background: var(--color-bg-subtle);
  font-weight: 800;
  padding: 1rem;
}

.total-amount {
  font-size: 1.125rem;
  color: var(--color-accent-copper, #c2410c);
}

/* Payment Strip */
.payment-clearance-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  background: var(--color-bg-subtle, #f5f2eb);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 1rem 1.25rem;
  flex-wrap: wrap;
}

.strip-col {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.strip-label {
  font-size: 0.6875rem;
  font-weight: 800;
  color: var(--color-text-subtle);
  letter-spacing: 0.05em;
}

.strip-val {
  font-size: 0.9375rem;
  color: var(--color-text-main);
}

/* Side Column */
.form-summary-column {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.proforma-summary-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.5rem;
  box-shadow: var(--shadow-subtle);
}

.card-kicker-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.proforma-badge-kicker {
  font-size: 0.6875rem;
  font-weight: 800;
  color: var(--color-accent-kicker);
  letter-spacing: 0.05em;
}

.proforma-badge-number {
  font-size: 0.8125rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.proforma-total-box {
  display: flex;
  flex-direction: column;
  text-align: center;
  padding: 1rem 0;
}

.proforma-total-label {
  font-size: 0.6875rem;
  font-weight: 700;
  color: var(--color-text-muted);
}

.proforma-total-amount {
  font-size: 1.75rem;
  font-weight: 800;
  color: var(--color-accent-copper, #c2410c);
  margin: 0.25rem 0;
}

.proforma-total-currency {
  font-size: 0.75rem;
  color: var(--color-text-muted);
}

.proforma-divider {
  height: 1px;
  background: var(--color-border);
  margin: 1rem 0;
}

.proforma-quick-stats {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.quick-stat-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.8125rem;
}

.stat-lbl { color: var(--color-text-muted); }
.stat-val { color: var(--color-text-main); font-weight: 600; }

/* Checklist */
.audit-checklist-card {
  background: var(--color-bg-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.5rem;
  box-shadow: var(--shadow-subtle);
}

.checklist-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-text-main);
  margin-bottom: 0.5rem;
}

.checklist-desc {
  font-size: 0.75rem;
  color: var(--color-text-muted);
  margin: 0 0 1rem 0;
  line-height: 1.4;
}

.checklist-list {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  margin-bottom: 1.25rem;
}

.checklist-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: var(--color-text-main);
}

.checklist-row.valid i {
  color: #15803d;
}

.checklist-audit-stamp {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: var(--color-bg-subtle, #f5f2eb);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  padding: 0.75rem 1rem;
}

.checklist-audit-stamp i {
  font-size: 1.5rem;
  color: var(--color-accent-kicker);
}

.checklist-audit-stamp strong {
  display: block;
  font-size: 0.8125rem;
  color: var(--color-text-main);
}

.checklist-audit-stamp small {
  font-size: 0.6875rem;
  color: var(--color-text-muted);
}

/* Panels */
.inline-action-panel {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  padding: 1.25rem 1.5rem;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 700;
}

.btn-close-panel {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
}

.panel-desc {
  font-size: 0.8125rem;
  color: var(--color-text-muted);
  margin: 0 0 1rem 0;
}

.form-textarea {
  width: 100%;
  padding: 0.625rem 0.875rem;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md, 8px);
  background: var(--color-bg-surface);
  color: var(--color-text-main);
  font-family: inherit;
  font-size: 0.875rem;
  resize: vertical;
}

.panel-actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}

/* Botones */
.btn-espresso {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--color-primary-espresso, #2b231d);
  color: #fff;
  border: 1px solid var(--color-primary-espresso, #2b231d);
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md, 8px);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-espresso:hover:not(:disabled) {
  background: var(--color-accent-copper, #c2410c);
  border-color: var(--color-accent-copper, #c2410c);
}

.btn-danger-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #fee2e2;
  color: #b91c1c;
  border: 1px solid #fecaca;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md, 8px);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-warm-action {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #ffedd5;
  color: #c2410c;
  border: 1px solid #fed7aa;
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md, 8px);
  font-size: 0.8125rem;
  font-weight: 700;
  cursor: pointer;
}

.btn-outline-warm {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: transparent;
  color: var(--color-text-main);
  border: 1px solid var(--color-border);
  padding: 0.5rem 1rem;
  border-radius: var(--radius-md, 8px);
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
}

.obs-full-text {
  font-size: 0.875rem;
  color: var(--color-text-main);
  background: var(--color-bg-subtle, #f5f2eb);
  padding: 0.875rem 1rem;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  margin: 0;
  line-height: 1.5;
}

.fade-in {
  animation: fadeIn 0.2s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1024px) {
  .form-content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
