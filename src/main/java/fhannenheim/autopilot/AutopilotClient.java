package fhannenheim.autopilot;

import fhannenheim.autopilot.flight.FlightHandler;
import fhannenheim.autopilot.util.ElytraConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class AutopilotClient implements ClientModInitializer {
    public static Logger LOGGER;
    public static ElytraConfig CONFIG;
    public static KeyBinding fourtyfourty;
    public static KeyBinding rockets;
    @Override
    public void onInitializeClient() {
        LOGGER = LogManager.getLogger("Autopilot");
        AutoConfig.register(ElytraConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(ElytraConfig.class).getConfig();


        rockets = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autopilot.rockets",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_N,
                "category.autopilot"
        ));
        fourtyfourty = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autopilot.4040",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                "category.autopilot"
        ));
        FlightHandler flightHandler = new FlightHandler();
        ClientTickEvents.END_WORLD_TICK.register((client) -> flightHandler.tick());
        ClientTickEvents.END_CLIENT_TICK.register((client) -> flightHandler.renderTick());
    }
}
