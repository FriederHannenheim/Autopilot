package fhannenheim.autopilot.util;

import com.google.common.collect.ImmutableSet;
import fhannenheim.autopilot.Autopilot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class InventoryUtils {
    public static void refillRockets(PlayerEntity player) {
        PlayerInventory inventory = player.inventory;
        if (inventory.mainInventory.get(inventory.currentItem).getItem() != Items.FIREWORK_ROCKET) {
            if (inventory.hasAny(ImmutableSet.of(Items.FIREWORK_ROCKET))) {
                int slot = -1;
                for (int i = 0; i < inventory.mainInventory.size(); ++i) {
                    if (inventory.mainInventory.get(i).getItem() == Items.FIREWORK_ROCKET) {
                        slot = i;
                    }
                }
                if (slot != -1) {
                    PlayerContainer container = player.container;
                    click(container, toServerSlotId(slot));
                    click(container, toServerSlotId(inventory.currentItem));
                }
            }
        }
    }

    public static void replaceElytra(PlayerEntity player) {
        PlayerInventory inventory = player.inventory;
        ItemStack elytra = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        Autopilot.LOGGER.info(inventory.getSlotFor(elytra));
        if (elytra.getDamage() == elytra.getMaxDamage() - 1) {
            if (inventory.hasAny(ImmutableSet.of(Items.ELYTRA))) {
                int slot = -1;
                for (int i = 0; i < inventory.mainInventory.size(); ++i) {
                    if (inventory.getStackInSlot(i).getItem() == Items.ELYTRA &&
                            inventory.getStackInSlot(i).getDamage() != inventory.getStackInSlot(i).getMaxDamage() - 1) {
                        slot = i;
                    }
                }
                if (slot != -1) {
                    Autopilot.LOGGER.info("le why?");
                    PlayerContainer container = player.container;
                    click(container, toServerSlotId(slot));
                    click(container, 6);
                    click(container, toServerSlotId(slot));
                }
            }
        }
    }

    private static void click(Container container, int slotId) {
        final PlayerController playerController = Minecraft.getInstance().playerController;
        if (playerController != null && Minecraft.getInstance().player != null)
            playerController.windowClick(container.windowId, slotId, 0, ClickType.PICKUP, Minecraft.getInstance().player);
    }

    private static int toServerSlotId(final int clientSlotId) {
        // Hotbar
        if (clientSlotId <= 8)
            return clientSlotId + 36;
        // Offhand
        if (clientSlotId == 40)
            return 45;
        return clientSlotId;
    }
}
