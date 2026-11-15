// node readme/build.cjs <paperize checkout> : README hero and feature panels, site screenshots and store listing art, from readme/inputs/
const fs = require('fs');
const path = require('path');
const { launch, fonts, paperizeCheckout, icon, render } = require('../lib.cjs');

const paperize = paperizeCheckout(process.argv[2]);
const inputs = path.join(__dirname, 'inputs');
const out = dir => path.join(paperize, dir);
const shot = name => 'data:image/png;base64,' + fs.readFileSync(path.join(inputs, `${name}.png`)).toString('base64');
const logo = (tile = '#ffd9e2') => 'data:image/svg+xml;base64,' + Buffer.from(icon(paperize)
  .replace('viewBox="0 0 108 108"', 'viewBox="16 16 76 76"').replace('fill="#ffd9e2"', `fill="${tile}"`)).toString('base64');

const base = dark => `<!doctype html><meta charset="utf-8">${fonts}
<style>
  :root { --bg: ${dark ? '#171012' : '#fff8f9'}; --ink: ${dark ? '#f1dee2' : '#1c0f13'}; --muted: ${dark ? '#b9a0a6' : '#6a5158'}; --accent: ${dark ? '#ff8fb0' : '#c2255c'};
    --wash: ${dark
      ? 'radial-gradient(58% 70% at 12% 8%, #92264f, transparent 75%), radial-gradient(42% 42% at 82% 105%, #a01e4e, transparent 75%), #4a1029'
      : 'radial-gradient(58% 70% at 12% 8%, #ffe9ee, transparent 75%), radial-gradient(42% 42% at 82% 105%, #f2a0ba, transparent 75%), #ffd9e2'}; }
  * { box-sizing: border-box; }
  html, body { margin: 0; background: transparent; }
  body { width: 1280px; overflow: hidden; color: var(--ink); font-family: system-ui, sans-serif; }
  .card { position: relative; width: 1280px; overflow: hidden; border-radius: 28px; background: var(--bg); }
  .serif { font-family: 'Instrument Serif', Georgia, serif; font-weight: 400; letter-spacing: -0.01em; }
  .serif i { color: var(--accent); }
  .stage { position: relative; overflow: hidden; background: var(--wash); }
  .phone { position: absolute; border: 7px solid #0d0709; border-radius: 32px; background: #0d0709; box-shadow: 0 28.8px 64px -19.2px #0009; }
  .phone img { display: block; width: 100%; border-radius: 25px; }
</style>`;
const phone = (name, style) => `<div class="phone" style="${style}"><img src="${shot(name)}"></div>`;

// Library and effects tilted behind the wallpaper tab.
const trio = (side, front, x = 0, y = 0, w = 320) => `
    ${phone(`${side}-library`, `left: ${152 + x}px; top: ${79.2 + y}px; width: ${w}px; transform: rotate(-5deg)`)}
    ${phone(`${side}-effects`, `left: ${888 - x - w}px; top: ${79.2 + y}px; width: ${w}px; transform: rotate(5deg)`)}
    ${phone(`${front}-home`, 'left: 360px; top: 44px; width: 320px')}`;

const hero = dark => {
  const [front, side] = dark ? ['dark', 'light'] : ['light', 'dark'];
  return `${base(dark)}<style>
  .card { height: 760px; text-align: center; }
  .brand { display: inline-flex; align-items: center; gap: 12px; margin-top: 48px; font-size: 22px; font-weight: 700; letter-spacing: -0.02em; }
  .brand img { width: 36px; height: 36px; border-radius: 8px; }
  h1 { margin: 28px 0 0; font-size: 112px; line-height: 0.98; }
  p { margin: 22px auto 0; max-width: 400px; font-size: 20px; line-height: 1.45; color: var(--muted); }
  .stage { position: absolute; left: 120px; right: 120px; top: 448px; height: 312px; border-radius: 24px 24px 0 0; }
</style>
<div class="card">
  <div class="brand"><img src="${logo()}">Paperize</div>
  <h1 class="serif">a fresh wallpaper,<br><i>right on time</i></h1>
  <p>Pick your albums and an interval. Paperize changes your home and lock screen for you.</p>
  <div class="stage">${trio(side, front)}</div>
</div>`;
};

// Store feature graphic: the hero's headline and phones on the flat brand colour.
const featureGraphic = `${base(false)}<style>
  body { position: relative; width: 1024px; height: 500px; background: #ffd9e2; }
  .brand { position: absolute; left: 64px; top: 128px; display: flex; align-items: center; gap: 12px; font-size: 24px; font-weight: 700; letter-spacing: -0.02em; }
  .brand img { width: 42px; height: 42px; border-radius: 9.33px; }
  h1 { position: absolute; left: 64px; top: 196px; margin: 0; font-size: 82px; line-height: 0.98; }
  .phones { position: absolute; left: 435.25px; top: 28.6px; transform: scale(0.66875); transform-origin: 0 0; }
</style>
<div class="brand"><img src="${logo('#fff8f9')}">Paperize</div>
<h1 class="serif">a fresh wallpaper,<br><i>right on time</i></h1>
<div class="phones">${trio('light', 'light', 46, 16, 298)}</div>`;

const feature = (dark, n, title, text, phones) => `${base(dark)}<style>
  .card { height: 560px; display: grid; grid-template-columns: 1fr 640px; }
  .copy { padding: 0 56px 0 72px; display: flex; flex-direction: column; justify-content: center; }
  .num { font: 500 15px ui-monospace, 'Cascadia Code', monospace; color: var(--muted); }
  h2 { margin: 14px 0 0; font-size: 64px; line-height: 1.02; }
  p { margin: 20px 0 0; font-size: 19px; line-height: 1.55; color: var(--muted); }
  .stage { margin: 28px 28px 28px 0; border-radius: 20px; }
</style>
<div class="card">
  <div class="copy"><div class="num">${n}</div><h2 class="serif">${title}</h2><p>${text}</p></div>
  <div class="stage">${phones}</div>
</div>`;

const features = scheme => {
  const dark = scheme === 'dark';
  return {
    'feature-albums': feature(dark, '01', 'Albums, <i>on rotation</i>', 'Build albums from single images or whole folders. Folders pick up new files on their own.',
      phone(`${scheme}-library`, 'left: 40px; top: 48px; width: 270px') + phone(`${scheme}-album`, 'left: 330px; top: 96.4px; width: 270px')),
    'feature-schedule': feature(dark, '02', 'Home and lock,<br><i>your call</i>', 'Give each screen its own album and interval, or keep them together. Change on a schedule, in order or shuffled.',
      phone(`${scheme}-home`, 'left: 40px; top: 48px; width: 270px') + phone(`${scheme}-schedule`, 'left: 330px; top: -150px; width: 270px')),
    'feature-effects': feature(dark, '03', 'Effects, <i>to taste</i>', 'Blur, dim, vignette or grayscale each screen separately, and choose how images scale to fit.',
      phone(`${scheme}-effects`, 'left: 185px; top: 48px; width: 270px')),
  };
};

// Store screenshots: the capture in a phone frame on a flat brand background.
const store = (name, dark) => `<!doctype html><meta charset="utf-8"><style>
  body { margin: 0; width: 1252px; height: 2486px; background: ${dark ? '#4a1029' : '#ffd9e2'}; }
  div { position: absolute; left: 122px; top: 111px; width: 982px; border: 13px solid #0d0709; border-radius: 77px; background: #0d0709; }
  img { display: block; width: 100%; border-radius: 64px; }
</style><div><img src="${shot(name)}"></div>`;

// Site screenshots: the captures at 720x1600, as WebP.
async function webp(browser, name, file) {
  const page = await browser.newPage();
  const data = await page.evaluate(async src => {
    const img = new Image(); img.src = src; await img.decode();
    const canvas = Object.assign(document.createElement('canvas'), { width: 720, height: 1600 });
    const g = canvas.getContext('2d'); g.imageSmoothingQuality = 'high'; g.drawImage(img, 0, 0, 720, 1600);
    return canvas.toDataURL('image/webp', 0.85);
  }, shot(name));
  fs.writeFileSync(file, Buffer.from(data.split(',')[1], 'base64'));
  await page.close();
}

(async () => {
  const browser = await launch();
  for (const scheme of ['light', 'dark']) {
    await render(browser, hero(scheme === 'dark'), out(`.github/readme/hero-${scheme}.png`), { width: 1280, height: 760, scale: 1.6, transparent: true });
    for (const [name, html] of Object.entries(features(scheme))) {
      await render(browser, html, out(`.github/readme/${name}-${scheme}.png`), { width: 1280, height: 560, scale: 1.6, transparent: true });
    }
    for (const name of ['home', 'library', 'album', 'schedule', 'effects']) await webp(browser, `${scheme}-${name}`, out(`site/img/${scheme}-${name}.webp`));
  }
  const listing = out('metadata/en-US/images');
  for (const [i, name] of ['light-home', 'light-albums', 'light-scaling', 'light-album', 'dark-home'].entries()) {
    await render(browser, store(name, name.startsWith('dark')), path.join(listing, `phoneScreenshots/${i + 1}.png`), { width: 1252, height: 2486 });
  }
  await render(browser, featureGraphic, path.join(listing, 'featureGraphic.png'), { width: 1024, height: 500 });
  await render(browser, `<body style="margin:0">${icon(paperize).replace('viewBox="0 0 108 108"', 'viewBox="18 18 72 72" width="512" height="512" style="display:block"')}`,
    path.join(listing, 'icon.png'), { width: 512, height: 512 });
  await browser.close();
})();
