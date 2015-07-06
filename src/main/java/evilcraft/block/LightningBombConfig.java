package evilcraft.block;

import evilcraft.EvilCraft;
import org.cyclops.cyclopscore.config.extendedconfig.BlockConfig;

/**
 * Config for the {@link LightningBomb}.
 * @author rubensworks
 *
 */
public class LightningBombConfig extends BlockConfig {
    
    /**
     * The unique instance.
     */
    public static LightningBombConfig _instance;

    /**
     * Make a new instance.
     */
    public LightningBombConfig() {
        super(
                EvilCraft._instance,
        	true,
            "lightningBomb",
            null,
            LightningBomb.class
        );
    }
    
}
