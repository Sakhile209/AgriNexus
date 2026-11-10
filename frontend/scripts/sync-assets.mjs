import {copyFileSync,mkdirSync} from 'node:fs'
mkdirSync('dist/vendor',{recursive:true})
for(const file of ['email-verification.js','email-verification.css','reports.js','reports.css','geolocation.js','live-data.js','reference-dashboard.js','market-home.js','live-data.css']) copyFileSync(`src/${file}`,`dist/${file}`)
for(const file of ['maplibre-gl.mjs','maplibre-gl-shared.mjs','maplibre-gl-worker.mjs','maplibre-gl.css'])copyFileSync(`node_modules/maplibre-gl/dist/${file}`,`dist/vendor/${file}`)
copyFileSync('node_modules/maplibre-gl/LICENSE.txt','dist/vendor/maplibre-LICENSE.txt')
