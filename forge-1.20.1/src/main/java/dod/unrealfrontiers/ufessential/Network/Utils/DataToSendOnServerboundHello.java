package dod.unrealfrontiers.ufessential.Network.Utils;

import dod.unrealfrontiers.ufessential.UFEssential;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//@OnlyIn(Dist.CLIENT)
public class DataToSendOnServerboundHello {

    private static final Map<UUID, String> dataToAppendPacket = new ConcurrentHashMap<>();

    public static void Add(UUID profileId, String data) {
        UFEssential.LOGGER.warn("For {} data set is {}", profileId, data);
        dataToAppendPacket.put(profileId, data);
    }

    public static String Consume(UUID profileId) {
        UFEssential.LOGGER.warn("Trying to get data for {}", profileId);
        var data = dataToAppendPacket.get(profileId);
        UFEssential.LOGGER.warn("Data is {}", data);
        dataToAppendPacket.remove(profileId);
        return data;
    }
}
