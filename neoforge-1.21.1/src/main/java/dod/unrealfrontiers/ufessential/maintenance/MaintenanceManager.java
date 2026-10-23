package dod.unrealfrontiers.ufessential.maintenance;

import dod.unrealfrontiers.ufessential.UFEssential;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceManager extends SavedData {
    private static final String DATA_NAME = "ufessential_maintenance";
    private boolean maintenanceMode = false;
    private final List<String> allowedPlayers = new ArrayList<>();
    
    private static MaintenanceManager INSTANCE = null;

    public MaintenanceManager() {
    }

    public MaintenanceManager(CompoundTag tag, HolderLookup.Provider registries) {
        this.maintenanceMode = tag.getBoolean("maintenanceMode");
        
        ListTag listTag = tag.getList("allowedPlayers", 8); // 8 is the NBT type ID for String
        for (int i = 0; i < listTag.size(); i++) {
            this.allowedPlayers.add(listTag.getString(i));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("maintenanceMode", maintenanceMode);
        
        ListTag listTag = new ListTag();
        for (String player : allowedPlayers) {
            listTag.add(StringTag.valueOf(player));
        }
        tag.put("allowedPlayers", listTag);
        
        return tag;
    }

    public static MaintenanceManager get(MinecraftServer server) {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        
        if (overworld != null) {
            INSTANCE = overworld.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MaintenanceManager::new, MaintenanceManager::new, null),
                DATA_NAME
            );
            return INSTANCE;
        }
        
        UFEssential.LOGGER.error("Failed to get overworld for maintenance manager");
        INSTANCE = new MaintenanceManager();
        return INSTANCE;
    }

    public boolean isMaintenanceModeEnabled() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean enabled) {
        if (this.maintenanceMode != enabled) {
            this.maintenanceMode = enabled;
            this.setDirty();
        }
    }

    public boolean toggleMaintenanceMode() {
        this.maintenanceMode = !this.maintenanceMode;
        this.setDirty();
        return this.maintenanceMode;
    }

    public boolean isPlayerAllowed(String playerName) {
        return allowedPlayers.contains(playerName.toLowerCase());
    }

    public void addAllowedPlayer(String playerName) {
        if (!isPlayerAllowed(playerName.toLowerCase())) {
            allowedPlayers.add(playerName.toLowerCase());
            this.setDirty();
        }
    }

    public void removeAllowedPlayer(String playerName) {
        if (allowedPlayers.remove(playerName.toLowerCase())) {
            this.setDirty();
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

    public Component getMaintenanceMessage() {
        return Component.translatable("message.ufessential.maintenance");
    }
} 