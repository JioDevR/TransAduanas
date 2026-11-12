# Landing Standalone Project Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the orphaned landing view in `frontend/` into a fully autonomous, production-ready Vite + Vue 3 project inside `/landing`, with self-contained design tokens, light/dark theme support, configurable access links to SGA and Portal Clientes, and clean up the legacy `frontend/` directory while remaining on the active branch `feature/V1.0.2`.

**Architecture:** A lightweight, independent Vue 3 Single Page Application configured with Vite. It extracts design system tokens from `app-sga` into a modular stylesheet, encapsulates the theme switcher into a zero-dependency composable, replaces internal ERP auth store dependencies with decoupled portal navigation links (configurable via `.env`), and cleans up the obsolete `frontend/` directory.

**Tech Stack:** Vue 3 (Composition API, `<script setup>`), Vite 6/8, Vue Router 4, PrimeIcons, Vanilla CSS Design Tokens.

**Spec:** Requirement from user to adapt `frontend/src/features/landing` into an independent standalone project in `/landing`, maintaining the current git branch `feature/V1.0.2`.

## Global Constraints

- Must stay on the active branch `feature/V1.0.2` without switching branches or altering other branches.
- Default development port for `landing` must be `5173` (matching the pre-configured `http://localhost:5173` in backend CORS origins).
- Zero broken imports: remove all tight couplings to internal ERP modules (`@/features/auth/stores/authStore`).
- Preserve the visual aesthetics (Warm Alabaster & Espresso Editorial design system with dark mode).
- Remove the obsolete `frontend/` folder once the standalone project is verified.

## Review Focus

- Portal navigation: CTA buttons correctly redirect to SGA (`:5180`) and Portal Cliente (`:5174`) via configurable environment variables.
- Theme switching: Light and dark mode toggle reliably and persist in `localStorage` without flickering or missing variables.
- Standalone build: `npm run build` in `landing/` succeeds with 0 errors and produces a static distribution in `dist/`.
- Styling completeness: All CSS variables (`--color-bg-base`, `--color-text-main`, etc.) are resolved within `landing/` without relying on sibling projects.
- Clean git status: No unmanaged legacy files left in `frontend/`.

---

### Task 1: Initialize Project Scaffolding and Configuration in `landing/`

**Files:**
- Create: `landing/package.json`
- Create: `landing/vite.config.js`
- Create: `landing/index.html`
- Create: `landing/.gitignore`
- Create: `landing/.env.example`
- Create: `landing/.env`

**Interfaces:**
- Consumes: Node.js / Vite build ecosystem.
- Produces: Base Vite + Vue 3 development environment on port `5173`.

- [ ] **Step 1: Create `landing/package.json`**
Define project metadata, scripts (`dev`, `build`, `preview`), and dependencies (`vue`, `vue-router`, `primeicons`, `@vitejs/plugin-vue`, `vite`).

- [ ] **Step 2: Create `landing/vite.config.js`**
Configure Vite with Vue plugin, `@` alias pointing to `/src`, and dev server running on port `5173`.

- [ ] **Step 3: Create `landing/index.html`**
HTML entry point with viewport configuration, title "TransAduanas - Soluciones Logísticas y Aduaneras", Google Fonts preconnect, and `<div id="app"></div>`.

- [ ] **Step 4: Create `landing/.gitignore`, `.env.example`, and `.env`**
Standard node ignores (`node_modules`, `dist`, `.env.local`) and environment variables:
`VITE_SGA_APP_URL=http://localhost:5180`
`VITE_PORTAL_CLIENTE_URL=http://localhost:5174`

- [ ] **Step 5: Verify configuration validity**
Run `cd landing && npm install --dry-run` or verify JSON syntax.

- [ ] **Step 6: Commit**
```bash
git add landing/package.json landing/vite.config.js landing/index.html landing/.gitignore landing/.env.example landing/.env
git commit -m "chore(landing): initialize standalone vite vue3 project configuration"
```

---

### Task 2: Implement Design System Tokens and Theme Composable

**Files:**
- Create: `landing/src/assets/styles/tokens.css`
- Create: `landing/src/assets/styles/main.css`
- Create: `landing/src/composables/useTheme.js`

**Interfaces:**
- Consumes: PrimeIcons CSS.
- Produces:
  - `useTheme()` composable returning `{ isDark, toggleTheme, setTheme }`.
  - Complete CSS custom properties for light and dark palettes (`--color-bg-base`, `--color-text-main`, `--color-primary`, etc.).

- [ ] **Step 1: Create `landing/src/assets/styles/tokens.css`**
Extract and consolidate the Warm Alabaster & Espresso Editorial palette, typography, spacing, and `.dark-theme` variable overrides.

- [ ] **Step 2: Create `landing/src/assets/styles/main.css`**
Import PrimeIcons and `tokens.css`, define reset rules, base typography, button utilities (`.btn`, `.btn-primary`, `.btn-light`, `.btn-ghost`, `.icon-btn`), and layout containers.

- [ ] **Step 3: Create `landing/src/composables/useTheme.js`**
Lightweight reactive composable that detects OS dark mode preference, checks `localStorage.getItem('transaduanas-theme')`, applies `.dark-theme` to `document.documentElement`, and exposes `toggleTheme`.

- [ ] **Step 4: Verify theme composable unit logic**
Ensure SSR-safe checks (`typeof document !== 'undefined'`) and persistence logic.

- [ ] **Step 5: Commit**
```bash
git add landing/src/assets/styles landing/src/composables
git commit -m "feat(landing): add design system tokens and theme composable"
```

---

### Task 3: Adapt and Modernize the Landing View Component

**Files:**
- Create: `landing/src/views/LandingView.vue`
- Create: `landing/src/router/index.js`
- Create: `landing/src/App.vue`
- Create: `landing/src/main.js`

**Interfaces:**
- Consumes: `useTheme` from `@/composables/useTheme.js`, PrimeIcons classes, and environment variables `import.meta.env`.
- Produces: Mountable Vue application routing to the landing page with direct access to external platforms (SGA Internal & B2B Portal).

- [ ] **Step 1: Create `landing/src/views/LandingView.vue`**
Adapt the original `LandingView.vue`:
  - Decouple from Pinia / `authStore`.
  - Add clear dual-access buttons in topbar and CTA sections: "Portal Clientes" (linking to `VITE_PORTAL_CLIENTE_URL`) and "Sistema SGA" (linking to `VITE_SGA_APP_URL`).
  - Preserve all content: Hero, Facts, Módulos, Flujo de trabajo, and Footer.
  - Smooth scroll helper `irASeccion(id)`.

- [ ] **Step 2: Create `landing/src/router/index.js`**
Vue Router instance with route `/` rendering `LandingView.vue`.

- [ ] **Step 3: Create `landing/src/App.vue` and `landing/src/main.js`**
Bootstrap Vue application, initialize theme with `initTheme()`, register router, and import `main.css`.

- [ ] **Step 4: Verify dependencies and imports**
Confirm all `@/` paths resolve correctly and no unresolved imports remain.

- [ ] **Step 5: Commit**
```bash
git add landing/src
git commit -m "feat(landing): implement standalone landing view and routing"
```

---

### Task 4: Cleanup Obsolete `frontend/` Directory and Verify Project Health

**Files:**
- Delete: `frontend/src/features/landing/views/LandingView.vue`
- Delete: `frontend/src/features/landing/routes/landing.routes.js`
- Delete: `frontend/` directory
- Update: `README.md` (document the new `landing/` project)

**Interfaces:**
- Consumes: Clean workspace.
- Produces: Zero orphan files, updated repository documentation.

- [ ] **Step 1: Remove obsolete `frontend/` folder**
Execute `git rm -r frontend` to remove the orphaned code.

- [ ] **Step 2: Update `README.md`**
Add `landing/` to the project directory structure and instructions on how to run `npm run dev` in `landing/`.

- [ ] **Step 3: Test build verification**
Run build check or verify that `landing` is structurally complete and syntactically valid.

- [ ] **Step 4: Commit**
```bash
git add -A
git commit -m "refactor: migrate frontend landing page to standalone landing project and cleanup"
```
