package com.itemorganizer.gui.util;

import com.itemorganizer.core.model.PaletteRow;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.TestBlockMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// handles palette block placement in the world with physics bypass and chat silence
public class PalettePlacementManager {
    public static final int SINGLEPLAYER_BLOCK_FLAGS =
            Block.NOTIFY_LISTENERS | Block.FORCE_STATE_AND_SKIP_CALLBACKS_AND_DROPS | Block.MOVED;

    private static final PalettePlacementManager INSTANCE = new PalettePlacementManager();

    private final Queue<Runnable> actionQueue = new ConcurrentLinkedQueue<>();
    private volatile long suppressionEndTime = 0;
    private volatile boolean pendingCompletionNotice = false;

    public static PalettePlacementManager getInstance() {
        return INSTANCE;
    }

    public static void init() {
        // filter incoming system messages to suppress command feedback during placement
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (INSTANCE.isSuppressingChat()) {
                if (isCommandFeedbackMessage(message)) {
                    return false;
                }
            }
            return true;
        });

        // process multiplayer block placement queue smoothly per client tick
        ClientTickEvents.END_CLIENT_TICK.register(INSTANCE::onClientTick);
    }

    public void startSuppression(long durationMs) {
        suppressionEndTime = System.currentTimeMillis() + durationMs;
    }

    public boolean isSuppressingChat() {
        return System.currentTimeMillis() < suppressionEndTime;
    }

    // checks if a game message is command feedback from setblock, teleport, or worldedit
    public static boolean isCommandFeedbackMessage(Text message) {
        if (message == null) return false;

        // check translatable translation keys
        if (message.getContent() instanceof TranslatableTextContent translatable) {
            String key = translatable.getKey();
            if (key != null) {
                if (key.startsWith("commands.setblock.")
                        || key.startsWith("commands.teleport.")
                        || key.startsWith("worldedit.")
                        || key.startsWith("fawe.")) {
                    return true;
                }
            }
        }

        // check raw text contents across languages
        String str = message.getString();
        if (str == null || str.isEmpty()) return false;
        String lower = str.toLowerCase(Locale.ROOT);

        return lower.contains("set the block at")
                || lower.contains("changed the block at")
                || lower.contains("se ha colocado el bloque")
                || lower.contains("bloque colocado en")
                || lower.contains("teleported ")
                || lower.contains("teletransportado ")
                || lower.contains("first position set to")
                || lower.contains("second position set to")
                || lower.contains("primera posición establecida")
                || lower.contains("segunda posición establecida")
                || lower.contains("operation completed")
                || lower.contains("operación completada")
                || lower.contains("blocks affected")
                || lower.contains("bloques afectados");
    }

    public void onClientTick(MinecraftClient client) {
        if (client == null || client.player == null || actionQueue.isEmpty()) {
            return;
        }

        Runnable action = actionQueue.poll();
        if (action != null) {
            action.run();
        }

        if (actionQueue.isEmpty() && pendingCompletionNotice) {
            pendingCompletionNotice = false;
            client.player.sendMessage(Text.translatable("palettes.itemorganizer.palette_placed"), true);
            SoundHelper.playChime();
        }
    }


    // resolves placeable block state for item id, supporting water, lava, and blockstate properties
    public static BlockState getBlockStateForPlacement(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) return null;
        String idStr = itemId.trim().toLowerCase(Locale.ROOT);

        if ("minecraft:water_bucket".equals(idStr) || "water_bucket".equals(idStr)) {
            try {
                return Blocks.WATER.getDefaultState();
            } catch (Throwable ignored) {
                return null;
            }
        }
        if ("minecraft:lava_bucket".equals(idStr) || "lava_bucket".equals(idStr)) {
            try {
                return Blocks.LAVA.getDefaultState();
            } catch (Throwable ignored) {
                return null;
            }
        }

        String baseId = idStr;
        String properties = null;
        int bracketStart = idStr.indexOf('[');
        int bracketEnd = idStr.lastIndexOf(']');
        if (bracketStart != -1 && bracketEnd > bracketStart) {
            baseId = idStr.substring(0, bracketStart).trim();
            properties = idStr.substring(bracketStart + 1, bracketEnd).trim();
        }

        Identifier id = Identifier.tryParse(baseId);
        if (id == null) return null;

        try {
            Item item = Registries.ITEM.get(id);
            if (item instanceof BlockItem blockItem) {
                BlockState state = blockItem.getBlock().getDefaultState();
                if (properties != null) {
                    if (state.getBlock() instanceof TestBlock) {
                        for (TestBlockMode mode : TestBlockMode.values()) {
                            if (properties.contains(mode.asString())) {
                                state = state.with(TestBlock.MODE, mode);
                                break;
                            }
                        }
                    } else if (state.getBlock() instanceof LightBlock) {
                        for (int lvl = 15; lvl >= 0; lvl--) {
                            if (properties.contains(String.valueOf(lvl))) {
                                state = state.with(LightBlock.LEVEL_15, lvl);
                                break;
                            }
                        }
                    }
                }
                return state;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public void placePalette(MinecraftClient client, PaletteRow row) {
        if (client == null || client.player == null || client.world == null || row == null) return;

        BlockPos startPos = client.player.getBlockPos().down();
        Direction facing = client.player.getHorizontalFacing();
        MinecraftServer server = client.getServer();

        Direction leftDir = facing.rotateYCounterclockwise();
        int dx = leftDir.getOffsetX();
        int dz = leftDir.getOffsetZ();
        double newX = client.player.getX() + dx;
        double newY = client.player.getY();
        double newZ = client.player.getZ() + dz;

        int slotCount = row.getSlotCount();
        startSuppression(4000);

        if (server != null) {
            // singleplayer: direct forced server placement without physics or neighbor updates
            RegistryKey<World> key = client.world.getRegistryKey();
            server.execute(() -> {
                ServerWorld serverWorld = server.getWorld(key);
                if (serverWorld != null) {
                    for (int i = 0; i < slotCount; i++) {
                        String itemId = row.getSlot(i);
                        BlockState state = getBlockStateForPlacement(itemId);
                        if (state != null) {
                            BlockPos targetPos = startPos.offset(facing, i);

                            if (state.getBlock() instanceof DoorBlock) {
                                serverWorld.setBlockState(targetPos, state.with(DoorBlock.HALF, DoubleBlockHalf.LOWER).with(DoorBlock.FACING, facing), SINGLEPLAYER_BLOCK_FLAGS);
                                serverWorld.setBlockState(targetPos.up(), state.with(DoorBlock.HALF, DoubleBlockHalf.UPPER).with(DoorBlock.FACING, facing), SINGLEPLAYER_BLOCK_FLAGS);
                            } else if (state.getBlock() instanceof TallPlantBlock) {
                                serverWorld.setBlockState(targetPos, state.with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER), SINGLEPLAYER_BLOCK_FLAGS);
                                serverWorld.setBlockState(targetPos.up(), state.with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER), SINGLEPLAYER_BLOCK_FLAGS);
                            } else if (state.getBlock() instanceof BedBlock) {
                                serverWorld.setBlockState(targetPos, state.with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, facing), SINGLEPLAYER_BLOCK_FLAGS);
                                serverWorld.setBlockState(targetPos.offset(facing), state.with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, facing), SINGLEPLAYER_BLOCK_FLAGS);
                            } else {
                                serverWorld.setBlockState(targetPos, state, SINGLEPLAYER_BLOCK_FLAGS);
                            }
                        }
                    }
                }
                ServerPlayerEntity serverPlayer = server.getPlayerManager().getPlayer(client.player.getUuid());
                if (serverPlayer != null) {
                    serverPlayer.requestTeleport(newX, newY, newZ);
                }
            });

            // update client world prediction
            for (int i = 0; i < slotCount; i++) {
                String itemId = row.getSlot(i);
                BlockState state = getBlockStateForPlacement(itemId);
                if (state != null) {
                    BlockPos targetPos = startPos.offset(facing, i);
                    if (state.getBlock() instanceof DoorBlock) {
                        client.world.setBlockState(targetPos, state.with(DoorBlock.HALF, DoubleBlockHalf.LOWER).with(DoorBlock.FACING, facing), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                        client.world.setBlockState(targetPos.up(), state.with(DoorBlock.HALF, DoubleBlockHalf.UPPER).with(DoorBlock.FACING, facing), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    } else if (state.getBlock() instanceof TallPlantBlock) {
                        client.world.setBlockState(targetPos, state.with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                        client.world.setBlockState(targetPos.up(), state.with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    } else if (state.getBlock() instanceof BedBlock) {
                        client.world.setBlockState(targetPos, state.with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, facing), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                        client.world.setBlockState(targetPos.offset(facing), state.with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, facing), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    } else {
                        client.world.setBlockState(targetPos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    }
                }
            }
            client.player.setPosition(newX, newY, newZ);
            client.player.sendMessage(Text.translatable("palettes.itemorganizer.palette_placed"), true);
            SoundHelper.playChime();
            return;
        }

        // multiplayer: paced queue using native setblock strict mode without support blocks
        actionQueue.clear();
        pendingCompletionNotice = true;

        for (int i = 0; i < slotCount; i++) {
            String itemId = row.getSlot(i);
            BlockState clientState = getBlockStateForPlacement(itemId);
            if (clientState == null) continue;

            BlockPos targetPos = startPos.offset(facing, i);
            String cleanId = itemId.trim();

            actionQueue.add(() -> {
                if (client.getNetworkHandler() != null) {
                    sendSetblockCommand(client, targetPos, cleanId, clientState, facing);
                }
                if (clientState.getBlock() instanceof DoorBlock) {
                    client.world.setBlockState(targetPos, clientState.with(DoorBlock.HALF, DoubleBlockHalf.LOWER).with(DoorBlock.FACING, facing), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    client.world.setBlockState(targetPos.up(), clientState.with(DoorBlock.HALF, DoubleBlockHalf.UPPER).with(DoorBlock.FACING, facing), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                } else if (clientState.getBlock() instanceof TallPlantBlock) {
                    client.world.setBlockState(targetPos, clientState.with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    client.world.setBlockState(targetPos.up(), clientState.with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                } else if (clientState.getBlock() instanceof BedBlock) {
                    client.world.setBlockState(targetPos, clientState.with(BedBlock.PART, BedPart.FOOT).with(BedBlock.FACING, facing), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                    client.world.setBlockState(targetPos.offset(facing), clientState.with(BedBlock.PART, BedPart.HEAD).with(BedBlock.FACING, facing), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                } else {
                    client.world.setBlockState(targetPos, clientState, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
                }
            });
        }

        // teleport player at the end of placement queue
        actionQueue.add(() -> {
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendChatCommand(
                        String.format(Locale.ROOT, "tp @s ~%d ~ ~%d", dx, dz)
                );
            }
            client.player.setPosition(newX, newY, newZ);
        });
    }

    private static void sendSetblockCommand(MinecraftClient client, BlockPos pos, String itemId, BlockState state, Direction facing) {
        if (client == null || client.getNetworkHandler() == null || state == null) return;

        if (state.getBlock() instanceof DoorBlock) {
            client.getNetworkHandler().sendChatCommand(
                    String.format(Locale.ROOT, "setblock %d %d %d %s[half=lower,facing=%s] strict",
                            pos.getX(), pos.getY(), pos.getZ(), itemId, facing.asString())
            );
            client.getNetworkHandler().sendChatCommand(
                    String.format(Locale.ROOT, "setblock %d %d %d %s[half=upper,facing=%s] strict",
                            pos.getX(), pos.getY() + 1, pos.getZ(), itemId, facing.asString())
            );
        } else if (state.getBlock() instanceof TallPlantBlock) {
            client.getNetworkHandler().sendChatCommand(
                    String.format(Locale.ROOT, "setblock %d %d %d %s[half=lower] strict",
                            pos.getX(), pos.getY(), pos.getZ(), itemId)
            );
            client.getNetworkHandler().sendChatCommand(
                    String.format(Locale.ROOT, "setblock %d %d %d %s[half=upper] strict",
                            pos.getX(), pos.getY() + 1, pos.getZ(), itemId)
            );
        } else if (state.getBlock() instanceof CropBlock) {
            client.getNetworkHandler().sendChatCommand(
                    String.format(Locale.ROOT, "setblock %d %d %d %s[age=7] strict",
                            pos.getX(), pos.getY(), pos.getZ(), itemId)
            );
        } else if (state.getBlock() instanceof NetherWartBlock) {
            client.getNetworkHandler().sendChatCommand(
                    String.format(Locale.ROOT, "setblock %d %d %d %s[age=3] strict",
                            pos.getX(), pos.getY(), pos.getZ(), itemId)
            );
        } else {
            client.getNetworkHandler().sendChatCommand(
                    String.format(Locale.ROOT, "setblock %d %d %d %s strict",
                            pos.getX(), pos.getY(), pos.getZ(), itemId)
            );
        }
    }
}
