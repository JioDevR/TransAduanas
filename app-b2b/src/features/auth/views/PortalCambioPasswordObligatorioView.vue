<template>
  <div class="sga-auth-page">
    <div class="sga-auth-wrapper sga-auth-wrapper--wide">
      <div class="sga-auth-card">
        <!-- Logo y Cabecera de Seguridad -->
        <div class="sga-auth-brand">
          <div class="sga-auth-logo" title="Seguridad TransAduanas">
            <i class="pi pi-shield"></i>
          </div>
          <h1 class="sga-auth-title">Primer Inicio de Sesión</h1>
          <Tag
            value="ACTUALIZACIÓN OBLIGATORIA DE CONTRASEÑA"
            severity="warn"
            icon="pi pi-lock"
            class="sga-auth-pill"
          />
          <p class="sga-auth-subtitle">
            Por políticas de confidencialidad y control aduanero B2B, debe reemplazar su contraseña temporal antes de ingresar a la plataforma.
          </p>
        </div>

        <!-- Ficha de la Empresa Autenticada -->
        <div class="sga-auth-entity">
          <div class="sga-auth-entity-icon">
            <i class="pi pi-building"></i>
          </div>
          <div class="sga-auth-entity-info">
            <h2 class="sga-auth-entity-name">{{ authStore.empresaNombre }}</h2>
            <div v-if="authStore.ruc || authStore.user?.username" class="sga-auth-entity-meta">
              <span v-if="authStore.ruc">RUC: {{ authStore.ruc }}</span>
              <span
                v-if="authStore.ruc && authStore.user?.username"
                class="sga-auth-entity-sep"
                aria-hidden="true"
              >&bull;</span>
              <span v-if="authStore.user?.username">Usuario: @{{ authStore.user.username }}</span>
            </div>
          </div>
        </div>

        <!-- Alerta de Error -->
        <Message v-if="errorMessage" severity="error" :closable="false" class="sga-auth-alert">
          {{ errorMessage }}
        </Message>

        <!-- Formulario de Cambio de Clave -->
        <form @submit.prevent="handleSubmit" class="sga-auth-form">
          <div class="sga-auth-field">
            <label for="current-pwd" class="sga-auth-label">Contraseña Temporal Asignada *</label>
            <IconField>
              <InputIcon class="pi pi-key" />
              <Password
                inputId="current-pwd"
                v-model="form.passwordActual"
                placeholder="Ingrese la contraseña temporal..."
                :feedback="false"
                toggleMask
                :disabled="loading"
                :inputProps="{ autocomplete: 'current-password', required: true }"
                fluid
              />
            </IconField>
            <small class="sga-auth-hint">Ingrese la clave temporal asignada al habilitar su cuenta.</small>
          </div>

          <div class="sga-auth-field">
            <label for="new-pwd" class="sga-auth-label">Nueva Contraseña Privada *</label>
            <IconField>
              <InputIcon class="pi pi-lock" />
              <Password
                inputId="new-pwd"
                v-model="form.nuevaPassword"
                placeholder="Mínimo 6 caracteres..."
                :feedback="false"
                toggleMask
                :disabled="loading"
                :inputProps="{ autocomplete: 'new-password', required: true }"
                fluid
              />
            </IconField>
          </div>

          <div class="sga-auth-field">
            <label for="confirm-pwd" class="sga-auth-label">Confirmar Nueva Contraseña *</label>
            <IconField>
              <InputIcon class="pi pi-lock-open" />
              <Password
                inputId="confirm-pwd"
                v-model="form.confirmarPassword"
                placeholder="Repita la nueva contraseña..."
                :feedback="false"
                toggleMask
                :disabled="loading"
                :inputProps="{ autocomplete: 'new-password', required: true }"
                fluid
              />
            </IconField>
          </div>

          <!-- Checklist de Validación -->
          <div class="sga-auth-checklist" aria-live="polite">
            <div :class="['sga-auth-rule', { 'is-valid': rules.isLengthValid }]">
              <i :class="rules.isLengthValid ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Al menos 6 caracteres</span>
            </div>
            <div :class="['sga-auth-rule', { 'is-valid': rules.isMatchValid }]">
              <i :class="rules.isMatchValid ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Las contraseñas coinciden</span>
            </div>
            <div :class="['sga-auth-rule', { 'is-valid': rules.isDifferentFromCurrent }]">
              <i :class="rules.isDifferentFromCurrent ? 'pi pi-check-circle' : 'pi pi-circle'"></i>
              <span>Es distinta a la contraseña temporal</span>
            </div>
          </div>

          <Button
            type="submit"
            id="btn-update-pwd-submit"
            label="Guardar Nueva Contraseña y Acceder"
            icon="pi pi-arrow-right"
            iconPos="right"
            :loading="loading"
            :disabled="loading || !isFormValid"
            class="sga-auth-submit"
            fluid
          />
        </form>

        <div class="sga-auth-secondary-action">
          <Button
            type="button"
            label="Cerrar sesión y volver más tarde"
            icon="pi pi-sign-out"
            severity="secondary"
            variant="text"
            size="small"
            :disabled="loading"
            @click="handleLogout"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import Button from 'primevue/button'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import Message from 'primevue/message'
import Password from 'primevue/password'
import Tag from 'primevue/tag'
import { useMandatoryPasswordForm } from '../composables/useMandatoryPasswordForm'
import '../../../../../sgaUI/sga-auth.css'

const {
  authStore,
  form,
  rules,
  isFormValid,
  loading,
  errorMessage,
  handleSubmit,
  handleLogout
} = useMandatoryPasswordForm()
</script>
