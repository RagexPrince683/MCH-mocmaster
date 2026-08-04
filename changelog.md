# HUD Layout Editor: Complete RWR Display

- Registered the complete RWR display as the single `builtin:rwr` / `rwr.display` HUD layout element.
- Added the RWR to the layout editor preview and suppressed its event-driven render while the editor is open.
- Added complete circle and threat-label bounds capture, stable center-based scaling, and saved offset/scale persistence through the existing HUD layout JSON system.
- Preserved the RWR texture, blend functions, blend enable state, and render color for subsequent HUD rendering.
