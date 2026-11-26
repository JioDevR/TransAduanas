<template>
  <Dialog
    :visible="visible"
    modal
    header="Liquidación Oficial de Despacho Aduanero"
    :style="{ width: getDialogWidth('760px') }"
    @update:visible="$emit('update:visible', $event)"
  >
    <div v-if="liquidacion" class="sga-dialog-body sga-invoice-sheet">
      <div class="sga-invoice-head">
        <div>
          <span class="sga-kicker">SISTEMA DE GESTIÓN ADUANERA (SGA)</span>
          <h3 class="sga-invoice-brand">TransAduanas — Agencia de Aduanas</h3>
          <span class="sga-cell-sub">Autorizada SUNAT · Liquidación de Despacho</span>
        </div>
        <div class="sga-invoice-code-box">
          <span class="sga-meta-label">LIQUIDACIÓN ADUANERA</span>
          <strong class="sga-invoice-num font-mono">{{ liquidacion.codigoLiquidacion }}</strong>
          <span class="sga-cell-sub font-mono">Emisión: {{ formatDate(liquidacion.fechaEmision) }}</span>
        </div>
      </div>

      <div class="sga-invoice-meta">
        <div class="sga-meta-item">
          <span class="sga-meta-label">CLIENTE IMPORTADOR</span>
          <span class="sga-meta-val">{{ liquidacion.clienteRazonSocial }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">RUC / DOCUMENTO</span>
          <span class="sga-meta-val font-mono">{{ liquidacion.clienteNumeroDocumento || '—' }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">EXPEDIENTE</span>
          <span class="sga-meta-val font-mono">{{ liquidacion.codigoExpediente || '—' }}</span>
        </div>
        <div class="sga-meta-item">
          <span class="sga-meta-label">DECLARACIÓN DAM</span>
          <span class="sga-meta-val font-mono">{{ liquidacion.numeroDam || 'S/N' }}</span>
        </div>
      </div>

      <table class="sga-invoice-table">
        <thead>
          <tr>
            <th>Rubro / Concepto Liquidado</th>
            <th class="text-right">Importe (S/)</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>
              <div class="sga-cell-primary">Tributos Liquidados ante SUNAT</div>
              <div class="sga-cell-sub">Ad-Valorem, IGV e IPM aduanero cancelados con DAM</div>
            </td>
            <td class="text-right font-mono">{{ formatPen(liquidacion.totalTributosSunat) }}</td>
          </tr>
          <tr>
            <td>
              <div class="sga-cell-primary">Gastos Portuarios & Depósito Temporal</div>
              <div class="sga-cell-sub">Almacenaje, pesaje VGM y manipuleo de contenedor</div>
            </td>
            <td class="text-right font-mono">{{ formatPen(liquidacion.gastosAlmacenPuerto) }}</td>
          </tr>
          <tr>
            <td>
              <div class="sga-cell-primary">Flete Terrestre de Distribución Local</div>
              <div class="sga-cell-sub">Traslado en unidad autorizada desde garita a planta</div>
            </td>
            <td class="text-right font-mono">{{ formatPen(liquidacion.fleteTransporteLocal) }}</td>
          </tr>
          <tr>
            <td>
              <div class="sga-cell-primary">Honorarios Profesionales de Agenciamiento</div>
              <div class="sga-cell-sub">Gestión de trámite, despacho y liquidación aduanera</div>
            </td>
            <td class="text-right font-mono">{{ formatPen(liquidacion.honorariosAgencia) }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td>Subtotal de Servicios Gravados</td>
            <td class="text-right font-mono">{{ formatPen(liquidacion.subtotalImponible) }}</td>
          </tr>
          <tr>
            <td>Impuesto General a las Ventas (IGV 18%)</td>
            <td class="text-right font-mono">{{ formatPen(liquidacion.igvMonto) }}</td>
          </tr>
          <tr v-if="liquidacion.aplicaDetraccion" class="sga-text-warn">
            <td>Detracción SPOT a Banco de la Nación (12%)</td>
            <td class="text-right font-mono">- {{ formatPen(liquidacion.montoDetraccion) }}</td>
          </tr>
          <tr class="is-total">
            <td>TOTAL GENERAL CONSOLIDADO (PEN)</td>
            <td class="text-right font-mono">{{ formatPen(liquidacion.totalGeneralPen) }}</td>
          </tr>
          <tr class="sga-cell-sub">
            <td>Equivalente en Dólares (T.C. {{ liquidacion.tipoCambio }})</td>
            <td class="text-right font-mono">{{ formatUsd(liquidacion.totalGeneralUsd) }}</td>
          </tr>
        </tfoot>
      </table>

      <div v-if="liquidacion.estadoPago === 'PAGADO'" class="sga-banner is-success">
        <i class="pi pi-check-circle"></i>
        <div>
          <strong>PAGADO / CONFORME:</strong> Abonado vía
          <strong>{{ liquidacion.bancoDestino || formatMetodoPagoLabel(liquidacion.metodoPago) }}</strong>
          el {{ formatDateTime(liquidacion.fechaPago) }} — Op:
          <strong class="font-mono">{{ liquidacion.numeroOperacion || '—' }}</strong>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="sga-dialog-footer">
        <Button
          type="button"
          label="Cerrar"
          severity="secondary"
          outlined
          size="small"
          @click="$emit('update:visible', false)"
        />
        <Button
          type="button"
          label="Imprimir / Exportar"
          icon="pi pi-print"
          size="small"
          @click="imprimirProforma"
        />
      </div>
    </template>
  </Dialog>
</template>

<script setup>
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import { useResponsive } from '@/shared/composables/useResponsive'
import { formatDate, formatDateTime } from '@/shared/utils/dateUtils'
import { formatPen, formatUsd, formatMetodoPagoLabel } from '../../utils/finanzasFormatters'

const props = defineProps({
  visible: { type: Boolean, default: false },
  liquidacion: { type: Object, default: null }
})

defineEmits(['update:visible'])

const { getDialogWidth } = useResponsive()

function imprimirProforma() {
  const l = props.liquidacion
  if (!l) return
  const popup = window.open('', '_blank', 'width=860,height=720')
  if (!popup) {
    window.print()
    return
  }

  const escapeHtml = (val) =>
    String(val ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')

  const detraccionRow = l.aplicaDetraccion
    ? `<tr><td>Detracción SPOT Banco de la Nación (12%)</td><td class="r">- ${escapeHtml(formatPen(l.montoDetraccion))}</td></tr>`
    : ''

  popup.document.write(`<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <title>${escapeHtml(l.codigoLiquidacion)} — Liquidación Aduanera</title>
  <style>
    body { font-family: Inter, system-ui, sans-serif; color: #2b2622; margin: 32px; }
    .head { display: flex; justify-content: space-between; border-bottom: 2px solid #352f2a; padding-bottom: 12px; margin-bottom: 18px; }
    .code { font-family: monospace; font-size: 18px; font-weight: 700; color: #824728; }
    .meta { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px 16px; background: #fbf9f6; padding: 12px; border: 1px solid #e4dacb; border-radius: 8px; margin-bottom: 18px; font-size: 13px; }
    .lbl { font-size: 11px; color: #6f6356; text-transform: uppercase; font-weight: 700; display: block; }
    table { width: 100%; border-collapse: collapse; font-size: 13px; }
    th, td { padding: 9px 8px; border-bottom: 1px solid #ede6db; text-align: left; }
    th { font-size: 11px; text-transform: uppercase; color: #6f6356; }
    .r { text-align: right; font-family: monospace; }
    .tot { font-weight: 700; font-size: 15px; border-top: 2px solid #352f2a; }
    @media print { body { margin: 16px; } }
  </style>
</head>
<body>
  <div class="head">
    <div>
      <div class="lbl">Sistema de Gestión Aduanera (SGA)</div>
      <h2 style="margin:4px 0 0;">TransAduanas — Liquidación Aduanera</h2>
    </div>
    <div style="text-align:right">
      <div class="code">${escapeHtml(l.codigoLiquidacion)}</div>
      <div style="font-size:12px;color:#6f6356;">Emisión: ${escapeHtml(formatDate(l.fechaEmision))}</div>
    </div>
  </div>
  <div class="meta">
    <div><span class="lbl">Cliente</span>${escapeHtml(l.clienteRazonSocial)}</div>
    <div><span class="lbl">RUC / Doc</span>${escapeHtml(l.clienteNumeroDocumento || '—')}</div>
    <div><span class="lbl">Expediente</span>${escapeHtml(l.codigoExpediente || '—')}</div>
    <div><span class="lbl">Declaración DAM</span>${escapeHtml(l.numeroDam || 'S/N')}</div>
  </div>
  <table>
    <thead><tr><th>Concepto</th><th class="r">Importe (S/)</th></tr></thead>
    <tbody>
      <tr><td>Tributos Liquidados ante SUNAT</td><td class="r">${escapeHtml(formatPen(l.totalTributosSunat))}</td></tr>
      <tr><td>Gastos Portuarios & Depósito Temporal</td><td class="r">${escapeHtml(formatPen(l.gastosAlmacenPuerto))}</td></tr>
      <tr><td>Flete Terrestre Local</td><td class="r">${escapeHtml(formatPen(l.fleteTransporteLocal))}</td></tr>
      <tr><td>Honorarios de Agenciamiento</td><td class="r">${escapeHtml(formatPen(l.honorariosAgencia))}</td></tr>
    </tbody>
    <tfoot>
      <tr><td>Subtotal Servicios Gravados</td><td class="r">${escapeHtml(formatPen(l.subtotalImponible))}</td></tr>
      <tr><td>IGV (18%)</td><td class="r">${escapeHtml(formatPen(l.igvMonto))}</td></tr>
      ${detraccionRow}
      <tr class="tot"><td>TOTAL GENERAL (PEN)</td><td class="r">${escapeHtml(formatPen(l.totalGeneralPen))}</td></tr>
      <tr><td>Equivalente USD (T.C. ${escapeHtml(l.tipoCambio)})</td><td class="r">${escapeHtml(formatUsd(l.totalGeneralUsd))}</td></tr>
    </tfoot>
  </table>
</body>
</html>`)
  popup.document.close()
  popup.focus()
  popup.print()
}
</script>

<style scoped>
.sga-invoice-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
  padding-bottom: 0.9rem;
  border-bottom: 1px solid var(--color-border);
}

.sga-invoice-brand {
  margin: 0.15rem 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--color-text-main);
}

.sga-invoice-code-box {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.15rem;
}

.sga-invoice-num {
  font-size: 1.1rem;
  color: var(--color-primary);
}

.sga-invoice-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  border-radius: 10px;
  background: var(--color-bg-subtle);
  border: 1px solid var(--color-border-subtle);
}

.sga-invoice-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}

.sga-invoice-table th,
.sga-invoice-table td {
  padding: 0.65rem 0.5rem;
  border-bottom: 1px solid var(--color-border-subtle);
  text-align: left;
}

.sga-invoice-table th {
  font-size: 0.6875rem;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--color-text-muted);
}

.sga-invoice-table tfoot tr.is-total td {
  border-top: 1.5px solid var(--color-border-strong);
  font-size: 0.98rem;
  font-weight: 700;
  color: var(--color-primary);
}
</style>
