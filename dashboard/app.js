import { initializeApp } from "firebase/app";
import {
  getAuth,
  inMemoryPersistence,
  onAuthStateChanged,
  setPersistence,
  signInWithEmailAndPassword,
  signOut,
} from "firebase/auth";
import {
  services,
  summarize,
} from "../supabase/functions/woodmatch-dashboard/metrics.mjs";
const $ = (id) => document.getElementById(id);
const el = (tag, text, cls) => {
  const n = document.createElement(tag);
  if (text !== undefined) n.textContent = text;
  if (cls) n.className = cls;
  return n;
};
const fmt = (n) =>
  n === null || n === undefined
    ? "Sin datos suficientes"
    : Number(n).toLocaleString("es-CO", { maximumFractionDigits: 1 });
let mode = "demo",
  page = "Resumen",
  data = null,
  auth = null,
  config = null,
  generation = 0;
const now = Date.now(), day = 86400000;
const fixtures = Array.from({ length: 30 }, (_, i) => ({
  id: `DEMO-${String(i + 1).padStart(3, "0")}`,
  type: i % 7 === 0
    ? "SATISFACCION"
    : i % 5 === 0
    ? "MODERACION"
    : "INCIDENCIA",
  rating: i % 5 + 1,
  serviceId: services[i % 15],
  status: ["NUEVO", "EN_REVISION", "EN_PROGRESO", "RESUELTO", "CERRADO"][i % 5],
  priority: ["BAJA", "MEDIA", "ALTA", "CRITICA"][i % 4],
  createdAt: now - (i + 1) * day,
  updatedAt: now - i * day,
  moderationAction: i % 10 === 0 ? "OCULTAR" : "",
  history: [{ at: now - (i + 1) * day, action: "CREADO" }],
}));
$("from").value = new Date(now - 30 * day).toISOString().slice(0, 10);
$("to").value = new Date(now).toISOString().slice(0, 10);
services.forEach((s) => $("service").append(new Option(s, s)));
function clear() {
  generation++;
  data = null;
  $("view").replaceChildren();
  $("detail").close();
  $("detailText").textContent = "";
}
function message(text) {
  $("message").textContent = text;
}
async function refresh() {
  const run = ++generation;
  data = null;
  $("view").replaceChildren(el("p", "Cargando…"));
  const from = Date.parse($("from").value + "T00:00:00Z"),
    to = Date.parse($("to").value + "T00:00:00Z") + day,
    service = $("service").value;
  try {
    let result;
    if (mode === "demo") {
      result = summarize(fixtures, from, to, service);
      result.source = "demo";
    } else {
      if (!auth?.currentUser) {
        throw Error("Inicia sesión con una cuenta autorizada.");
      }
      const token = await auth.currentUser.getIdToken();
      const url = new URL(config.endpoint);
      url.search = new URLSearchParams({
        from,
        to,
        ...(service ? { service } : {}),
      });
      const response = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
        signal: AbortSignal.timeout(30000),
      });
      if (!response.ok) {
        throw Error(
          response.status === 401
            ? "Sesión caducada. Cierra sesión y vuelve a entrar."
            : response.status === 403
            ? "Acceso denegado. Tu cuenta no tiene permiso de lectura."
            : response.status === 429
            ? "Límite de consultas. Espera un minuto."
            : "No fue posible consultar el servicio.",
        );
      }
      result = await response.json();
    }
    if (run !== generation) return;
    data = result;
    render();
  } catch (e) {
    if (run !== generation) return;
    clear();
    message(
      e.message === "range"
        ? "Selecciona un periodo válido de hasta 366 días."
        : e.message,
    );
  }
}
function card(title, value, caption) {
  const c = el("article", undefined, "card stat");
  c.append(el("small", title), el("strong", fmt(value)), el("small", caption));
  return c;
}
function bars(title, items) {
  const box = el("section", undefined, "card");
  box.append(el("h2", title));
  const max = Math.max(1, ...items.map((x) => x.count));
  for (const x of items) {
    const row = el("div", undefined, "bar"), p = el("progress");
    p.max = max;
    p.value = x.count;
    p.setAttribute("aria-label", `${x.id}: ${x.count}`);
    row.append(
      el("span", x.id.replaceAll("_", " ")),
      p,
      el("strong", String(x.count)),
    );
    box.append(row);
  }
  return box;
}
function table(rows) {
  const box = el("section", undefined, "card");
  box.append(el("h2", "Casos · solo lectura"));
  const search = el("input", undefined, "search");
  search.placeholder = "Buscar ID, tipo o estado";
  search.setAttribute("aria-label", "Buscar casos");
  box.append(search);
  const wrap = el("div", undefined, "table-wrap"),
    t = el("table"),
    head = el("thead"),
    tr = el("tr");
  ["Caso", "Servicio", "Prioridad", "Estado", "Registro UTC", "Detalle"]
    .forEach((x) => tr.append(el("th", x)));
  head.append(tr);
  const body = el("tbody");
  t.append(head, body);
  wrap.append(t);
  box.append(wrap);
  const fill = () => {
    body.replaceChildren();
    const q = search.value.toLocaleLowerCase();
    const matches = rows.filter((r) =>
      `${r.id} ${r.type} ${r.status}`.toLocaleLowerCase().includes(q)
    );
    for (const r of matches) {
      const tr = el("tr");
      [
        r.id,
        r.serviceId || "Sin clasificar",
        r.priority,
        r.status,
        new Date(r.createdAt).toISOString().slice(0, 10),
      ].forEach((x) => tr.append(el("td", x)));
      const td = el("td"), b = el("button", "Ver");
      b.setAttribute("aria-label", `Ver caso ${r.id}`);
      b.onclick = () => {
        $("detailText").textContent =
          `${r.id} · ${r.type}\n${r.status} · ${r.priority}\nActualización: ${
            r.updatedAt ? new Date(r.updatedAt).toISOString() : "Sin datos"
          }\n` + r.history.map((h) =>
            `${new Date(h.at).toISOString()} · ${h.action}`
          ).join("\n");
        $("detail").showModal();
      };
      td.append(b);
      tr.append(td);
      body.append(tr);
    }
    if (!matches.length) {
      const r = el("tr"), c = el("td", "No hay casos para este filtro.");
      c.colSpan = 6;
      r.append(c);
      body.append(r);
    }
  };
  search.oninput = fill;
  fill();
  return box;
}
function render() {
  if (!data) return;
  const v = $("view");
  v.replaceChildren();
  const title = el("h2", page);
  v.append(title);
  if (page === "Documentación") {
    const c = el("section", undefined, "card");
    for (
      const [label, path] of [["Centro de documentación", "Home.md"], [
        "KPI y manual del dashboard",
        "Dashboard-Web.md",
      ], [
        "Calidad, versiones y evidencias",
        "Control-de-calidad-y-evidencias.md",
      ], ["Catálogo de 15 servicios", "Catalogo-de-servicios-TI.md"]]
    ) {
      const p = el("p"), a = el("a", label);
      a.href =
        `https://github.com/HOLA-GG/WoodMatch/blob/21932966e28aef8622ad47735a99efa72e19cf4f/docs/wiki/${path}`;
      p.append(a);
      c.append(p);
    }
    c.append(
      el(
        "p",
        "Documentación enlazada a la revisión documental verificable de este panel.",
      ),
    );
    v.append(c);
    return;
  }
  if (page === "Operación") {
    v.append(
      el(
        "div",
        "Sin datos suficientes para reservas vencidas, entrega push o disponibilidad. La existencia de un aviso no prueba su recepción en el teléfono.",
        "card notice",
      ),
    );
    return;
  }
  if (page === "Servicios TI") {
    v.append(
      bars("Casos por servicio", data.byService),
      el(
        "p",
        `Sin clasificar: ${data.unclassified}. Cero casos no implica disponibilidad del servicio.`,
        "notice",
      ),
    );
    return;
  }
  if (page === "Moderación") {
    v.append(
      card("Reportes recibidos", data.moderation.received, "ST-13"),
      card(
        "Con acción registrada",
        data.moderation.handled,
        "Ocultar / restaurar",
      ),
      card(
        "Pendientes",
        data.moderation.received - data.moderation.handled,
        "Sin acción registrada",
      ),
      table(data.cases.filter((r) => r.type === "MODERACION")),
    );
    return;
  }
  if (page === "Incidentes") {
    v.append(table(data.cases.filter((r) => r.type === "INCIDENCIA")));
    return;
  }
  const stats = el("div", undefined, "stats");
  stats.append(
    card(
      "Incidencias registradas",
      data.incidents,
      "ST-12 · cohorte por creación",
    ),
    card(
      "Resueltas / cerradas",
      data.resolved,
      `${fmt(data.resolutionRate)}${
        data.resolutionRate === null ? "" : " %"
      } · estado actual`,
    ),
    card(
      "Alta / crítica pendientes",
      data.urgent,
      `${data.open} incidencias abiertas`,
    ),
    card(
      "Satisfacción media",
      data.satisfaction,
      `${data.ratingCount} respuestas · escala 1–5`,
    ),
  );
  v.append(stats);
  const grid = el("div", undefined, "grid");
  grid.append(
    bars(
      "Incidencias por estado",
      ["NUEVO", "EN_REVISION", "EN_PROGRESO", "RESUELTO", "CERRADO"].map(
        (id) => ({
          id,
          count:
            data.cases.filter((r) => r.type === "INCIDENCIA" && r.status === id)
              .length,
        }),
      ),
    ),
    bars(
      "Casos por tramo del periodo",
      Array.from(
        { length: 5 },
        (_, i) => ({
          id: `Tramo ${i + 1}/5`,
          count:
            data.cases.filter((r) =>
              r.createdAt >= data.from + i * (data.to - data.from) / 5 &&
              r.createdAt < data.from + (i + 1) * (data.to - data.from) / 5
            ).length,
        }),
      ),
    ),
  );
  v.append(grid);
  v.append(
    el(
      "p",
      `Reaperturas observadas: ${data.reopened} (${fmt(data.reopenRate)}${
        data.reopenRate === null ? "" : " %"
      } de incidencias). Historial retenido: mínimo observado. Tiempos: Sin datos suficientes.`,
      "notice",
    ),
    table(data.cases.slice(0, 10)),
  );
  for (const text of data.limitations) v.append(el("p", text, "muted"));
}
async function setupAuth() {
  if (auth) return;
  const res = await fetch("/config.json", { cache: "no-store" });
  if (
    !res.ok || !res.headers.get("content-type")?.includes("application/json")
  ) {
    throw Error(
      "Conexión real pendiente de configurar. La demostración sigue disponible.",
    );
  }
  config = await res.json();
  const endpoint = new URL(config.endpoint);
  if (
    endpoint.protocol !== "https:" ||
    !endpoint.hostname.endsWith(".supabase.co") ||
    endpoint.pathname !== "/functions/v1/woodmatch-dashboard"
  ) throw Error("Endpoint no válido.");
  auth = getAuth(initializeApp(config.firebase));
  await setPersistence(auth, inMemoryPersistence);
  onAuthStateChanged(auth, (user) => {
    if (mode !== "real") return;
    clear();
    $("login").hidden = !!user;
    $("logout").hidden = !user;
    if (user) {
      message("Sesión iniciada. El servidor verificará el permiso de lectura.");
      refresh();
    } else message("Inicia sesión para consultar datos reales.");
  });
}
$("demo").onclick = async () => {
  clear();
  mode = "demo";
  $("mode").textContent = "DATOS SIMULADOS";
  $("login").hidden = true;
  $("logout").hidden = true;
  if (auth) await signOut(auth);
  message("Demostración con datos ficticios. No representa producción.");
  refresh();
};
$("real").onclick = async () => {
  clear();
  mode = "real";
  $("mode").textContent = "DATOS REALES · ACCESO PRIVADO";
  message("Preparando acceso…");
  try {
    await setupAuth();
    $("login").hidden = !!auth.currentUser;
    $("logout").hidden = !auth.currentUser;
    if (auth.currentUser) refresh();
    else message("Inicia sesión para consultar datos reales.");
  } catch (e) {
    message(e.message);
  }
};
$("login").onsubmit = async (e) => {
  e.preventDefault();
  try {
    await signInWithEmailAndPassword(
      auth,
      $("email").value,
      $("password").value,
    );
    $("password").value = "";
  } catch {
    clear();
    $("password").value = "";
    message("No se pudo iniciar sesión. Verifica tu cuenta y configuración.");
  }
};
$("logout").onclick = async () => {
  clear();
  await signOut(auth);
  message("Sesión cerrada.");
};
$("filters").onsubmit = (e) => {
  e.preventDefault();
  refresh();
};
$("close").onclick = () => $("detail").close();
for (const b of document.querySelectorAll("nav button")) {
  b.onclick = () => {
    page = b.dataset.page;
    document.querySelectorAll("nav button").forEach((x) =>
      x.removeAttribute("aria-current")
    );
    b.setAttribute("aria-current", "page");
    render();
  };
}
refresh();
