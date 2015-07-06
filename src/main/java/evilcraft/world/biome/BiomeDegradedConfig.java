package evilcraft.world.biome;

import evilcraft.EvilCraft;
import evilcraft.Reference;
import org.cyclops.cyclopscore.config.extendedconfig.BiomeConfig;

/**
 * Config for {@link BiomeDegraded}.
 * @author rubensworks
 *
 */
public class BiomeDegradedConfig extends BiomeConfig {
    
    /**
     * The unique instance.
     */
    public static BiomeDegradedConfig _instance;

    /**
     * Make a new instance.
     */
    public BiomeDegradedConfig() {
        super(
                EvilCraft._instance,
            Reference.BIOME_DEGRADED,
            "biomeDegraded",
            null,
            BiomeDegraded.class
        );
    }
    
    @Override
    public void registerBiomeDictionary() {
        // Do not register this biome in the dictionary to avoid worldgen with this.
    }
    
}
