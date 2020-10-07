package fhannenheim.autopilot;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.RootCommandNode;
import com.sun.javafx.geom.Vec2d;
import fhannenheim.autopilot.flight.FlightExecutor;
import fhannenheim.autopilot.util.FlightType;
import fhannenheim.autopilot.util.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.command.arguments.Vec2Argument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.text.DecimalFormat;

public class FlightHandler {
    public static FlightHandler instance;
    public FlightExecutor flightExecutor;
    public boolean isAutoFlying;
    public Vec3d destination;
    public FlightType flightType;
    public boolean shallDisconnect;
    private double totalDistance;

    public void onClientSetup() {
        instance = this;
        flightExecutor = new FlightExecutor(this);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        PlayerEntity playerEntity = Minecraft.getInstance().player;

        boolean rockets = KeybindHandler.flyForwardROCKETS.isPressed();
        boolean angle4040 = KeybindHandler.flyForward4040.isPressed();
        if (playerEntity != null && (rockets || angle4040)) {
            destination = null;
            if (!isAutoFlying && !playerEntity.onGround) {
                if (!playerEntity.isElytraFlying()) {
                    playerEntity.startFallFlying();
                }
                if (rockets) flightType = FlightType.ROCKETS;
                else flightType = FlightType.ANGLE4040;
                isAutoFlying = true;
            } else {
                isAutoFlying = false;
            }
        }
    }

    public void flyTo(Vec3d pos, FlightType type) {
        destination = pos;
        this.flightType = type;
        isAutoFlying = true;
        if (Minecraft.getInstance().player != null) {
            totalDistance = destination.distanceTo(Minecraft.getInstance().player.getPositionVec());
        }
    }


    @SubscribeEvent
    // this is for the commandDispatcher injection, I can't parameterize it and IDEA won't shut up about it.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.side != LogicalSide.CLIENT || event.phase != TickEvent.Phase.END)
            return;
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
        PlayerEntity playerEntity = Minecraft.getInstance().player;
        if (playerEntity == null || !playerEntity.isAlive()) {
            isAutoFlying = false;
            destination = null;
            return;
        }
        if (playerEntity.onGround) {
            playerEntity.stopFallFlying();
            isAutoFlying = false;
            destination = null;
            return;
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

            if (flightType == FlightType.ROCKETS)
                flightExecutor.rocketFlight(playerEntity);

            if (flightType == FlightType.ANGLE4040)
                flightExecutor.fourtyfourtyFlight(playerEntity);
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
