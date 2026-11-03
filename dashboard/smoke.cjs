const { chromium } = require("playwright"),
  { spawn } = require("child_process"),
  path = require("path");
const server = spawn(process.execPath, [
  path.join(__dirname, "node_modules/vite/bin/vite.js"),
  "--host",
  "127.0.0.1",
  "--port",
  "5173",
], { cwd: __dirname, stdio: "ignore" });
(async () => {
  for (let i = 0; i < 40; i++) {
    try {
      await fetch("http://127.0.0.1:5173");
      break;
    } catch {
      await new Promise((r) => setTimeout(r, 100));
    }
  }
  const browser = await chromium.launch({
      headless: true,
      ...(process.env.WM_TEST_CHROMIUM
        ? { executablePath: process.env.WM_TEST_CHROMIUM }
        : {}),
      args: ["--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu"],
    }),
    page = await browser.newPage({ viewport: { width: 1440, height: 1000 } }),
    errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  // Browser tests intercept network boundaries; no test switch or sample account ships in the application.
  await page.route("**/config.json", (r) =>
    r.fulfill({
      json: {
        firebase: {
          apiKey: "fixture",
          authDomain: "fixture.firebaseapp.com",
          projectId: "fixture",
          appId: "fixture",
        },
        endpoint: "https://fixture.supabase.co/functions/v1/woodmatch-admin",
      },
    }));
  const now = Math.floor(Date.now() / 1000),
    encode = (o) => Buffer.from(JSON.stringify(o)).toString("base64url"),
    token = encode({ alg: "RS256" }) + "." +
      encode({
        sub: "fixture",
        iat: now,
        exp: now + 3600,
        auth_time: now,
        aud: "fixture",
        iss: "https://securetoken.google.com/fixture",
        firebase: { sign_in_provider: "password" },
      }) + ".fixture";
  await page.route("**/identitytoolkit.googleapis.com/**", async (route) => {
    const url = route.request().url();
    await route.fulfill({
      json: url.includes("accounts:lookup")
        ? {
          users: [{
            localId: "fixture",
            email: "fixture@example.invalid",
            emailVerified: true,
            providerUserInfo: [{ providerId: "password" }],
          }],
        }
        : {
          localId: "fixture",
          email: "fixture@example.invalid",
          idToken: token,
          refreshToken: "fixture",
          expiresIn: "3600",
          registered: true,
        },
    });
  });
  const c = {
    id: "case-fixture",
    subject: "Seguimiento de publicación",
    detail: "Consulta de la comunidad",
    type: "INCIDENCIA",
    status: "NUEVO",
    priority: "MEDIA",
    createdAt: Date.now() - 86400000,
    updatedAt: 1,
    history: [],
  };
  let mutation;
  await page.route("https://fixture.supabase.co/**", async (route) => {
    const input = route.request().postDataJSON();
    let data = {};
    switch (input.operation) {
      case "summary":
        data = {
          incidents: 1,
          resolved: 0,
          resolutionRate: 0,
          urgent: 0,
          open: 1,
          satisfaction: null,
          ratingCount: 0,
          reopened: 0,
          unclassified: 1,
          byService: [],
          cases: [c],
        };
        break;
      case "cases":
        data = { cases: [c] };
        break;
      case "caseDetails":
        data = c;
        break;
      case "users":
        data = {
          users: [{
            uid: "community-fixture",
            name: "Participante",
            version: "v1",
          }],
        };
        break;
      case "publications":
        data = { publications: [] };
        break;
      case "audit":
        data = { events: [] };
        break;
      default:
        mutation = input;
        data = { ok: true };
    }
    await route.fulfill({ json: { data } });
  });
  await page.goto("http://127.0.0.1:5173");
  await page.getByRole("button", { name: "Iniciar sesión", exact: true })
    .waitFor();
  if (await page.getByText("DATOS SIMULADOS").count()) {
    throw Error("demo leaked");
  }
  await page.getByLabel("Correo", { exact: true }).fill(
    "fixture@example.invalid",
  );
  await page.getByLabel("Contraseña", { exact: true }).fill(
    "FixturePassword123",
  );
  await page.getByRole("button", { name: "Iniciar sesión", exact: true })
    .click();
  await page.getByRole("heading", { name: "Casos recientes", exact: true })
    .waitFor();
  await page.screenshot({
    path: path.join(__dirname, "../docs/dashboard-desktop.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: "◇ Incidentes", exact: true }).click();
  await page.getByLabel("Buscar registros").fill("sin coincidencia");
  await page.getByText("No hay registros para esta consulta.").waitFor();
  await page.getByLabel("Buscar registros").fill("");
  await page.getByRole("button", { name: "Abrir", exact: true }).click();
  await page.getByRole("dialog").waitFor();
  await page.getByLabel("Motivo", { exact: true }).fill(
    "Verificación automatizada de seguimiento",
  );
  await page.getByRole("button", { name: "Guardar cambio", exact: true })
    .click();
  await page.getByRole("dialog").waitFor({ state: "hidden" });
  if (mutation.operation !== "updateSupportCase" || !mutation.data.reason) {
    throw Error("mutation reason");
  }
  await page.getByLabel("Tema", { exact: true }).selectOption("AUTUMN");
  await page.getByLabel("Apariencia", { exact: true }).selectOption("DARK");
  if (
    await page.evaluate(() =>
      getComputedStyle(document.documentElement).getPropertyValue("--primary")
        .trim()
    ) !== "#FFB68A"
  ) throw Error("theme mismatch");
  await page.getByLabel("Tema", { exact: true }).selectOption("SUMMER");
  await page.getByLabel("Apariencia", { exact: true }).selectOption("LIGHT");
  await page.getByRole("button", { name: "▣ Resumen", exact: true }).click();
  await page.getByRole("heading", { name: "Casos recientes", exact: true })
    .waitFor();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: path.join(__dirname, "../docs/dashboard-mobile.png"),
    fullPage: true,
  });
  if (
    await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)
  ) throw Error("overflow");
  await page.getByRole("button", { name: "Cerrar sesión", exact: true })
    .click();
  if ((await page.locator("#view").innerText()).trim()) {
    throw Error("private state retained");
  }
  if (errors.length) throw Error(errors.join("\n"));
  await browser.close();
  server.kill();
  console.log(
    "PASS: authenticated fixture, production login, case search/detail/update reason, exact seasonal palette, mobile, logout clearing and no JS exceptions",
  );
})().catch((e) => {
  server.kill();
  console.error(e);
  process.exit(1);
});
