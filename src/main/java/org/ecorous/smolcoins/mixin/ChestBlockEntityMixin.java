package org.ecorous.smolcoins.mixin;

import com.google.gson.Gson;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.ecorous.smolcoins.Smolcoins;
import org.ecorous.smolcoins.TextObject;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin {
	@Shadow
	protected abstract Text getContainerName();

	@Shadow
	private DefaultedList<ItemStack> inventory;

	private static String removeFirstAndLastCharacter(String input) {
		if (input == null || input.length() <= 2) {
			// If the input string is null or has length 1 or 2, it cannot have first and last characters to remove.
			// You can decide how you want to handle such cases, here we just return an empty string.
			return "";
		}

		// Use substring to remove the first and last characters.
		return input.substring(1, input.length() - 1);
	}


	@Inject(method = "onScheduledTick", at = @At("TAIL"))
	public void smolcoins$onScheduledTick(CallbackInfo ci) {
		try {
			Gson gson = new Gson();
			NbtElement original = ((BlockEntity) (Object) this).toNbt().get("CustomName");
			if (original != null) {
				String strOriginal = original.toString();
				String finalJson = removeFirstAndLastCharacter(strOriginal);
				TextObject x = gson.fromJson(finalJson, TextObject.class);
				String customName = x.text;
				if (customName.equalsIgnoreCase("[admin] smolcoin convertor")) {
					for (int slot = 0; slot < this.inventory.size(); slot++) {
						ItemStack itemStack = this.inventory.get(slot);
						Item item = itemStack.getItem();
						Identifier itemId = Registries.ITEM.getId(item);
						Integer value = Smolcoins.INSTANCE.getSMOLCOIN_CONVERSION().get(itemId);

						if (value != null) {
							int count = itemStack.getCount();
							int smolcoinValue = value;

							int totalSmolcoins = smolcoinValue * count;

							List<ItemStack> itemStacks = Arrays.stream(Smolcoins.INSTANCE.smolcoinsToItems(totalSmolcoins)).toList();
							if (!itemStacks.isEmpty()) {
								inventory.set(slot, ItemStack.EMPTY);
							}
							for (ItemStack i : itemStacks) {
								Integer newSlot = Smolcoins.INSTANCE.getNextEmptySlot(inventory);
								if (newSlot != null) {
									inventory.set(newSlot, i);
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e) {
			Smolcoins.INSTANCE.getLOGGER().error("Hopefully stopping crash <3");
			e.printStackTrace();

		}
	}
}
