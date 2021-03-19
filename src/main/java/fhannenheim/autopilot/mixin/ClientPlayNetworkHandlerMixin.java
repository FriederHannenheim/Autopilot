package fhannenheim.autopilot.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.Vec2ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow private CommandDispatcher<CommandSource> commandDispatcher;

    @Inject(at = @At("TAIL"),method = "onCommandTree(Lnet/minecraft/network/packet/s2c/play/CommandTreeS2CPacket;)V")
    private void addCommand(CallbackInfo info){
        CommandDispatcher<ServerCommandSource> dispatcher = new CommandDispatcher<>((RootCommandNode) commandDispatcher.getRoot());
        dispatcher.register(
                CommandManager.literal("flyto")
                        .then(CommandManager.argument("location", Vec2ArgumentType.vec2())
                                .then(CommandManager.literal("rockets"))
                                .then(CommandManager.literal("4040")))
                        .then(CommandManager.argument("location",Vec2ArgumentType.vec2()))
        );
        commandDispatcher = new CommandDispatcher<>((RootCommandNode)dispatcher.getRoot());
    }
}
