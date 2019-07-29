package org.cyclops.evilcraft.item;

import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.cyclops.cyclopscore.config.configurable.ConfigurableItem;
import org.cyclops.cyclopscore.config.extendedconfig.ExtendedConfig;
import org.cyclops.cyclopscore.config.extendedconfig.ItemConfig;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.cyclopscore.helper.ItemStackHelpers;
import org.cyclops.cyclopscore.helper.L10NHelpers;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A bowl of promises.
 * @author rubensworks
 *
 */
public class BowlOfPromises extends ConfigurableItem {

    private static BowlOfPromises _instance = null;
    public static final int ACTIVE_META = 2;

    /**
     * Get the unique instance.
     * @return The instance.
     */
    public static BowlOfPromises getInstance() {
        return _instance;
    }

    public BowlOfPromises(ExtendedConfig<ItemConfig> eConfig) {
        super(eConfig);
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
    }

    @Override
    public ItemStack getContainerItem(ItemStack itemStack) {
        if(itemStack.getItemDamage() >= ACTIVE_META) {
            return new ItemStack(this, 1, 1);
        }
        return super.getContainerItem(itemStack);
    }

    @Override
    public boolean hasContainerItem(ItemStack itemStack) {
        return itemStack.getItemDamage() >= ACTIVE_META;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void getSubItems(CreativeTabs creativeTabs, NonNullList<ItemStack> list) {
        if (!ItemStackHelpers.isValidCreativeTab(this, creativeTabs)) return;
        for(int i = 0; i < ACTIVE_META + ((BowlOfPromisesConfig) getConfig()).getTiers(); i++) {
            list.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public String getTranslationKey(ItemStack itemStack) {
        String suffix = "active";
        if(itemStack.getItemDamage() == 0) suffix = "dusted";
        if(itemStack.getItemDamage() == 1) suffix = "empty";
        return super.getTranslationKey(itemStack) + "." + suffix;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemStack, World world, List<String> list, ITooltipFlag flag) {
        super.addInformation(itemStack, world, list, flag);
        if(itemStack.getItemDamage() >= ACTIVE_META) {
            int tier = itemStack.getItemDamage() - ACTIVE_META;
            list.add(L10NHelpers.localize(super.getTranslationKey(itemStack) + ".strength") + " " +
                    (tier == 0 ? 0 : L10NHelpers.localize("enchantment.level." + tier)));
        }
    }

    @Nullable
    @Override
    @SideOnly(Side.CLIENT)
    public IItemColor getItemColorHandler() {
        return new ItemColor();
    }

    @SideOnly(Side.CLIENT)
    public static class ItemColor implements IItemColor {
        @Override
        public int colorMultiplier(ItemStack itemStack, int renderPass) {
            if(itemStack.getItemDamage() > 1 && renderPass == 0) {
                float division = (((float) ((BowlOfPromisesConfig._instance.getTiers() -
                        (itemStack.getItemDamage() - 2)) - 1) / 3) + 1);
                int channel = (int) (255 / division);
                return Helpers.RGBToInt(channel, channel, channel);
            }
            return -1;
        }
    }

}
