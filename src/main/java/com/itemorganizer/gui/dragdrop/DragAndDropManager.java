package com.itemorganizer.gui.dragdrop;

import com.itemorganizer.gui.util.RenderHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

// global drag and drop manager between mod panels
public class DragAndDropManager {
    private static final DragAndDropManager instance = new DragAndDropManager();
    private DragPayload activePayload;

    private DragAndDropManager() {
    }

    public static DragAndDropManager getInstance() {
        return instance;
    }

    public boolean isDragging() {
        return activePayload != null;
    }

    public DragPayload getActivePayload() {
        return activePayload;
    }

    public void startDrag(DragPayload payload) {
        this.activePayload = payload;
    }

    public void cancelDrag() {
        this.activePayload = null;
    }

    public DragPayload consumePayload() {
        DragPayload payload = this.activePayload;
        this.activePayload = null;
        return payload;
    }

    // render floating dragged item anchored to cursor
    public void renderFloatingItem(DrawContext context, int mouseX, int mouseY) {
        renderFloatingItem(context, mouseX, mouseY, 1.0f);
    }

    public void renderFloatingItem(DrawContext context, int mouseX, int mouseY, float itemScale) {
        if (activePayload != null && activePayload.getItemStack() != null && !activePayload.getItemStack().isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            int x = mouseX - 8;
            int y = mouseY - 8;

            // subtle halo to highlight the floating item
            context.fill(x - 2, y - 2, x + 18, y + 18, 0x66000000);
            RenderHelper.drawBorder(context, x - 2, y - 2, 20, 20, 0xAAFFFFFF);

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(mouseX, mouseY);
            context.getMatrices().scale(itemScale, itemScale);

            context.drawItem(activePayload.getItemStack(), -8, -8);
            context.drawStackOverlay(client.textRenderer, activePayload.getItemStack(), -8, -8);

            context.getMatrices().popMatrix();
        }
    }
}

