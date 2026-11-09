#!/usr/bin/env node
// Renders each numbered board of the active design version to a PNG, for side-by-side comparison
// with screenshots of the app (adb exec-out screencap -p). Uses the Playwright of the server's web
// app, so nothing is installed here.
//
// Usage: scripts/render-design.mjs [path of the prumo repository, default ~/git/prumo]
// Output: design/renders/v1/<area> - <board>.png (git-ignored; 2x, the whole board with its caption)
//
// The boards load PrumoPhone.jsx and the design system with fetch, which browsers refuse on file://,
// so the script serves design/v1/ on a local port while it renders.
import { createRequire } from 'node:module';
import fs from 'node:fs';
import http from 'node:http';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const server = process.argv[2] ?? path.join(os.homedir(), 'git', 'prumo');
const source = path.join(here, 'design', 'v1');
const out = path.join(here, 'design', 'renders', 'v1');
const require = createRequire(path.join(server, 'frontend', 'package.json'));
const { chromium } = require('@playwright/test');

const types = { '.html': 'text/html', '.js': 'text/javascript', '.jsx': 'text/javascript', '.css': 'text/css', '.ttf': 'font/ttf', '.json': 'application/json' };
const files = http.createServer((request, response) => {
  const file = path.join(source, decodeURIComponent(new URL(request.url, 'http://x').pathname));
  if (!file.startsWith(source) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) {
    response.writeHead(404).end();
    return;
  }
  response.writeHead(200, { 'content-type': types[path.extname(file)] ?? 'application/octet-stream' });
  fs.createReadStream(file).pipe(response);
});
await new Promise((resolve) => files.listen(0, '127.0.0.1', resolve));
const base = `http://127.0.0.1:${files.address().port}/`;

fs.mkdirSync(out, { recursive: true });
const boards = fs.readdirSync(source).filter((f) => f.startsWith('Prumo Fase 2 - ') && f.endsWith('.dc.html') && !f.includes('Handoff'));
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1400, height: 1000 }, deviceScaleFactor: 2 });
for (const file of boards) {
  await page.goto(base + encodeURIComponent(file));
  await page.waitForFunction(() => !!window.HenriqueSebastiOAndroidDesignSystem_5ac522, null, { timeout: 30000 });
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(2500);
  const labels = await page.$$eval('[data-screen-label]', (els) => els.map((e) => e.getAttribute('data-screen-label')).filter((l) => /^\d\d /.test(l)));
  const area = file.replace('Prumo Fase 2 - ', '').replace('.dc.html', '');
  for (const label of labels) {
    // Only the board being shot is visible: a sheet of another board is fixed and would cover it.
    const rect = await page.evaluate((label) => {
      const board = document.querySelector(`[data-screen-label="${label}"]`);
      for (const b of document.querySelectorAll('[data-screen-label]')) {
        if (/^\d\d /.test(b.getAttribute('data-screen-label'))) b.style.visibility = b === board ? 'visible' : 'hidden';
      }
      board.scrollIntoView({ block: 'start', inline: 'start' });
      window.scrollBy(-8, -8);
      const r = board.getBoundingClientRect();
      return { x: r.x, y: r.y, width: r.width, height: r.height };
    }, label);
    const name = `${area} - ${label}.png`.replace(/[\\/]/g, '-');
    await page.screenshot({ path: path.join(out, name), clip: rect, animations: 'disabled' });
    console.log(path.relative(here, path.join(out, name)));
  }
}
await browser.close();
files.close();
