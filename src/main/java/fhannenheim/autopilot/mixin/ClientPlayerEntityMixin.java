package fhannenheim.autopilot.mixin;

import fhannenheim.autopilot.flight.FlightHandler;
import fhannenheim.autopilot.util.FlightType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(at = @At("HEAD"), method = "sendChatMessage(Ljava/lang/String;)V", cancellable = true)
    public void checkForFlytoCommand(String message,CallbackInfo info){
        if(message.startsWith("/flyto")){
            info.cancel();
            String[] commands = message.split(" ");
            if(commands.length < 3){
                showSyntax();
                return;
            }
            Vec3d pos;
            FlightType flightType = FlightType.ROCKETS;
            try {
                pos = new Vec3d(Integer.parseInt(commands[1]), 0, Integer.parseInt(commands[2]));
            } catch (NumberFormatException e){
                showSyntax();
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
                        return;
                }
            }

            FlightHandler.INSTANCE.flyTo(pos,flightType);
        }
    }
    private void showSyntax(){
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText("Invalid Syntax").fillStyle(Style.EMPTY.withColor(Formatting.RED)));
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText("Usage:\n" +
                "    /flyto ~ ~ [rockets/4040]\n" +
                "Example:\n    /flyto 123 456 rockets"));
    }
}
