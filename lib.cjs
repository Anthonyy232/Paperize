// Shared by releases/banner.cjs and readme/build.cjs.
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const font = (file, style) => `@font-face { font-family: 'Instrument Serif'; font-style: ${style}; font-weight: 400;
  src: url(data:font/woff2;base64,${fs.readFileSync(path.join(__dirname, 'fonts', file)).toString('base64')}) format('woff2'); }`;
const fonts = `<style>${font('instrument-serif.woff2', 'normal')}\n${font('instrument-serif-italic.woff2', 'italic')}</style>`;

function paperizeCheckout(arg) {
  const dir = path.resolve(arg ?? '');
  if (!arg || !fs.existsSync(path.join(dir, 'app/src/main/ic_launcher.svg'))) throw new Error('Pass the path of a Paperize checkout of master');
  return dir;
}

// The master logo lives on master: the launcher icon, and its three sheets without the background.
const icon = paperize => fs.readFileSync(path.join(paperize, 'app/src/main/ic_launcher.svg'), 'utf8');
const sheets = (paperize, viewBox, attrs = '') => icon(paperize)
  .replace(/viewBox="[^"]*"/, `viewBox="${viewBox}" ${attrs}`).replace(/\s*<rect[^>]*\/>/, '');

// One raster thread: with more, rotated images come out a few pixels different from run to run.
const launch = () => chromium.launch({ args: ['--num-raster-threads=1'] });

async function render(browser, html, file, { width, height, scale = 1, transparent = false, type = 'png' }) {
  const page = await browser.newPage({ viewport: { width, height }, deviceScaleFactor: scale });
  await page.setContent(html, { waitUntil: 'load' });
  await page.evaluate(() => Promise.all([...document.fonts].map(face => face.load())));
  await page.evaluate(() => Promise.all([...document.images].map(img => img.decode())));
  await page.screenshot({ path: file, omitBackground: transparent, type });
  await page.close();
}

module.exports = { launch, fonts, paperizeCheckout, icon, sheets, render };
