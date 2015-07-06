package evilcraft.item;

import evilcraft.EvilCraft;
import org.cyclops.cyclopscore.config.extendedconfig.ItemConfig;

/**
 * Config for the {@link ResurgenceEgg}.
 * @author rubensworks
 *
 */
public class ResurgenceEggConfig extends ItemConfig {
    
    /**
     * The unique instance.
     */
    public static ResurgenceEggConfig _instance;

    /**
     * Make a new instance.
     */
    public ResurgenceEggConfig() {
        super(
                EvilCraft._instance,
        	true,
            "resurgenceEgg",
            null,
            ResurgenceEgg.class
        );
    }
    
    @Override
    public boolean isHardDisabled() {
    	return true;
    }
    
}
