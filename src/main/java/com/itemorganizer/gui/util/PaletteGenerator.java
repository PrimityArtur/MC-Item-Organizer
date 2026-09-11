package com.itemorganizer.gui.util;

import com.itemorganizer.gui.palette.CreatePaletteState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

// generates perceptual palette gradients using oklch anchor interpolation
public class PaletteGenerator {

    public static class Candidate {
        public final String itemId;
        public final double[] oklab;
        public final double L;
        public final double a;
        public final double b;

        public Candidate(String itemId, double[] oklab) {
            this.itemId = itemId;
            this.oklab = oklab;
            this.L = oklab[0];
            this.a = oklab[1];
            this.b = oklab[2];
        }
    }

    // hard blacklist of patterns/substrings: items matching these will never be evaluated in palettes
    public static final List<String> PALETTE_BLACKLIST = BlockPropertyHelper.PALETTE_BLACKLIST;

    public static boolean isBlacklisted(String itemId) {
        return BlockPropertyHelper.isPaletteBlacklisted(itemId);
    }

    private static String lastFilterKey = "";
    private static List<Candidate> cachedCandidates = null;

    public static synchronized void clearCache() {
        lastFilterKey = "";
        cachedCandidates = null;
    }

    public static void generate(CreatePaletteState state) {
        if (state == null) return;
        List<Candidate> candidates = getFilteredCandidates(state);
        if (candidates.isEmpty()) {
            candidates = getAllCandidates();
        }

        int slotCount = state.getSlotCount();
        List<String> input = state.getInputSlots();

        // locate user anchors
        List<int[]> anchors = new ArrayList<>();
        for (int i = 0; i < slotCount && i < input.size(); i++) {
            String id = input.get(i);
            if (id != null && !id.trim().isEmpty()) {
                anchors.add(new int[]{i, ItemColorHelper.getItemColor(id)});
            }
        }

        for (int r = 0; r < state.getResultRowCount(); r++) {
            int[] seeds = state.getSeedOffsets(r);
            List<String> row = generateSingleRow(slotCount, input, anchors, candidates, seeds);
            state.setResultRow(r, row);
        }
    }

    private static List<String> generateSingleRow(int slotCount, List<String> input, List<int[]> anchors,
                                                  List<Candidate> candidates, int[] seeds) {
        List<String> result = new ArrayList<>(slotCount);
        if (anchors.isEmpty()) {
            for (int i = 0; i < slotCount; i++) {
                result.add("");
            }
            return result;
        }

        // single anchor harmonic shading
        if (anchors.size() == 1) {
            int anchorIdx = anchors.get(0)[0];
            String anchorId = input.get(anchorIdx);
            double[] anchorOk = ItemColorHelper.srgbToOklab(anchors.get(0)[1]);
            double[] anchorLch = ItemColorHelper.oklabToOklch(anchorOk[0], anchorOk[1], anchorOk[2]);

            String prev1 = null;
            String prev2 = null;

            for (int s = 0; s < slotCount; s++) {
                if (s == anchorIdx) {
                    result.add(anchorId);
                    prev2 = prev1;
                    prev1 = anchorId;
                } else {
                    double offsetT = (double) (s - anchorIdx) / (Math.max(1, slotCount - 1));
                    double targetL = Math.max(0.08, Math.min(0.96, anchorLch[0] + (offsetT * 0.35)));
                    double[] targetOk = ItemColorHelper.oklchToOklab(targetL, anchorLch[1], anchorLch[2]);
                    int seed = (s < seeds.length) ? seeds[s] : 0;
                    String best = pickBestCandidate(targetOk[0], targetOk[1], targetOk[2], candidates, seed, prev1, prev2);
                    result.add(best);
                    prev2 = prev1;
                    prev1 = best;
                }
            }
            return result;
        }

        // multi-anchor piecewise interpolation in oklch space
        String prev1 = null;
        String prev2 = null;

        for (int s = 0; s < slotCount; s++) {
            String explicitInput = (s < input.size()) ? input.get(s) : "";
            if (explicitInput != null && !explicitInput.trim().isEmpty()) {
                result.add(explicitInput);
                prev2 = prev1;
                prev1 = explicitInput;
                continue;
            }

            double[] targetOk;
            if (s <= anchors.get(0)[0]) {
                targetOk = ItemColorHelper.srgbToOklab(anchors.get(0)[1]);
            } else if (s >= anchors.get(anchors.size() - 1)[0]) {
                targetOk = ItemColorHelper.srgbToOklab(anchors.get(anchors.size() - 1)[1]);
            } else {
                int leftAnchor = 0;
                int rightAnchor = 1;
                for (int i = 0; i < anchors.size() - 1; i++) {
                    if (s > anchors.get(i)[0] && s < anchors.get(i + 1)[0]) {
                        leftAnchor = i;
                        rightAnchor = i + 1;
                        break;
                    }
                }
                int idx0 = anchors.get(leftAnchor)[0];
                int idx1 = anchors.get(rightAnchor)[0];
                double[] ok0 = ItemColorHelper.srgbToOklab(anchors.get(leftAnchor)[1]);
                double[] ok1 = ItemColorHelper.srgbToOklab(anchors.get(rightAnchor)[1]);

                double t = (double) (s - idx0) / (idx1 - idx0);

                double[] lch0 = ItemColorHelper.oklabToOklch(ok0[0], ok0[1], ok0[2]);
                double[] lch1 = ItemColorHelper.oklabToOklch(ok1[0], ok1[1], ok1[2]);

                double tL = (1.0 - t) * lch0[0] + t * lch1[0];
                double tC = (1.0 - t) * lch0[1] + t * lch1[1];

                double h0 = lch0[2];
                if (h0 < 0) h0 += 2 * Math.PI;
                double h1 = lch1[2];
                if (h1 < 0) h1 += 2 * Math.PI;

                double th;
                boolean neutral0 = lch0[1] < 0.03;
                boolean neutral1 = lch1[1] < 0.03;

                if (neutral0 && neutral1) {
                    th = 0.0;
                } else if (neutral0) {
                    th = h1;
                } else if (neutral1) {
                    th = h0;
                } else {
                    double dh = h1 - h0;
                    while (dh > Math.PI) dh -= 2 * Math.PI;
                    while (dh < -Math.PI) dh += 2 * Math.PI;
                    th = h0 + t * dh;
                }

                targetOk = ItemColorHelper.oklchToOklab(tL, tC, th);
            }

            int seed = (s < seeds.length) ? seeds[s] : 0;
            String best = pickBestCandidate(targetOk[0], targetOk[1], targetOk[2], candidates, seed, prev1, prev2);
            result.add(best);
            prev2 = prev1;
            prev1 = best;
        }

        return result;
    }

    private static String pickBestCandidate(double tL, double ta, double tb, List<Candidate> candidates, int seedOffset,
                                            String prev1, String prev2) {
        if (candidates.isEmpty()) return "minecraft:stone";

        record Scored(Candidate candidate, double distSq) {}
        List<Scored> scored = new ArrayList<>(candidates.size());
        for (Candidate c : candidates) {
            double dL = c.L - tL;
            double da = c.a - ta;
            double db = c.b - tb;
            // weighted perceptual metric penalizing chroma and hue deviations
            double distSq = dL * dL + 4.0 * (da * da + db * db);

            // repetition penalty to prevent identical blocks from stagnating the gradient
            if (prev1 != null && prev1.equals(c.itemId)) {
                distSq += 0.0028;
            }
            if (prev2 != null && prev2.equals(c.itemId)) {
                distSq += 0.0065;
            }

            scored.add(new Scored(c, distSq));
        }

        scored.sort(Comparator.comparingDouble(s -> s.distSq));

        // select within close perceptual cluster to prevent distant color jumps
        double minDistSq = scored.get(0).distSq;
        double maxTolSq = minDistSq + 0.0035;
        double relTolSq = minDistSq * 1.6;

        List<Scored> cluster = new ArrayList<>();
        for (Scored s : scored) {
            if (s.distSq <= maxTolSq || s.distSq <= relTolSq) {
                cluster.add(s);
                if (cluster.size() >= 5) break;
            } else {
                break;
            }
        }

        int rank = Math.abs(seedOffset) % cluster.size();
        return cluster.get(rank).candidate.itemId;
    }

    private static synchronized List<Candidate> getFilteredCandidates(CreatePaletteState state) {
        String key = state.isFilterCube() + "_" + state.isFilterSolid() + "_" +
                state.isFilterTransparent() + "_" + state.isFilterUniformTexture() + "_" +
                state.isFilterOres() + "_" + state.isFilterGlazed() + "_" + state.isFilterLights();

        if (cachedCandidates != null && key.equals(lastFilterKey)) {
            return cachedCandidates;
        }

        List<Candidate> all = getAllCandidates();
        List<Candidate> filtered = new ArrayList<>();

        for (Candidate c : all) {
            if (isBlacklisted(c.itemId)) continue;
            if (!BlockPropertyHelper.isBuildingBlock(c.itemId)) continue;
            if (state.isFilterCube() && !BlockPropertyHelper.isFullCube(c.itemId)) continue;
            if (state.isFilterSolid() && !BlockPropertyHelper.isSolidCollision(c.itemId)) continue;
            if (state.isFilterTransparent()) {
                if (!BlockPropertyHelper.isTransparent(c.itemId)) continue;
            } else {
                if (BlockPropertyHelper.isTransparent(c.itemId)) continue;
            }
            if (state.isFilterUniformTexture() && !BlockPropertyHelper.hasUniformTexture(c.itemId)) continue;
            if (!state.isFilterOres() && BlockPropertyHelper.isOre(c.itemId)) continue;
            if (!state.isFilterGlazed() && BlockPropertyHelper.isGlazedTerracotta(c.itemId)) continue;
            if (!state.isFilterLights() && BlockPropertyHelper.isLightBlock(c.itemId)) continue;
            filtered.add(c);
        }

        if (filtered.isEmpty()) {
            for (Candidate c : all) {
                if (isBlacklisted(c.itemId)) continue;
                if (!BlockPropertyHelper.isBuildingBlock(c.itemId)) continue;
                if (state.isFilterTransparent()) {
                    if (BlockPropertyHelper.isTransparent(c.itemId)) filtered.add(c);
                } else {
                    if (!BlockPropertyHelper.isTransparent(c.itemId)) filtered.add(c);
                }
            }
            if (filtered.isEmpty()) {
                filtered.addAll(all);
            }
        }

        lastFilterKey = key;
        cachedCandidates = filtered;
        return filtered;
    }

    private static List<Candidate> getAllCandidates() {
        Set<String> seen = new HashSet<>();
        List<Candidate> list = new ArrayList<>();

        // 1. inspect registered items
        try {
            for (Identifier id : Registries.ITEM.getIds()) {
                String idStr = id.toString();
                if (!isBlacklisted(idStr) && BlockPropertyHelper.isBuildingBlock(idStr)) {
                    seen.add(idStr);
                    int rgb = ItemColorHelper.getItemColor(idStr);
                    list.add(new Candidate(idStr, ItemColorHelper.srgbToOklab(rgb)));
                }
            }
        } catch (Throwable ignored) {
        }

        // 2. inspect texture_colors dynamically loaded entries
        try {
            for (Map.Entry<String, Integer> entry : ItemColorHelper.getTextureColors().entrySet()) {
                String idStr = entry.getKey();
                if (!idStr.startsWith("minecraft:")) {
                    idStr = "minecraft:" + idStr;
                }
                if (!isBlacklisted(idStr) && seen.add(idStr) && BlockPropertyHelper.isBuildingBlock(idStr)) {
                    list.add(new Candidate(idStr, ItemColorHelper.srgbToOklab(entry.getValue())));
                }
            }
        } catch (Throwable ignored) {
        }


        // 3. default core block catalogue for test or fallback
        if (list.size() < 10) {
            String[] coreBlocks = {
                    "minecraft:white_concrete", "minecraft:light_gray_concrete", "minecraft:gray_concrete",
                    "minecraft:black_concrete", "minecraft:brown_concrete", "minecraft:red_concrete",
                    "minecraft:orange_concrete", "minecraft:yellow_concrete", "minecraft:lime_concrete",
                    "minecraft:green_concrete", "minecraft:cyan_concrete", "minecraft:light_blue_concrete",
                    "minecraft:blue_concrete", "minecraft:purple_concrete", "minecraft:magenta_concrete",
                    "minecraft:pink_concrete", "minecraft:white_wool", "minecraft:light_gray_wool",
                    "minecraft:gray_wool", "minecraft:black_wool", "minecraft:brown_wool",
                    "minecraft:red_wool", "minecraft:orange_wool", "minecraft:yellow_wool",
                    "minecraft:lime_wool", "minecraft:green_wool", "minecraft:cyan_wool",
                    "minecraft:light_blue_wool", "minecraft:blue_wool", "minecraft:purple_wool",
                    "minecraft:magenta_wool", "minecraft:pink_wool", "minecraft:white_terracotta",
                    "minecraft:orange_terracotta", "minecraft:magenta_terracotta", "minecraft:light_blue_terracotta",
                    "minecraft:yellow_terracotta", "minecraft:lime_terracotta", "minecraft:pink_terracotta",
                    "minecraft:gray_terracotta", "minecraft:light_gray_terracotta", "minecraft:cyan_terracotta",
                    "minecraft:purple_terracotta", "minecraft:blue_terracotta", "minecraft:brown_terracotta",
                    "minecraft:green_terracotta", "minecraft:red_terracotta", "minecraft:black_terracotta",
                    "minecraft:sand", "minecraft:sandstone", "minecraft:cut_sandstone",
                    "minecraft:stone", "minecraft:andesite", "minecraft:diorite", "minecraft:granite",
                    "minecraft:deepslate", "minecraft:cobblestone", "minecraft:bricks", "minecraft:nether_bricks",
                    "minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:birch_planks",
                    "minecraft:jungle_planks", "minecraft:acacia_planks", "minecraft:dark_oak_planks",
                    "minecraft:mangrove_planks", "minecraft:cherry_planks", "minecraft:bamboo_planks",
                    "minecraft:moss_block", "minecraft:copper_block", "minecraft:weathered_copper",
                    "minecraft:glass", "minecraft:white_stained_glass", "minecraft:red_stained_glass",
                    "minecraft:blue_stained_glass", "minecraft:yellow_stained_glass", "minecraft:lime_stained_glass"
            };
            for (String b : coreBlocks) {
                if (seen.add(b)) {
                    int rgb = ItemColorHelper.getItemColor(b);
                    list.add(new Candidate(b, ItemColorHelper.srgbToOklab(rgb)));
                }
            }
        }

        return list;
    }
}
