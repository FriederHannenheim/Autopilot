package fhannenheim.autopilot;

import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.RootCommandNode;
import com.sun.javafx.geom.Vec2d;
import fhannenheim.autopilot.util.Config;
import fhannenheim.autopilot.util.FlightType;
import fhannenheim.autopilot.util.OnArrive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

public class FlightHandler {
    public static KeyBinding flyForward;
    public static FlightHandler instance;
    public boolean isAutoFlying;
    public Vec3d destination;
    public FlightType type;
    private int ticksSinceRocket;
    private boolean shallDisconnect;

    public void onClientSetup() {
        flyForward = new KeyBinding("keybind.autopilot.flyforward",
                GLFW.GLFW_KEY_V, "category.autopilot");
        ClientRegistry.registerKeyBinding(flyForward);
        instance = this;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        PlayerEntity playerEntity = Minecraft.getInstance().player;
        if (Minecraft.getInstance().currentScreen instanceof ChatScreen) {
            TextFieldWidget inputField = ((ChatScreen) Minecraft.getInstance().currentScreen).inputField;

            // Don't read this. It is bad. It could probably be improved but I don't care since it works
            if (inputField.getText().startsWith(".")) {
                inputField.setSuggestion(".flyto ~ ~".replace(inputField.getText(), ""));
                if (inputField.getText().startsWith(".flyto ")) {
                    inputField.setSuggestion("~ ~".substring(
                            MathHelper.clamp(numberOfSpaces(
                                    inputField.getText().replace(".flyto ", "")) * 2 + 1, 0, 3)));
                    if (numberOfSpaces(inputField.getText().replace(".flyto ", "")) * 2 + 1 >= 4 && inputField.getText().charAt(inputField.getText().length() - 1) == ' ') {
                        inputField.setSuggestion("[rockets,4040]");
                    }
                } else {
                    if (event.getKey() == GLFW.GLFW_KEY_TAB) {
                        inputField.setText(".flyto ");
                    }
                }
            }
        }
        if (playerEntity != null && flyForward.isPressed()) {
            if (!isAutoFlying) {
                playerEntity.rotationPitch = -3;
                if (!playerEntity.isElytraFlying()) {
                    destination = null;
                    playerEntity.startFallFlying();
                }
                destination = null;
                type = FlightType.ROCKETS;
                isAutoFlying = true;
            } else {
                isAutoFlying = false;
            }
        }
    }

    public void flyTo(Vec3d _destination, FlightType flightType) {
        destination = _destination;
        type = flightType;
        isAutoFlying = true;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) throws IOException {
        if (event.side != LogicalSide.CLIENT || event.phase != TickEvent.Phase.END)
            return;
        ticksSinceRocket++;
        PlayerEntity playerEntity = Minecraft.getInstance().player;
        if (playerEntity == null)
            return;


        CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<CommandSource>((RootCommandNode)Minecraft.getInstance().getConnection().commandDispatcher.getRoot());
        dispatcher.register(Commands.literal("flyto")
                .then(Commands.argument("location", Vec2Argument.vec2())
                        .then(Commands.literal("rockets"))
                        .then(Commands.literal("4040")))
                .then(Commands.argument("location", Vec2Argument.vec2()))
        );
        Minecraft.getInstance().getConnection().commandDispatcher = new CommandDispatcher<ISuggestionProvider>((RootCommandNode)dispatcher.getRoot());
        if (isAutoFlying) {
            if (!playerEntity.isElytraFlying() && !playerEntity.onGround) {
                // If the player isn't elytra flying but the autopilot is still on the elytra has probably broken. Replace it
                replaceElytra(playerEntity);

                // Start flying again
                playerEntity.startFallFlying();
                Minecraft.getInstance().getConnection().sendPacket(new CEntityActionPacket(playerEntity, CEntityActionPacket.Action.START_FALL_FLYING));
            }

            if (destination != null) {
                playerEntity.lookAt(EntityAnchorArgument.Type.EYES, destination);

            }
            playerEntity.rotationPitch = -3;

            PlayerInventory inventory = playerEntity.inventory;

            // Place new rockets in hand if needed
            refillRockets(playerEntity);

            if (destination != null && Vec2d.distance(destination.x , playerEntity.getPosX(), destination.z, playerEntity.getPosZ()) > 5) {
                // If the player is lower than the flying altitude and is flying too slow, use a rocket to boost speed
                if (Math.sqrt(Math.pow(Math.abs(playerEntity.getMotion().x), 2) + Math.pow(Math.abs(playerEntity.getMotion().z), 2)) < 1.5f
                        && playerEntity.getPosition().getY() < Config.flight_level.get()
                        && ticksSinceRocket > 3) {
                    Minecraft.getInstance().getConnection().sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                    ticksSinceRocket = 0;
                }
            }
            else {
                if (Config.on_arrive.get() == OnArrive.Disconnect) {
                    Autopilot.LOGGER.info("gae");
                    shallDisconnect = true;
                }else if(Config.on_arrive.get() == OnArrive.TryToLand){
                    playerEntity.playSound(SoundEvents.BLOCK_BELL_USE,4,1);
                }
            }

        }
    }
    @SubscribeEvent
    public void disconnect(TickEvent.RenderTickEvent event){
        if(shallDisconnect){
            shallDisconnect = false;
            isAutoFlying = false;
            if (Minecraft.getInstance().world != null) {
                Minecraft.getInstance().world.sendQuittingDisconnectingPacket();
            }

            Minecraft.getInstance().unloadWorld(new DirtMessageScreen(new TranslationTextComponent("menu.savingLevel")));
            Minecraft.getInstance().displayGuiScreen(new MainMenuScreen());
        }
    }

    public void flyToCoord(Vec3d pos, FlightType flightType) {
        destination = pos;
        type = flightType;

    }

    private void refillRockets(PlayerEntity player) {
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

    private void replaceElytra(PlayerEntity player) {
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

    private void click(Container container, int slotId) {
        final PlayerController playerController = Minecraft.getInstance().playerController;
        if (playerController != null)
            playerController.windowClick(container.windowId, slotId, 0, ClickType.PICKUP, Minecraft.getInstance().player);
    }

    private int toServerSlotId(final int clientSlotId) {
        // Hotbar
        if (clientSlotId <= 8)
            return clientSlotId + 36;
        // Offhand
        if (clientSlotId == 40)
            return 45;
        return clientSlotId;
    }

    private int numberOfSpaces(String string) {
        int counter = 0;
        for (int i = 0; i < string.length(); ++i) {
            if (string.charAt(i) == ' ')
                counter++;
        }
        return counter;
    }
}
