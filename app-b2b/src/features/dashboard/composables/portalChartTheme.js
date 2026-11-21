/**
 * Paleta y opciones Chart.js unificadas con Warm Editorial Pro (sgaUI).
 */
export function getPortalChartPalette(isDark) {
  return isDark
    ? {
        text: '#c4bcab',
        grid: 'rgba(255, 255, 255, 0.08)',
        surfaceBorder: '#2b2622',
        primary: '#f0b892',
        success: '#5cb874',
        info: '#60a5fa',
        warning: '#fbbf24',
        danger: '#f87171',
        muted: '#6f6759'
      }
    : {
        text: '#6f6356',
        grid: '#ede6db',
        surfaceBorder: '#ffffff',
        primary: '#d48252',
        success: '#1e6638',
        info: '#21598f',
        warning: '#8c5b16',
        danger: '#9e2a2b',
        muted: '#998c7e'
      }
}

export function buildDoughnutOptions(palette) {
  return {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '70%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: palette.text,
          boxWidth: 10,
          boxHeight: 10,
          usePointStyle: true,
          pointStyle: 'circle',
          padding: 14,
          font: { family: 'Inter, system-ui, sans-serif', size: 11, weight: '600' }
        }
      },
      tooltip: {
        padding: 10,
        cornerRadius: 8,
        titleFont: { family: 'Inter, system-ui, sans-serif', size: 12, weight: '600' },
        bodyFont: { family: 'Inter, system-ui, sans-serif', size: 12 }
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
        display: false
      },
      tooltip: {
        padding: 10,
        cornerRadius: 8,
        titleFont: { family: 'Inter, system-ui, sans-serif', size: 12, weight: '600' },
        bodyFont: { family: 'Inter, system-ui, sans-serif', size: 12 }
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          color: palette.text,
          font: { family: 'Inter, system-ui, sans-serif', size: 11, weight: '500' }
        }
      },
      y: {
        beginAtZero: true,
        ticks: {
          precision: 0,
          color: palette.text,
          font: { family: 'JetBrains Mono, monospace', size: 11 }
        },
        grid: { color: palette.grid }
      }
    }
  }
}
