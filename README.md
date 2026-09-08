# Item Organizer

<p align="center">
  <img src="src/main/resources/assets/item-organizer/icon.png" alt="Item Organizer Logo" width="128" height="128">
</p>

<p align="center">
  <strong>A visual inventory and palette manager for Minecraft Fabric.</strong><br>
  Sort blocks and items by chromatic color gradient, manage hotbar palettes, and customize your creative workflow.
</p>

<p align="center">
  <a href="https://github.com/PrimityArtur/MC-Item-Organizer/releases"><img src="https://img.shields.io/github/v/release/PrimityArtur/MC-Item-Organizer?color=brightgreen&label=release" alt="Release"></a>
  <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Loader-Fabric-blue" alt="Fabric"></a>
  <a href="https://minecraft.net/"><img src="https://img.shields.io/badge/Minecraft-1.21.10-orange" alt="Minecraft 1.21.10"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-green" alt="License"></a>
</p>

---

## Overview

**Item Organizer** is a client-side Fabric mod designed for builders, creators, and organizers. It analyzes the color of blocks and items—including modded items—to arrange them in a  chromatic color gradient.

Whether you need quick hotbar palettes for building gradients or want a structured overview of all blocks in the game, Item Organizer streamlines your workflow.

---

## Features

- **Chromatic Color Sorting**: Automatically sorts blocks and items by perceptual hue, saturation, and luminance.
- **Dynamic Model & Texture Inspection**: Intelligently identifies non-cube items, glass panes, bars, pottery sherds, mob heads, slabs, and stairs without requiring hardcoded lists.
- **Hotbar Palette Manager**:
  - Build and store custom palettes.
  - Search filter to quickly locate specific palettes.
  - One-click load directly to your player hotbar.
  - Compact controls to reorder, duplicate, rename, or delete palettes.
- **Profile System**:
  - Create multiple organization profiles.
  - Clone active setups with all locked items preserved.
  - Easily switch between building palettes and resource layouts.
- **Item Locking (Blocker)**:
  - Double right-click any item to lock or unlock it.
  - Keep unwanted items from cluttering your organized workspace.
- **Visual Customization**:
  - **Custom Blur**: Adjustable background blur from subtle glassmorphism to ultra-deep blur.
  - **Grid & Item Scaling**: Independent sliders for Grid Zoom, Item Scale, Palette Scale, and Text Scale.
- **Server Safe (Client-Side)**: 100% client-side. Works on singleplayer, LAN, and multiplayer servers (vanilla, Fabric, Paper, Purpur) without server installation.
- **Multilingual Support**: Available in English (`en_us`) and Spanish (`es_es`).

---

## Controls & Keybindings

| Key / Action | Function |
| :--- | :--- |
| **`O`** *(default)* | Open Item Organizer screen |
| **`Left Click`** | Select or drag an item / click buttons |
| **`Double Right Click`** | Lock / unlock item to Blocker list |
| **`Escape`** | Close the screen |

*All keybindings can be customized in the in-game **Config** tab or in the standard Minecraft Controls menu.*

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.10.
2. Download the latest version of [Fabric API](https://modrinth.com/mod/fabric-api).
3. Download **Item Organizer** from [Modrinth](https://modrinth.com/mod/item-organizer)
4. Place both `.jar` files into your `.minecraft/mods` folder.
5. Launch Minecraft and press `O` to open the organizer.

## License

This project is licensed under the [MIT License](LICENSE).
