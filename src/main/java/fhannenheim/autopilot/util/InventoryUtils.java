package fhannenheim.autopilot.util;

import com.google.common.collect.ImmutableSet;
import fhannenheim.autopilot.AutopilotClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public class InventoryUtils {
    public static void refillRockets(PlayerEntity player) {
        PlayerInventory inventory = player.inventory;
        if (inventory.main.get(inventory.selectedSlot).getItem() != Items.FIREWORK_ROCKET) {
            if (inventory.containsAny(ImmutableSet.of(Items.FIREWORK_ROCKET))) {
                int slot = -1;
                for (int i = 0; i < inventory.main.size(); ++i) {
                    if (inventory.main.get(i).getItem() == Items.FIREWORK_ROCKET) {
                        slot = i;
                    }
                }
                if (slot != -1) {
                    AutopilotClient.LOGGER.info("Hekc");
                    ScreenHandler container = player.currentScreenHandler;
                    click(container, toServerSlotId(slot));
                    click(container, toServerSlotId(inventory.selectedSlot));
                }
            }
        }
    }

    public static void replaceElytra(PlayerEntity player) {
        PlayerInventory inventory = player.inventory;
        ItemStack elytra = player.getEquippedStack(EquipmentSlot.CHEST);
        if (elytra.getDamage() == elytra.getMaxDamage() - 1) {
            if (inventory.containsAny(ImmutableSet.of(Items.ELYTRA))) {
                int slot = -1;
                for (int i = 0; i < inventory.main.size(); ++i) {
                    if (inventory.getStack(i).getItem() == Items.ELYTRA &&
                            inventory.getStack(i).getDamage() != inventory.getStack(i).getMaxDamage() - 1) {
                        slot = i;
                    }
                }
                if (slot != -1 && toServerSlotId(slot) != 6) {
                    ScreenHandler container = player.currentScreenHandler;
                    click(container, 6);
                    click(container, toServerSlotId(slot));
                    click(container, 6);
                }
            }
        }
    }

    public static boolean hasDurableElytra(PlayerEntity player) {
        PlayerInventory inventory = player.inventory;
        if (inventory.containsAny(ImmutableSet.of(Items.ELYTRA))) {
            int slot = -1;
            for (int i = 0; i < inventory.main.size(); ++i) {
                if (inventory.getStack(i).getItem() == Items.ELYTRA &&
                        inventory.getStack(i).getDamage() < inventory.getStack(i).getMaxDamage() - AutopilotClient.CONFIG.low_durability) {
                    slot = i;
                }
            }
            return slot != -1 && toServerSlotId(slot) != 6;
        } else {
            return false;
        }
    }

    public static int currentElytraDurability(PlayerEntity player) {
        ItemStack elytra = player.getEquippedStack(EquipmentSlot.CHEST);
        return elytra.getMaxDamage() - elytra.getDamage();
    }

    private static void click(ScreenHandler container, int slotId) {
        final ClientPlayerInteractionManager playerController = MinecraftClient.getInstance().interactionManager;
        if (playerController != null && MinecraftClient.getInstance().player != null)
            playerController.clickSlot(container.syncId, slotId, 0, SlotActionType.PICKUP, MinecraftClient.getInstance().player);
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
