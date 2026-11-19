<script setup>
import { ref } from 'vue'
import Card from 'primevue/card'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import InputNumber from 'primevue/inputnumber'
import Password from 'primevue/password'
import InputMask from 'primevue/inputmask'
import InputOtp from 'primevue/inputotp'
import Checkbox from 'primevue/checkbox'
import RadioButton from 'primevue/radiobutton'
import ToggleSwitch from 'primevue/toggleswitch'
import Select from 'primevue/select'
import MultiSelect from 'primevue/multiselect'
import AutoComplete from 'primevue/autocomplete'
import DatePicker from 'primevue/datepicker'
import Slider from 'primevue/slider'
import Rating from 'primevue/rating'
import Knob from 'primevue/knob'
import ColorPicker from 'primevue/colorpicker'
import SelectButton from 'primevue/selectbutton'
import ToggleButton from 'primevue/togglebutton'
import FloatLabel from 'primevue/floatlabel'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'

const textVal = ref('TransAduanas SAC')
const textareaVal = ref('Carga consolidada vía marítima Callao')
const numberVal = ref(1250.5)
const passwordVal = ref('Secreto123')
const maskVal = ref('')
const otpVal = ref('')
const checked = ref(true)
const multiCheck = ref(['Marítimo'])
const radioVal = ref('Importación')
const switchVal = ref(true)
const selectedCity = ref({ name: 'Callao', code: 'PECLL' })
const selectedCities = ref([])
const cities = [
  { name: 'Callao', code: 'PECLL' },
  { name: 'Paita', code: 'PEPAI' },
  { name: 'Matarani', code: 'PEMAT' },
  { name: 'Ilo', code: 'PEILO' }
]
const acValue = ref('')
const acItems = ref([])
const searchPorts = (e) => {
  acItems.value = ['Callao', 'Paita', 'Matarani', 'Chancay', 'Ilo', 'Salaverry'].filter((p) =>
    p.toLowerCase().includes(e.query.toLowerCase())
  )
}
const dateVal = ref(new Date())
const sliderVal = ref(45)
const ratingVal = ref(4)
const knobVal = ref(68)
const colorVal = ref('10b981')
const selectBtnVal = ref('Marítimo')
const toggleBtnVal = ref(false)
const floatVal = ref('')
</script>

<template>
  <div>
    <h2 class="section-title">Formularios e Inputs</h2>
    <p class="section-desc">Todos los controles de entrada, selección, fechas y valores numéricos de PrimeVue 4.</p>

    <div class="showcase-grid">
      <Card>
        <template #title>Texto, IconField & FloatLabel</template>
        <template #content>
          <div class="demo-box">
            <InputText v-model="textVal" placeholder="InputText básico" fluid />
            <IconField>
              <InputIcon class="pi pi-search" />
              <InputText placeholder="Buscar con IconField..." fluid />
            </IconField>
            <FloatLabel variant="on">
              <InputText id="float_in" v-model="floatVal" fluid />
              <label for="float_in">FloatLabel (variant="on")</label>
            </FloatLabel>
            <Textarea v-model="textareaVal" rows="2" placeholder="Textarea" fluid />
          </div>
        </template>
      </Card>

      <Card>
        <template #title>InputNumber, Password, Mask & OTP</template>
        <template #content>
          <div class="demo-box">
            <InputNumber v-model="numberVal" mode="currency" currency="USD" locale="en-US" fluid />
            <Password v-model="passwordVal" toggleMask fluid />
            <InputMask v-model="maskVal" mask="99-99999999-9" placeholder="RUC: 99-99999999-9" fluid />
            <div>
              <small>InputOtp (6 dígitos):</small>
              <InputOtp v-model="otpVal" :length="6" />
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>Select, MultiSelect & AutoComplete</template>
        <template #content>
          <div class="demo-box">
            <Select v-model="selectedCity" :options="cities" optionLabel="name" filter placeholder="Select con filtro" fluid />
            <MultiSelect v-model="selectedCities" :options="cities" optionLabel="name" placeholder="MultiSelect" display="chip" fluid />
            <AutoComplete v-model="acValue" :suggestions="acItems" @complete="searchPorts" placeholder="AutoComplete (ej. Callao)" fluid />
            <DatePicker v-model="dateVal" showIcon dateFormat="dd/mm/yy" fluid />
          </div>
        </template>
      </Card>

      <Card>
        <template #title>Checkbox, Radio, Switch & Toggles</template>
        <template #content>
          <div class="demo-box">
            <div class="demo-row">
              <Checkbox v-model="checked" binary inputId="chk1" />
              <label for="chk1">Checkbox binario ({{ checked }})</label>
            </div>
            <div class="demo-row">
              <Checkbox v-model="multiCheck" inputId="m1" value="Marítimo" />
              <label for="m1">Marítimo</label>
              <Checkbox v-model="multiCheck" inputId="m2" value="Aéreo" />
              <label for="m2">Aéreo</label>
            </div>
            <div class="demo-row">
              <RadioButton v-model="radioVal" inputId="r1" value="Importación" />
              <label for="r1">Importación</label>
              <RadioButton v-model="radioVal" inputId="r2" value="Exportación" />
              <label for="r2">Exportación</label>
            </div>
            <div class="demo-row">
              <ToggleSwitch v-model="switchVal" />
              <span>ToggleSwitch: {{ switchVal ? 'Activo' : 'Inactivo' }}</span>
            </div>
            <div class="demo-row">
              <SelectButton v-model="selectBtnVal" :options="['Marítimo', 'Aéreo', 'Terrestre']" />
              <ToggleButton v-model="toggleBtnVal" onLabel="Bloqueado" offLabel="Libre" onIcon="pi pi-lock" offIcon="pi pi-lock-open" />
            </div>
          </div>
        </template>
      </Card>

      <Card>
        <template #title>Slider, Rating, Knob & ColorPicker</template>
        <template #content>
          <div class="demo-box">
            <div>
              <small>Slider: {{ sliderVal }}%</small>
              <Slider v-model="sliderVal" style="margin-top: 0.5rem" />
            </div>
            <div class="demo-row">
              <span>Rating:</span>
              <Rating v-model="ratingVal" />
            </div>
            <div class="demo-row">
              <Knob v-model="knobVal" :size="85" />
              <div>
                <small>ColorPicker: #{{ colorVal }}</small><br />
                <ColorPicker v-model="colorVal" />
              </div>
            </div>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>
