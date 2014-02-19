package evilcraft.blocks;

import evilcraft.Reference;
import evilcraft.api.config.BlockConfig;

/**
 * Config for the {@link DarkBlock}.
 * @author rubensworks
 *
 */
public class DarkBlockConfig extends BlockConfig {
    
    /**
     * The unique instance.
     */
    public static DarkBlockConfig _instance;

    /**
     * Make a new instance.
     */
    public DarkBlockConfig() {
        super(
            "darkBlock",
            null,
            DarkBlock.class
        );
    }
    
}
