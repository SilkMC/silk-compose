package net.silkmc.silk.compose.mixin;

import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.silkmc.silk.compose.impl.ItemFrameMapsComposeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
        method = "handleAnimate",
        at = @At("RETURN")
    )
    private void onHandSwingInject(ServerboundSwingPacket packet, CallbackInfo ci) {
        ItemFrameMapsComposeGui.PlayerHolder.onSwingHand(player, packet);
    }

    @Inject(
        method = "handleSetCarriedItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;getInventory()Lnet/minecraft/world/entity/player/Inventory;",
            ordinal = 1,
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void onUpdateSelectedSlotInject(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
        if (ItemFrameMapsComposeGui.PlayerHolder.onUpdateSelectedSlot(player, packet)) {
            ci.cancel();
        }
    }
}
