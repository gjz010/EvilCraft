package evilcraft.entity.monster;

import evilcraft.Configs;
import evilcraft.block.NetherfishSpawn;
import evilcraft.block.NetherfishSpawnConfig;
import org.cyclops.cyclopscore.config.configurable.IConfigurable;
import org.cyclops.cyclopscore.config.extendedconfig.ExtendedConfig;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A silverfish for the nether.
 * @author rubensworks
 *
 */
public class Netherfish extends EntitySilverfish implements IConfigurable{
    
    private static final int MAX_FIRE_DURATION = 3;
    private static final double FIRE_CHANCE = 0.5;

    /**
     * Make a new instance.
     * @param world The world.
     */
    public Netherfish(World world) {
        super(world);
        this.isImmuneToFire = true;
        this.experienceValue = 10;
        this.tasks.addTask(5, new Netherfish.AIHideInStone());
    }
    
    @Override
    protected Item getDropItem() {
        return Items.gunpowder;
    }
    
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataWatcher.addObject(16, new Byte((byte) 0));
    }
    
    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        // A bit stronger than those normal silverfish...
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(12.0D);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(0.8D);
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(2.0D);
    }
    
    @Override
    public boolean attackEntityAsMob(Entity entity) {
        // Ignite the attacked entity for a certain duration with a certain chance.
        if(this.rand.nextFloat() < FIRE_CHANCE)
            entity.setFire(this.rand.nextInt(MAX_FIRE_DURATION));
        return super.attackEntityAsMob(entity);
    }
    
    @Override
    public void onLivingUpdate() {
        // TODO: for some reason, this does not work, although it is called client side, it just doesn't render those damn particles...
        if(!this.worldObj.isRemote) {
            for (int i = 0; i < 2; ++i) {
                this.worldObj.spawnParticle(EnumParticleTypes.FLAME, this.posX + (this.rand.nextDouble() - 0.5D) * (double)this.width, this.posY + this.rand.nextDouble() * (double)this.height, this.posZ + (this.rand.nextDouble() - 0.5D) * (double)this.width, 0.0D, 0.0D, 0.0D);
            }
        }
        super.onLivingUpdate();
    }
    
    @Override
    public boolean isBurning() {
        // A line copied from EntityBlaze
        return (this.dataWatcher.getWatchableObjectByte(16) & 1) != 0;
    }

    @Override
    public ExtendedConfig<?> getConfig() {
        return null;
    }

    class AIHideInStone extends EntityAIWander {

        private EnumFacing field_179483_b;
        private boolean field_179484_c;
        private static final String __OBFID = "CL_00002205";

        public AIHideInStone()
        {
            super(Netherfish.this, 1.0D, 10);
            this.setMutexBits(1);
        }

        /**
         * Returns whether the EntityAIBase should begin execution.
         */
        public boolean shouldExecute()
        {
            if (Netherfish.this.getAttackTarget() != null) {
                return false;
            } else if (!Netherfish.this.getNavigator().noPath()) {
                return false;
            } else {
                Random random = Netherfish.this.getRNG();

                if (Configs.isEnabled(NetherfishSpawnConfig.class) && random.nextInt(10) == 0)
                {
                    this.field_179483_b = EnumFacing.random(random);
                    BlockPos blockpos = (new BlockPos(Netherfish.this.posX, Netherfish.this.posY + 0.5D, Netherfish.this.posZ)).offset(this.field_179483_b);
                    int meta = NetherfishSpawn.getInstance().getMetadataFromBlock(Netherfish.this.worldObj.getBlockState(blockpos).getBlock());

                    if (meta >= 0) {
                        Netherfish.this.worldObj.setBlockState(getPosition(), NetherfishSpawn.getInstance().getDefaultState().withProperty(NetherfishSpawn.FAKEMETA, meta));
                        this.field_179484_c = true;
                        return true;
                    }
                }

                this.field_179484_c = false;
                return super.shouldExecute();
            }
        }

        /**
         * Returns whether an in-progress EntityAIBase should continue executing
         */
        public boolean continueExecuting()
        {
            return this.field_179484_c ? false : super.continueExecuting();
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void startExecuting()
        {
            if (!this.field_179484_c) {
                super.startExecuting();
            } else {
                World world = Netherfish.this.worldObj;
                BlockPos blockpos = (new BlockPos(Netherfish.this.posX, Netherfish.this.posY + 0.5D, Netherfish.this.posZ)).offset(this.field_179483_b);
                IBlockState iblockstate = world.getBlockState(blockpos);

                if (BlockSilverfish.canContainSilverfish(iblockstate)) {
                    world.setBlockState(blockpos, Blocks.monster_egg.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.forModelBlock(iblockstate)), 3);
                    Netherfish.this.spawnExplosionParticle();
                    Netherfish.this.setDead();
                }
            }
        }

    }
    
}
