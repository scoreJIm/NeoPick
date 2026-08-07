# NEOPick Frontend

Vue 3 + Vite. Talks to the backend via REST and WebSocket through the
Vite dev proxy.

## Quick Start

```bash
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:3000`. API calls proxy to `localhost:8080`.

## Project Layout

```
frontend/
├── index.html
├── package.json
├── vite.config.js       # Dev proxy to backend
└── src/
    ├── main.js          # App entry, router setup
    ├── App.vue          # Shell layout
    └── views/           # Route-level components
        ├── Home.vue
        ├── Login.vue
        └── TeacherDetail.vue
```

## Build

```bash
npm run build     # Output to dist/
npm run preview   # Preview production build
```
