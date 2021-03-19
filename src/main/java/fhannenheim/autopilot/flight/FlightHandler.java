package fhannenheim.autopilot.flight;


import com.sun.javafx.geom.Vec2d;
import fhannenheim.autopilot.AutopilotClient;
import fhannenheim.autopilot.util.FlightType;
import fhannenheim.autopilot.util.InventoryUtils;
import fhannenheim.autopilot.util.SpecialActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.text.DecimalFormat;

public class FlightHandler {
    public static FlightHandler INSTANCE;
    public FlightExecutor flightExecutor;
    public boolean isAutoFlying;
    public Vec3d destination;
    public FlightType flightType;
    public boolean shallDisconnect;
    private double totalDistance;

    public FlightHandler (){
        INSTANCE = this;
        flightExecutor = new FlightExecutor(this);
    }
    public void flyTo(Vec3d pos, FlightType type) {
        destination = pos;
        flightType = type;
        isAutoFlying = true;

        if (MinecraftClient.getInstance().player != null) {
            totalDistance = destination.distanceTo(MinecraftClient.getInstance().player.getPos());
        }
    }
    public void tick(){
        PlayerEntity playerEntity = MinecraftClient.getInstance().player;

        boolean rockets = AutopilotClient.rockets.wasPressed();
        boolean angle4040 = AutopilotClient.fourtyfourty.wasPressed();
        if (playerEntity != null && (rockets || angle4040)) {
            destination = null;
            if (!isAutoFlying && !playerEntity.isOnGround()) {
                startFlying(playerEntity);
                if (rockets) flightType = FlightType.ROCKETS;
                else flightType = FlightType.ANGLE4040;
                isAutoFlying = true;

                if (isAutoFlying && flightType == FlightType.ANGLE4040) {
                    flightExecutor.isDescending = true;
                }
            } else {
                isAutoFlying = false;
            }
        }

        if (playerEntity == null || !playerEntity.isAlive()) {
            isAutoFlying = false;
            destination = null;
            return;
        }
        if (playerEntity.isOnGround() && playerEntity.isFallFlying()) {
            playerEntity.stopFallFlying();
            isAutoFlying = false;
            destination = null;
            return;
        }

        if (isAutoFlying) {
            flightExecutor.preventRocket = false;
            if (!playerEntity.isFallFlying() && !playerEntity.isOnGround()) {
                // If the player isn't elytra flying but the autopilot is still on the elytra has probably broken. Replace it
                InventoryUtils.replaceElytra(playerEntity);

                // Start flying again
                startFlying(playerEntity);
            }
            if (destination != null) {
                Vec3d vec3d = EntityAnchorArgumentType.EntityAnchor.EYES.positionAt(playerEntity);
                double d = destination.x - vec3d.x;
                double f = destination.z - vec3d.z;

                playerEntity.yaw = MathHelper.wrapDegrees((float)(MathHelper.atan2(f,d) * 57.2957763671875D) - 90.0F);
            }
            if (InventoryUtils.currentElytraDurability(playerEntity) < AutopilotClient.CONFIG.low_durability && !InventoryUtils.hasDurableElytra(playerEntity)) {
                if (AutopilotClient.CONFIG.on_low_durability == SpecialActions.Alert) {
                    flightExecutor.preventRocket = true;
                    playerEntity.playSound(SoundEvents.BLOCK_BELL_USE, 4, 1);
                } else {
                    shallDisconnect = true;
                    isAutoFlying = false;
                    destination = null;
                }
            }

            if (flightType == FlightType.ROCKETS)
                flightExecutor.rocketFlight(playerEntity);

            if (flightType == FlightType.ANGLE4040)
                flightExecutor.fourtyfourtyFlight(playerEntity);
        }
    }
    public void renderTick() {
        if (shallDisconnect) {
            shallDisconnect = false;
            isAutoFlying = false;
            if (MinecraftClient.getInstance().world != null) {
                MinecraftClient.getInstance().world.disconnect();
            }

            MinecraftClient.getInstance().disconnect(new SaveLevelScreen(new TranslatableText("autopilot.disconnect")));
            MinecraftClient.getInstance().openScreen(new TitleScreen());
        }
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (destination == null || minecraft.player == null)
            return;

        double progress = 100 * (1 - new Vec2d(destination.x, destination.z).distance(minecraft.player.getX(), minecraft.player.getZ()) / totalDistance);

        DecimalFormat df = new DecimalFormat("###.#");
        String text = "Flying progress: " +
                df.format(progress)
                + "%";
        if (new Vec2d(destination.x, destination.z).distance(minecraft.player.getX(), minecraft.player.getZ()) <= 3)
            text = "Arrived at destination";
        minecraft.inGameHud.setOverlayMessage(new LiteralText(text), false);
    }
    public void startFlying(PlayerEntity playerEntity) {
        if(playerEntity.checkFallFlying())
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(new ClientCommandC2SPacket(playerEntity, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
    }
}
