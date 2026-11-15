// Demo wallpapers for the screenshots: two albums of abstract 1080x2400 JPEGs, into readme/wallpapers/<Album>/.
//   node readme/wallpapers.cjs
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const W = 1080, H = 2400;

// Closed path under a ridge of summed sines, filled to the bottom edge.
const ridge = (base, waves) => {
  let d = `M0 ${H} `;
  for (let x = 0; x <= W; x += 20) {
    const y = base + waves.reduce((s, [amp, len, phase]) => s + amp * Math.sin((x / len + phase) * 2 * Math.PI), 0);
    d += `L${x} ${y.toFixed(1)} `;
  }
  return d + `L${W} ${H}Z`;
};
const ridges = layers => layers.map(([fill, base, waves]) => `<path fill="${fill}" d="${ridge(base, waves)}"/>`).join('');
const sky = (top, bottom) => `<defs><linearGradient id="sky" x2="0" y2="1"><stop offset="0" stop-color="${top}"/><stop offset="1" stop-color="${bottom}"/></linearGradient></defs><rect width="${W}" height="${H}" fill="url(#sky)"/>`;
const rings = (cx, cy, colors, step) => colors.map((c, i) => `<circle cx="${cx}" cy="${cy}" r="${step * (colors.length - i)}" fill="${c}"/>`).join('');
const blobs = (bg, list) => `<defs><filter id="blur" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="150"/></filter></defs>
  <rect width="${W}" height="${H}" fill="${bg}"/><g filter="url(#blur)">${list.map(([cx, cy, r, c]) => `<circle cx="${cx}" cy="${cy}" r="${r}" fill="${c}"/>`).join('')}</g>`;
// A rounded sheet, a band over it and a dark ground.
const sheets = (bg, light, mid, dark) => `<rect width="${W}" height="${H}" fill="${bg}"/>
  <path fill="${light}" stroke="${light}" stroke-width="200" stroke-linejoin="round" d="M-200 820L820 360L1240 1240L-200 1240Z"/>
  <path fill="${mid}" d="M0 940L1080 650L1080 2400L0 2400Z"/>
  <path fill="${dark}" d="M0 1210L1080 1100L1080 2400L0 2400Z"/>`;

const albums = {
  Paper: [
    sheets('#fed9e1', '#df3a72', '#c2255c', '#3a1024'),
    sheets('#e8f0e5', '#9ec5a8', '#4f8a6e', '#1f3d33'),
    `<rect width="${W}" height="${H}" fill="#f6f0e4"/>${rings(0, H, ['#edd9c0', '#e2b88e', '#d88e61', '#b85c43', '#3c2a26'], 230)}`,
    sheets('#e7ebf7', '#9db4e8', '#5772c9', '#1c2550'),
    `<rect width="${W}" height="${H}" fill="#fae9d7"/>${rings(W, 300, ['#f6c9a0', '#f29e7a', '#e0655e', '#8f2d56'], 230)}`,
    blobs('#f8f1e9', [[900, 1000, 500, '#d7ccf6'], [250, 1200, 380, '#cce2e0'], [100, 2150, 460, '#ffc9d6'], [1080, 1850, 380, '#fae2be']]),
  ],
  Dusk: [
    sky('#ffd7a9', '#fcc398') + `<circle cx="760" cy="760" r="172" fill="#fef3d5"/>` + ridges([
      ['#ef8a6c', 1060, [[40, 900, 0.1], [20, 400, 0.6]]],
      ['#c65569', 1300, [[60, 700, 0.3], [25, 300, 0.2]]],
      ['#873b6a', 1560, [[70, 800, 0.7], [30, 350, 0.4]]],
      ['#4a295e', 1820, [[80, 900, 0.2], [30, 420, 0.9]]],
      ['#261a3e', 2080, [[110, 1200, 0.05], [30, 380, 0.5]]],
    ]),
    sky('#f6c9b4', '#cdc1c3') + `<circle cx="296" cy="620" r="120" fill="#ffffff"/>` + ridges([
      ['#5f80a6', 1060, [[90, 1300, 0.15], [20, 360, 0.3]]],
      ['#3f5f88', 1400, [[110, 900, 0.55], [30, 380, 0.1]]],
      ['#2b4570', 1720, [[120, 1000, 0.85], [30, 400, 0.6]]],
      ['#19294a', 2000, [[130, 1100, 0.3], [35, 330, 0.2]]],
    ]),
    blobs('#6c304a', [[540, 620, 620, '#ffb86a'], [540, 2000, 600, '#ff7a59']]),
    `<rect width="${W}" height="${H}" fill="#1d1836"/>${rings(540, H, ['#2c2453', '#492f6e', '#8a3a79', '#d9557a', '#ffb19a'], 230)}`,
    sky('#fae5c8', '#f6cba0') + `<circle cx="540" cy="900" r="220" fill="#f7713e"/>` + ridges([
      ['#eaa15d', 1080, [[120, 1080, -0.25], [20, 360, 0.25]]],
      ['#c96f4a', 1420, [[60, 1000, 0.35], [25, 380, 0.7]]],
      ['#8e4a3f', 1700, [[70, 900, 0.9], [25, 330, 0.3]]],
      ['#532f33', 2020, [[130, 1200, 0.55], [30, 420, 0.1]]],
    ]),
    blobs('#0e2034', [[640, 240, 420, '#8dc9c1'], [300, 620, 380, '#556599'], [400, 1150, 500, '#e7a0c0'], [700, 2000, 520, '#1b556b']]),
  ],
};

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: W, height: H } });
  for (const [album, designs] of Object.entries(albums)) {
    const dir = path.join(__dirname, 'wallpapers', album);
    fs.mkdirSync(dir, { recursive: true });
    for (const [i, svg] of designs.entries()) {
      await page.setContent(`<body style="margin:0"><svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" style="display:block">${svg}</svg>`);
      await page.screenshot({ path: path.join(dir, `${album.toLowerCase()}-${i + 1}.jpg`), type: 'jpeg', quality: 92 });
    }
  }
  await browser.close();
})();
