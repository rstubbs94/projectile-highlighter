# Projectile Highlighter Plugin

## Purpose

A RuneLite plugin that allows players to visually highlight projectiles in-game. This is useful for:
- Learning boss mechanics by identifying dangerous projectiles
- PvP situations to track incoming attacks
- General gameplay awareness

## Features

### Core Features
- **Projectile Detection**: Automatically detects all projectiles in the game world
- **Visual Highlighting**: Renders overlays on projectiles using different styles (Hull, Outline, Filled, Tile)
- **Debug Mode**: Shows projectile IDs on-screen to help identify unknown projectiles
- **Group Management**: Organize projectiles into named groups that can be enabled/disabled together

### Sidebar Panel
- **Groups Section**: Create, rename, delete, and toggle projectile groups
- **Recent Projectiles**: Shows recently seen projectiles with ability to add them to groups
- **Per-Projectile Settings**: Each entry has its own color and overlay style

### Persistence
- Groups and settings saved to `~/.runelite/projectile-highlighter/groups.json`
- Survives client restarts

## Architecture

```
src/main/java/com/projectilehighlighter/
├── ProjectileHighlighterPlugin.java      - Main plugin, event handling
├── ProjectileHighlighterConfig.java      - Plugin settings
├── ProjectileHighlighterOverlay.java     - Renders projectile overlays
├── model/
│   ├── ProjectileGroup.java              - Group with name, enabled state, entries
│   ├── ProjectileEntry.java              - Single projectile config (id, color, style)
│   └── RecentProjectile.java             - Tracked recent projectile data
├── ui/
│   ├── ProjectileHighlighterPanel.java   - Main sidebar panel
│   ├── GroupPanel.java                   - Expandable group with entries
│   └── RecentProjectilePanel.java        - Recent projectile row
└── util/
    ├── GroupStorage.java                 - JSON persistence
    └── ProjectileNames.java              - ID to human-readable name mapping
```

## UI Design

The sidebar panel has limited horizontal space (~225px). All UI components must fit within this constraint.

### Groups Section
```
┌─────────────────────────────────┐
│ Groups                      [+] │  <- Add new group button
├─────────────────────────────────┤
│ ▶ ● Zulrah (3)                  │  <- Collapsed group
│   [On] [✎] [✕]                  │  <- Buttons on second row
├─────────────────────────────────┤
│ ▼ ● Olm (2)                     │  <- Expanded group
│   [On] [✎] [✕]                  │
│   ┌─────────────────────────┐   │
│   │ ■ 1347: Olm Fire        │   │  <- Entry row 1: color + name
│   │ [Hull ▼] [Name] [−]     │   │  <- Entry row 2: controls
│   └─────────────────────────┘   │
│   [+ Add Projectile]            │
└─────────────────────────────────┘
```

### Recent Projectiles Section
```
┌─────────────────────────────────┐
│ Recent Projectiles      [Clear] │
├─────────────────────────────────┤
│ ID: 1339                    [+] │
│ Zulrah Snakeling                │
│ Zulrah -> You | 5s ago          │
└─────────────────────────────────┘
```

---

## TODO

### Critical - UI Layout Issues
- [x] Fix GroupPanel buttons disappearing when group is expanded
- [x] Fix entry row controls being cut off (dropdown selector)
- [x] Ensure all UI fits within RuneLite's ~225px sidebar width
- [x] Reference tick-replay-logger patterns for proper width handling

### UI Improvements
- [x] Change color picker to match other RuneLite plugins (use RuneLiteColorPicker)
- [x] Improve UI experience with proper rows and sub-rows layout
- [x] Add color to UI elements:
  - Green "+" for add buttons
  - Red "-" for delete buttons
  - Colored status indicators
- [x] Move debug button from panel header to plugin settings toggle
- [x] Move group action buttons so they align flush to the right
- [x] Match visual styling from projectile-highlighter reference:
  - Swap eye icons to the minimalist white/gray set shown in the screenshot
  - Embed a green "+" button inside the entry list instead of the current "+ Add Projectile" row
  - Restyle all plus/minus buttons to use the thin red/green bars like the screenshot
- [x] Simplify recent projectiles list to unique projectile IDs only (no target or timestamp columns)
- [x] Restore the Groups header "+" button so new groups can be added from the sidebar
- [x] Replace the eye icons with the exact white/gray set from the provided reference
- [x] Reduce the width of the overlay-style dropdown so other entry controls retain space
- [x] Add alternating banding to projectile entry rows for easier scanning
- [x] Make the entire group header (not only the arrow) toggle expansion for entries
- [x] Streamline projectile entry creation (instant default add + inline projectile ID editing, no blocking popups)

### Current UI Refinements
- [x] Fix title header - text drawing over it (remove debug hint text)
- [x] Change group "Rename" button to an icon (pencil/edit icon)
- [x] Align projectile entry rename/delete buttons to the right of the row
- [x] Use external PNG icon for edit button (from Flaticon, recolored to match theme)
- [x] Remove redundant header "+" button (keep only the one in Groups section)
- [x] Change green "+" buttons to simple icon style (matching other icons)
- [x] Widen overlay style dropdown for better readability (72px -> 88px)

### Functionality
- [ ] Verify overlay rendering works correctly for all styles
- [ ] Test group enable/disable affects overlay correctly
- [ ] Ensure recent projectiles list updates properly
- [ ] Verify JSON persistence saves and loads correctly

### Code Quality
- [ ] Ensure thread safety (EDT for UI, game thread for events)
- [ ] Add proper error handling for edge cases
- [ ] Clean up any unused code or imports

### Testing
- [ ] Test with actual in-game projectiles
- [ ] Test creating/deleting groups
- [ ] Test adding/removing entries
- [ ] Test color picker functionality
- [ ] Test overlay style switching
- [ ] Test persistence across client restarts

### Future Enhancements (Nice to Have)
- [ ] Import/export groups as JSON
- [ ] Preset groups for common bosses (Zulrah, CoX, ToB, etc.)
- [ ] Sound alerts for specific projectiles
- [ ] Projectile trajectory prediction lines

---

## Icon Attribution

Icons used in this plugin:

- **Edit Icon**: [Edit by Pixel perfect](https://www.flaticon.com/free-icon/edit_1827933) from Flaticon
  - Location: `src/main/resources/com/projectilehighlighter/ui/edit_icon.png`
  - Recolored to light gray (200, 200, 200) at runtime to match theme

- **Save Icon**: [Diskette by Freepik](https://www.flaticon.com/free-icon/diskette_2874091) from Flaticon
  - Location: `src/main/resources/com/projectilehighlighter/ui/save_icon.png`
  - Recolored to soft green at runtime for the inline save button state

- **Visible/Invisible Icons**: Custom icons for group visibility toggle
  - Location: `src/main/resources/com/projectilehighlighter/ui/visible_icon.png`
  - Location: `src/main/resources/com/projectilehighlighter/ui/invisible_icon.png`
