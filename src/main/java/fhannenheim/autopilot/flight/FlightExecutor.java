package fhannenheim.autopilot.flight;

import com.sun.javafx.geom.Vec2d;
import fhannenheim.autopilot.AutopilotClient;
import fhannenheim.autopilot.util.InventoryUtils;
import fhannenheim.autopilot.util.SpecialActions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

public class FlightExecutor {
    public FlightHandler flightHandler;
    public int ticksSinceRocket;
    public boolean preventRocket;

    public boolean isDescending;
    public boolean pullDown;
    public boolean pullUp;
    public double currentVelocity;

    public FlightExecutor(FlightHandler flightHandler) {
        this.flightHandler = flightHandler;
    }


    public void rocketFlight(PlayerEntity playerEntity) {
        ticksSinceRocket++;


        playerEntity.pitch = playerEntity.getY() + 10 < AutopilotClient.CONFIG.flight_level ? -15 : -3;

        // Place new rockets in hand if needed
        InventoryUtils.refillRockets(playerEntity);

        if (flightHandler.destination == null || Vec2d.distance(flightHandler.destination.x, flightHandler.destination.z, playerEntity.getX(), playerEntity.getZ()) > 3) {
            // If the player is lower than the flying altitude and is flying too slow, use a rocket to boost speed
            if (Math.sqrt(Math.pow(playerEntity.getVelocity().x, 2) + Math.pow(playerEntity.getVelocity().z, 2)) < AutopilotClient.CONFIG.rocket_speed
                    && playerEntity.getPos().getY() < AutopilotClient.CONFIG.flight_level
                    && ticksSinceRocket > 3) {
                useRocket();
                ticksSinceRocket = 0;
            }
        } else if (flightHandler.destination != null) {
            if (AutopilotClient.CONFIG.on_arrive == SpecialActions.Disconnect) {
                flightHandler.shallDisconnect = true;
                playerEntity.pitch = -90;
                useRocket();
                ticksSinceRocket = 0;
            } else if (AutopilotClient.CONFIG.on_arrive == SpecialActions.Alert) {
                playerEntity.playSound(SoundEvents.BLOCK_BELL_USE, 4, 1);
            }
        }
    }
    // Btw most of this is not my code. Someone made a pull request on the forge version and I just copy pasted that here. So don't shame me for it
    public void fourtyfourtyFlight(PlayerEntity playerEntity) {
        this.currentVelocity = getVelocity(playerEntity);

        if(playerEntity.getPos().y > AutopilotClient.CONFIG.flight_level)
            isDescending = true;

        if (this.isDescending) {
            this.pullUp = false;
            this.pullDown = true;
            if (this.currentVelocity >= FlightValues.pullDownMaxVelocity) {
                this.isDescending = false;
                this.pullDown = false;
                this.pullUp = true;
            }
        } else {
            this.pullUp = true;
            this.pullDown = false;
            if (this.currentVelocity <= FlightValues.pullUpMinVelocity) {
                this.isDescending = true;
                this.pullDown = true;
                this.pullUp = false;
            }
        }


        if (this.pullUp) {
            playerEntity.pitch = (float)((double)playerEntity.pitch - FlightValues.pullUpSpeed);
            if ((double)playerEntity.pitch <= FlightValues.pullUpAngle) {
                playerEntity.pitch = (float)FlightValues.pullUpAngle;
            }
        }

        if (this.pullDown) {
            playerEntity.pitch = (float)((double)playerEntity.pitch + FlightValues.pullDownSpeed);
            if ((double)playerEntity.pitch >= FlightValues.pullDownAngle) {
                playerEntity.pitch = (float)FlightValues.pullDownAngle;
            }
        }

        if (flightHandler.destination != null && Vec2d.distance(flightHandler.destination.x, flightHandler.destination.z, playerEntity.getX(), playerEntity.getZ()) < 3) {
            if (AutopilotClient.CONFIG.on_arrive == SpecialActions.Disconnect) {
                flightHandler.shallDisconnect = true;
                playerEntity.pitch = -90;
                useRocket();
                ticksSinceRocket = 0;
            } else if (AutopilotClient.CONFIG.on_arrive == SpecialActions.Alert) {
                playerEntity.playSound(SoundEvents.BLOCK_BELL_USE, 4, 1);
            }
        }
    }

    private double getVelocity(PlayerEntity playerEntity) {
        return playerEntity.getVelocity().length();
    }

    @SuppressWarnings("ConstantConditions")
    private void useRocket() {
        if (preventRocket)
            return;
        try {
            MinecraftClient.getInstance().getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND));
        } catch (NullPointerException e) {
            AutopilotClient.LOGGER.warn("Couldn't fire rocket");
        }
    }
    private static class FlightValues{
        public static final double pullUpAngle = -46.633514D;
        public static final double pullDownAngle = 37.19872D;
        public static final double pullUpMinVelocity = 1.9102669D;
        public static final double pullDownMaxVelocity = 2.3250866D;
        public static final double pullUpSpeed = 6.4815372D;
        public static final double pullDownSpeed = 0.61635801D;
    }
}
