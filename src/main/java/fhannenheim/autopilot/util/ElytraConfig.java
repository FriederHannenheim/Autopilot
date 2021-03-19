package fhannenheim.autopilot.util;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "elytraconfig")
public class ElytraConfig implements ConfigData {
    @ConfigEntry.BoundedDiscrete(min = 1, max = 1000)
    @Comment("Altitude the autopilot flies at. It will slowly rise to the specified y level and then stay there. The default is 350 so the Autopilot won't run into blocks")
    public int flight_level = 350;

    @Comment("The default flight type that will be used if you don't specify anything in the flyto command.")
    public FlightType default_flight_type;

    @Comment("What to do if the autopilot arrives at the destination. It can either disconnect, or try to land and alert you.")
    public SpecialActions on_arrive;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 200)
    @Comment("What is defined as low durability")
    public int low_durability = 20;

    @Comment("What to do if the current elytra has low durability and there's no other to replace it in your inventory. It can either disconnect, or try to land and alert you.")
    public SpecialActions on_low_durability;

    @Override
    public void validatePostLoad() throws ValidationException {
        if(flight_level > 1000 || flight_level < 1)
            throw new ValidationException("flight_level must be between 1 and 1000");
        if(low_durability > 200 || low_durability < 1)
            throw new ValidationException("low_durabillity must be between 1 and 200");
    }
}
