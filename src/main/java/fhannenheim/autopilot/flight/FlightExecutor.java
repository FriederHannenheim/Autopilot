package fhannenheim.autopilot.flight;

import com.sun.javafx.geom.Vec2d;
import fhannenheim.autopilot.Autopilot;
import fhannenheim.autopilot.util.Config;
import fhannenheim.autopilot.util.ElytraConfig;
import fhannenheim.autopilot.util.InventoryUtils;
import fhannenheim.autopilot.util.SpecialActions;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.client.MinecraftForgeClient;

public class FlightExecutor {
    public FlightHandler flightHandler;
    public int ticksSinceRocket;
    private FlightPhase flightPhase;
    public boolean preventRocket;

    public boolean isDescending;
    public boolean pullDown;
    public boolean pullUp;
    public double currentVelocity;
    public ElytraConfig config = new ElytraConfig();

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
        this.currentVelocity = getVelocity(playerEntity);

        if (this.isDescending) {
            Autopilot.LOGGER.warn("here3");
            this.pullUp = false;
            this.pullDown = true;
            if (this.currentVelocity >= this.config.pullDownMaxVelocity) {
                Autopilot.LOGGER.warn("here1");
                this.isDescending = false;
                this.pullDown = false;
                this.pullUp = true;
            }
        } else {
            this.pullUp = true;
            this.pullDown = false;
            if (this.currentVelocity <= this.config.pullUpMinVelocity) {
                this.isDescending = true;
                this.pullDown = true;
                this.pullUp = false;
            }
        }


        if (this.pullUp) {
            playerEntity.rotationPitch = (float)((double)playerEntity.rotationPitch - this.config.pullUpSpeed);
            if ((double)playerEntity.rotationPitch <= this.config.pullUpAngle) {
                playerEntity.rotationPitch = (float)this.config.pullUpAngle;
            }
        }

        if (this.pullDown) {
            playerEntity.rotationPitch = (float)((double)playerEntity.rotationPitch + this.config.pullDownSpeed);
            if ((double)playerEntity.rotationPitch >= this.config.pullDownAngle) {
                playerEntity.rotationPitch = (float)this.config.pullDownAngle;
            }
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
