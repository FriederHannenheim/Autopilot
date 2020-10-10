package fhannenheim.autopilot.flight;

import com.sun.javafx.geom.Vec2d;
import fhannenheim.autopilot.Autopilot;
import fhannenheim.autopilot.util.Config;
import fhannenheim.autopilot.util.InventoryUtils;
import fhannenheim.autopilot.util.SpecialActions;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;

public class FlightExecutor {
    public FlightHandler flightHandler;
    public int ticksSinceRocket;
    private FlightPhase flightPhase;
    public boolean preventRocket;

    public FlightExecutor(FlightHandler flightHandler) {
        this.flightHandler = flightHandler;
    }


    public void rocketFlight(PlayerEntity playerEntity) {
        ticksSinceRocket++;


        playerEntity.rotationPitch = playerEntity.getPosY() + 10 < Config.flight_level.get() ? -15 : -3;

        // Place new rockets in hand if needed
        InventoryUtils.refillRockets(playerEntity);

        if (flightHandler.destination == null || Vec2d.distance(flightHandler.destination.x, flightHandler.destination.z, playerEntity.getPosX(), playerEntity.getPosZ()) > 3) {
            // If the player is lower than the flying altitude and is flying too slow, use a rocket to boost speed
            if (Math.sqrt(Math.pow(playerEntity.getMotion().x, 2) + Math.pow(playerEntity.getMotion().z, 2)) < (playerEntity.getPosY() + 10 < Config.flight_level.get() ? 1 : 1.5f)
                    && playerEntity.getPosition().getY() < Config.flight_level.get()
                    && ticksSinceRocket > 3) {
                useRocket();
                ticksSinceRocket = 0;
            }
        } else if (flightHandler.destination != null) {
            if (Config.on_arrive.get() == SpecialActions.Disconnect) {
                flightHandler.shallDisconnect = true;
                playerEntity.rotationPitch = -90;
                useRocket();
                ticksSinceRocket = 0;
            } else if (Config.on_arrive.get() == SpecialActions.Alert) {
                playerEntity.playSound(SoundEvents.BLOCK_BELL_USE, 4, 1);
            }
        }
    }

    // values from https://www.reddit.com/r/Minecraft/comments/5ic9la/using_a_genetic_algorithm_to_power_infinite/
    public void fourtyfourtyFlight(PlayerEntity playerEntity) {
        // Place new rockets in hand if needed
        InventoryUtils.refillRockets(playerEntity);

        // apparently switch statements can't handle null-objects
        flightPhase = flightPhase == null ? FlightPhase.DESCEND : flightPhase;

        switch (flightPhase) {
            case ASCEND:
                playerEntity.rotationPitch = -49.44969f;
                break;
            case DESCEND:
                playerEntity.rotationPitch = 37.7458839f;
                break;
            default:
                flightPhase = FlightPhase.DESCEND;
                break;
        }

        double velocity = getVelocity(playerEntity);
        if (flightPhase == FlightPhase.DESCEND && velocity > 2.08719635f)
            flightPhase = FlightPhase.ASCEND;
        else if (flightPhase == FlightPhase.ASCEND && velocity < 0.224041611f)
            flightPhase = FlightPhase.DESCEND;
        if (flightPhase == FlightPhase.ASCEND && playerEntity.getPosY() < Config.flight_level.get() && velocity < 0.75f) {
            useRocket();
            ticksSinceRocket = 0;
        }
        if (flightHandler.destination != null && Vec2d.distance(flightHandler.destination.x, flightHandler.destination.z, playerEntity.getPosX(), playerEntity.getPosZ()) < 3) {
            if (Config.on_arrive.get() == SpecialActions.Disconnect) {
                flightHandler.shallDisconnect = true;
                playerEntity.rotationPitch = -90;
                useRocket();
                ticksSinceRocket = 0;
            } else if (Config.on_arrive.get() == SpecialActions.Alert) {
                playerEntity.playSound(SoundEvents.BLOCK_BELL_USE, 4, 1);
            }
        }
    }

    private double getVelocity(PlayerEntity playerEntity) {
        return Math.sqrt(
                Math.pow(playerEntity.getMotion().x, 2) +
                        Math.pow(playerEntity.getMotion().y, 2) +
                        Math.pow(playerEntity.getMotion().z, 2)
        );
    }

    @SuppressWarnings("ConstantConditions")
    private void useRocket() {
        if (preventRocket)
            return;
        try {
            Minecraft.getInstance().getConnection().sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
        } catch (NullPointerException e) {
            Autopilot.LOGGER.warn("Couldn't fire rocket");
        }
    }

    private enum FlightPhase {
        ASCEND,
        DESCEND,
    }
}
