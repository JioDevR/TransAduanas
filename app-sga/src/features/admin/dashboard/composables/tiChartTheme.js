/**
 * Paleta y opciones Chart.js unificadas para el Dashboard de Administración TI.
 */

export function getTiChartPalette(isDark) {
  return isDark
    ? {
        text: '#c4bcab',
        grid: 'rgba(255, 255, 255, 0.08)',
        surfaceBorder: '#2b2622',
        doughnut: ['#f0b892', '#5cb874', '#38bdf8', '#eab308', '#a78bfa', '#fb923c', '#f87171', '#94a3b8'],
        ok: '#5cb874',
        fail: '#f87171'
      }
    : {
        text: '#6f6356',
        grid: '#ede6db',
        surfaceBorder: '#ffffff',
        doughnut: ['#d48252', '#2e7d32', '#21598f', '#8c5b16', '#6366f1', '#c2413b', '#0d9488', '#6f6356'],
        ok: '#2e7d32',
        fail: '#9e2a2b'
      }
}

export function buildDoughnutOptions(palette) {
  return {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '68%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: palette.text,
          boxWidth: 10,
          padding: 12,
          font: { family: 'Inter, system-ui, sans-serif', size: 11 }
        }
      }
    }
  }
}

export function buildBarOptions(palette) {
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'top',
        align: 'end',
        labels: {
          color: palette.text,
          boxWidth: 10,
          font: { family: 'Inter, system-ui, sans-serif', size: 11 }
        }
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: { color: palette.text, font: { family: 'Inter, system-ui, sans-serif', size: 11 } }
      },
      y: {
        beginAtZero: true,
        ticks: { precision: 0, color: palette.text, font: { family: 'Inter, system-ui, sans-serif', size: 11 } },
        grid: { color: palette.grid }
      }
    }
  }
}
