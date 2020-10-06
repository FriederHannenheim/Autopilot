package fhannenheim.autopilot.chat;

import com.mojang.brigadier.CommandDispatcher;
import fhannenheim.autopilot.Autopilot;
import fhannenheim.autopilot.FlightHandler;
import fhannenheim.autopilot.util.FlightType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.http.auth.AUTH;
import org.lwjgl.glfw.GLFW;


public class ChatCommandHandler {
    @SubscribeEvent
    public void onChatInput(ClientChatEvent event){
        String message = event.getMessage();
        Autopilot.LOGGER.warn(message);
        if(message.startsWith("/flyto "))
        {
            String[] commands = message.split(" ");
            if(commands.length < 3){
                showSyntax();
                event.setCanceled(true);
                return;
            }
            Vec3d pos = Vec3d.ZERO;
            FlightType flightType = FlightType.ROCKETS;
            try {
                pos = new Vec3d(Integer.parseInt(commands[1]),0,Integer.parseInt(commands[2]));
            } catch (NumberFormatException e){
                showSyntax();
                event.setCanceled(true);
                return;
            }
            if(commands.length > 3){
                switch (commands[3]){
                    case "rockets":
                        break;
                    case "4040":
                        flightType = FlightType.ANGLE4040;
                        break;
                    default:
                        showSyntax();
                        event.setCanceled(true);
                        return;
                }
            }

            FlightHandler.instance.flyTo(pos,flightType);
            event.setCanceled(true);
        }
    }

    private void showSyntax(){
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent("Invalid Syntax").applyTextStyle(TextFormatting.RED));
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent("Usage:\n" +
                "    /flyto ~ ~ [rockets/4040]\n" +
                "Example:\n    /flyto 123 456 rockets"));
    }
}
