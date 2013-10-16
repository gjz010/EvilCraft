package evilcraft.entities.item;

import evilcraft.api.config.ExtendedConfig;
import evilcraft.events.BloodBucketEventHook;
import net.minecraftforge.common.MinecraftForge;

public class BucketBloodConfig extends ExtendedConfig {
    
    public static BucketBloodConfig _instance;

    public BucketBloodConfig() {
        super(
            1,
            "Blood Bucket",
            "bucketblood",
            null,
            BucketBlood.class
        );
    }
    
    @Override
    public void onRegistered() {
        MinecraftForge.EVENT_BUS.register(new BloodBucketEventHook());
    }
    
}
