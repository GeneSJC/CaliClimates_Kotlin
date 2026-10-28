# README artwork

The images used by the front-page `README.md`.

| File | Used for | Replace with |
|---|---|---|
| `banner.png` | The hero image at the top | Generated from the app's own icon, terrain picture and region shapes; keep, or a new design at about 1600 x 520 |
| `divider.png` | The gradient rule between sections | Keep |
| `icon.png` | The footer icon | Keep (the app's launcher icon) |
| `placeholder-home.png`, `placeholder-elevation.png`, `placeholder-regions.png` | The three phone screenshots in the "Screens" gallery | Real screenshots, about 540 x 1100 (or any phone-shaped size). Save as `screen-home.png`, `screen-elevation.png`, `screen-regions.png` and change the three `src` paths in the README |
| `placeholder-demo.png` | The demo slot under the gallery | An animated `demo.gif` (keep it under about 8 MB; GitHub shows it inline). Change the one `src` path in the README |

`make_art.py` generates the banner, divider, footer icon and placeholders (needs Pillow). It reads the app's launcher icon
(`app/src/main/ic_launcher-playstore.png`), the statewide terrain picture (`app/src/main/assets/terrain/california.webp`) and
California's region shapes and colors from `StateUsaCA.kt`, so the banner follows the app. Run it from this folder after
changing the app's name or colors: `python3 make_art.py`. The name and tagline are drawn into the banner as pixels, so a
rename means running it again (and searching the README for the old name). The real screenshots and GIF are not generated;
just add them.

Tips: take screenshots in light mode with nothing personal on screen, and keep each PNG under about 400 KB so the page loads
fast (an optimizer such as `pngquant` or Squoosh helps).
