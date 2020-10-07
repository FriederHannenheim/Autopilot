package fhannenheim.autopilot;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class KeybindHandler {

    public static KeyBinding flyForwardROCKETS;
    public static KeyBinding flyForward4040;

    public static void onClientSetup() {
        flyForwardROCKETS = new KeyBinding("keybind.autopilot.flyforwardrockets", GLFW.GLFW_KEY_V, "category.autopilot");
        flyForward4040 = new KeyBinding("keybind.autopilot.flyforward4040", GLFW.GLFW_KEY_B, "category.autopilot");
        ClientRegistry.registerKeyBinding(flyForwardROCKETS);
        ClientRegistry.registerKeyBinding(flyForward4040);
    }
}
