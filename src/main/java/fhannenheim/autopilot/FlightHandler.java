package fhannenheim.autopilot;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.RootCommandNode;
import com.sun.javafx.geom.Vec2d;
import fhannenheim.autopilot.util.Config;
import fhannenheim.autopilot.util.FlightType;
import fhannenheim.autopilot.util.InventoryUtils;
import fhannenheim.autopilot.util.OnArrive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;

public class FlightHandler {
    public static KeyBinding flyForward;
    public static FlightHandler instance;
    public boolean isAutoFlying;
    public Vec3d destination;
    public FlightType type;
    private int ticksSinceRocket;
    private boolean shallDisconnect;
    private double totalDistance;

    public void onClientSetup() {
        flyForward = new KeyBinding("keybind.autopilot.flyforward",
                GLFW.GLFW_KEY_V, "category.autopilot");
        ClientRegistry.registerKeyBinding(flyForward);
        instance = this;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        PlayerEntity playerEntity = Minecraft.getInstance().player;
        if (playerEntity != null && flyForward.isPressed()) {
            destination = null;
            if (!isAutoFlying) {
                playerEntity.rotationPitch = -3;
                if (!playerEntity.isElytraFlying()) {
                    playerEntity.startFallFlying();
                }
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
        if (Minecraft.getInstance().player != null) {
            totalDistance = destination.distanceTo(Minecraft.getInstance().player.getPositionVec());
        }
    }

    // this is for the commandDispatcher injection, I can't parameterize it and IDEA won't shut up about it.
    @SuppressWarnings({"unchecked", "rawtypes"})
    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.side != LogicalSide.CLIENT || event.phase != TickEvent.Phase.END)
            return;
        ticksSinceRocket++;
        PlayerEntity playerEntity = Minecraft.getInstance().player;
        if (playerEntity == null || !playerEntity.isAlive()) {
            isAutoFlying = false;
            destination = null;
            return;
        }

        if (Minecraft.getInstance().getConnection() != null) {
            CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<CommandSource>((RootCommandNode) Minecraft.getInstance().getConnection().commandDispatcher.getRoot());
            dispatcher.register(Commands.literal("flyto")
                    .then(Commands.argument("location", Vec2Argument.vec2())
                            .then(Commands.literal("rockets"))
                            .then(Commands.literal("4040")))
                    .then(Commands.argument("location", Vec2Argument.vec2()))
            );
            Minecraft.getInstance().getConnection().commandDispatcher = new CommandDispatcher<ISuggestionProvider>((RootCommandNode) dispatcher.getRoot());
        }


        if (isAutoFlying) {
            if (!playerEntity.isElytraFlying() && !playerEntity.onGround) {
                // If the player isn't elytra flying but the autopilot is still on the elytra has probably broken. Replace it
                InventoryUtils.replaceElytra(playerEntity);

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
            InventoryUtils.refillRockets(playerEntity);

            if (destination == null || Vec2d.distance(destination.x, destination.z, playerEntity.getPosX(), playerEntity.getPosZ()) > 3) {
                // If the player is lower than the flying altitude and is flying too slow, use a rocket to boost speed
                if (Math.sqrt(Math.pow(Math.abs(playerEntity.getMotion().x), 2) + Math.pow(Math.abs(playerEntity.getMotion().z), 2)) < 1.5f
                        && playerEntity.getPosition().getY() < Config.flight_level.get()
                        && ticksSinceRocket > 3) {
                    Minecraft.getInstance().getConnection().sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                    ticksSinceRocket = 0;
                }
            } else if (destination != null) {
                if (Config.on_arrive.get() == OnArrive.Disconnect) {
                    shallDisconnect = true;
                    playerEntity.rotationPitch = -90;
                    Minecraft.getInstance().getConnection().sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                    ticksSinceRocket = 0;
                } else if (Config.on_arrive.get() == OnArrive.TryToLand) {
                    playerEntity.playSound(SoundEvents.BLOCK_BELL_USE, 4, 1);
                }
            }

        }
    }

    @SubscribeEvent
    public void disconnect(TickEvent.RenderTickEvent event) {
        if (shallDisconnect) {
            shallDisconnect = false;
            isAutoFlying = false;
            if (Minecraft.getInstance().world != null) {
                Minecraft.getInstance().world.sendQuittingDisconnectingPacket();
            }

            Minecraft.getInstance().unloadWorld(new DirtMessageScreen(new TranslationTextComponent("autopilot.disconnect")));
            Minecraft.getInstance().displayGuiScreen(new MainMenuScreen());
        }
    }

    @SubscribeEvent
    public void renderOverlay(TickEvent.RenderTickEvent event) {

        Minecraft minecraft = Minecraft.getInstance();
        if (destination == null || minecraft.player == null)
            return;

        double progress = 100 * (1 - new Vec2d(destination.x, destination.z).distance(minecraft.player.getPosX(), minecraft.player.getPosZ()) / totalDistance);

        DecimalFormat df = new DecimalFormat("###.#");
        String text = "Flying progress: " +
                df.format(progress)
                + "%";
        if (new Vec2d(destination.x, destination.z).distance(minecraft.player.getPosX(), minecraft.player.getPosZ()) <= 3)
            text = "Arrived at destination";
        minecraft.ingameGUI.setOverlayMessage(text, false);
    }
}
