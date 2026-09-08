package com.itemorganizer.gui;

import com.itemorganizer.gui.navigation.LeftTab;
import com.itemorganizer.gui.navigation.OrdenadoSubTab;
import com.itemorganizer.gui.navigation.RightTab;
import com.itemorganizer.gui.viewmodel.OrganizerViewModel;
import com.itemorganizer.gui.util.RenderHelper;
import com.itemorganizer.gui.util.SoundHelper;
import com.itemorganizer.gui.util.TextScaleHelper;
import com.itemorganizer.gui.widget.HotbarWidget;
import com.itemorganizer.gui.widget.TabButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

// main screen layout with left/right panels and hotbar
public class OrganizerScreen extends Screen {
    private final OrganizerViewModel viewModel;
    private HotbarWidget hotbarWidget;
    private com.itemorganizer.gui.widget.OrderedGridWidget orderedGridWidget;
    private com.itemorganizer.gui.widget.BlockedGridWidget blockedGridWidget;
    private com.itemorganizer.gui.widget.ProfileManagerWidget profileManagerWidget;
    private com.itemorganizer.gui.widget.ConfigWidget configWidget;
    private com.itemorganizer.gui.widget.PaletteListWidget paletteListWidget;
    private com.itemorganizer.gui.widget.UnorganizedGridWidget unorganizedGridWidget;
    private com.itemorganizer.gui.widget.VersionCatalogWidget versionCatalogWidget;

    // panel bounds
    private int leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight;
    private int rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight;
    private int leftContentY, leftContentHeight, rightContentY, rightContentHeight;

    public enum ConfirmAction {
        NONE,
        SORT_COLOR,
        COMPACT
    }

    private ConfirmAction activeConfirmAction = ConfirmAction.NONE;
    private String activeToolbarTooltip = null;
    private int originalVanillaBlur = -1;

    private final java.util.List<TabButtonWidget> leftTabs = new java.util.ArrayList<>();
    private final java.util.List<TabButtonWidget> rightTabs = new java.util.ArrayList<>();

    public OrganizerScreen(OrganizerViewModel viewModel) {
        super(Text.translatable("title.itemorganizer"));
        this.viewModel = viewModel;
    }

    private static java.lang.reflect.Field simpleOptionValueField = null;

    private static void setOptionValue(net.minecraft.client.option.SimpleOption<Integer> option, int value) {
        if (option == null) return;
        try {
            if (value <= 10) {
                option.setValue(value);
            } else {
                if (simpleOptionValueField == null) {
                    for (java.lang.reflect.Field f : net.minecraft.client.option.SimpleOption.class.getDeclaredFields()) {
                        if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) && !java.lang.reflect.Modifier.isFinal(f.getModifiers())) {
                            f.setAccessible(true);
                            simpleOptionValueField = f;
                            break;
                        }
                    }
                }
                if (simpleOptionValueField != null) {
                    simpleOptionValueField.set(option, value);
                } else {
                    option.setValue(Math.min(10, value));
                }
            }
        } catch (Throwable t) {
            try {
                option.setValue(Math.min(10, value));
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (originalVanillaBlur != -1 && client != null && client.options != null) {
            setOptionValue(client.options.getMenuBackgroundBlurriness(), originalVanillaBlur);
            originalVanillaBlur = -1;
        }
    }

    public void applyBlurSetting() {
        if (client != null && client.options != null) {
            float blurVal = viewModel.getConfig().getBlur();
            int targetVal = Math.max(0, Math.min(50, Math.round(blurVal * 10.0f)));
            if (client.options.getMenuBackgroundBlurrinessValue() != targetVal) {
                setOptionValue(client.options.getMenuBackgroundBlurriness(), targetVal);
            }
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (client != null && client.world == null) {
            this.renderPanoramaBackground(context, delta);
        }
        applyBlurSetting();
        float blurPercent = viewModel.getConfig().getBlur();
        if (blurPercent > 0.01f) {
            try {
                context.applyBlur();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void init() {
        super.init();

        if (originalVanillaBlur == -1 && client != null && client.options != null) {
            originalVanillaBlur = client.options.getMenuBackgroundBlurrinessValue();
        }
        applyBlurSetting();

        // analyze missing items in background
        com.itemorganizer.gui.util.TextureColorAnalyzer.analyzeMissingItemsAsync(false, count -> {
            if (count != null && count > 0) {
                com.itemorganizer.ItemOrganizer.LOGGER.info("ItemOrganizer: analyzed {} mod items in background", count);
            }
        });

        leftTabs.clear();
        rightTabs.clear();

        // left navigation tabs
        TabButtonWidget tabOrdenado = new TabButtonWidget(
                0, 0, 10, 16,
                LeftTab.ORDENADO.getText(),
                btn -> viewModel.setActiveLeftTab(LeftTab.ORDENADO),
                () -> viewModel.getActiveLeftTab() == LeftTab.ORDENADO
        );
        TabButtonWidget tabPerfiles = new TabButtonWidget(
                0, 0, 10, 16,
                LeftTab.PERFILES.getText(),
                btn -> viewModel.setActiveLeftTab(LeftTab.PERFILES),
                () -> viewModel.getActiveLeftTab() == LeftTab.PERFILES
        );
        TabButtonWidget tabConfig = new TabButtonWidget(
                0, 0, 10, 16,
                LeftTab.CONFIG.getText(),
                btn -> viewModel.setActiveLeftTab(LeftTab.CONFIG),
                () -> viewModel.getActiveLeftTab() == LeftTab.CONFIG
        );
        TabButtonWidget tabBlocker = new TabButtonWidget(
                0, 0, 10, 16,
                Text.translatable("tab.itemorganizer.blocker"),
                btn -> viewModel.toggleBlocker(),
                viewModel::isBlockerActive,
                true
        );
        leftTabs.add(tabOrdenado);
        leftTabs.add(tabPerfiles);
        leftTabs.add(tabConfig);
        leftTabs.add(tabBlocker);
        leftTabs.forEach(this::addDrawableChild);

        // right navigation tabs
        TabButtonWidget tabPaletas = new TabButtonWidget(
                0, 0, 10, 16,
                RightTab.PALETAS.getText(),
                btn -> viewModel.setActiveRightTab(RightTab.PALETAS),
                () -> viewModel.getActiveRightTab() == RightTab.PALETAS
        );
        TabButtonWidget tabPorOrganizar = new TabButtonWidget(
                0, 0, 10, 16,
                RightTab.POR_ORGANIZAR.getText(),
                btn -> viewModel.setActiveRightTab(RightTab.POR_ORGANIZAR),
                () -> viewModel.getActiveRightTab() == RightTab.POR_ORGANIZAR
        );
        TabButtonWidget tabPorVersion = new TabButtonWidget(
                0, 0, 10, 16,
                RightTab.POR_VERSION.getText(),
                btn -> viewModel.setActiveRightTab(RightTab.POR_VERSION),
                () -> viewModel.getActiveRightTab() == RightTab.POR_VERSION
        );
        rightTabs.add(tabPaletas);
        rightTabs.add(tabPorOrganizar);
        rightTabs.add(tabPorVersion);
        rightTabs.forEach(this::addDrawableChild);

        // bottom hotbar widget
        hotbarWidget = new HotbarWidget(0, 0);
        addDrawableChild(hotbarWidget);

        // left content widgets
        orderedGridWidget = new com.itemorganizer.gui.widget.OrderedGridWidget(
                viewModel, 0, 0, 10, 10
        );
        blockedGridWidget = new com.itemorganizer.gui.widget.BlockedGridWidget(
                viewModel, 0, 0, 10, 10
        );
        profileManagerWidget = new com.itemorganizer.gui.widget.ProfileManagerWidget(
                viewModel, 0, 0, 10, 10
        );
        configWidget = new com.itemorganizer.gui.widget.ConfigWidget(
                viewModel, 0, 0, 10, 10
        );

        // right content widgets
        paletteListWidget = new com.itemorganizer.gui.widget.PaletteListWidget(
                viewModel, 0, 0, 10, 10
        );
        unorganizedGridWidget = new com.itemorganizer.gui.widget.UnorganizedGridWidget(
                viewModel, 0, 0, 10, 10
        );
        versionCatalogWidget = new com.itemorganizer.gui.widget.VersionCatalogWidget(
                viewModel, 0, 0, 10, 10
        );

        updateLayout();
    }

    public void updateLayout() {
        int margin = 10;
        int topMargin = 8;
        float hotbarScale = viewModel.getConfig().getHotbarScale();
        float splitRatio = viewModel.getConfig().getSplitRatio();
        float textScale = viewModel.getConfig().getTextScale();

        int hotbarSlotSize = Math.max(10, Math.round(HotbarWidget.BASE_SLOT_SIZE * hotbarScale));
        int bottomReserved = hotbarSlotSize + 16;
        int panelGap = 16;
        int tabHeight = Math.max(16, Math.round(16 * textScale));
        int tabSpacing = 3;
        int tabGap = 4;

        int availableWidth = this.width - (margin * 2) - panelGap;
        leftPanelWidth = Math.round(availableWidth * splitRatio);
        rightPanelWidth = availableWidth - leftPanelWidth;

        leftPanelX = margin;
        leftPanelY = topMargin;
        leftPanelHeight = this.height - topMargin - bottomReserved;

        rightPanelX = margin + leftPanelWidth + panelGap;
        rightPanelY = topMargin;
        rightPanelHeight = leftPanelHeight;

        // position left tabs
        int leftTabCount = leftTabs.size();
        if (leftTabCount > 0) {
            int totalSpacing = tabSpacing * (leftTabCount - 1);
            int leftTabWidth = Math.max(10, (leftPanelWidth - totalSpacing) / leftTabCount);
            for (int i = 0; i < leftTabCount; i++) {
                int tx = leftPanelX + (i * (leftTabWidth + tabSpacing));
                int tw = (i == leftTabCount - 1) ? (leftPanelX + leftPanelWidth - tx) : leftTabWidth;
                leftTabs.get(i).setBounds(tx, leftPanelY, tw, tabHeight);
            }
        }

        // position right tabs
        int rightTabCount = rightTabs.size();
        if (rightTabCount > 0) {
            int totalSpacing = tabSpacing * (rightTabCount - 1);
            int rightTabWidth = Math.max(10, (rightPanelWidth - totalSpacing) / rightTabCount);
            for (int i = 0; i < rightTabCount; i++) {
                int tx = rightPanelX + (i * (rightTabWidth + tabSpacing));
                int tw = (i == rightTabCount - 1) ? (rightPanelX + rightPanelWidth - tx) : rightTabWidth;
                rightTabs.get(i).setBounds(tx, rightPanelY, tw, tabHeight);
            }
        }

        // position bottom hotbar
        int totalHotbarWidth = HotbarWidget.SLOT_COUNT * hotbarSlotSize;
        int hotbarX = (this.width - totalHotbarWidth) / 2;
        int hotbarY = this.height - hotbarSlotSize - 6;
        if (hotbarWidget != null) {
            hotbarWidget.setBounds(hotbarX, hotbarY, hotbarScale, textScale);
        }

        // position content panels
        boolean isOrdenado = (viewModel.getActiveLeftTab() == LeftTab.ORDENADO);
        int toolbarHeight = 11;
        int toolbarGap = 3;

        if (isOrdenado) {
            leftContentY = leftPanelY + tabHeight + toolbarGap + toolbarHeight + tabGap;
            leftContentHeight = leftPanelHeight - (leftContentY - leftPanelY);
        } else {
            leftContentY = leftPanelY + tabHeight + tabGap;
            leftContentHeight = leftPanelHeight - tabHeight - tabGap;
        }

        if (orderedGridWidget != null) {
            orderedGridWidget.setBounds(leftPanelX, leftContentY, leftPanelWidth, leftContentHeight);
            orderedGridWidget.reflowIfNeeded();
        }
        if (blockedGridWidget != null) blockedGridWidget.setBounds(leftPanelX, leftContentY, leftPanelWidth, leftContentHeight);
        if (profileManagerWidget != null) profileManagerWidget.setBounds(leftPanelX, leftContentY, leftPanelWidth, leftContentHeight);
        if (configWidget != null) configWidget.setBounds(leftPanelX, leftContentY, leftPanelWidth, leftContentHeight);

        rightContentY = rightPanelY + tabHeight + tabGap;
        rightContentHeight = rightPanelHeight - tabHeight - tabGap;
        if (paletteListWidget != null) paletteListWidget.setBounds(rightPanelX, rightContentY, rightPanelWidth, rightContentHeight);
        if (unorganizedGridWidget != null) unorganizedGridWidget.setBounds(rightPanelX, rightContentY, rightPanelWidth, rightContentHeight);
        if (versionCatalogWidget != null) versionCatalogWidget.setBounds(rightPanelX, rightContentY, rightPanelWidth, rightContentHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        activeToolbarTooltip = null;

        // update layout and blur
        updateLayout();
        applyBlurSetting();

        // render background
        int bgColor = viewModel.getConfig().getArgbColor();
        context.fill(0, 0, this.width, this.height, bgColor);

        // toolbar below left tabs when organized tab is active
        if (viewModel.getActiveLeftTab() == LeftTab.ORDENADO && client != null && client.textRenderer != null) {
            renderOrdenadoToolbar(context, client.textRenderer, mouseX, mouseY);
        }

        // render content panels
        renderLeftContentArea(context, mouseX, mouseY, delta);
        renderRightContentArea(context, mouseX, mouseY, delta);

        // render widgets and children
        super.render(context, mouseX, mouseY, delta);

        // render floating dragged item
        com.itemorganizer.gui.dragdrop.DragAndDropManager.getInstance().renderFloatingItem(context, mouseX, mouseY, viewModel.getConfig().getItemScale());

        // confirmation modal or tooltips
        if (activeConfirmAction != ConfirmAction.NONE && client != null && client.textRenderer != null) {
            renderConfirmModal(context, client.textRenderer, mouseX, mouseY);
        } else if (activeToolbarTooltip != null && client != null && client.textRenderer != null) {
            context.drawTooltip(client.textRenderer, Text.literal(activeToolbarTooltip), mouseX, mouseY);
        }
    }

    private void renderLeftContentArea(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(leftPanelX, leftContentY, leftPanelX + leftPanelWidth, leftContentY + leftContentHeight, 0x00000000);
        RenderHelper.drawBorder(context, leftPanelX, leftContentY, leftPanelWidth, leftContentHeight, 0x00000000);

        LeftTab tab = viewModel.getActiveLeftTab();
        if (tab == LeftTab.ORDENADO) {
            if (viewModel.getActiveOrdenadoSubTab() == OrdenadoSubTab.BLOQUEADO) {
                if (blockedGridWidget != null) blockedGridWidget.render(context, mouseX, mouseY, delta);
            } else {
                if (orderedGridWidget != null) orderedGridWidget.render(context, mouseX, mouseY, delta);
            }
        } else if (tab == LeftTab.PERFILES && profileManagerWidget != null) {
            profileManagerWidget.render(context, mouseX, mouseY, delta);
        } else if (tab == LeftTab.CONFIG && configWidget != null) {
            configWidget.render(context, mouseX, mouseY, delta);
        }
    }

    private void renderRightContentArea(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(rightPanelX, rightContentY, rightPanelX + rightPanelWidth, rightContentY + rightContentHeight, 0x00000000);
        RenderHelper.drawBorder(context, rightPanelX, rightContentY, rightPanelWidth, rightContentHeight, 0x00000000);

        RightTab tab = viewModel.getActiveRightTab();
        if (tab == RightTab.PALETAS && paletteListWidget != null) {
            paletteListWidget.render(context, mouseX, mouseY, delta);
        } else if (tab == RightTab.POR_ORGANIZAR && unorganizedGridWidget != null) {
            unorganizedGridWidget.render(context, mouseX, mouseY, delta);
        } else if (tab == RightTab.POR_VERSION && versionCatalogWidget != null) {
            versionCatalogWidget.render(context, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean bl) {
        // modal handles click first
        if (activeConfirmAction != ConfirmAction.NONE) {
            int modalW = Math.min(270, this.width - 24);
            int modalH = 88;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;
            int btnW = (modalW - 28) / 2;
            int btnH = 18;
            int btnY = modalY + modalH - 24;

            int confirmBtnX = modalX + 10;
            if (click.button() == 0 && click.x() >= confirmBtnX && click.x() <= confirmBtnX + btnW && click.y() >= btnY && click.y() <= btnY + btnH) {
                if (activeConfirmAction == ConfirmAction.SORT_COLOR) {
                    executeSortByColor();
                } else if (activeConfirmAction == ConfirmAction.COMPACT) {
                    executeCompactItems();
                }
                activeConfirmAction = ConfirmAction.NONE;
                return true;
            }

            int cancelBtnX = confirmBtnX + btnW + 8;
            if (click.button() == 0 && click.x() >= cancelBtnX && click.x() <= cancelBtnX + btnW && click.y() >= btnY && click.y() <= btnY + btnH) {
                activeConfirmAction = ConfirmAction.NONE;
                playClickSound();
                return true;
            }

            // click outside modal cancels it
            if (click.button() == 0 && (click.x() < modalX || click.x() > modalX + modalW || click.y() < modalY || click.y() > modalY + modalH)) {
                activeConfirmAction = ConfirmAction.NONE;
                playClickSound();
                return true;
            }

            return true;
        }

        // subtab toolbar in organized view
        LeftTab leftTab = viewModel.getActiveLeftTab();
        if (leftTab == LeftTab.ORDENADO) {
            float textScale = viewModel.getConfig().getTextScale();
            int tabHeight = Math.max(16, Math.round(16 * textScale));
            int toolbarY = leftPanelY + tabHeight + 3;
            int toolbarH = 11;
            int gap = 3;
            int sub1W = Math.min(58, Math.max(46, (leftPanelWidth - 20) / 5));
            int sub2W = Math.min(68, Math.max(52, (leftPanelWidth - 20) / 5));

            int sub1X = leftPanelX;
            int sub2X = sub1X + sub1W + gap;

            if (click.button() == 0 && click.y() >= toolbarY && click.y() <= toolbarY + toolbarH) {
                if (click.x() >= sub1X && click.x() <= sub1X + sub1W) {
                    viewModel.setActiveOrdenadoSubTab(OrdenadoSubTab.ORGANIZADO);
                    playClickSound();
                    return true;
                }
                if (click.x() >= sub2X && click.x() <= sub2X + sub2W) {
                    viewModel.setActiveOrdenadoSubTab(OrdenadoSubTab.BLOQUEADO);
                    playClickSound();
                    return true;
                }

                if (viewModel.getActiveOrdenadoSubTab() == OrdenadoSubTab.ORGANIZADO) {
                    int remainingW = (leftPanelX + leftPanelWidth) - (sub2X + sub2W + gap * 2);
                    int btnW = Math.min(78, Math.max(48, (remainingW - gap) / 2));
                    int btn1X = sub2X + sub2W + gap * 2;
                    int btn2X = btn1X + btnW + gap;

                    if (click.x() >= btn1X && click.x() <= btn1X + btnW) {
                        activeConfirmAction = ConfirmAction.SORT_COLOR;
                        playClickSound();
                        return true;
                    }
                    if (click.x() >= btn2X && click.x() <= btn2X + btnW) {
                        activeConfirmAction = ConfirmAction.COMPACT;
                        playClickSound();
                        return true;
                    }
                }
            }
        }

        if (leftTab == LeftTab.ORDENADO) {
            if (viewModel.getActiveOrdenadoSubTab() == OrdenadoSubTab.BLOQUEADO) {
                if (blockedGridWidget != null && blockedGridWidget.mouseClicked(click, bl)) {
                    return true;
                }
            } else {
                if (orderedGridWidget != null && orderedGridWidget.mouseClicked(click, bl)) {
                    return true;
                }
            }
        }
        if (leftTab == LeftTab.PERFILES && profileManagerWidget != null && profileManagerWidget.mouseClicked(click, bl)) {
            return true;
        }
        if (leftTab == LeftTab.CONFIG && configWidget != null && configWidget.mouseClicked(click, bl)) {
            return true;
        }

        RightTab rightTab = viewModel.getActiveRightTab();
        if (rightTab == RightTab.PALETAS && paletteListWidget != null && paletteListWidget.mouseClicked(click, bl)) {
            return true;
        }
        if (rightTab == RightTab.POR_ORGANIZAR && unorganizedGridWidget != null && unorganizedGridWidget.mouseClicked(click, bl)) {
            return true;
        }
        if (rightTab == RightTab.POR_VERSION && versionCatalogWidget != null && versionCatalogWidget.mouseClicked(click, bl)) {
            return true;
        }

        if (hotbarWidget != null && hotbarWidget.mouseClicked(click, bl)) {
            return true;
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        com.itemorganizer.gui.dragdrop.DragAndDropManager dragManager = com.itemorganizer.gui.dragdrop.DragAndDropManager.getInstance();

        if (dragManager.isDragging()) {
            // drop onto bottom hotbar
            if (hotbarWidget != null) {
                int hotbarSlot = hotbarWidget.getSlotAt(click.x(), click.y());
                if (hotbarSlot >= 0 && client != null && client.player != null) {
                    com.itemorganizer.gui.dragdrop.DragPayload payload = dragManager.getActivePayload();
                    if (payload != null) {
                        if (payload.getSource() == com.itemorganizer.gui.dragdrop.DragSource.HOTBAR) {
                            int sourceSlot = payload.getSourceIndex();
                            int targetSlot = hotbarSlot;
                            if (sourceSlot != targetSlot && sourceSlot >= 0 && sourceSlot < 9) {
                                net.minecraft.entity.player.PlayerInventory inv = client.player.getInventory();
                                net.minecraft.item.ItemStack sourceStack = inv.getStack(sourceSlot).copy();
                                net.minecraft.item.ItemStack targetStack = inv.getStack(targetSlot).copy();
                                inv.setStack(targetSlot, sourceStack);
                                inv.setStack(sourceSlot, targetStack);
                                if (client.getNetworkHandler() != null && client.player.isCreative()) {
                                    client.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(36 + targetSlot, sourceStack));
                                    client.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(36 + sourceSlot, targetStack));
                                }
                                SoundHelper.playClick();
                            }
                        } else if (client.player.isCreative()) {
                            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(
                                    payload.getItemStack().getItem(),
                                    1
                            );
                            client.player.getInventory().setStack(hotbarSlot, stack);
                            if (client.getNetworkHandler() != null) {
                                client.getNetworkHandler().sendPacket(new net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket(36 + hotbarSlot, stack));
                            }
                            SoundHelper.playClick();
                        }
                    }
                    dragManager.consumePayload();
                    return true;
                }
            }

            // drop onto right panel
            if (click.x() >= rightPanelX && click.x() <= rightPanelX + rightPanelWidth) {
                RightTab rightTab = viewModel.getActiveRightTab();
                if (rightTab == RightTab.PALETAS && paletteListWidget != null && paletteListWidget.mouseReleased(click)) {
                    return true;
                }
                if (rightTab == RightTab.POR_ORGANIZAR && unorganizedGridWidget != null && unorganizedGridWidget.mouseReleased(click)) {
                    return true;
                }
                if (rightTab == RightTab.POR_VERSION && versionCatalogWidget != null && versionCatalogWidget.mouseReleased(click)) {
                    return true;
                }
            }

            // drop onto left panel
            if (click.x() >= leftPanelX && click.x() <= leftPanelX + leftPanelWidth) {
                LeftTab leftTab = viewModel.getActiveLeftTab();
                if (leftTab == LeftTab.ORDENADO) {
                    if (viewModel.getActiveOrdenadoSubTab() == OrdenadoSubTab.BLOQUEADO) {
                        if (blockedGridWidget != null && blockedGridWidget.mouseReleased(click)) return true;
                    } else {
                        if (orderedGridWidget != null && orderedGridWidget.mouseReleased(click)) return true;
                    }
                }
                if (leftTab == LeftTab.PERFILES && profileManagerWidget != null && profileManagerWidget.mouseReleased(click)) {
                    return true;
                }
                if (leftTab == LeftTab.CONFIG && configWidget != null && configWidget.mouseReleased(click)) {
                    return true;
                }
            }

            dragManager.consumePayload();
            return true;
        }

        // delegate to widgets when not dragging
        LeftTab leftTab = viewModel.getActiveLeftTab();
        if (leftTab == LeftTab.ORDENADO) {
            if (viewModel.getActiveOrdenadoSubTab() == OrdenadoSubTab.BLOQUEADO) {
                if (blockedGridWidget != null && blockedGridWidget.mouseReleased(click)) return true;
            } else {
                if (orderedGridWidget != null && orderedGridWidget.mouseReleased(click)) return true;
            }
        }
        if (leftTab == LeftTab.PERFILES && profileManagerWidget != null && profileManagerWidget.mouseReleased(click)) {
            return true;
        }
        if (leftTab == LeftTab.CONFIG && configWidget != null && configWidget.mouseReleased(click)) {
            return true;
        }

        RightTab rightTab = viewModel.getActiveRightTab();
        if (rightTab == RightTab.PALETAS && paletteListWidget != null && paletteListWidget.mouseReleased(click)) {
            return true;
        }
        if (rightTab == RightTab.POR_ORGANIZAR && unorganizedGridWidget != null && unorganizedGridWidget.mouseReleased(click)) {
            return true;
        }
        if (rightTab == RightTab.POR_VERSION && versionCatalogWidget != null && versionCatalogWidget.mouseReleased(click)) {
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double deltaX, double deltaY) {
        LeftTab leftTab = viewModel.getActiveLeftTab();
        if (leftTab == LeftTab.ORDENADO) {
            if (viewModel.getActiveOrdenadoSubTab() == OrdenadoSubTab.BLOQUEADO) {
                if (blockedGridWidget != null && blockedGridWidget.mouseDragged(click, deltaX, deltaY)) return true;
            } else {
                if (orderedGridWidget != null && orderedGridWidget.mouseDragged(click, deltaX, deltaY)) return true;
            }
        }
        if (leftTab == LeftTab.PERFILES && profileManagerWidget != null && profileManagerWidget.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }
        if (leftTab == LeftTab.CONFIG && configWidget != null && configWidget.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }

        RightTab rightTab = viewModel.getActiveRightTab();
        if (rightTab == RightTab.PALETAS && paletteListWidget != null && paletteListWidget.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }
        if (rightTab == RightTab.POR_ORGANIZAR && unorganizedGridWidget != null && unorganizedGridWidget.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }
        if (rightTab == RightTab.POR_VERSION && versionCatalogWidget != null && versionCatalogWidget.mouseDragged(click, deltaX, deltaY)) {
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        LeftTab leftTab = viewModel.getActiveLeftTab();
        if (leftTab == LeftTab.ORDENADO) {
            if (viewModel.getActiveOrdenadoSubTab() == OrdenadoSubTab.BLOQUEADO) {
                if (blockedGridWidget != null && blockedGridWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
            } else {
                if (orderedGridWidget != null && orderedGridWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
            }
        }
        if (leftTab == LeftTab.PERFILES && profileManagerWidget != null && profileManagerWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        if (leftTab == LeftTab.CONFIG && configWidget != null && configWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        RightTab rightTab = viewModel.getActiveRightTab();
        if (rightTab == RightTab.PALETAS && paletteListWidget != null && paletteListWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        if (rightTab == RightTab.POR_ORGANIZAR && unorganizedGridWidget != null && unorganizedGridWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        if (rightTab == RightTab.POR_VERSION && versionCatalogWidget != null && versionCatalogWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        // confirmation modal key handling
        if (activeConfirmAction != ConfirmAction.NONE) {
            if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
                activeConfirmAction = ConfirmAction.NONE;
                playClickSound();
                return true;
            }
            if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) {
                if (activeConfirmAction == ConfirmAction.SORT_COLOR) {
                    executeSortByColor();
                } else if (activeConfirmAction == ConfirmAction.COMPACT) {
                    executeCompactItems();
                }
                activeConfirmAction = ConfirmAction.NONE;
                return true;
            }
            return true;
        }

        // cancel drag with ESC
        com.itemorganizer.gui.dragdrop.DragAndDropManager dragManager = com.itemorganizer.gui.dragdrop.DragAndDropManager.getInstance();
        if (dragManager.isDragging() && input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            dragManager.cancelDrag();
            return true;
        }

        LeftTab leftTab = viewModel.getActiveLeftTab();
        if (leftTab == LeftTab.ORDENADO) {
            if (viewModel.getActiveOrdenadoSubTab() == OrdenadoSubTab.BLOQUEADO) {
                if (blockedGridWidget != null && blockedGridWidget.keyPressed(input)) return true;
            } else {
                if (orderedGridWidget != null && orderedGridWidget.keyPressed(input)) return true;
            }
        }
        if (leftTab == LeftTab.PERFILES && profileManagerWidget != null && profileManagerWidget.keyPressed(input)) {
            return true;
        }
        if (leftTab == LeftTab.CONFIG && configWidget != null && configWidget.keyPressed(input)) {
            return true;
        }

        RightTab rightTab = viewModel.getActiveRightTab();
        if (rightTab == RightTab.PALETAS && paletteListWidget != null && paletteListWidget.keyPressed(input)) {
            return true;
        }
        if (rightTab == RightTab.POR_ORGANIZAR && unorganizedGridWidget != null && unorganizedGridWidget.keyPressed(input)) {
            return true;
        }
        if (rightTab == RightTab.POR_VERSION && versionCatalogWidget != null && versionCatalogWidget.keyPressed(input)) {
            return true;
        }

        // quick append key to organized grid
        net.minecraft.client.util.InputUtil.Key pressedKey = net.minecraft.client.util.InputUtil.fromKeyCode(input);
        String appendKey = viewModel.getConfig().getKeyQuickAppend();
        if (pressedKey != null && pressedKey.getTranslationKey().equalsIgnoreCase(appendKey)) {
            String hoveredItem = null;
            if (rightTab == RightTab.POR_ORGANIZAR && unorganizedGridWidget != null) {
                hoveredItem = unorganizedGridWidget.getHoveredItemId();
            } else if (rightTab == RightTab.POR_VERSION && versionCatalogWidget != null) {
                hoveredItem = versionCatalogWidget.getHoveredItemId();
            }

            if (hoveredItem != null && !hoveredItem.isEmpty()) {
                quickAppendToOrdered(hoveredItem);
                return true;
            }
        }

        if (hotbarWidget != null && hotbarWidget.keyPressed(input)) {
            return true;
        }

        return super.keyPressed(input);
    }

    // append hovered item to the right of the last placed block in organized grid
    private void quickAppendToOrdered(String itemId) {
        if (itemId == null || itemId.isEmpty()) return;

        // do not append if blocker is active
        if (viewModel.isBlockerActive()) {
            SoundHelper.playLock();
            return;
        }

        com.itemorganizer.core.model.ProfileData profile = viewModel.getActiveProfile();
        if (profile == null) return;

        // remove existing copy before re-appending to end
        profile.removeItem(itemId);

        int cols = profile.getColumnCount() > 0 ? profile.getColumnCount() : (orderedGridWidget != null ? orderedGridWidget.getColumnCount() : 9);
        cols = Math.max(1, cols);

        int nextIndex = 0;
        if (!profile.getItems().isEmpty()) {
            int maxIdx = -1;
            for (com.itemorganizer.core.model.ItemSlotPosition pos : profile.getItems()) {
                int idx = pos.getY() * cols + pos.getX();
                if (idx > maxIdx) {
                    maxIdx = idx;
                }
            }
            nextIndex = maxIdx + 1;
        }

        int targetX = nextIndex % cols;
        int targetY = nextIndex / cols;

        profile.setItemAt(itemId, targetX, targetY);
        com.itemorganizer.storage.StorageManager.getInstance().getProfileRepository().saveProfile(profile);
        viewModel.recomputeUnorganizedItems();

        SoundHelper.playPickup();
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput input) {
        LeftTab leftTab = viewModel.getActiveLeftTab();
        if (leftTab == LeftTab.PERFILES && profileManagerWidget != null && profileManagerWidget.charTyped(input)) {
            return true;
        }
        if (leftTab == LeftTab.CONFIG && configWidget != null && configWidget.charTyped(input)) {
            return true;
        }

        RightTab rightTab = viewModel.getActiveRightTab();
        if (rightTab == RightTab.PALETAS && paletteListWidget != null && paletteListWidget.charTyped(input)) {
            return true;
        }

        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    public OrganizerViewModel getViewModel() {
        return viewModel;
    }

    private void renderOrdenadoToolbar(DrawContext context, net.minecraft.client.font.TextRenderer tr, int mouseX, int mouseY) {
        float textScale = viewModel.getConfig().getTextScale();
        int tabHeight = Math.max(16, Math.round(16 * textScale));
        int toolbarY = leftPanelY + tabHeight + 3;
        int toolbarH = 11;
        int gap = 3;

        int sub1W = Math.min(58, Math.max(46, (leftPanelWidth - 20) / 5));
        int sub2W = Math.min(68, Math.max(52, (leftPanelWidth - 20) / 5));

        // subtab 1: organized
        int sub1X = leftPanelX;
        boolean isSub1Active = (viewModel.getActiveOrdenadoSubTab() == OrdenadoSubTab.ORGANIZADO);
        boolean hoverSub1 = (activeConfirmAction == ConfirmAction.NONE && mouseX >= sub1X && mouseX <= sub1X + sub1W && mouseY >= toolbarY && mouseY <= toolbarY + toolbarH);
        int bgSub1 = isSub1Active ? 0x3338BDF8 : (hoverSub1 ? 0x22FFFFFF : 0x14FFFFFF);
        int borderSub1 = isSub1Active ? 0xFF38BDF8 : (hoverSub1 ? 0x4DFFFFFF : 0x25FFFFFF);
        int textSub1 = isSub1Active ? 0xFF38BDF8 : (hoverSub1 ? 0xFFFFFFFF : 0xFFCBD5E1);

        context.fill(sub1X, toolbarY, sub1X + sub1W, toolbarY + toolbarH, bgSub1);
        RenderHelper.drawBorder(context, sub1X, toolbarY, sub1W, toolbarH, borderSub1);
        Text sub1Text = Text.literal("▦ ").append(OrdenadoSubTab.ORGANIZADO.getText());
        TextScaleHelper.drawVerticallyCenteredScaledText(
                context, tr, sub1Text, sub1X + sub1W / 2, toolbarY + toolbarH / 2, textSub1, true, Math.min(0.70f, textScale * 0.80f)
        );

        // subtab 2: blocked
        int blockedCount = (viewModel.getActiveProfile() != null) ? viewModel.getActiveProfile().getBlockedItems().size() : 0;
        Text sub2Text = (blockedCount > 0)
                ? Text.literal("🔒 ").append(OrdenadoSubTab.BLOQUEADO.getText()).append(" (" + blockedCount + ")")
                : Text.literal("🔒 ").append(OrdenadoSubTab.BLOQUEADO.getText());
        int sub2X = sub1X + sub1W + gap;
        boolean isSub2Active = (viewModel.getActiveOrdenadoSubTab() == OrdenadoSubTab.BLOQUEADO);
        boolean hoverSub2 = (activeConfirmAction == ConfirmAction.NONE && mouseX >= sub2X && mouseX <= sub2X + sub2W && mouseY >= toolbarY && mouseY <= toolbarY + toolbarH);
        int bgSub2 = isSub2Active ? 0x3338BDF8 : (hoverSub2 ? 0x22FFFFFF : 0x14FFFFFF);
        int borderSub2 = isSub2Active ? 0xFF38BDF8 : (hoverSub2 ? 0x4DFFFFFF : 0x25FFFFFF);
        int textSub2 = isSub2Active ? 0xFF38BDF8 : (hoverSub2 ? 0xFFFFFFFF : 0xFFCBD5E1);

        context.fill(sub2X, toolbarY, sub2X + sub2W, toolbarY + toolbarH, bgSub2);
        RenderHelper.drawBorder(context, sub2X, toolbarY, sub2W, toolbarH, borderSub2);
        TextScaleHelper.drawVerticallyCenteredScaledText(
                context, tr, sub2Text, sub2X + sub2W / 2, toolbarY + toolbarH / 2, textSub2, true, Math.min(0.68f, textScale * 0.78f)
        );

        if (isSub1Active) {
            // action buttons
            int remainingW = (leftPanelX + leftPanelWidth) - (sub2X + sub2W + gap * 2);
            int btnW = Math.min(78, Math.max(48, (remainingW - gap) / 2));
            int btn1X = sub2X + sub2W + gap * 2;

            // gradient sort button
            boolean hover1 = (activeConfirmAction == ConfirmAction.NONE && mouseX >= btn1X && mouseX <= btn1X + btnW && mouseY >= toolbarY && mouseY <= toolbarY + toolbarH);
            int bg1 = hover1 ? 0x4D2A4A6A : 0x22FFFFFF;
            int border1 = hover1 ? 0xFF38BDF8 : 0x33FFFFFF;
            int text1 = hover1 ? 0xFFFFFFFF : 0xFFCBD5E1;

            context.fill(btn1X, toolbarY, btn1X + btnW, toolbarY + toolbarH, bg1);
            RenderHelper.drawBorder(context, btn1X, toolbarY, btnW, toolbarH, border1);
            Text gradientBtnText = Text.literal("🎨 ").append(Text.translatable("button.itemorganizer.gradient"));
            TextScaleHelper.drawVerticallyCenteredScaledText(
                    context, tr, gradientBtnText, btn1X + btnW / 2, toolbarY + toolbarH / 2, text1, true, Math.min(0.70f, textScale * 0.80f)
            );
            if (hover1) {
                activeToolbarTooltip = Text.translatable("tooltip.itemorganizer.gradient").getString();
            }

            // compact button
            int btn2X = btn1X + btnW + gap;
            boolean hover2 = (activeConfirmAction == ConfirmAction.NONE && mouseX >= btn2X && mouseX <= btn2X + btnW && mouseY >= toolbarY && mouseY <= toolbarY + toolbarH);
            int bg2 = hover2 ? 0x4D2E5538 : 0x22FFFFFF;
            int border2 = hover2 ? 0xFF34D399 : 0x33FFFFFF;
            int text2 = hover2 ? 0xFFFFFFFF : 0xFFCBD5E1;

            context.fill(btn2X, toolbarY, btn2X + btnW, toolbarY + toolbarH, bg2);
            RenderHelper.drawBorder(context, btn2X, toolbarY, btnW, toolbarH, border2);
            Text compactBtnText = Text.literal("🧹 ").append(Text.translatable("button.itemorganizer.compact"));
            TextScaleHelper.drawVerticallyCenteredScaledText(
                    context, tr, compactBtnText, btn2X + btnW / 2, toolbarY + toolbarH / 2, text2, true, Math.min(0.70f, textScale * 0.80f)
            );
            if (hover2) {
                activeToolbarTooltip = Text.translatable("tooltip.itemorganizer.compact").getString();
            }
        } else {
            // hint for unlocking
            int tipX = sub2X + sub2W * 2 + gap;
            TextScaleHelper.drawVerticallyCenteredScaledText(
                    context, tr, Text.translatable("tip.itemorganizer.unlock_hint"), tipX, toolbarY + toolbarH / 2, 0xFF94A3B8, false, Math.min(0.70f, textScale * 0.78f)
            );
        }
    }

    private void renderConfirmModal(DrawContext context, net.minecraft.client.font.TextRenderer tr, int mouseX, int mouseY) {
        float textScale = viewModel.getConfig().getTextScale();

        // dark backdrop
        context.fill(0, 0, this.width, this.height, 0x99000000);

        // modal dialog box
        int modalW = Math.min(270, this.width - 24);
        int modalH = 88;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        context.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF141820);

        int borderColor = (activeConfirmAction == ConfirmAction.SORT_COLOR) ? 0xFF38BDF8 : 0xFF34D399;
        RenderHelper.drawBorder(context, modalX, modalY, modalW, modalH, borderColor);

        // title
        Text title = (activeConfirmAction == ConfirmAction.SORT_COLOR)
                ? Text.translatable("modal.itemorganizer.sort_color.title")
                : Text.translatable("modal.itemorganizer.compact.title");
        int titleColor = (activeConfirmAction == ConfirmAction.SORT_COLOR) ? 0xFF38BDF8 : 0xFF34D399;
        TextScaleHelper.drawCenteredScaledText(
                context, tr, title, modalX + modalW / 2, modalY + 8, titleColor, textScale
        );

        // description
        Text desc = (activeConfirmAction == ConfirmAction.SORT_COLOR)
                ? Text.translatable("modal.itemorganizer.sort_color.desc")
                : Text.translatable("modal.itemorganizer.compact.desc");
        TextScaleHelper.drawCenteredScaledText(
                context, tr, desc, modalX + modalW / 2, modalY + 24, 0xFFD1D5DB, textScale * 0.9f
        );

        Text warning = Text.translatable("modal.itemorganizer.warning");
        TextScaleHelper.drawCenteredScaledText(
                context, tr, warning, modalX + modalW / 2, modalY + 38, 0xFF94A3B8, textScale * 0.85f
        );

        // confirm and cancel buttons
        int btnW = (modalW - 28) / 2;
        int btnH = 18;
        int btnY = modalY + modalH - 24;

        int confirmBtnX = modalX + 10;
        boolean hoverConfirm = (mouseX >= confirmBtnX && mouseX <= confirmBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH);
        int confirmBg = hoverConfirm ? ((activeConfirmAction == ConfirmAction.SORT_COLOR) ? 0xDD1E40AF : 0xDD065F46) : 0xAA1E293B;
        int confirmBorder = hoverConfirm ? ((activeConfirmAction == ConfirmAction.SORT_COLOR) ? 0xFF60A5FA : 0xFF34D399) : 0xFF475569;
        context.fill(confirmBtnX, btnY, confirmBtnX + btnW, btnY + btnH, confirmBg);
        RenderHelper.drawBorder(context, confirmBtnX, btnY, btnW, btnH, confirmBorder);
        TextScaleHelper.drawCenteredScaledText(
                context, tr, Text.translatable("button.itemorganizer.confirm"), confirmBtnX + btnW / 2, btnY + 5, 0xFFFFFFFF, textScale
        );

        int cancelBtnX = confirmBtnX + btnW + 8;
        boolean hoverCancel = (mouseX >= cancelBtnX && mouseX <= cancelBtnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH);
        int cancelBg = hoverCancel ? 0xDD7F1D1D : 0xAA2A2A2A;
        int cancelBorder = hoverCancel ? 0xFFEF4444 : 0xFF555555;
        context.fill(cancelBtnX, btnY, cancelBtnX + btnW, btnY + btnH, cancelBg);
        RenderHelper.drawBorder(context, cancelBtnX, btnY, btnW, btnH, cancelBorder);
        TextScaleHelper.drawCenteredScaledText(
                context, tr, Text.translatable("button.itemorganizer.cancel"), cancelBtnX + btnW / 2, btnY + 5, 0xFFFFFFFF, textScale
        );
    }

    private void executeSortByColor() {
        if (viewModel.isBlockerActive()) {
            playLockedSound();
            return;
        }
        com.itemorganizer.core.model.ProfileData profile = viewModel.getActiveProfile();
        if (profile == null || profile.getItems().isEmpty()) return;

        int cols = profile.getColumnCount() > 0 ? profile.getColumnCount() : (orderedGridWidget != null ? orderedGridWidget.getColumnCount() : 9);
        cols = Math.max(1, cols);

        try {
            profile.sortItems(com.itemorganizer.gui.util.ItemColorHelper.getColorComparator(), cols);
            com.itemorganizer.storage.StorageManager.getInstance().getProfileRepository().saveProfile(profile);
            viewModel.recomputeUnorganizedItems();
            if (orderedGridWidget != null) {
                orderedGridWidget.reflowIfNeeded();
            }
            playSuccessSound();
        } catch (Throwable t) {
            com.itemorganizer.ItemOrganizer.LOGGER.error("Error sorting items by color: ", t);
            playLockedSound();
        }
    }

    private void executeCompactItems() {
        if (viewModel.isBlockerActive()) {
            playLockedSound();
            return;
        }
        com.itemorganizer.core.model.ProfileData profile = viewModel.getActiveProfile();
        if (profile == null || profile.getItems().isEmpty()) return;

        int cols = profile.getColumnCount() > 0 ? profile.getColumnCount() : (orderedGridWidget != null ? orderedGridWidget.getColumnCount() : 9);
        cols = Math.max(1, cols);

        try {
            profile.compactItems(cols);
            com.itemorganizer.storage.StorageManager.getInstance().getProfileRepository().saveProfile(profile);
            viewModel.recomputeUnorganizedItems();
            if (orderedGridWidget != null) {
                orderedGridWidget.reflowIfNeeded();
            }
            playSuccessSound();
        } catch (Throwable t) {
            com.itemorganizer.ItemOrganizer.LOGGER.error("Error compacting items: ", t);
            playLockedSound();
        }
    }

    private void playClickSound() {
        SoundHelper.playClick();
    }

    private void playSuccessSound() {
        SoundHelper.playChime();
    }

    private void playLockedSound() {
        SoundHelper.playLock();
    }
}
