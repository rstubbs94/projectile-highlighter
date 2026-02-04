# Projectile Highlighter Plugin

## Purpose

A RuneLite plugin that allows players to visually highlight projectiles in-game. This is useful for:
- Learning boss mechanics by identifying dangerous projectiles
- PvP situations to track incoming attacks
- General gameplay awareness

## Features

### Core Features
- **Projectile Detection**: Automatically detects all projectiles in the game world
- **Visual Highlighting**: Renders overlays on projectiles using different styles (Outline, Hull/Filled Outline, Filled, Tile)
- **Debug Mode**: Shows projectile IDs in chat to help identify unknown projectiles
- **Group Management**: Organize projectiles into named groups that can be enabled/disabled together
- **Import/Export**: Share groups via clipboard JSON with format validation

### Sidebar Panel
- **Groups Section**: Create, rename, delete, toggle, import/export projectile groups
- **Recent Projectiles**: Table showing recently seen projectiles with quick-add to groups
- **Per-Projectile Settings**: Each entry has its own color and overlay style (icon buttons)

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
│   └── RecentProjectilePanel.java        - Recent projectile table row
└── util/
    ├── GroupStorage.java                 - JSON persistence with import/export
    └── ProjectileNames.java              - ID to human-readable name mapping
```

## UI Design

The sidebar panel has limited horizontal space (~225px). All UI components must fit within this constraint.

### Groups Section
```
┌─────────────────────────────────┐
│ Groups              [⬇][⬆][+]  │  <- Import, Export, Add buttons
├─────────────────────────────────┤
│ ▶ ● Zulrah (3)  [👁][✎][⬆][−] │  <- Collapsed group with action buttons
├─────────────────────────────────┤
│ ▼ ● Olm (2)     [👁][✎][⬆][−] │  <- Expanded group
│   ┌─────────────────────────┐   │
│   │ ■ [1347] [Olm Fire    ] │   │  <- Color swatch, ID field, name field
│   │ [◇][▣][■][▦] [✎][−]    │   │  <- Style icons, edit/remove buttons
│   └─────────────────────────┘   │
│   [+]                           │  <- Add new entry
└─────────────────────────────────┘
```

### Recent Projectiles Section (Table Layout)
```
┌─────────────────────────────────┐
│ Recent Projectiles      [Clear] │
├─────────────────────────────────┤
│ [+] │ ID   │ Source             │  <- Header row
├─────────────────────────────────┤
│ [+] │ 1339 │ Zulrah • Snakeling │  <- Data rows (max 10)
│ [+] │ 1340 │ Zulrah • Magic     │     Source truncates with tooltip
│ [+] │ 27   │ Unknown Source     │
└─────────────────────────────────┘
```

## Icons

### External Icons (Flaticon - with attribution in code)
- `import_icon.png` / `export_icon.png` - by Dewi Sari
- `edit_icon.png` - by Pixel perfect
- `save_icon.png` - by Freepik
- `visible_icon.png` / `invisible_icon.png` - eye icons for group toggle
- `outline_icon.png`, `shaded_icon.png`, `solid_icon.png`, `tile_icon.png` - overlay style icons

### Programmatic Icons
- Green plus icon - created via Graphics2D (shared between GroupPanel and RecentProjectilePanel)
- Red minus icon - created via Graphics2D

---

## Completed Features

### UI
- [x] Expandable/collapsible group panels with entry count
- [x] Click anywhere on group header to expand/collapse
- [x] Alternating row colors for visual clarity
- [x] Inline editing for projectile entries (no blocking popups)
- [x] Color picker using RuneLite's RuneLiteColorPicker
- [x] Icon buttons for overlay style selection (Outline, Hull, Filled, Tile)
- [x] Style icons colored to match projectile color when selected
- [x] Import/Export buttons with clipboard JSON and format validation
- [x] Per-group export button
- [x] Recent projectiles table with fixed column widths
- [x] Source column truncates with full text in tooltip
- [x] Consistent icon styling throughout (edit, save, plus, minus, eye, export)

### Functionality
- [x] Group enable/disable affects overlay rendering
- [x] Per-projectile color and overlay style
- [x] JSON persistence with auto-save
- [x] Recent projectiles list (max 10, newest first)
- [x] Debug mode toggle in plugin settings
- [x] Projectile name lookup from RuneLite's ProjectileID constants

---

## Testing Checklist

- [ ] Test with actual in-game projectiles
- [ ] Test creating/deleting groups
- [ ] Test adding/removing entries
- [ ] Test color picker functionality
- [ ] Test overlay style switching (all 4 styles render correctly)
- [ ] Test import/export between clients
- [ ] Test persistence across client restarts

## Future Enhancements (Nice to Have)

- [ ] Preset groups for common bosses (Zulrah, CoX, ToB, etc.)
- [ ] Sound alerts for specific projectiles
- [ ] Projectile trajectory prediction lines

---
