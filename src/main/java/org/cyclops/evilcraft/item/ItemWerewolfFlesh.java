package org.cyclops.evilcraft.item;

import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.cyclops.cyclopscore.helper.Helpers;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Random flesh drop from werewolves, gives some fine boosts at night.
 * @author rubensworks
 *
 */
public class ItemWerewolfFlesh extends Item {
    
    private static final int POISON_DURATION = 10;
    private static final int POWER_DURATION = 60;
    private static final int POWER_DURATION_BONUS = POWER_DURATION * 4;

    private final boolean humanoid;

    private boolean power = false;

    public ItemWerewolfFlesh(Item.Properties properties, boolean humanoid) {
        super(properties
                .food((new Food.Builder())
                        .nutrition(-5)
                        .saturationMod(0)
                        .alwaysEat()
                        .build()));
        this.humanoid = humanoid;
        if (this.humanoid) {
            MinecraftForge.EVENT_BUS.addListener(this::dropHumanoidFleshEvent);
        }
    }

    /**
     * The length of one Minecraft day.
     */
    public static final int MINECRAFT_DAY = 24000;

    /**
     * Check if it's day in this world.
     * @param world The world.
     * @return If it is day in the world, checked with the world time.
     */
    public static boolean isDay(World world) {
        return world.getDayTime() % MINECRAFT_DAY < MINECRAFT_DAY / 2;
    }
    
    private boolean isPower(World world) {
        return world == null ? power : (power = !isDay(world));
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public Rarity getRarity(ItemStack itemStack){
        return isHumanFlesh(itemStack) ? Rarity.RARE : Rarity.EPIC;
    }
    
    @Override
    public boolean isFoil(ItemStack itemStack){
        return isPower(null);
    }
    
    @Override
    public void inventoryTick(ItemStack itemStack, World world, Entity player, int par4, boolean par5) {
        isPower(world);
    }
    
    private int getPowerDuration(ItemStack itemStack) {
    	if(isHumanFlesh(itemStack)) {
    		return POWER_DURATION;
    	}
    	return POWER_DURATION_BONUS;
    }
    
    private boolean isHumanFlesh(ItemStack itemStack) {
    	return this.humanoid;
    }
    
    private boolean isOwnCanibal(ItemStack itemStack, PlayerEntity player) {
    	if(itemStack.hasTag()) {
			GameProfile profile = NBTUtil.readGameProfile(itemStack.getTag());
			return player.getGameProfile().equals(profile);
		}
    	return false;
    }
    
    @Override
    public ItemStack finishUsingItem(ItemStack itemStack, @Nullable World world, LivingEntity entity) {
        if(world != null && entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (player instanceof ServerPlayerEntity) {
                CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayerEntity) player, itemStack);
            }
            itemStack.shrink(1);
            if (isOwnCanibal(itemStack, player)) {
                if (!world.isClientSide()) {
                    player.addEffect(new EffectInstance(Effects.WITHER,
                            POISON_DURATION * 20, 1));
                    player.addEffect(new EffectInstance(Effects.BLINDNESS,
                            getPowerDuration(itemStack) * 20, 1));
                }
                world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.WOLF_HURT, SoundCategory.HOSTILE, 0.5F,
                        world.random.nextFloat() * 0.1F + 0.9F);
            } else if (isPower(world)) {
                int foodLevel = getFoodProperties().getNutrition();
                float saturationLevel = getFoodProperties().getSaturationModifier();
                player.getFoodData().eat(foodLevel, saturationLevel);
                if (!world.isClientSide()) {
                    player.addEffect(new EffectInstance(Effects.DAMAGE_BOOST,
                            getPowerDuration(itemStack) * 20, 2));
                    player.addEffect(new EffectInstance(Effects.MOVEMENT_SPEED,
                            getPowerDuration(itemStack) * 20, 2));
                    player.addEffect(new EffectInstance(Effects.JUMP,
                            getPowerDuration(itemStack) * 20, 2));
                    player.addEffect(new EffectInstance(Effects.NIGHT_VISION,
                            getPowerDuration(itemStack) * 20, 2));
                }
                world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.WOLF_HOWL, SoundCategory.HOSTILE, 0.5F,
                        world.random.nextFloat() * 0.1F + 0.9F);
            } else {
                if (!world.isClientSide()) {
                    player.addEffect(new EffectInstance(Effects.POISON,
                            POISON_DURATION * 20, 1));
                }
                world.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.WOLF_HURT, SoundCategory.HOSTILE, 0.5F,
                        world.random.nextFloat() * 0.1F + 0.9F);
            }
            entity.eat(world, itemStack);
        }
        return itemStack;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack itemStack, World world, List<ITextComponent> list, ITooltipFlag flag) {
    	super.appendHoverText(itemStack, world, list, flag);
    	if(isHumanFlesh(itemStack)) {
    		String player = TextFormatting.ITALIC + "None";
    		if(itemStack.hasTag()) {
    			GameProfile profile = NBTUtil.readGameProfile(itemStack.getTag());
    			if (profile != null) {
                    player = profile.getName();
                }
    		}
    		list.add(new StringTextComponent("Player: ")
                    .withStyle(TextFormatting.WHITE)
                    .append(player));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ItemColor implements IItemColor {
        @Override
        public int getColor(ItemStack itemStack, int renderPass) {
            if (((ItemWerewolfFlesh) itemStack.getItem()).isHumanFlesh(itemStack)) {
                return Helpers.RGBToInt(255, 200, 180);
            }
            return -1;
        }
    }

    public void dropHumanoidFleshEvent(LivingDeathEvent event) {
        if(event.getEntityLiving() instanceof ServerPlayerEntity
                && !event.getEntityLiving().level.isClientSide()
                && event.getEntityLiving().level.random.nextInt(ItemWerewolfFleshConfig.humanoidFleshDropChance) == 0) {
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntityLiving();
            ItemStack itemStack = new ItemStack(this);
            CompoundNBT tag = itemStack.getOrCreateTag();
            NBTUtil.writeGameProfile(tag, player.getGameProfile());
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            ItemEntity entity = new ItemEntity(player.level, x, y, z, itemStack);
            player.level.addFreshEntity(entity);
        }
    }

}
