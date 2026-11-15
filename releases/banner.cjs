// node releases/banner.cjs <paperize checkout> [<tag> "<highlight>"] : 1280x640 release banners in releases/<tag>.png
//   with a tag: records the highlight in releases/highlights.json and renders that banner; without: renders every banner
const fs = require('fs');
const path = require('path');
const { chromium, fonts, paperizeCheckout, sheets, render } = require('../lib.cjs');

const file = path.join(__dirname, 'highlights.json');
const highlights = JSON.parse(fs.readFileSync(file, 'utf8'));
const [checkout, tag, highlight] = process.argv.slice(2);
const paperize = paperizeCheckout(checkout);
if (tag) {
  highlights[tag] = highlight;
  fs.writeFileSync(file, JSON.stringify(highlights, null, 2) + '\n');
}
const esc = s => s.replace(/&/g, '&amp;').replace(/</g, '&lt;');

const html = ([number, pre], text) => `<!doctype html><meta charset="utf-8">${fonts}
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
<div class="brand">${sheets(paperize, '24 28 60 56')}Paperize</div>
<div class="copy"><div class="version">${number}${pre ? `<span class="pre">${pre}</span>` : ''}</div><div class="highlight">${esc(text)}</div></div>
${sheets(paperize, '24 28 60 56', 'class="mark"')}`;

(async () => {
  const browser = await chromium.launch();
  for (const t of tag ? [tag] : Object.keys(highlights)) {
    await render(browser, html(t.slice(1).split('-'), highlights[t]), path.join(__dirname, `${t}.png`), { width: 1280, height: 640 });
  }
  await browser.close();
})();
