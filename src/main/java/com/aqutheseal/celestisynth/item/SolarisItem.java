package com.aqutheseal.celestisynth.item;

import com.aqutheseal.celestisynth.animation.AnimationManager;
import com.aqutheseal.celestisynth.entities.CSEffect;
import com.aqutheseal.celestisynth.entities.helper.CSEffectTypes;
import com.aqutheseal.celestisynth.item.helpers.CSUtilityFunctions;
import com.aqutheseal.celestisynth.registry.CSSoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

public class SolarisItem extends SwordItem {
    public static final String DIRECTION_INDEX_KEY = "cs.directionIndex";
    public static final String HEAD_ROT_LOCK_KEY = "cs.headRotLock";
    public static final String ANIMATION_TIMER_KEY = "cs.animationTimer";
    public static final String ANIMATION_BEGUN_KEY = "cs.hasAnimationBegun";
    public static final SoundEvent[] SOLARIS_SOUNDS = {
            CSSoundRegistry.SOLARIS_1.get(),
            CSSoundRegistry.SOLARIS_2.get(),
            CSSoundRegistry.SOLARIS_3.get(),
            CSSoundRegistry.SOLARIS_5.get(),
            CSSoundRegistry.SOLARIS_6.get(),
            CSSoundRegistry.SOLARIS_4.get()
    };

    public SolarisItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        ItemStack itemstack = player.getItemInHand(interactionHand);
        CompoundTag itemTag = itemstack.getOrCreateTagElement("csController");

        if (!player.getCooldowns().isOnCooldown(itemstack.getItem()) && !itemTag.getBoolean(ANIMATION_BEGUN_KEY)) {
            if (level.isClientSide()) {
                AnimationManager.playAnimation(AnimationManager.AnimationsList.ANIM_SOLARIS_SPIN);
            }
            itemTag.putBoolean(ANIMATION_BEGUN_KEY, true);
            if (player.isShiftKeyDown()) {
                itemTag.putInt(DIRECTION_INDEX_KEY, 2);
                itemTag.putFloat(HEAD_ROT_LOCK_KEY, player.getYRot());
                player.getCooldowns().addCooldown(itemstack.getItem(), 160);
            } else {
                itemTag.putInt(DIRECTION_INDEX_KEY, player.getRandom().nextInt(2));
                player.getCooldowns().addCooldown(itemstack.getItem(), 100);
            }
        }
        return InteractionResultHolder.success(itemstack);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        CompoundTag itemTag = item.getOrCreateTagElement("csController");
        if (itemTag.getBoolean(ANIMATION_BEGUN_KEY)) {
            return false;
        }
        return super.onDroppedByPlayer(item, player);
    }

    @Override
    public boolean hurtEnemy(ItemStack itemStack, LivingEntity entity, LivingEntity source) {
        entity.setSecondsOnFire(5);
        return super.hurtEnemy(itemStack, entity, source);
    }

    public int getUseDuration(ItemStack p_42933_) {
        return 32;
    }

    @Override
    public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int itemSlot, boolean isSelected) {
        CompoundTag data = itemStack.getOrCreateTagElement("csController");
        if (entity instanceof Player player && data.getBoolean(ANIMATION_BEGUN_KEY)) {
            player.getInventory().selected = itemSlot;
            if (level.isClientSide()) {
                if (Minecraft.getInstance().screen != null) {
                    Minecraft.getInstance().screen = null;
                }
            }

            int animationTimer = data.getInt(ANIMATION_TIMER_KEY);
            data.putInt(ANIMATION_TIMER_KEY, animationTimer + 1);
            if (animationTimer == 22) {
                player.playSound(CSSoundRegistry.CS_STEP.get());
                for (int i = 0; i < 15; i++) {
                    Random rand = new Random();
                    if (level instanceof ServerLevel) {
                        CSUtilityFunctions.sendParticles((ServerLevel) level, ParticleTypes.LARGE_SMOKE, player.getX(), player.getY(), player.getZ(), 0, (-1 + rand.nextFloat(2)) * 0.5, 0.1, (-1 + rand.nextFloat(2)) * 0.5);
                    }
                }
            }

            if (animationTimer > 0 && animationTimer < 35) {
                if (level instanceof ServerLevel) {
                    if (data.getInt(DIRECTION_INDEX_KEY) == 2) {
                        for (int i = 0; i < 10; i++) {
                            CSUtilityFunctions.sendParticles((ServerLevel) level, ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY(), player.getZ(), 0, -1 + new Random().nextFloat(2), 0.1, -1 + new Random().nextFloat(2));
                        }
                    } else {
                        CSUtilityFunctions.sendParticles((ServerLevel) level, ParticleTypes.FLAME, player.getX(), player.getY(), player.getZ(), 0, 0, 0.1, 0);
                    }
                }
                player.setDeltaMovement(0, 0, 0);
                player.hurtMarked = true;
            } else if (animationTimer > 35 && animationTimer < 60) {
                BlockPos blockPosForAttack = player.blockPosition();
                boolean isStraight = data.getInt(DIRECTION_INDEX_KEY) == 2;
                int range = isStraight ? 7 : 4;
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(blockPosForAttack.offset(-(range), -(range), -(range)), blockPosForAttack.offset(range, range, range)));
                for (LivingEntity target : entities) {
                    if (target != player && target.isAlive()) {
                        double preAttribute = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).getValue();
                        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(100);
                        target.invulnerableTime = 0;
                        target.hurt(player.damageSources().playerAttack(player), (isStraight ? 1.5f : 1.0f) + ((float) EnchantmentHelper.getTagEnchantmentLevel(Enchantments.SHARPNESS, itemStack) / 2.5F));
                        target.setSecondsOnFire(5);
                        target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(preAttribute);
                    }
                }

                player.playSound(SoundEvents.SWEET_BERRY_BUSH_BREAK);
                if (data.getInt(DIRECTION_INDEX_KEY) == 2) {
                    movePlayerInStraightMotion(player, data.getInt(HEAD_ROT_LOCK_KEY));
                    CSEffect.createInstance(player, CSEffectTypes.SOLARIS_BLITZ_SOUL);
                    CSEffect.createInstance(player, CSEffectTypes.SOLARIS_AIR_LARGE);
                    playRandomBladeSound(player, SOLARIS_SOUNDS.length);
                } else if (data.getInt(DIRECTION_INDEX_KEY) == 0) {
                    movePlayerInCircularMotion(player, animationTimer, false);
                    CSEffect.createInstance(player, CSEffectTypes.SOLARIS_BLITZ);
                    CSEffect.createInstance(player, CSEffectTypes.SOLARIS_AIR);
                    playRandomBladeSound(player, 4);
                } else if (data.getInt(DIRECTION_INDEX_KEY) == 1) {
                    movePlayerInCircularMotion(player, animationTimer, true);
                    CSEffect.createInstance(player, CSEffectTypes.SOLARIS_BLITZ);
                    CSEffect.createInstance(player, CSEffectTypes.SOLARIS_AIR);
                    playRandomBladeSound(player, 4);
                }

                BlockPos playerPos = player.blockPosition();
                double radius = 3;
                double particleCount = 50;
                double angleIncrement = (2 * Math.PI) / particleCount;

                for (int i = 0; i < particleCount; i++) {
                    double angle = i * angleIncrement;
                    double rotationX = level.random.nextDouble() * 360.0;
                    double rotationZ = level.random.nextDouble() * 360.0;
                    double x = playerPos.getX() + radius * Math.cos(angle);
                    double y = playerPos.getY() + 1.5;
                    double z = playerPos.getZ() + radius * Math.sin(angle);

                    double motionX = Math.sin(Math.toRadians(rotationX)) * Math.cos(Math.toRadians(rotationZ));
                    double motionY = Math.sin(Math.toRadians(rotationZ));
                    double motionZ = Math.cos(Math.toRadians(rotationX)) * Math.cos(Math.toRadians(rotationZ));

                    if (level instanceof ServerLevel) {
                        CSUtilityFunctions.sendParticles((ServerLevel) level, data.getInt(DIRECTION_INDEX_KEY) == 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, x + 0.5, y, z + 0.5, 0, motionX, motionY, motionZ);
                        if (data.getInt(DIRECTION_INDEX_KEY) == 2) {
                            CSUtilityFunctions.sendParticles((ServerLevel) level, ParticleTypes.SOUL, x + 0.5, y, z + 0.5, 0, motionX, motionY, motionZ);
                        }
                    }
                }
            }
            if (animationTimer >= 80) {
                data.putInt(DIRECTION_INDEX_KEY, player.getRandom().nextInt(2));
                data.putFloat(HEAD_ROT_LOCK_KEY, 0);
                data.putInt(ANIMATION_TIMER_KEY, 0);
                data.putBoolean(ANIMATION_BEGUN_KEY, false);
            }
        }

        super.inventoryTick(itemStack, level, entity, itemSlot, isSelected);
    }

    public void playRandomBladeSound(Entity entity, int length) {
        SoundEvent randomSound = SOLARIS_SOUNDS[new Random().nextInt(length)];
        entity.playSound(randomSound, 0.7F, 0.5F + new Random().nextFloat());
    }

    private void movePlayerInCircularMotion(Player player, int tick, boolean isRight) {
        double radius = 1.5;
        double forwardX = -Math.sin(Math.toRadians(player.getYRot()));
        double forwardZ = Math.cos(Math.toRadians(player.getYRot()));
        double perpendicularX = -forwardZ;
        double perpendicularZ = forwardX;
        double angle = (tick - 45) / 25.0 * Math.PI * 2.0;
        double offsetX = radius * Math.cos(angle);
        double offsetZ = radius * Math.sin(angle);
        double finalX = isRight ? player.getX() + forwardX * offsetX - perpendicularX * offsetZ : player.getX() + forwardX * offsetX + perpendicularX * offsetZ;
        double finalZ = isRight ? player.getZ() + forwardZ * offsetX - perpendicularZ * offsetZ : player.getZ() + forwardZ * offsetX + perpendicularZ * offsetZ;
        player.setDeltaMovement(finalX - player.getX(), player.getDeltaMovement().y, finalZ - player.getZ());
    }

    private void movePlayerInStraightMotion(Player player, float yRot) {
        double speed = 1.5;
        double lookX = -Math.sin(Math.toRadians(yRot));
        double lookZ = Math.cos(Math.toRadians(yRot));
        double motionX = lookX * speed;
        double motionZ = lookZ * speed;
        player.setDeltaMovement(motionX, player.getDeltaMovement().y, motionZ);
    }
}