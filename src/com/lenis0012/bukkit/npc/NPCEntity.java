package com.lenis0012.bukkit.npc;

import net.minecraft.server.v1_7_R4.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_7_R4.CraftServer;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class NPCEntity extends EntityPlayer {
    private final NPC npc;
    private boolean invulnerable = true;
    private boolean gravity = true;

    public NPCEntity(World world, NPCProfile profile, NPCNetworkManager networkManager) {
        super(((CraftServer) Bukkit.getServer()).getServer(), ((CraftWorld) world).getHandle(), profile, new PlayerInteractManager(((CraftWorld) world).getHandle()));
        playerInteractManager.b(EnumGamemode.SURVIVAL);
        this.playerConnection = new NPCPlayerConnection(networkManager, this);
        this.fauxSleeping = true;
        this.bukkitEntity = new CraftPlayer((CraftServer) Bukkit.getServer(), this);
        this.npc = new NPC(this);
    }

    @Override
    public CraftPlayer getBukkitEntity() {
        return (CraftPlayer) bukkitEntity;
    }

    public NPC getNPC() {
        return npc;
    }

    public boolean isGravity() {
        return gravity;
    }

    public void setGravity(boolean gravity) {
        this.gravity = gravity;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }

    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    @Override
    public void h() {
        super.h();
        this.C();

        npc.onTick();
        if (world.getType(MathHelper.floor(locX), MathHelper.floor(locY), MathHelper.floor(locZ)).getMaterial() == Material.FIRE) {
            setOnFire(15);
        }

        //Apply velocity etc.
        this.motY = onGround ? Math.max(0.0, motY) : motY;
        move(motX, motY, motZ);
        this.motX *= 0.800000011920929;
        this.motY *= 0.800000011920929;
        this.motZ *= 0.800000011920929;
        if (gravity && !this.onGround) {
            this.motY -= 0.1; //Most random value, don't judge.
        }
    }

    @Override
    public boolean a(EntityHuman entity) {
        NPCInteractEvent event = new NPCInteractEvent(npc, entity.getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        return super.a(entity);
    }

    @Override
    public boolean damageEntity(DamageSource source, float damage) {
        if (invulnerable || noDamageTicks > 0) {
            return false;
        }

        DamageCause cause = null;
        org.bukkit.entity.Entity bEntity = null;
        if (source instanceof EntityDamageSource) {
            Entity damager = source.getEntity();
            cause = DamageCause.ENTITY_ATTACK;
            if (source instanceof EntityDamageSourceIndirect) {
                damager = ((EntityDamageSourceIndirect) source).getProximateDamageSource();
                if (damager.getBukkitEntity() instanceof ThrownPotion) {
                    cause = DamageCause.MAGIC;
                } else if (damager.getBukkitEntity() instanceof Projectile) {
                    cause = DamageCause.PROJECTILE;
                }
            }

            bEntity = damager.getBukkitEntity();
        } else if (source == DamageSource.FIRE)
            cause = DamageCause.FIRE;
        else if (source == DamageSource.STARVE)
            cause = DamageCause.STARVATION;
        else if (source == DamageSource.WITHER)
            cause = DamageCause.WITHER;
        else if (source == DamageSource.STUCK)
            cause = DamageCause.SUFFOCATION;
        else if (source == DamageSource.DROWN)
            cause = DamageCause.DROWNING;
        else if (source == DamageSource.BURN)
            cause = DamageCause.FIRE_TICK;
        else if (source == CraftEventFactory.MELTING)
            cause = DamageCause.MELTING;
        else if (source == CraftEventFactory.POISON)
            cause = DamageCause.POISON;
        else if (source == DamageSource.MAGIC) {
            cause = DamageCause.MAGIC;
        } else if (source == DamageSource.OUT_OF_WORLD) {
            cause = DamageCause.VOID;
        }

        if (cause != null) {
            NPCDamageEvent event = new NPCDamageEvent(npc, bEntity, cause, (double) damage);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                return super.damageEntity(source, (float) event.getDamage());
            } else {
                return false;
            }
        }

        if (super.damageEntity(source, damage)) {
            if (bEntity != null) {
                Entity e = ((CraftEntity) bEntity).getHandle();
                double d0 = e.locX - this.locX;

                double d1;
                for (d1 = e.locZ - this.locZ; d0 * d0 + d1 * d1 < 0.0001D; d1 = (Math.random() - Math.random()) * 0.01D) {
                    d0 = (Math.random() - Math.random()) * 0.01D;
                }

                a(e, damage, d0, d1);
            }

            return true;
        } else {
            return false;
        }
    }
}