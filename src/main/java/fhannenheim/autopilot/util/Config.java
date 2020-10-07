package fhannenheim.autopilot.util;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.io.File;

@Mod.EventBusSubscriber
public class Config {
    private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec CONFIG;

    public static ForgeConfigSpec.IntValue flight_level;
    public static ForgeConfigSpec.EnumValue<FlightType> default_flight_type;
    public static ForgeConfigSpec.EnumValue<OnArrive> on_arrive;
    static {
        init(builder);

        CONFIG = builder.build();
    }

    public static void init(ForgeConfigSpec.Builder config){
        config.comment("Autopilot config");
        flight_level = config
                .comment("Altitude the autopilot flies at. It will slowly rise to the specified y level and then stay there.\n" +
                        "The default is 400 so the Autopilot won't run into blocks")
                .defineInRange("autopilot.flight_level", 400, 1, 1000000);
        default_flight_type = config
                .comment("The default flight type that will be used if you don't specify anything in the flyto command.")
                .defineEnum("autopilot.default_flight_type", FlightType.ROCKETS);
        on_arrive = config
                .comment("What to do if the autopilot arrives at the destination." +
                        "\nIt can either disconnect, or try to land and then disconnect.")
                .defineEnum("autopilot.on_arrive", OnArrive.Disconnect);
    }


    public static void loadConfig(ForgeConfigSpec config, String path){
        final CommentedFileConfig file = CommentedFileConfig.builder(new File(path)).sync().autosave().writingMode(WritingMode.REPLACE).build();
        file.load();
        config.setConfig(file);
    }
}
