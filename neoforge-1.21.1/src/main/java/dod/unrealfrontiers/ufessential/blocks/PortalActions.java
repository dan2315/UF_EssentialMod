package dod.unrealfrontiers.ufessential.blocks;

import dod.unrealfrontiers.ufessential.Network.ServerTransfer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What a portal does with a player, the same for the old crouch block and the rift: {@code redirect:<server>} sends them
 * to another server of the network, {@code tp:<dimension>} to that dimension's spawn.
 */
public final class PortalActions {
    private static final long COOLDOWN_MS = 3000;
    private static final Map<UUID, Long> RECENT = new ConcurrentHashMap<>();

    private PortalActions() {}

    public static void run(ServerPlayer player, String cmd) {
        if (cmd == null || cmd.isEmpty()) return;
        if (cmd.startsWith("redirect:")) {
            // To another server of the network; a refusal (down, full) shows above the hotbar
            String serverId = cmd.substring(9);
            ServerTransfer.send(player, serverId, failure -> player.displayClientMessage(failure, true));
        } else if (cmd.startsWith("tp:")) {
            ResourceKey<Level> targetDim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(cmd.substring(3)));
            ServerLevel targetLevel = player.server.getLevel(targetDim);
            if (targetLevel != null) {
                // Lands at the target dimension's world spawn
                player.changeDimension(new DimensionTransition(targetLevel, player, DimensionTransition.DO_NOTHING));
            }
        }
    }

    /** A player stepped into a rift: a flash of its colours and a sound, then the action (once per few seconds). */
    public static void enterRift(ServerPlayer player, String cmd) {
        long now = System.currentTimeMillis();
        Long last = RECENT.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_MS) return;
        RECENT.put(player.getUUID(), now);
        ServerLevel level = player.serverLevel();
        double x = player.getX(), y = player.getY() + 1, z = player.getZ();
        level.sendParticles(new DustParticleOptions(new Vector3f(0f, 1f, 0.64f), 1.6f), x, y, z, 40, 0.5, 0.9, 0.5, 0.02);
        level.sendParticles(new DustParticleOptions(new Vector3f(0.16f, 0.47f, 1f), 1.6f), x, y, z, 40, 0.5, 0.9, 0.5, 0.02);
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8f, 0.6f);
        run(player, cmd);
    }
}
