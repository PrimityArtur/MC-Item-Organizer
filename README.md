# Item Organizer 🎨

<p align="center">
  <img src="https://github.com/PrimityArtur/MC-Item-Organizer/blob/1.21.11/src/main/resources/assets/item-organizer/icon.png?raw=true" alt="Item Organizer Logo" width="128" height="128">
</p>

<p align="center">
  <strong>Visual inventory, palette manager, and builder workspace for Minecraft Fabric.</strong><br>
  Customizable layouts, chromatic color gradients, and hotbar building palettes.
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

**Item Organizer** is a client-side Fabric mod that replaces the creative inventory with an organized builder workspace. It features custom grid layouts, texture-based color sorting, and hotbar palette management. In Survival mode, it serves as a visual item catalog and reference guide.

### 🌟 Core Features & Panels

* 📋 **Ordered Panel**: Organize blocks in a grid with adjustable columns, hide unwanted items via the Blocked list, sort by texture color gradients (hue, saturation, brightness), and compact empty slots.
* 📦 **Unorganized Panel**: Displays all items not yet placed in the Ordered grid or added to the Blocked list.
* 📁 **Profiles Panel**: Create, load, rename, duplicate, and delete layout profiles for different projects or themes.
* 🎨 **Palettes Panel**: Save 9-slot block combinations to hotbar cards to move, duplicate, load into/copy from your hotbar, or place horizontally in the world.
* 🔍 **Palette Search & Sorting**: Search palettes by text or index, add new palettes instantly (`+ New`), and sort columns by color gradient without modifying saved JSON files.
* 🎯 **Slot-Based Search Filter**: Place items into search slots to filter palettes by exact slot position, row presence, or color similarity.
* ♾️ **Infinite Palette Panel**: Multi-row palettes for large block collections, supporting sequential hotbar pasting and slot search filtering.
* ⏳ **By Version Panel**: Browse items chronologically by Minecraft release version.
* 🛡️ **Blocker Mode**: Locks the grid to prevent accidental item moving, blocking, or layout changes while building.
* ⚙️ **Config Panel**: In-game keybinding reference and interface scaling (background blur, grid zoom, slot size, palette scale, and text scale).
* 🔄 **Undo & Redo System**: Full `Ctrl + Z` (Undo) and `Ctrl + Y` (Redo) support across all panels and layout edits.

---

## 💡 Why You Should Use This Mod

* 🎯 **Faster Workflow**: Keep frequently used palettes one click away instead of manually searching creative tabs or chests.
* 🌈 **Texture-Based Gradients**: Automatically extracts block colors to generate smooth palette transitions without manual testing.
* 🏗️ **Direct World Placement**: Place palette rows directly in-game without placing support blocks underneath.
* 🔀 **Flexible Controls**: Move items using drag-and-drop or single-click carry-and-place.
* 🔒 **Safe Editing**: Protect layouts with grid locking (Blocker Mode) and multi-level Undo/Redo (`Ctrl + Z` / `Ctrl + Y`).

---

## ⚠️ Information Before Downloading

* 🖥️ **Client-Side Only**: Does not need to be installed on servers; works on multiplayer networks.
* 🧩 **Dependencies**: Requires [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api).
* 🎮 **Default Keybind**: Press **`O`** to open the interface (rebindable in `Options > Controls > Key Binds` or in the Config panel).

---

## 🎮 Controls & Keybindings

| Key / Action | Function |
| :--- | :--- |
| **`O`** | Open or close the Item Organizer interface |
| **`Ctrl + Z`** | **Undo** last change (grid layout, blocking, palette edit/deletion, filter) |
| **`Ctrl + Y`** / **`Ctrl + Shift + Z`** | **Redo** last undone action |
| **`Left Click`** | Click UI buttons, drag & drop items, or click to carry item on cursor and click again to place |
| **`Shift + Left Click`** | **Item Panels**: Send item directly to hotbar<br>**Hotbar**: Remove item from hotbar |
| **`Hotbar Keys (1 - 9)`** | Assign hovered item directly into that hotbar slot |
| **`Right Click`** | Select an item in the **Ordered** grid |
| **`Arrow Keys`** | Move the selected item around the Ordered grid |
| **`Right Click + DEL / Backspace`** | Remove selected item from Ordered grid (returns to Unorganized) |
| **`Double Right Click`** | **Ordered / Unorganized**: Send item to **Blocked** list<br>**Blocked**: Unblock item |
| **`A`** | Append hovered item into the **Ordered** grid |
| **`Escape`** | Close current modal or exit Item Organizer screen |

---

## 📥 Installation

1. Install **Minecraft Fabric**.
2. Download and place the latest **[Fabric API](https://modrinth.com/mod/fabric-api)** into your `.minecraft/mods` folder.
3. Download **Item Organizer** from Modrinth and place the `.jar` into your `.minecraft/mods` folder.
4. Launch the game, join a world or server, and press **`O`**.

---

## 📄 License

This project is licensed under the **MIT License**.