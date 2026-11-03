const { chromium } = require("playwright");
const path = require("path");
const { spawn } = require("child_process");
const server = spawn(process.execPath, [
  path.join(__dirname, "node_modules/vite/bin/vite.js"),
  "--host",
  "127.0.0.1",
  "--port",
  "5173",
], { cwd: __dirname, stdio: "ignore" });

(async () => {
  for (let i = 0; i < 30; i++) {
    try {
      await fetch("http://127.0.0.1:5173");
      break;
    } catch {
      await new Promise((r) => setTimeout(r, 100));
    }
  }
  const b = await chromium.launch({
    headless: true,
    ...(process.env.WM_TEST_CHROMIUM
      ? { executablePath: process.env.WM_TEST_CHROMIUM }
      : {}),
    args: ["--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu"],
  });
  const p = await b.newPage({ viewport: { width: 1440, height: 1000 } });
  const errors = [];
  p.on("pageerror", (e) => errors.push(e.message));
  await p.goto("http://127.0.0.1:5173");
  await p.getByRole("heading", { name: "Incidencias por estado" }).waitFor();
  await p.screenshot({
    path: path.join(__dirname, "../docs/dashboard-desktop.png"),
    fullPage: true,
  });
  await p.getByRole("button", { name: "◇ Incidentes", exact: true }).click();
  await p.getByLabel("Buscar casos").fill("NO_EXISTE");
  if (!await p.getByText("No hay casos para este filtro.").isVisible()) {
    throw Error("search");
  }
  await p.getByLabel("Buscar casos").fill("");
  await p.getByRole("button", { name: /Ver caso/ }).first().click();
  await p.getByRole("dialog").waitFor();
  await p.getByRole("button", { name: "Cerrar", exact: true }).click();
  await p.getByRole("button", { name: "Conectar datos reales" }).click();
  await p.getByText("Conexión real pendiente de configurar.", { exact: false })
    .waitFor();
  if (await p.locator("#view").innerText()) throw Error("real retained demo");
  await p.getByRole("button", { name: "Ver demostración" }).click();
  await p.getByRole("button", { name: "▣ Resumen", exact: true }).click();
  await p.getByRole("heading", { name: "Incidencias por estado" }).waitFor();
  await p.setViewportSize({ width: 390, height: 844 });
  await p.screenshot({
    path: path.join(__dirname, "../docs/dashboard-mobile.png"),
    fullPage: true,
  });
  if (
    await p.evaluate(() =>
      document.documentElement.scrollWidth > window.innerWidth
    )
  ) throw Error("mobile overflow");
  if (errors.length) throw Error(errors.join("\n"));
  console.log(
    "PASS: desktop, mobile no overflow, navigation, search, dialog, real/demo isolation, no JS exceptions",
  );
  await b.close();
  server.kill();
})().catch((error) => {
  server.kill();
  console.error(error);
  process.exit(1);
});
