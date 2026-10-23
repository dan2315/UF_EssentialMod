package dod.unrealfrontiers.ufessential.maintenance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import dod.unrealfrontiers.ufessential.UFEssential;
import dod.unrealfrontiers.ufessential.UFServerConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Maintenance state, kept in ufessential_maintenance.json in the server folder and written on every change.
 * It used to live in the world's saved data, which is never written on servers that turn world saving off
 * (the Zaruba gamemode does), so maintenance silently reset to off on restart.
 */
public class MaintenanceManager {
    private static final String FILE_NAME = "ufessential_maintenance.json";
    /** Name of the old world saved data, read once if there is no JSON file yet. */
    private static final String LEGACY_DATA_NAME = "ufessential_maintenance";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static MaintenanceManager INSTANCE = null;
    /** The MOTD from server.properties while the maintenance one is shown. */
    private static String normalMotd = null;

    private final transient MinecraftServer server;
    private final transient Path file;
    private boolean maintenanceMode = false;
    private final List<String> allowedPlayers = new ArrayList<>();

    /** What the JSON file holds. */
    private record State(boolean maintenanceMode, List<String> allowedPlayers) {}

    private MaintenanceManager(MinecraftServer server) {
        this.server = server;
        this.file = server.getServerDirectory().resolve(FILE_NAME);
    }

    public static MaintenanceManager get(MinecraftServer server) {
        if (INSTANCE == null || INSTANCE.server != server) {
            INSTANCE = new MaintenanceManager(server);
            INSTANCE.load();
        }
        return INSTANCE;
    }

    private void load() {
        if (Files.exists(file)) {
            try {
                State state = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), State.class);
                if (state != null) {
                    maintenanceMode = state.maintenanceMode();
                    if (state.allowedPlayers() != null) {
                        allowedPlayers.addAll(state.allowedPlayers());
                    }
                }
            } catch (IOException | JsonParseException e) {
                UFEssential.LOGGER.error("[UF] Could not read {}, maintenance mode is off", file, e);
            }
            return;
        }

        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        LegacyData legacy = overworld == null ? null
                : overworld.getDataStorage().get(new SavedData.Factory<>(LegacyData::new, LegacyData::new, null), LEGACY_DATA_NAME);
        if (legacy != null) {
            maintenanceMode = legacy.maintenanceMode;
            allowedPlayers.addAll(legacy.allowedPlayers);
            UFEssential.LOGGER.info("[UF] Moved maintenance state from the world data to {}", file);
            save();
        }
    }

    private void save() {
        try {
            Path tmp = file.resolveSibling(FILE_NAME + ".tmp");
            Files.writeString(tmp, GSON.toJson(new State(maintenanceMode, allowedPlayers)), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            UFEssential.LOGGER.error("[UF] Could not save {}", file, e);
        }
    }

    public boolean isMaintenanceModeEnabled() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean enabled) {
        if (this.maintenanceMode != enabled) {
            this.maintenanceMode = enabled;
            save();
        }
    }

    public boolean toggleMaintenanceMode() {
        setMaintenanceMode(!maintenanceMode);
        return maintenanceMode;
    }

    public boolean isPlayerAllowed(String playerName) {
        return allowedPlayers.contains(playerName.toLowerCase());
    }

    public void addAllowedPlayer(String playerName) {
        if (!isPlayerAllowed(playerName.toLowerCase())) {
            allowedPlayers.add(playerName.toLowerCase());
            save();
        }
    }

    public void removeAllowedPlayer(String playerName) {
        if (allowedPlayers.remove(playerName.toLowerCase())) {
            save();
        }
    }

    public List<String> getAllowedPlayers() {
        return new ArrayList<>(allowedPlayers);
    }

    /** Whether this player may be on the server right now: maintenance off, an op, or on the list. */
    public boolean canJoin(MinecraftServer server, GameProfile profile) {
        return !maintenanceMode
                || server.getPlayerList().isOp(profile)
                || isPlayerAllowed(profile.getName());
    }

    /** Disconnects online players who may not stay during maintenance. Returns how many were kicked. */
    public int kickDisallowedPlayers(MinecraftServer server) {
        int kicked = 0;
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            if (!canJoin(server, player.getGameProfile())) {
                player.connection.disconnect(getMaintenanceMessage());
                UFEssential.LOGGER.info("Player {} was disconnected due to maintenance mode being enabled", player.getName().getString());
                kicked++;
            }
        }
        return kicked;
    }

    /**
     * Shows the maintenance MOTD in the server list while maintenance is on and puts the normal one back when it's off.
     * Only changes the running server; server.properties stays as it is.
     */
    public void applyMotd(MinecraftServer server) {
        String maintenanceMotd = UFServerConfig.MAINTENANCE_MOTD.get();
        if (maintenanceMode && !maintenanceMotd.isEmpty()) {
            if (normalMotd == null) {
                normalMotd = server.getMotd();
            }
            server.setMotd(maintenanceMotd);
        } else if (normalMotd != null) {
            server.setMotd(normalMotd);
            normalMotd = null;
        }
        server.invalidateStatus();
    }

    public Component getMaintenanceMessage() {
        return Component.translatable("message.ufessential.maintenance");
    }

    /** Reads the old world saved data; never written any more. */
    private static final class LegacyData extends SavedData {
        private boolean maintenanceMode;
        private final List<String> allowedPlayers = new ArrayList<>();

        LegacyData() {
        }

        LegacyData(CompoundTag tag, HolderLookup.Provider registries) {
            maintenanceMode = tag.getBoolean("maintenanceMode");
            ListTag list = tag.getList("allowedPlayers", 8); // 8 is the NBT type ID for String
            for (int i = 0; i < list.size(); i++) {
                allowedPlayers.add(list.getString(i));
            }
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            return tag;
        }
    }
}
