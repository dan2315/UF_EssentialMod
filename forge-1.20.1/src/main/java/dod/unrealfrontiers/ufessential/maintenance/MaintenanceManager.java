package dod.unrealfrontiers.ufessential.maintenance;

import dod.unrealfrontiers.ufessential.UFEssential;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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

    public MaintenanceManager(CompoundTag tag) {
        this.maintenanceMode = tag.getBoolean("maintenanceMode");
        
        ListTag listTag = tag.getList("allowedPlayers", 8); // 8 is the NBT type ID for String
        for (int i = 0; i < listTag.size(); i++) {
            this.allowedPlayers.add(listTag.getString(i));
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
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
                MaintenanceManager::new,
                MaintenanceManager::new,
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

    public Component getMaintenanceMessage() {
        return Component.translatable("message.ufessential.maintenance");
    }
} 