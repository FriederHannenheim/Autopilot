package fhannenheim.autopilot.chat;

import fhannenheim.autopilot.flight.FlightHandler;
import fhannenheim.autopilot.util.FlightType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class ChatCommandHandler {
    @SubscribeEvent
    public void onChatInput(ClientChatEvent event){
        String message = event.getMessage();
        if(message.startsWith("/flyto "))
        {
            String[] commands = message.split(" ");
            if(commands.length < 3){
                showSyntax();
                event.setCanceled(true);
                return;
            }
            Vector3d pos;
            FlightType flightType = FlightType.ROCKETS;
            try {
                pos = new Vector3d(Integer.parseInt(commands[1]), 0, Integer.parseInt(commands[2]));
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
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent("Invalid Syntax").mergeStyle(TextFormatting.RED));
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(new StringTextComponent("Usage:\n" +
                "    /flyto ~ ~ [rockets/4040]\n" +
                "Example:\n    /flyto 123 456 rockets"));
    }
}
