package com.aqutheseal.celestisynth.entities;

import com.aqutheseal.celestisynth.Celestisynth;
import com.aqutheseal.celestisynth.entities.helper.CSEffectTypes;
import com.aqutheseal.celestisynth.registry.CSEntityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Random;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CSEffect extends Entity implements GeoEntity {
    public CSEffect(EntityType<? extends CSEffect> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
        this.noCulling = true;
    }

    private static final EntityDataAccessor<String> TYPE_ID = SynchedEntityData.defineId(CSEffect.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> FRAME_LEVEL = SynchedEntityData.defineId(CSEffect.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SET_ROT_X = SynchedEntityData.defineId(CSEffect.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SET_ROT_Z = SynchedEntityData.defineId(CSEffect.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private LivingEntity owner;
    public int lifespan;
    public int frameTimer;

    public CSEffectTypes getEffectType() {
        for (CSEffectTypes candids : CSEffectTypes.values()) {
            if (candids.getName().equals(this.getTypeID())) {
                return candids;
            }
        }
        return getDefaultEffect();
    }

    public void setEffectType(CSEffectTypes getEffectType) {
        this.setTypeID(getEffectType.getName());
    }

    public String getTypeID() {
        return this.entityData.get(TYPE_ID);
    }

    public void setTypeID(String value) {
        this.entityData.set(TYPE_ID, value);
    }

    public int getFrameLevel() {
        return this.entityData.get(FRAME_LEVEL);
    }

    public void setFrameLevel(int value) {
        this.entityData.set(FRAME_LEVEL, value);
    }

    public void setRotationX(int rotationX) {
        this.entityData.set(SET_ROT_X, rotationX);
    }

    public void setRotationZ(int rotationZ) {
        this.entityData.set(SET_ROT_Z, rotationZ);
    }

    public int getRotationX() {
        return this.entityData.get(SET_ROT_X);
    }

    public int getRotationZ() {
        return this.entityData.get(SET_ROT_Z);
    }

    public LivingEntity getOwner() {
        return this.owner;
    }

    public void setOwner(LivingEntity livingEntity) {
        this.owner = livingEntity;
    }

    public void setLifespan(int value) {
        lifespan = value;
    }

    public static CSEffect getEffectInstance(Entity owner, CSEffectTypes effectTypes, double offsetX, double offsetY, double offsetZ) {
        CSEffect slash = CSEntityRegistry.CS_EFFECT.get().create(owner.level);
        slash.setEffectType(effectTypes);
        slash.setRandomRotation();
        Random rand = new Random(69420);
        float offset = -(rand.nextFloat() / 2) + rand.nextFloat();
        float offsetRot = -10 + rand.nextInt(10);
        slash.moveTo((owner.getX() + offset) + offsetX, (owner.getY() - 1.5) + offsetY, (owner.getZ() + offset) + offsetZ);
        if (owner instanceof LivingEntity living) {
            slash.setOwner(living);
            slash.setYRot(owner.getYRot() + offsetRot);
            slash.yRotO = slash.getYRot();
            slash.setRot(slash.getYRot() + offsetRot, slash.getXRot());
        }
        return slash;
    }

    public void setRandomRotation() {
        int rotationX = random.nextInt(360);
        int rotationZ = random.nextInt(360);
        this.entityData.set(SET_ROT_X, rotationX);
        this.entityData.set(SET_ROT_Z, rotationZ);
    }

    public static void createInstance(Entity owner, CSEffectTypes effectTypes) {
       createInstance(owner, effectTypes, 0, 0, 0);
    }

    public static void createInstance(Entity owner, CSEffectTypes effectTypes, double xOffset, double yOffset, double zOffset) {
        owner.level.addFreshEntity(getEffectInstance(owner, effectTypes, xOffset, yOffset, zOffset));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, event -> {
            if (getEffectType() != null) {
                return event.setAndContinue(RawAnimation.begin().thenLoop(getEffectType().getAnimation().getAnimationString()));
            } else {
                Celestisynth.LOGGER.warn("EffectType for CSEffect is null!");
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.cs_effect.spin"));
            }
        }));
    }

    @Override
    public void tick() {
        if (owner == null) {
            this.remove(RemovalReason.DISCARDED);
        }

        ++lifespan;
        if (lifespan >= this.getEffectType().getAnimation().getLifespan()) {
            this.setLifespan(0);
            this.remove(RemovalReason.DISCARDED);
        }
        ++frameTimer;
        if (frameTimer >= (this.getEffectType().getFramesSpeed() - 1)) {
            this.setFrameLevel(this.getFrameLevel() == this.getEffectType().getFrames() ? 1 : this.getFrameLevel() + 1);
            frameTimer = 0;
        }
        super.tick();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TYPE_ID, "none");
        this.entityData.define(FRAME_LEVEL, 1);
        this.entityData.define(SET_ROT_X, 0);
        this.entityData.define(SET_ROT_Z, 0);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundNBT) {
        this.setLifespan(compoundNBT.getInt("lifespan"));
        this.setTypeID(compoundNBT.getString("typeId"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundNBT) {
        compoundNBT.putInt("lifespan", this.lifespan);
        compoundNBT.putString("typeId", this.getTypeID());
    }

    public CSEffectTypes getDefaultEffect() {
        return CSEffectTypes.SOLARIS_BLITZ;
    }


    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
