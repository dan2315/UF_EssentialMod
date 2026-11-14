package dod.unrealfrontiers.ufessential.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dod.unrealfrontiers.ufessential.blocks.ModBlockEntities;
import dod.unrealfrontiers.ufessential.blocks.Rift.RiftBlock;
import dod.unrealfrontiers.ufessential.blocks.Rift.RiftBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Draws a rift: a pixelated tear (cells of 1/8 block, so its jagged edge steps like Minecraft blocks) with a dark starry
 * void inside, a bright edge in the rift's gradient (mint at the edge, cyan at 55 %, blue at the core), a soft halo,
 * cracks running out into the air, and bits of ground and stone pulled in. The rift itself is solid (you don't see
 * through it); only the halo and the cracks glow additively, like lightning. Closed to a thin crack when nobody is near.
 * It always turns its face to the viewer (around the upright axis), so it looks the same from every side.
 */
public class RiftRenderer implements BlockEntityRenderer<RiftBlockEntity> {
    private static final float CELL = 0.125f;
    private static final float GLOW_OFFSET = 0.03f;
    private static final BlockState[] FRAGMENTS = {
            Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.STONE.defaultBlockState(),
            Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(),
            Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.GRASS_BLOCK.defaultBlockState()};

    /** Cracks per rift (position and size): segments u0 v0 u1 v1 alpha, drawn for a fully open rift. */
    private final Map<Long, List<float[]>> cracks = new HashMap<>();

    public RiftRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.RIFT_BE.get(), RiftRenderer::new);
    }

    @Override
    public void render(RiftBlockEntity rift, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Level level = rift.getLevel();
        if (level == null) return;
        float time = ((level.getGameTime() % 72000L) + partialTick) / 20f;
        float open = Mth.lerp(partialTick, rift.openBefore, rift.open);
        float w = rift.width(), h = rift.height(), halfW = w / 2f;
        long seed = rift.getBlockPos().asLong();

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        // Face the camera: the rift's plane is x-y, so its front (+z) turns towards the viewer
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        RiftBlockEntity.viewer = camera;
        double dx = camera.x - (rift.getBlockPos().getX() + 0.5), dz = camera.z - (rift.getBlockPos().getZ() + 0.5);
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(dx, dz)));
        Matrix4f m = pose.last().pose();
        VertexConsumer dark = buffers.getBuffer(RenderType.debugQuads());
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());

        int cols = Math.round(w / CELL), rows = Math.round(h / CELL);
        for (int j = 0; j < rows; j++) {
            float v0 = j * CELL, v1 = v0 + CELL;
            float y = ((v0 + v1) / 2 - h / 2) / (h / 2);
            float hw = RiftBlockEntity.halfWidth(y) * open;
            if (hw <= 0.0005f) continue;
            int band = j / 2; // the edge steps every two cells
            for (int i = 0; i < cols; i++) {
                float u0 = -halfW + i * CELL, u1 = u0 + CELL, uc = (u0 + u1) / 2;
                int side = uc < 0 ? 0 : 1;
                float jag = (noise(band * 0.45f + time * 0.6f, seed + side * 31L) - 0.5f) * 0.26f
                        + (hash(band, seed + side * 7L) - 0.5f) * 0.16f;
                float f = Math.abs(uc) / (halfW * hw) + jag;
                if (f < 1) {
                    // Solid, not see-through: the gradient laid over a deep navy void, bright at the edge
                    float t = Mth.clamp(1 - f, 0, 1);
                    float edge = smooth(0.7f, 1f, f);
                    float b = Mth.clamp(0.22f + 0.78f * edge + 0.08f * Mth.sin(time * 1.3f + uc * 3 + y * 4) * (1 - edge), 0, 1);
                    int c = gradient(t);
                    float r = Mth.lerp(b, 0.02f, red(c)), g = Mth.lerp(b, 0.04f, green(c)), bl = Mth.lerp(b, 0.14f, blue(c));
                    float star = hash(i * 7919L + j, seed + 5);
                    if (star < 0.035f && f < 0.8f) {
                        float twinkle = 0.5f + 0.5f * Mth.sin(time * (2 + star * 60) + star * 100);
                        r = Mth.lerp(0.75f * twinkle, r, 0.85f);
                        g = Mth.lerp(0.75f * twinkle, g, 0.97f);
                        bl = Mth.lerp(0.75f * twinkle, bl, 1f);
                    }
                    quadBoth(dark, m, u0, v0, u1, v1, r, g, bl, 1f);
                } else if (f < 1.24f) {
                    float a = (1.24f - f) / 0.24f * 0.45f;
                    quadGlow(glow, m, u0, v0, u1, v1, red(RiftBlockEntity.MINT), green(RiftBlockEntity.MINT), blue(RiftBlockEntity.MINT), a);
                }
            }
        }

        // Cracks in the air around it: they shrink with the rift
        for (float[] s : cracks(seed, w, h)) {
            float a = s[4] * open * (0.75f + 0.25f * Mth.sin(time * 2 + s[0] * 5));
            if (a < 0.02f) continue;
            line(glow, m, s[0] * open, s[1], s[2] * open, s[3], 0.035f, red(RiftBlockEntity.CYAN), green(RiftBlockEntity.CYAN), blue(RiftBlockEntity.CYAN), a);
        }

        // Bits of the ground pulled in, spinning, smaller as they fall in
        if (open > 0.3f) {
            BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
            for (int k = 0; k < FRAGMENTS.length; k++) {
                float ph = hash(k, seed + 99);
                float cycle = (time * 0.05f + ph) % 1f;
                float rad = Mth.lerp(cycle, 1f, 0.15f);
                float ang = ph * Mth.TWO_PI + time * (0.25f + ph * 0.2f);
                float pu = Mth.cos(ang) * (halfW + 1.6f) * rad;
                float pv = h / 2 + Mth.sin(ang) * (h / 2 + 0.8f) * rad;
                float pn = (hash(k, seed + 7) - 0.5f) * 1.4f * rad;
                float scale = 0.3f * (0.35f + 0.65f * rad) * (0.6f + 0.4f * hash(k, seed + 3)) * (open - 0.3f) / 0.7f;
                if (scale < 0.02f) continue;
                pose.pushPose();
                pose.translate(pu, pv, pn);
                pose.mulPose(Axis.YP.rotationDegrees(time * 40 + ph * 360));
                pose.mulPose(Axis.XP.rotationDegrees(time * 25 + ph * 180));
                pose.scale(scale, scale, scale);
                pose.translate(-0.5, -0.5, -0.5);
                blocks.renderSingleBlock(FRAGMENTS[k], pose, buffers, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
        }
        pose.popPose();
    }

    private List<float[]> cracks(long seed, float w, float h) {
        long key = seed * 31 + (long) w * 1009 + (long) h;
        return cracks.computeIfAbsent(key, k -> {
            List<float[]> out = new ArrayList<>();
            Random rnd = new Random(seed ^ 0x5EEDL);
            float halfW = w / 2;
            for (int c = 0; c < 12; c++) {
                float y0 = rnd.nextFloat() * 1.7f - 0.85f;
                float side = rnd.nextBoolean() ? 1 : -1;
                float u = side * halfW * RiftBlockEntity.halfWidth(y0) * 0.97f, v = h / 2 + y0 * h / 2;
                double ang = (side > 0 ? 0 : Math.PI) + side * y0 * 0.9 + (rnd.nextDouble() - 0.5) * 0.9;
                walk(out, rnd, u, v, ang, 0.9f, 3 + rnd.nextInt(4));
            }
            walk(out, rnd, 0, h, Math.PI / 2 + (rnd.nextDouble() - 0.5) * 0.6, 0.9f, 4); // from the top tip
            return out;
        });
    }

    private static void walk(List<float[]> out, Random rnd, float u, float v, double ang, float a, int segments) {
        for (int s = 0; s < segments; s++) {
            float len = 0.18f + rnd.nextFloat() * 0.32f;
            ang += (rnd.nextDouble() - 0.5) * 0.9;
            float nu = u + (float) Math.cos(ang) * len, nv = v + (float) Math.sin(ang) * len;
            out.add(new float[]{u, v, nu, nv, a});
            if (rnd.nextFloat() < 0.3f) {
                double b = ang + (rnd.nextBoolean() ? 0.8 : -0.8);
                out.add(new float[]{nu, nv, nu + (float) Math.cos(b) * len * 0.7f, nv + (float) Math.sin(b) * len * 0.7f, a * 0.6f});
            }
            u = nu;
            v = nv;
            a *= 0.75f;
        }
    }

    // ---- drawing ----

    /** A translucent cell seen from both sides (the render type doesn't cull). */
    private static void quadBoth(VertexConsumer vc, Matrix4f m, float u0, float v0, float u1, float v1, float r, float g, float b, float a) {
        vc.addVertex(m, u0, v0, 0).setColor(r, g, b, a);
        vc.addVertex(m, u1, v0, 0).setColor(r, g, b, a);
        vc.addVertex(m, u1, v1, 0).setColor(r, g, b, a);
        vc.addVertex(m, u0, v1, 0).setColor(r, g, b, a);
    }

    /** A glowing cell just in front of the void on each side, each facing out (this render type culls back faces). */
    private static void quadGlow(VertexConsumer vc, Matrix4f m, float u0, float v0, float u1, float v1, float r, float g, float b, float a) {
        float n = GLOW_OFFSET;
        vc.addVertex(m, u0, v0, n).setColor(r, g, b, a);
        vc.addVertex(m, u1, v0, n).setColor(r, g, b, a);
        vc.addVertex(m, u1, v1, n).setColor(r, g, b, a);
        vc.addVertex(m, u0, v1, n).setColor(r, g, b, a);
        vc.addVertex(m, u0, v0, -n).setColor(r, g, b, a);
        vc.addVertex(m, u0, v1, -n).setColor(r, g, b, a);
        vc.addVertex(m, u1, v1, -n).setColor(r, g, b, a);
        vc.addVertex(m, u1, v0, -n).setColor(r, g, b, a);
    }

    /** A thin glowing line in the rift's plane, fading towards its end. */
    private static void line(VertexConsumer vc, Matrix4f m, float u0, float v0, float u1, float v1, float width, float r, float g, float b, float a) {
        float du = u1 - u0, dv = v1 - v0, len = Mth.sqrt(du * du + dv * dv);
        if (len < 1e-4f) return;
        float pu = -dv / len * width / 2, pv = du / len * width / 2, a1 = a * 0.35f, n = GLOW_OFFSET;
        for (int face = 0; face < 2; face++) {
            float z = face == 0 ? n : -n;
            float s = face == 0 ? 1 : -1;
            vc.addVertex(m, u0 - pu * s, v0 - pv * s, z).setColor(r, g, b, a);
            vc.addVertex(m, u1 - pu * s, v1 - pv * s, z).setColor(r, g, b, a1);
            vc.addVertex(m, u1 + pu * s, v1 + pv * s, z).setColor(r, g, b, a1);
            vc.addVertex(m, u0 + pu * s, v0 + pv * s, z).setColor(r, g, b, a);
        }
    }

    /** Mint at 0 (the edge), cyan at 0.55, blue at 1 (the core). */
    private static int gradient(float t) {
        if (t <= 0.55f) return mix(RiftBlockEntity.MINT, RiftBlockEntity.CYAN, t / 0.55f);
        return mix(RiftBlockEntity.CYAN, RiftBlockEntity.BLUE, (t - 0.55f) / 0.45f);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255));
        int g = Math.round(Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255));
        int bl = Math.round(Mth.lerp(t, a & 255, b & 255));
        return (r << 16) | (g << 8) | bl;
    }

    private static float red(int c) { return ((c >> 16) & 255) / 255f; }
    private static float green(int c) { return ((c >> 8) & 255) / 255f; }
    private static float blue(int c) { return (c & 255) / 255f; }

    private static float smooth(float e0, float e1, float x) {
        float t = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static float hash(long a, long seed) {
        long x = a * 0x9E3779B97F4A7C15L + seed;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        return (x >>> 40) / (float) (1L << 24);
    }

    private static float noise(float x, long seed) {
        int i = Mth.floor(x);
        float f = x - i;
        f = f * f * (3 - 2 * f);
        return Mth.lerp(f, hash(i, seed), hash(i + 1L, seed));
    }

    @Override
    public AABB getRenderBoundingBox(RiftBlockEntity rift) {
        double r = rift.width() / 2.0 + 3;
        return new AABB(rift.getBlockPos()).inflate(r, 0, r).expandTowards(0, rift.height() + 2, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(RiftBlockEntity rift) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
