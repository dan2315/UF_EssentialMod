package dod.unrealfrontiers.ufessential.blocks.Rift;

import dod.unrealfrontiers.ufessential.blocks.ModBlockEntities;
import dod.unrealfrontiers.ufessential.blocks.PortalActions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A rift: a tear in space {@link #width} x {@link #height} blocks, standing on this block (its bottom centre), flat
 * along the block state's axis. Walking into it does the portal command ({@link PortalActions}). It opens wide when a
 * player comes near and closes to a crack when nobody is around (drawn by the client, {@code RiftRenderer}).
 */
public class RiftBlockEntity extends BlockEntity {
    /** The rift's colours: mint at the edge, cyan at 55 %, blue at the core. */
    public static final int MINT = 0x00FFA3, CYAN = 0x00E5FF, BLUE = 0x2979FF;

    private String command = "";
    private int width = 3, height = 5;

    /** Client only: how open it is now (0.18 a crack, 1 fully open) and before this tick, for smooth frames. */
    public float open = 0.18f, openBefore = 0.18f;

    public RiftBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RIFT_BE.get(), pos, state);
    }

    public String getCommand() { return command; }
    public int width() { return width; }
    public int height() { return height; }

    public void configure(String command, int width, int height) {
        this.command = command;
        this.width = width;
        this.height = height;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Half the rift's width at height {@code y} (-1 bottom tip .. 1 top tip), as a share of its full half width. */
    public static float halfWidth(float y) {
        float a = Math.abs(y);
        if (a >= 1) return 0;
        return (float) Math.pow(1 - Math.pow(a, 1.6), 0.7);
    }

    private boolean alongX() {
        return getBlockState().getValue(RiftBlock.AXIS) == Direction.Axis.X;
    }

    /**
     * Is this point inside the opening (the fully open shape, a little smaller than drawn)? The rift turns its face to
     * whoever looks at it, so the opening is round: the drawn outline spun around the upright axis.
     */
    public boolean inside(Vec3 p) {
        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5;
        double r = Math.hypot(p.x - cx, p.z - cz);
        double v = p.y - worldPosition.getY();
        float y = (float) ((v - height / 2.0) / (height / 2.0));
        float hw = halfWidth(y);
        return hw > 0 && r < Math.max(0.6, width / 2.0 * hw * 0.9);
    }

    public AABB area() {
        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5, y = worldPosition.getY();
        double hu = Math.max(0.6, width / 2.0);
        return new AABB(cx - hu, y, cz - hu, cx + hu, y + height, cz + hu);
    }

    /**
     * Walking into it does the command; standing in it doesn't. Who was already inside when it appeared (or its chunk
     * loaded), or came onto the server inside it (arrived from a rift elsewhere: their position here was in this one),
     * has to step out and in again: no ping-pong between two servers.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, RiftBlockEntity rift) {
        if (level.getGameTime() % 2 != 0 || rift.command.isEmpty()) return;
        java.util.Set<java.util.UUID> now = new java.util.HashSet<>();
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, rift.area())) {
            if (player.isSpectator() || !player.isAlive()) continue;
            if (!rift.inside(player.position().add(0, player.getBbHeight() * 0.5, 0))) continue;
            now.add(player.getUUID());
            boolean entered = rift.primed && !rift.insideBefore.contains(player.getUUID()) && player.tickCount > JOIN_GRACE_TICKS;
            if (entered) PortalActions.enterRift(player, rift.command);
        }
        rift.insideBefore.clear();
        rift.insideBefore.addAll(now);
        rift.primed = true;
    }

    /** A player who came onto the server less than this ago doesn't go through a rift they're standing in. */
    private static final int JOIN_GRACE_TICKS = 60;
    /** Server: who was inside at the last check; primed once the first check has seen who is already there. */
    private final java.util.Set<java.util.UUID> insideBefore = new java.util.HashSet<>();
    private boolean primed;

    /** Client: where the camera was at the last frame (the renderer sets it); rifts and their sparks face it. */
    public static volatile Vec3 viewer;

    /** Client: opens towards a nearby player, sparks drift into it, a low hum now and then. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, RiftBlockEntity rift) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + rift.height / 2.0, cz = pos.getZ() + 0.5;
        Player near = level.getNearestPlayer(cx, cy, cz, 14, false);
        rift.openBefore = rift.open;
        rift.open += ((near != null ? 1f : 0.18f) - rift.open) * 0.05f;

        RandomSource r = level.random;
        // The plane the rift is drawn in: facing the camera
        Vec3 eye = viewer;
        double yaw = eye == null ? (rift.alongX() ? 0 : Math.PI / 2) : Math.atan2(eye.x - cx, eye.z - cz);
        double ux = Math.cos(yaw), uz = -Math.sin(yaw), nx = Math.sin(yaw), nz = Math.cos(yaw);
        for (int i = 0; i < 2; i++) {
            float y = r.nextFloat() * 1.8f - 0.9f;
            float side = r.nextBoolean() ? 1 : -1;
            double u = side * rift.width / 2.0 * halfWidth(y) * rift.open * (1.05 + r.nextFloat() * 0.6);
            double v = rift.height / 2.0 + y * rift.height / 2.0;
            double n = (r.nextFloat() - 0.5) * 0.3;
            double x = cx + u * ux + n * nx, z = cz + u * uz + n * nz;
            double du = -u * 0.04;
            int color = r.nextInt(3) == 0 ? MINT : CYAN;
            level.addParticle(new DustParticleOptions(rgb(color), 0.9f), x, pos.getY() + v, z, du * ux, 0, du * uz);
        }
        if (r.nextInt(6) == 0) {
            double y = pos.getY() + r.nextFloat() * rift.height;
            level.addParticle(ParticleTypes.END_ROD, cx + (r.nextFloat() - 0.5) * 0.4, y, cz + (r.nextFloat() - 0.5) * 0.4, 0, 0.01, 0);
        }
        if (r.nextInt(100) == 0) {
            level.playLocalSound(cx, cy, cz, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.3f, 0.5f + r.nextFloat() * 0.2f, false);
        }
    }

    public static Vector3f rgb(int color) {
        return new Vector3f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("PortalCommand", command);
        tag.putInt("Width", width);
        tag.putInt("Height", height);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        command = tag.getString("PortalCommand");
        if (tag.contains("Width")) width = Math.max(1, tag.getInt("Width"));
        if (tag.contains("Height")) height = Math.max(2, tag.getInt("Height"));
    }

    /** The client needs the size (not the command). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Width", width);
        tag.putInt("Height", height);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
