import { initializeApp } from "firebase/app";
import {
  getAuth,
  inMemoryPersistence,
  onAuthStateChanged,
  setPersistence,
  signInWithEmailAndPassword,
  signOut,
} from "firebase/auth";
import { services } from "../supabase/functions/woodmatch-dashboard/metrics.mjs";
import { applyTheme } from "./themes.js";
const $ = (id) => document.getElementById(id),
  el = (tag, text, cls) => {
    const n = document.createElement(tag);
    if (text !== undefined) n.textContent = text;
    if (cls) n.className = cls;
    return n;
  };
const number = (v) =>
  v == null
    ? "Sin datos suficientes"
    : Number(v).toLocaleString("es-CO", { maximumFractionDigits: 1 });
const date = (v) =>
  v
    ? new Date(v).toLocaleString("es-CO", { timeZone: "America/Bogota" })
    : "Sin registro";
const message = (text) => $("message").textContent = text;
let auth,
  config,
  page = "Resumen",
  generation = 0,
  authorized = false,
  sessionVersion = 0;
const day = 86400000;
$("from").value = new Date(Date.now() - 90 * day).toISOString().slice(0, 10);
$("to").value = new Date().toISOString().slice(0, 10);
services.forEach((s) => $("service").append(new Option(s, s)));
function clear() {
  generation++;
  sessionVersion++;
  authorized = false;
  $("view").replaceChildren();
  $("filters").hidden = true;
  $("detail").close();
  $("detailContent").replaceChildren();
}
function setBusy(form, busy) {
  form.querySelectorAll("button,input,select,textarea").forEach((n) =>
    n.disabled = busy
  );
}
async function api(operation, data = {}) {
  if (!auth?.currentUser) throw Error("Inicia sesión para continuar.");
  const requestSession = sessionVersion, requestUid = auth.currentUser.uid;
  const token = await auth.currentUser.getIdToken();
  const response = await fetch(config.endpoint, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ operation, data }),
    cache: "no-store",
    signal: AbortSignal.timeout(operation === "deleteUser" ? 120000 : 30000),
  });
  const body = await response.json();
  if (
    requestSession !== sessionVersion || auth.currentUser?.uid !== requestUid
  ) throw Error("La sesión cambió.");
  if (!response.ok) {
    if ([401, 403].includes(response.status)) {
      clear();
      message(body.error || "Vuelve a iniciar sesión.");
    }
    throw Error(body.error || "No se pudo completar la operación.");
  }
  return body.data;
}
function card(title, value, caption) {
  const box = el("article", undefined, "card stat");
  box.append(
    el("small", title),
    el("strong", number(value)),
    el("small", caption),
  );
  return box;
}
function field(form, label, type = "text", value = "") {
  const l = el("label", label),
    n = el(
      type === "textarea" ? "textarea" : type === "select" ? "select" : "input",
    );
  if (n.tagName === "INPUT") n.type = type;
  if (type === "textarea") n.rows = 3;
  n.value = value;
  l.append(n);
  form.append(l);
  return n;
}
function choice(form, label, values, value) {
  const s = field(form, label, "select");
  values.forEach((v) => s.append(new Option(v.replaceAll("_", " "), v)));
  s.value = value;
  return s;
}
function open(title) {
  $("detailTitle").textContent = title;
  $("detailContent").replaceChildren();
  $("detail").showModal();
  return $("detailContent");
}
function actionForm(container, operation, data, fields, destructive = false) {
  const f = el("form", undefined, "operation-form");
  const inputs = fields(f), reason = field(f, "Motivo", "textarea");
  reason.required = true;
  reason.minLength = 8;
  reason.maxLength = 1000;
  let confirmation;
  if (destructive) {
    f.append(
      el(
        "p",
        "La eliminación retira la cuenta, sus publicaciones y fotografías. La trazabilidad de entregas se anonimiza. Esta acción no se puede deshacer.",
        "notice",
      ),
    );
    confirmation = field(
      f,
      "Escribe el identificador de la cuenta para confirmar",
    );
    confirmation.required = true;
  }
  const result = el("p", undefined, "form-result");
  result.setAttribute("role", "status");
  const submit = el(
    "button",
    destructive ? "Eliminar cuenta" : "Guardar cambio",
  );
  f.append(submit, result);
  let operationId = crypto.randomUUID();
  f.onsubmit = async (e) => {
    e.preventDefault();
    if (!f.reportValidity()) return;
    setBusy(f, true);
    try {
      const values = Object.fromEntries(
        Object.entries(inputs).map(([k, n]) => [k, n.value]),
      );
      await api(operation, {
        ...data,
        ...values,
        reason: reason.value,
        operationId,
        ...(confirmation ? { confirmation: confirmation.value } : {}),
      });
      message("Cambio guardado.");
      $("detail").close();
      await render();
    } catch (e) {
      result.textContent = e.message;
    } finally {
      setBusy(f, false);
    }
  };
  container.append(f);
  return f;
}
function table(title, headers, rows, values, openRow) {
  const box = el("section", undefined, "card");
  box.append(el("h2", title));
  const q = el("input", undefined, "search");
  q.setAttribute("aria-label", "Buscar registros");
  q.placeholder = "Buscar";
  box.append(q);
  const wrap = el("div", undefined, "table-wrap"),
    t = el("table"),
    head = el("thead"),
    tr = el("tr");
  headers.forEach((h) => tr.append(el("th", h)));
  if (openRow) tr.append(el("th", "Detalle"));
  head.append(tr);
  const body = el("tbody");
  t.append(head, body);
  wrap.append(t);
  box.append(wrap);
  function fill() {
    body.replaceChildren();
    const matched = rows.filter((r) =>
      values(r).join(" ").toLocaleLowerCase().includes(
        q.value.toLocaleLowerCase(),
      )
    );
    for (const r of matched) {
      const tr = el("tr");
      values(r).forEach((v) => tr.append(el("td", String(v ?? ""))));
      if (openRow) {
        const td = el("td"), b = el("button", "Abrir");
        b.onclick = () =>
          Promise.resolve(openRow(r)).catch((e) => message(e.message));
        td.append(b);
        tr.append(td);
      }
      body.append(tr);
    }
    if (!matched.length) {
      const tr = el("tr"),
        td = el("td", "No hay registros para esta consulta.");
      td.colSpan = headers.length + (openRow ? 1 : 0);
      tr.append(td);
      body.append(tr);
    }
  }
  q.oninput = fill;
  fill();
  return box;
}
async function caseDetail(r) {
  const row = await api("caseDetails", { id: r.id }),
    box = open(row.subject || "Seguimiento del caso");
  box.append(
    el("p", row.detail),
    el("p", `${row.type} · ${row.status} · ${row.priority}`),
    el("p", "Creación: " + date(row.createdAt)),
  );
  if (row.response) box.append(el("p", "Respuesta: " + row.response));
  actionForm(box, "updateSupportCase", {
    id: row.id,
    expectedUpdatedAt: row.updatedAt,
  }, (f) => ({
    status: choice(f, "Estado", [
      "NUEVO",
      "EN_REVISION",
      "EN_PROGRESO",
      "RESUELTO",
      "CERRADO",
    ], row.status),
    priority: choice(
      f,
      "Prioridad",
      ["BAJA", "MEDIA", "ALTA", "CRITICA"],
      row.priority,
    ),
    response: field(
      f,
      "Respuesta para el usuario",
      "textarea",
      row.response || "",
    ),
  }));
  if (["RESUELTO", "CERRADO"].includes(row.status)) {
    box.append(el("h3", "Reabrir caso"));
    actionForm(box, "reopenCase", {
      id: row.id,
      expectedUpdatedAt: row.updatedAt,
    }, () => ({}));
  }
  box.append(el("h3", "Historial"));
  for (const h of row.history || []) {
    box.append(
      el(
        "p",
        `${date(h.at)} · ${h.action.replaceAll("_", " ")}${
          h.reason ? " · " + h.reason : ""
        }`,
      ),
    );
  }
  if (row.publicationId) {
    box.append(el("h3", "Publicación reportada"));
    const publications = await api("publications");
    const p = publications.publications.find((p) => p.id === row.publicationId);
    if (p) {
      box.append(el("p", p.title), el("p", p.description));
      moderationForm(box, p, row.id);
    }
  }
}
function moderationForm(box, r, caseId) {
  actionForm(
    box,
    "moderatePublication",
    { id: r.id, expectedRevision: r.revision, ...(caseId ? { caseId } : {}) },
    (f) => ({
      action: choice(
        f,
        "Acción",
        ["OCULTAR", "RESTAURAR"],
        r.status === "MODERADO" ? "RESTAURAR" : "OCULTAR",
      ),
    }),
  );
}
function userDetail(r) {
  const box = open(r.name || "Cuenta");
  box.append(el("p", "Identificador: " + r.uid));
  box.append(el("h3", "Corregir nombre"));
  actionForm(
    box,
    "renameUser",
    { id: r.uid, version: r.version },
    (f) => ({ name: field(f, "Nombre permitido", "text", r.name) }),
  );
  box.append(el("h3", "Enviar aviso"));
  actionForm(
    box,
    "notifyUser",
    { id: r.uid },
    (f) => ({
      title: field(f, "Título"),
      body: field(f, "Mensaje", "textarea"),
    }),
  );
  box.append(el("h3", "Eliminar cuenta"));
  actionForm(box, "deleteUser", { id: r.uid }, () => ({}), true);
}
async function render() {
  const run = ++generation;
  const view = $("view");
  view.replaceChildren(el("p", "Cargando…"));
  try {
    let body;
    if (page === "Resumen" || page === "Servicios TI") {
      const from = Date.parse($("from").value + "T00:00:00Z"),
        to = Date.parse($("to").value + "T00:00:00Z") + day;
      body = await api("summary", { from, to, service: $("service").value });
    } else {body = await api(
        page === "Moderación"
          ? "publications"
          : page === "Usuarios" || page === "Alertas"
          ? "users"
          : page === "Auditoría"
          ? "audit"
          : "cases",
      );}
    if (run !== generation) return;
    authorized = true;
    $("filters").hidden = !(page === "Resumen" || page === "Servicios TI");
    view.replaceChildren(el("h2", page));
    if (page === "Resumen") {
      const stats = el("div", undefined, "stats");
      stats.append(
        card("Incidencias recibidas", body.incidents, "Periodo seleccionado"),
        card(
          "Resueltas / cerradas",
          body.resolved,
          `${number(body.resolutionRate)}${
            body.resolutionRate == null ? "" : " %"
          } de resolución`,
        ),
        card(
          "Prioridad alta / crítica",
          body.urgent,
          `${body.open} incidencias abiertas`,
        ),
        card(
          "Satisfacción media",
          body.satisfaction,
          `${body.ratingCount} respuestas · escala 1–5`,
        ),
      );
      view.append(
        stats,
        table(
          "Casos recientes",
          ["Caso", "Tipo", "Estado", "Prioridad", "Registro"],
          body.cases.slice().sort((a, b) => b.createdAt - a.createdAt).slice(
            0,
            10,
          ),
          (r) => [
            r.id.slice(0, 8),
            r.type,
            r.status,
            r.priority,
            date(r.createdAt),
          ],
          caseDetail,
        ),
      );
      view.append(
        el(
          "p",
          "Reaperturas observadas: " + body.reopened +
            ". Tiempos de atención: Sin datos suficientes.",
          "muted",
        ),
      );
    } else if (page === "Servicios TI") {
      view.append(
        table(
          "Casos por servicio",
          ["Servicio", "Casos"],
          body.byService,
          (r) => [r.id, r.count],
        ),
        el(
          "p",
          `Sin clasificar: ${body.unclassified}. El número de casos no mide la disponibilidad de un servicio.`,
          "muted",
        ),
      );
    } else if (page === "Incidentes") {
      view.append(
        table(
          "Bandeja de atención",
          ["Asunto", "Tipo", "Estado", "Prioridad", "Actualización"],
          body.cases,
          (r) => [
            r.subject || r.id.slice(0, 8),
            r.type,
            r.status,
            r.priority,
            date(r.updatedAt),
          ],
          caseDetail,
        ),
      );
    } else if (page === "Moderación") {
      view.append(
        table(
          "Publicaciones",
          ["Título", "Estado", "Revisión"],
          body.publications,
          (r) => [r.title, r.status, r.revision],
          (r) => {
            const box = open(r.title);
            box.append(el("p", r.description));
            moderationForm(box, r);
          },
        ),
      );
    } else if (page === "Usuarios") {
      view.append(
        table(
          "Cuentas de la comunidad",
          ["Nombre", "Identificador", "Estado"],
          body.users,
          (r) => [
            r.name,
            r.uid,
            r.deleting ? "Eliminación en curso" : "Activa",
          ],
          userDetail,
        ),
      );
    } else if (page === "Alertas") {
      view.append(
        table(
          "Enviar aviso",
          ["Nombre", "Identificador"],
          body.users,
          (r) => [r.name, r.uid],
          (r) => {
            const box = open("Aviso para " + r.name);
            actionForm(
              box,
              "notifyUser",
              { id: r.uid },
              (f) => ({
                title: field(f, "Título"),
                body: field(f, "Mensaje", "textarea"),
              }),
            );
          },
        ),
      );
    } else if (page === "Auditoría") {
      view.append(
        table(
          "Registro administrativo",
          ["Acción", "Destino", "Motivo", "Fecha", "Estado"],
          body.events,
          (r) => [r.operation, r.target, r.reason, date(r.at), r.state],
        ),
      );
    }
  } catch (e) {
    if (run !== generation) return;
    view.replaceChildren();
    message(e.message);
  }
}
async function start() {
  try {
    const response = await fetch("/config.json", { cache: "no-store" });
    if (!response.ok) throw Error("No se pudo cargar la configuración.");
    config = await response.json();
    const endpoint = new URL(config.endpoint);
    if (
      endpoint.protocol !== "https:" ||
      !endpoint.hostname.endsWith(".supabase.co") ||
      endpoint.pathname !== "/functions/v1/woodmatch-admin"
    ) throw Error("Configuración no válida.");
    auth = getAuth(initializeApp(config.firebase));
    await setPersistence(auth, inMemoryPersistence);
    onAuthStateChanged(auth, (user) => {
      clear();
      $("login").hidden = !!user;
      $("logout").hidden = !user;
      message(user ? "Verificando acceso…" : "Inicia sesión para continuar.");
      if (user) {
        render().then(() => {
          if (authorized) message("Sesión administrativa activa.");
        });
      }
    });
  } catch (e) {
    message(e.message);
  }
}
$("login").onsubmit = async (e) => {
  e.preventDefault();
  if (!auth) return;
  setBusy($("login"), true);
  try {
    await signInWithEmailAndPassword(
      auth,
      $("email").value.trim(),
      $("password").value,
    );
    $("password").value = "";
  } catch {
    clear();
    $("password").value = "";
    message(
      "No fue posible iniciar sesión. Verifica tus datos e inténtalo de nuevo.",
    );
  } finally {
    setBusy($("login"), false);
  }
};
$("logout").onclick = async () => {
  clear();
  await signOut(auth);
};
$("close").onclick = () => $("detail").close();
$("filters").onsubmit = (e) => {
  e.preventDefault();
  render();
};
for (const b of document.querySelectorAll("nav button")) {
  b.onclick = () => {
    if (!auth?.currentUser) return;
    page = b.dataset.page;
    document.querySelectorAll("nav button").forEach((n) =>
      n.removeAttribute("aria-current")
    );
    b.setAttribute("aria-current", "page");
    $("detail").close();
    render();
  };
}
for (const id of ["season", "appearance"]) {
  $(id).value = localStorage.getItem("woodmatch-" + id) || $(id).value;
  $(id).onchange = () => {
    localStorage.setItem("woodmatch-" + id, $(id).value);
    applyTheme($("season").value, $("appearance").value);
  };
}
matchMedia("(prefers-color-scheme: dark)").addEventListener(
  "change",
  () => applyTheme($("season").value, $("appearance").value),
);
applyTheme($("season").value, $("appearance").value);
start();
