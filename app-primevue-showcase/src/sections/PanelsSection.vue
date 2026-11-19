<script setup>
import { ref } from 'vue'
import Card from 'primevue/card'
import Panel from 'primevue/panel'
import Accordion from 'primevue/accordion'
import AccordionPanel from 'primevue/accordionpanel'
import AccordionHeader from 'primevue/accordionheader'
import AccordionContent from 'primevue/accordioncontent'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Fieldset from 'primevue/fieldset'
import Divider from 'primevue/divider'
import Splitter from 'primevue/splitter'
import SplitterPanel from 'primevue/splitterpanel'
import Stepper from 'primevue/stepper'
import StepList from 'primevue/steplist'
import Step from 'primevue/step'
import StepPanels from 'primevue/steppanels'
import StepPanel from 'primevue/steppanel'
import Toolbar from 'primevue/toolbar'
import ScrollPanel from 'primevue/scrollpanel'
import Button from 'primevue/button'

const activeStep = ref('1')
</script>

<template>
  <div>
    <h2 class="section-title">Paneles, Tabs, Stepper y Layouts</h2>
    <p class="section-desc">Card, Panel, Accordion, Tabs, Fieldset, Divider, Splitter, Stepper, Toolbar y ScrollPanel.</p>

    <div class="demo-box">
      <Toolbar>
        <template #start>
          <Button icon="pi pi-plus" label="Nuevo" class="mr-2" />
          <Button icon="pi pi-print" severity="secondary" text />
        </template>
        <template #center><strong>Toolbar de PrimeVue</strong></template>
        <template #end><Button label="Exportar" icon="pi pi-upload" severity="contrast" /></template>
      </Toolbar>

      <div class="showcase-grid">
        <Panel header="Panel colapsable" toggleable>
          <p style="margin: 0">Contenido dentro de un <code>Panel</code> con botón de colapsar.</p>
          <Divider align="center"><b>Divider</b></Divider>
          <Fieldset legend="Fieldset">
            <p style="margin: 0">Agrupación clásica con etiqueta superior.</p>
          </Fieldset>
        </Panel>

        <Card>
          <template #title>Accordion</template>
          <template #content>
            <Accordion value="0">
              <AccordionPanel value="0">
                <AccordionHeader>1. Agenciamiento Aduanero</AccordionHeader>
                <AccordionContent>Gestión de DAM, canales SUNAT y levante autorizado.</AccordionContent>
              </AccordionPanel>
              <AccordionPanel value="1">
                <AccordionHeader>2. Depósito Temporal</AccordionHeader>
                <AccordionContent>Control de tarjas y pesaje certificado VGM.</AccordionContent>
              </AccordionPanel>
            </Accordion>
          </template>
        </Card>
      </div>

      <Card>
        <template #title>Tabs</template>
        <template #content>
          <Tabs value="0">
            <TabList>
              <Tab value="0">General</Tab>
              <Tab value="1">Documentos</Tab>
              <Tab value="2">Historial</Tab>
            </TabList>
            <TabPanels>
              <TabPanel value="0">Contenido de la pestaña General.</TabPanel>
              <TabPanel value="1">Contenido de la pestaña Documentos.</TabPanel>
              <TabPanel value="2">Contenido de la pestaña Historial.</TabPanel>
            </TabPanels>
          </Tabs>
        </template>
      </Card>

      <Card>
        <template #title>Stepper</template>
        <template #content>
          <Stepper v-model:value="activeStep">
            <StepList>
              <Step value="1">Datos</Step>
              <Step value="2">Carga</Step>
              <Step value="3">Confirmación</Step>
            </StepList>
            <StepPanels>
              <StepPanel v-slot="{ activateCallback }" value="1">
                <p>Paso 1: Ingresa los datos del cliente.</p>
                <Button label="Siguiente" icon="pi pi-arrow-right" iconPos="right" @click="activateCallback('2')" />
              </StepPanel>
              <StepPanel v-slot="{ activateCallback }" value="2">
                <p>Paso 2: Define subpartida y peso.</p>
                <div class="demo-row">
                  <Button label="Atrás" severity="secondary" @click="activateCallback('1')" />
                  <Button label="Siguiente" @click="activateCallback('3')" />
                </div>
              </StepPanel>
              <StepPanel v-slot="{ activateCallback }" value="3">
                <p>Paso 3: ¡Listo para enviar!</p>
                <Button label="Atrás" severity="secondary" @click="activateCallback('2')" />
              </StepPanel>
            </StepPanels>
          </Stepper>
        </template>
      </Card>

      <div class="showcase-grid">
        <Card>
          <template #title>Splitter (arrastrable)</template>
          <template #content>
            <Splitter style="height: 140px">
              <SplitterPanel :size="40" style="display: flex; align-items: center; justify-content: center;">Panel Izquierdo</SplitterPanel>
              <SplitterPanel :size="60" style="display: flex; align-items: center; justify-content: center;">Panel Derecho</SplitterPanel>
            </Splitter>
          </template>
        </Card>

        <Card>
          <template #title>ScrollPanel</template>
          <template #content>
            <ScrollPanel style="width: 100%; height: 140px">
              <p v-for="n in 8" :key="n" style="margin: 0 0 0.5rem 0">
                Línea de ejemplo {{ n }} dentro de ScrollPanel personalizado de PrimeVue.
              </p>
            </ScrollPanel>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>
