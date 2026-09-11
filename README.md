# Item Organizer 🎨

<p align="center">
  <img src="https://github.com/PrimityArtur/MC-Item-Organizer/blob/1.21.11/src/main/resources/assets/item-organizer/icon.png?raw=true" alt="Item Organizer Logo" width="128" height="128">
</p>

<p align="center">
  <strong>The visual inventory, palette manager, and builder workspace for Minecraft Fabric.</strong><br>
  Replace the clutter of the creative inventory with customizable layouts, chromatic color gradients, and hotbar building palettes.
</p>

<p align="center">
  <a href="https://github.com/PrimityArtur/MC-Item-Organizer/releases"><img src="https://img.shields.io/github/v/release/PrimityArtur/MC-Item-Organizer?color=brightgreen&label=release" alt="Release"></a>
  <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Loader-Fabric-blue" alt="Fabric"></a>
  <a href="https://minecraft.net/"><img src="https://img.shields.io/badge/Minecraft-1.21.11-orange" alt="Minecraft 1.21.11"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-green" alt="License"></a>
</p>

<p align="center">
  <iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/v0TQolDc2f4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
</p>

---

## 📖 What the Mod Does

**Item Organizer** is a client-side mod engineered to overhaul the traditional Minecraft creative inventory into a high-productivity builder interface. It provides granular item control, layout customization, color-gradient sorting, and advanced palette management to drastically accelerate your building workflow. In Survival mode, it can also be used as a convenient item catalog and visual lookup interface.

### 🌟 Core Features & Panels

* 📋 **Ordered Panel (Organized Grid)**:
  * **Organized**: Position blocks and items precisely where you want them in a customizable grid with flexible columns.
  * **Blocked Items**: Filter out and hide blocks you don't need so they never clutter your building workspace.
  * **Gradient Sort (Degradado)**: Automatically arrange all items across your grid by chromatic color gradients (hue, saturation, brightness) analyzed from their textures.
  * **Compact**: Instantly remove gaps and collapse empty slots forward.

* 📦 **Unorganized Panel**:
  * Displays every available Minecraft item that has not yet been placed in your Ordered layout or hidden in your Blocked list.

* 📁 **Profiles Panel**:
  * Create, load, rename, duplicate, and delete multiple independent profiles for different build themes, projects, or material types.

* 🎨 **Palettes Panel (9-Slot Hotbar Palettes)**:
  * Save frequently used combinations of 9 blocks into dedicated palette cards.
  * **Search & Filter Bar**:
    * **Text Search**: Filter palettes by item name, palette title, or palette number index.
    * **Column Color Sort Toggle**: Visually sorts all palette rows by their column color gradients without modifying your saved JSON files.
    * **`+ New` Button**: Instantly creates a new palette at the top (index 0) of the list.
  * **Palette Actions**:
    * ⬆️ / ⬇️ **Move**: Reorder palettes up and down in your collection.
    * 📑 **Duplicate**: Clone any palette card with one click.
    * 📥 **Paste Hotbar to Palette**: Copy your current player hotbar into the palette's 9 slots.
    * 📤 **Load Palette to Hotbar**: Instantly equip all 9 blocks into your hotbar.
    * 🌍 **Place Palette in World**: Places all palette blocks in the world horizontally right where you are looking, forcing in-place placement without requiring extra support blocks underneath (torches, crops, doors, plants, and gravity blocks stay cleanly in place like in Axiom, without FAWE or polluting undo history).
    * 🗑️ **Delete**: Remove palettes (with full undo support).
  * **Slot-Based Search Filter**:
    * Place blocks into the search slots next to your hotbar to find matching palettes, ranked automatically by:
      1. **Exact slot match**: Matching items in the exact same slot index.
      2. **Item presence**: Palettes containing the item anywhere in the row.
      3. **Color similarity**: Palettes containing blocks with matching color shades.

* ♾️ **Infinite Palette Panel**:
  * Extended multi-slot palettes that wrap across rows for massive color swatches or project collections.
  * Paste hotbar items sequentially after the last placed block in the palette.
  * Compatible with the slot-based search filter.

* ⏳ **By Version Panel**:
  * Explore and retrieve blocks chronologically grouped by their official Minecraft release versions.

* 🛡️ **Blocker Mode**:
  * Dedicated toggle button to lock the grid, preventing accidental item blocking, unblocking, or layout reorganization while you focus on building (state persists across game restarts).

* ⚙️ **Config Panel**:
  * Complete interactive keybindings guide and UI scaling options: background blur, grid zoom, item slot size, palette scale, and text scaling.

* 🔄 **Full Undo & Redo System**:
  * Robust `Ctrl + Z` (Undo) and `Ctrl + Y` (Redo) support across all panels: grid dragging, item blocking, color sorting, compacting, palette creation, deletion, and slot filter edits.

---

## 💡 Why You Should Use This Mod

* 🎯 **Build Faster Without Inventory Fatigue**: Stop repeatedly searching through creative tabs or looking through chests for blocks. Keep your go-to palettes one click away.
* 🌈 **Flawless Color Transitions & Gradients**: Dynamically extracts texture colors to suggest matching palettes and smooth gradients, helping you achieve seamless block gradients with zero guesswork.
* 🏗️ **Forced In-Place World Placement**: Test your palette directly in-game by placing it in the world with pure in-place forcing. No additional support blocks are generated underneath, keeping your build area completely clean.
* 🔀 **Flexible Drag, Drop & Pick-and-Place**: Move items with fluid drag-and-drop or click once to pick up an item on your cursor and click again to place it anywhere.
* 🔒 **Layout Safety**: Protect your curated layouts with Blocker Mode and universal multi-level Undo/Redo (`Ctrl+Z` / `Ctrl+Y`).
* ⚡ **Zero Server Setup Needed**: Works completely on the client side; connect to vanilla, Fabric, Paper, or Purpur servers without requiring any server plugins.

---

## ⚠️ Critical Information Before Downloading

* 🖥️ **Client-Side Only**: This mod is 100% client-side. It does not need to be installed on servers. In multiplayer servers, placement uses vanilla commands without requiring server-side mods.
* 🧩 **Required Dependencies**:
  * [Fabric Loader](https://fabricmc.net/) 
  * [Fabric API](https://modrinth.com/mod/fabric-api)
* 🎮 **Default Keybinding**: Press **`O`** to open the Item Organizer screen. You can customize this key in `Options > Controls > Key Binds` or in the mod's **Config** panel.
* 🛡️ **Safe World Placement**: Placing palettes in multiplayer uses native `/setblock` commands with automated chat suppression to prevent spam and does not tamper with external WorldEdit/FAWE undo histories.

---

## 🎮 Controls & Keybindings

| Key / Action | Function |
| :--- | :--- |
| **`O`** | Open or close the Item Organizer interface |
| **`Ctrl + Z`** | **Undo** last change (grid layout, blocking, palette edit/deletion, filter) |
| **`Ctrl + Y`** / **`Ctrl + Shift + Z`** | **Redo** last undone action |
| **`Left Click`** | Click UI buttons, drag & drop items, or click to carry item on cursor and click again to place |
| **`Shift + Left Click`** | **Item Panels**: Send item directly to hotbar<br>**Hotbar**: Remove item from hotbar |
| **`Hotbar Keys (1 - 9)`** | Quick-assign hovered item directly into that hotbar slot |
| **`Right Click`** | Select an item in the **Ordered** grid |
| **`Arrow Keys`** | Move the selected item around the Ordered grid |
| **`Right Click + DEL / Backspace`** | Remove selected item from Ordered grid (returns to Unorganized) |
| **`Double Right Click`** | **Ordered / Unorganized**: Send item to **Blocked** list<br>**Blocked**: Unblock item |
| **`A`** | Quick-append hovered item into the **Ordered** grid |
| **`Escape`** | Close current modal or exit Item Organizer screen |

*All shortcuts and controls can be reviewed in the in-game **Config** tab.*

---

## 📥 Installation

1. Make sure you have **Minecraft Fabric** installed for version **1.21.10** or **1.21.11**.
2. Download and place the latest **[Fabric API](https://modrinth.com/mod/fabric-api)** in your `.minecraft/mods` folder.
3. Download **Item Organizer** from Modrinth and place the `.jar` in your `.minecraft/mods` folder.
4. Launch Minecraft, join any singleplayer world or multiplayer server, and press **`O`**!

---

## 📄 License

This project is licensed under the **MIT License**.
