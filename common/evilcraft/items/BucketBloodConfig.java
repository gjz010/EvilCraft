package evilcraft.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import evilcraft.api.BucketHandler;
import evilcraft.api.config.ExtendedConfig;
import evilcraft.blocks.LiquidBlockBlood;
import evilcraft.liquids.Blood;

public class BucketBloodConfig extends ExtendedConfig {
    
    public static BucketBloodConfig _instance;

    public BucketBloodConfig() {
        super(
            4004,
            "Blood Bucket",
            "bucketblood",
            null,
            BucketBlood.class
        );
    }
    
    public void onRegistered() {
        FluidContainerRegistry.registerFluidContainer(
                FluidRegistry.getFluidStack(Blood.getInstance().getName(), FluidContainerRegistry.BUCKET_VOLUME),
                new ItemStack(BucketBlood.getInstance()),
                new ItemStack(Item.bucketEmpty)
        );
        BucketHandler.getInstance().buckets.put(LiquidBlockBlood.getInstance(), BucketBlood.getInstance());
    }
    
}
