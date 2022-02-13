package org.cyclops.evilcraft.entity.item;

import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SChunkDataPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.FoliageColors;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimerWrapper;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.Level;
import org.cyclops.cyclopscore.CyclopsCore;
import org.cyclops.cyclopscore.client.particle.ParticleBlurData;
import org.cyclops.cyclopscore.helper.Helpers;
import org.cyclops.evilcraft.EvilCraft;
import org.cyclops.evilcraft.RegistryEntries;
import org.cyclops.evilcraft.core.algorithm.OrganicSpread;
import org.cyclops.evilcraft.core.algorithm.Wrapper;
import org.cyclops.evilcraft.core.entity.item.EntityThrowable;
import org.cyclops.evilcraft.item.ItemBiomeExtract;
import org.cyclops.evilcraft.network.packet.ResetChunkColorsPacket;

import javax.annotation.Nonnull;
import java.util.Random;
import java.util.Set;

/**
 * Entity for the {@link ItemBiomeExtract}.
 * @author rubensworks
 *
 */
public class EntityBiomeExtract extends EntityThrowable {

    private static final DataParameter<ItemStack> ITEMSTACK_INDEX = EntityDataManager.<ItemStack>defineId(EntityBiomeExtract.class, DataSerializers.ITEM_STACK);

    public EntityBiomeExtract(EntityType<? extends EntityThrowable> type, World world) {
        super(type, world);
    }

    public EntityBiomeExtract(World world) {
        this(RegistryEntries.ENTITY_BIOME_EXTRACT, world);
    }

    public EntityBiomeExtract(World world, LivingEntity entity) {
        this(world, entity, new ItemStack(RegistryEntries.ITEM_BIOME_EXTRACT));
    }

    public EntityBiomeExtract(World world, LivingEntity entity, ItemStack stack) {
        super(RegistryEntries.ENTITY_BIOME_EXTRACT, world, entity);
        setItemStack(stack);
    }

    @Nonnull
    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void onHit(final RayTraceResult movingobjectposition) {
        if (!level.isClientSide() && movingobjectposition.getType() == RayTraceResult.Type.BLOCK) {
            ItemStack itemStack = getItem();

            final Biome biome = ItemBiomeExtract.getBiome(itemStack);
            if (biome != null) {
                // Update biome in organic spread
                Set<ChunkPos> updatedChunks = Sets.newHashSet();
                OrganicSpread spread = new OrganicSpread(level, 2, 5, new OrganicSpread.IOrganicSpreadable() {
                    @Override
                    public boolean isDone(World world, BlockPos location) {
                        return world.getBiome(location) == biome;
                    }

                    @Override
                    public void spreadTo(World world, BlockPos location) {
                        setBiome((ServerWorld) world, location, biome);
                        updatedChunks.add(new ChunkPos(location));
                        // int color = biome.getFoliageColor(); // Only accessible client-side, so we copy and modify its implementation...
                        int color = biome.specialEffects.getFoliageColorOverride().orElseGet(() -> {
                            double d0 = (double)MathHelper.clamp(biome.climateSettings.temperature, 0.0F, 1.0F);
                            double d1 = (double)MathHelper.clamp(biome.climateSettings.downfall, 0.0F, 1.0F);
                            // Following is also on accessible client-side...
                            //return FoliageColors.get(d0, d1);
                            return Helpers.RGBToInt(20, 200, 20);
                        });
                        showChangedBiome((ServerWorld) world, new BlockPos(location.getX(), ((BlockRayTraceResult) movingobjectposition).getBlockPos().getY(),
                                location.getZ()), color);
                    }
                });
                BlockPos pos = new BlockPos(movingobjectposition.getLocation());
                for (int i = 0; i < 50; i++) {
                    spread.spreadTick(pos);
                }

                // Send chunk biome data to all players, and reset their grass colors
                if (!level.isClientSide()) {
                    for (ChunkPos chunkPos : updatedChunks) {
                        updateChunkAfterBiomeChange(level, chunkPos);
                    }
                }
            }

            // Play sound and show particles of splash potion of harming
            this.level.globalLevelEvent(2002, blockPosition(), 16428);

            remove();
        }
    }

    /**
     * Set the biome for the given coordinates.
     * Make sure to send updates to clients after calling this using {@link EntityBiomeExtract#updateChunkAfterBiomeChange(World, ChunkPos)}.
     * @param world The world.
     * @param posIn The position.
     * @param biome The biome to change to.
     */
    public static void setBiome(ServerWorld world, BlockPos posIn, Biome biome) {
        // Worldgen applies some funk "magnifier" position transformation to a "noise position",
        // which can change the pos into some other internal pos.
        // In a hacky way, we can apply transformation as follows:
        Wrapper<BlockPos> posWrapper = new Wrapper<>();
        world.dimensionType().getBiomeZoomer().getBiome(world.getSeed(), posIn.getX(), posIn.getY(), posIn.getZ(), new BiomeManager.IBiomeReader() {
            @Override
            public Biome getNoiseBiome(int x, int y, int z) {
                posWrapper.set(new BlockPos(x, y, z));
                return null;
            }
        });

        // Update biome data in chunk
        BlockPos noisePos = posWrapper.get();
        IChunk chunk = world.getChunk(noisePos.getX() >> 2, noisePos.getZ() >> 2, ChunkStatus.BIOMES, false);
        if (chunk instanceof ChunkPrimerWrapper) {
            //chunk = ((ChunkPrimerWrapper) chunk).getChunk();
        }
        if(chunk != null) {
            // Update biome in chunk
            Biome[] biomeArray = chunk.getBiomes().biomes;
            int i = noisePos.getX() & BiomeContainer.HORIZONTAL_MASK;
            int j = MathHelper.clamp(noisePos.getY(), 0, BiomeContainer.VERTICAL_MASK);
            int k = noisePos.getZ() & BiomeContainer.HORIZONTAL_MASK;

            // HACK
            // Due to some weird thing in MC, different instances of the same biome can exist.
            // This hack allows us to convert to the biome instance that is required for chunk serialization.
            // This avoids weird errors in the form of "Received invalid biome id: -1" (#818)
            MutableRegistry<Biome> biomeRegistry = world.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
            Biome biomeHack = biomeRegistry.get(RegistryKey.create(Registry.BIOME_REGISTRY, biome.getRegistryName()));

            biomeArray[j << BiomeContainer.WIDTH_BITS + BiomeContainer.WIDTH_BITS | k << BiomeContainer.WIDTH_BITS | i] = biomeHack;
            chunk.setUnsaved(true);
        } else {
            CyclopsCore.clog(Level.WARN, "Tried changing biome at non-existing chunk for position " + noisePos);
        }
    }

    /**
     * This should be called after {@link EntityBiomeExtract#setBiome(ServerWorld, BlockPos, Biome)}}
     * to notify players of biome change.
     * @param world The world.
     * @param chunkPos The chunk position in which one or more biome positions were changed.
     */
    public static void updateChunkAfterBiomeChange(World world, ChunkPos chunkPos) {
        Chunk chunkSafe = world.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
        ((ServerChunkProvider) world.getChunkSource()).chunkMap.getPlayers(chunkPos, false).forEach((player) -> {
            player.connection.send(new SChunkDataPacket(chunkSafe, 65535));
            EvilCraft._instance.getPacketHandler().sendToPlayer(new ResetChunkColorsPacket(chunkPos.x, chunkPos.z), player);
        });
    }

    private void showChangedBiome(ServerWorld world, BlockPos pos, int color) {
        Triple<Float, Float, Float> c = Helpers.intToRGB(color);
        Random rand = world.random;
        for (int j = 0; j < 2 + rand.nextInt(5); j++) {
            float x = pos.getX() + -0.5F + rand.nextFloat();
            float y = pos.getY() + -0.5F + rand.nextFloat();
            float z = pos.getZ() + -0.5F + rand.nextFloat();

            float scale = 0.2F - rand.nextFloat() * 0.2F;
            float red = c.getLeft() + rand.nextFloat() * 0.1F;
            float green = c.getMiddle() + rand.nextFloat() * 0.1F;
            float blue = c.getRight() + rand.nextFloat() * 0.1F;
            float ageMultiplier = 10 + rand.nextInt(15);

            double motionX = -0.1F + rand.nextFloat() * 0.2F;
            double motionY = 0.1F + rand.nextFloat() * 0.2F;
            double motionZ = -0.1F + rand.nextFloat() * 0.2F;

            world.sendParticles(
                    new ParticleBlurData(red, green, blue, scale, ageMultiplier),
                    x, y, z, 1, motionX, motionY, motionZ, 0.1);
        }
    }

    @Override
    protected float getGravity() {
        // The bigger, the faster the entity falls to the ground
        return 0.1F;
    }

    @Override
    public ItemStack getItem() {
        return entityData.get(ITEMSTACK_INDEX);
    }
    
    private void setItemStack(ItemStack stack) {
        entityData.set(ITEMSTACK_INDEX, stack);
    }
    
    @Override
    protected void defineSynchedData() {
        entityData.define(ITEMSTACK_INDEX, new ItemStack(RegistryEntries.ITEM_BIOME_EXTRACT));
    }
}
