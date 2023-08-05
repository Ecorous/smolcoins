package org.ecorous.smolcoins.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.ecorous.smolcoins.Smolcoins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(BlockItem.class)
public abstract class ItemMixin extends Item{
	public ItemMixin(Settings settings) {
		super(settings);
	}

	@Shadow
	public abstract Block getBlock();



	@Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"))
	public void smolcoins$onInit(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (this.getBlock() instanceof ChestBlock && this.getName().toString().equalsIgnoreCase("[admin] smolcoin convertor")) {
			if (context.getPlayer() == null) {
				cir.cancel();
			} else if (!context.getPlayer().getAbilities().creativeMode || context.getPlayer().getPermissionLevel() <= 2) {
				cir.cancel();
			}
		}
		Smolcoins.INSTANCE.getLOGGER().info("This line is printed by an example mod mixin!");
	}
}
