<template>
  <div class="hero-banner-card">
    <div class="hero-content">
      <div v-if="kicker || $slots.kicker" class="hero-kicker-row">
        <slot name="kicker">
          <span class="badge-kicker">{{ kicker }}</span>
        </slot>
        <slot name="extra" />
      </div>
      <h2 class="hero-title">{{ title }}</h2>
      <p v-if="description" class="hero-desc">{{ description }}</p>
      <slot />
    </div>
    <div class="hero-watermark">
      <span>{{ watermark }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  kicker: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  watermark: {
    type: String,
    default: 'SGA'
  }
})
</script>

<style scoped>
.hero-banner-card {
  position: relative;
  background: linear-gradient(135deg, #1c1917 0%, #292524 60%, #453406 100%);
  border: 1px solid rgba(234, 179, 8, 0.25);
  border-radius: var(--radius-lg, 12px);
  padding: 1.75rem 2rem;
  color: #fafaf9;
  overflow: hidden;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.05);
}

.hero-content {
  position: relative;
  z-index: 2;
  max-width: 820px;
}

.hero-kicker-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-bottom: 0.35rem;
}

.badge-kicker {
  display: inline-block;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: #eab308;
  text-transform: uppercase;
}

.hero-title {
  font-size: 1.625rem;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.25;
  margin: 0.15rem 0 0.45rem;
  letter-spacing: -0.015em;
}

.hero-desc {
  font-size: 0.875rem;
  color: #d6d3d1;
  line-height: 1.5;
  margin: 0;
}

.hero-watermark {
  position: absolute;
  right: 1.5rem;
  top: 50%;
  transform: translateY(-50%);
  font-size: 7rem;
  font-weight: 900;
  color: rgba(255, 255, 255, 0.03);
  pointer-events: none;
  user-select: none;
}

@media (max-width: 640px) {
  .hero-banner-card {
    padding: 1.25rem 1.5rem;
  }
  .hero-watermark {
    display: none;
  }
}
</style>
