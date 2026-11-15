// Release banners, 1280x640, into releases/<tag>.png. Needs Playwright (npm i playwright, or NODE_PATH to one).
//   node banner.cjs v4.3.0 "short highlight"   adds the tag to highlights.json and renders it
//   node banner.cjs                            renders every tag in highlights.json
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const file = path.join(__dirname, 'highlights.json');
const highlights = JSON.parse(fs.readFileSync(file, 'utf8'));
const [tag, highlight] = process.argv.slice(2);
if (tag) {
  highlights[tag] = highlight;
  fs.writeFileSync(file, JSON.stringify(highlights, null, 2) + '\n');
}

// The three sheets of app/src/main/ic_launcher.svg on master.
const sheets = (box, extra = '') => `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${box}" ${extra}>
  <path d="M68.25 36.59L77.37 42.74A5.5 5.5 0 0 1 78.85 50.38L65.01 70.9L64.68 70.08L68.2 36.56A5.5 5.5 0 0 1 68.25 36.59Z" fill="#e03a72"/>
  <path d="M49.61 32.64L60.55 33.79A5.5 5.5 0 0 1 65.44 39.83L62.76 65.34L49.55 32.63A5.5 5.5 0 0 1 49.61 32.64Z" fill="#c2255c"/>
  <path d="M31.29 37.9L41.49 33.78A5.5 5.5 0 0 1 48.65 36.82L61.01 67.42A5.5 5.5 0 0 1 57.97 74.58L47.77 78.7A5.5 5.5 0 0 1 40.61 75.66L28.25 45.06A5.5 5.5 0 0 1 31.29 37.9Z" fill="#000"/>
</svg>`;
const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;');

const html = ([number, pre], text) => `<!doctype html><meta charset="utf-8">
<link href="https://fonts.googleapis.com/css2?family=Instrument+Serif:ital@0;1&display=swap" rel="stylesheet">
<style>
  html, body { margin: 0; }
  body { position: relative; width: 1280px; height: 640px; overflow: hidden; background: #ffd9e2; color: #1c0f13; font-family: system-ui, sans-serif; }
  .brand { position: absolute; left: 80px; top: 64px; display: flex; align-items: center; gap: 12px; font-size: 26px; font-weight: 700; letter-spacing: -0.02em; }
  .brand svg { width: 40px; height: 40px; }
  .copy { position: absolute; left: 80px; bottom: 68px; width: 700px; font-family: 'Instrument Serif', Georgia, serif; letter-spacing: -0.01em; }
  .version { font-size: 190px; line-height: 0.9; }
  .pre { margin-left: 14px; font-size: 64px; }
  .highlight { margin-top: 18px; font-size: 60px; line-height: 1.04; font-style: italic; color: #c2255c; text-wrap: balance; }
  .mark { position: absolute; right: -40px; top: 30px; width: 640px; height: 640px; }
</style>
<div class="brand">${sheets('24 28 60 56')}Paperize</div>
<div class="copy"><div class="version">${number}${pre ? `<span class="pre">${pre}</span>` : ''}</div><div class="highlight">${esc(text)}</div></div>
${sheets('24 28 60 56', 'class="mark"')}`;

(async () => {
  fs.mkdirSync(path.join(__dirname, 'releases'), { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1280, height: 640 } });
  for (const t of tag ? [tag] : Object.keys(highlights)) {
    await page.setContent(html(t.slice(1).split('-'), highlights[t]), { waitUntil: 'networkidle' });
    await page.evaluate(() => document.fonts.ready);
    if (!await page.evaluate(() => document.fonts.check("64px 'Instrument Serif'"))) throw new Error('Instrument Serif did not load');
    await page.screenshot({ path: path.join(__dirname, 'releases', `${t}.png`) });
  }
  await browser.close();
})();
