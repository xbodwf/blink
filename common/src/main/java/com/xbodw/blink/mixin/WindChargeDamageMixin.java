package com.xbodw.blink.mixin;

import com.xbodw.blink.item.CompressedWindChargeItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractWindCharge.class)
public class WindChargeDamageMixin {

    @ModifyArg(
        method = "onHitEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
        index = 1
    )
    private float multiplyDamage(float original) {
        AbstractWindCharge self = (AbstractWindCharge)(Object)this;
        Entity owner = self.getOwner();
        if (owner instanceof Player player) {
            for (ItemStack stack : player.getHandSlots()) {
                if (stack.getItem() instanceof CompressedWindChargeItem cwc) {
                    return original * cwc.getCompressionLevel();
                }
            }
        }
        return original;
    }
}
