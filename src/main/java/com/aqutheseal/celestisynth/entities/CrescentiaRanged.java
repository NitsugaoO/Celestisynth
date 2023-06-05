package com.aqutheseal.celestisynth.entities;

import com.aqutheseal.celestisynth.entities.helper.CSEffectTypes;
import com.aqutheseal.celestisynth.item.CrescentiaItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class CrescentiaRanged extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(CrescentiaRanged.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Float> ANGLE_X = SynchedEntityData.defineId(CrescentiaRanged.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ANGLE_Y = SynchedEntityData.defineId(CrescentiaRanged.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ANGLE_Z = SynchedEntityData.defineId(CrescentiaRanged.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ANGLE_ADD_X = SynchedEntityData.defineId(CrescentiaRanged.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ANGLE_ADD_Y = SynchedEntityData.defineId(CrescentiaRanged.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ANGLE_ADD_Z = SynchedEntityData.defineId(CrescentiaRanged.class, EntityDataSerializers.FLOAT);
    
    public CrescentiaRanged(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @Override
    public void tick() {
        super.tick();
        Player player = level.getPlayerByUUID(getOwnerUuid());
        if (player == null || player.isDeadOrDying()) {
            this.remove(RemovalReason.DISCARDED);
        }

        setAngleX(getAngleX() + getAddAngleX());
        setAngleY(getAngleY() + getAddAngleY());
        setAngleZ(getAngleZ() + getAddAngleZ());

        double newX = getX() + getAngleX();
        double newY = getY() + getAngleY();
        double newZ = getZ() + getAngleZ();

        if (new Random().nextBoolean()) {
            CSEffect.createInstance(this, CSEffectTypes.CRESCENTIA_THROW, getAngleX(), getAngleY() - 1.5, getAngleZ());
        } else {
            CSEffect.createInstance(this, CSEffectTypes.CRESCENTIA_THROW_INVERTED, getAngleX(), getAngleY() - 1.5, getAngleZ());
        }
        CSEffect.createInstance(this, CSEffectTypes.SOLARIS_AIR, getAngleX(), getAngleY(), getAngleZ());
        playRandomBladeSound(CrescentiaItem.CRESENTIA_SOUNDS.length, newX, newY, newZ);
        double range = 7.0;
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(newX + range, newY + range, newZ + range, newX - range, newY - range, newZ - range));
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        for (Entity entityBatch : entities) {
            if (entityBatch instanceof LivingEntity target) {
                if (target != player && target.isAlive()) {
                    double preAttribute = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getValue();
                    target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(100);
                    target.invulnerableTime = 0;
                    target.hurt(player.damageSources().playerAttack(player), 0.7f + ((float) EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SHARPNESS, stack) / 3F));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                    target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(preAttribute);
                }
            }
            if (entityBatch instanceof Projectile projectile) {
                CrescentiaItem.createCrescentiaFirework(stack, level, player, projectile.getX(), projectile.getY(), projectile.getZ(), true, tickCount);
                projectile.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
                projectile.remove(RemovalReason.DISCARDED);
            }
        }

        if (tickCount == 100) {
            level.explode(player, newX, newY, newZ, 1.0F, Level.ExplosionInteraction.MOB);
            CrescentiaItem.createCrescentiaFirework(stack, level, player, newX, newY, newZ, true, tickCount);
            this.remove(RemovalReason.DISCARDED);
        }
    }

    public void playRandomBladeSound(int length, double x, double y, double z) {
        SoundEvent randomSound = CrescentiaItem.CRESENTIA_SOUNDS[new Random().nextInt(length)];
        level.playSound(level.getPlayerByUUID(getOwnerUuid()), x, y, z, randomSound, SoundSource.HOSTILE, 0.25F, 0.5F + new Random().nextFloat());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(ANGLE_X, 0F);
        this.entityData.define(ANGLE_Y, 0F);
        this.entityData.define(ANGLE_Z, 0F);
        this.entityData.define(ANGLE_ADD_X, 0F);
        this.entityData.define(ANGLE_ADD_Y, 0F);
        this.entityData.define(ANGLE_ADD_Z, 0F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setOwnerUuid(tag.getUUID("cs.ownerUuid"));
        setAngleX(tag.getFloat("cs.angleX"));
        setAngleY(tag.getFloat("cs.angleY"));
        setAngleZ(tag.getFloat("cs.angleZ"));
        setAddAngleX(tag.getFloat("cs.angleAddX"));
        setAddAngleY(tag.getFloat("cs.angleAddY"));
        setAddAngleZ(tag.getFloat("cs.angleAddZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putUUID("cs.ownerUuid", getOwnerUuid());
        tag.putFloat("cs.angleX", getAngleX());
        tag.putFloat("cs.angleY", getAngleY());
        tag.putFloat("cs.angleZ", getAngleZ());
        tag.putFloat("cs.angleAddX", getAddAngleX());
        tag.putFloat("cs.angleAddY", getAddAngleY());
        tag.putFloat("cs.angleAddZ", getAddAngleZ());
    }

    public void setOwnerUuid(@Nullable UUID ownerUuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(ownerUuid));
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public void setAngleX(float angleX) {
        this.entityData.set(ANGLE_X, angleX);
    }

    public float getAngleX() {
        return this.entityData.get(ANGLE_X);
    }

    public void setAngleY(float angleY) {
        this.entityData.set(ANGLE_Y, angleY);
    }

    public float getAngleY() {
        return this.entityData.get(ANGLE_Y);
    }

    public void setAngleZ(float angleZ) {
        this.entityData.set(ANGLE_Z, angleZ);
    }

    public float getAngleZ() {
        return this.entityData.get(ANGLE_Z);
    }

    public void setAddAngleX(float angleX) {
        this.entityData.set(ANGLE_ADD_X, angleX);
    }

    public float getAddAngleX() {
        return this.entityData.get(ANGLE_ADD_X);
    }

    public void setAddAngleY(float angleY) {
        this.entityData.set(ANGLE_ADD_Y, angleY);
    }

    public float getAddAngleY() {
        return this.entityData.get(ANGLE_ADD_Y);
    }

    public void setAddAngleZ(float angleZ) {
        this.entityData.set(ANGLE_ADD_Z, angleZ);
    }

    public float getAddAngleZ() {
        return this.entityData.get(ANGLE_ADD_Z);
    }
}