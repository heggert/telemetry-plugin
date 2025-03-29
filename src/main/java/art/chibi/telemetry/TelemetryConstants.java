package art.chibi.telemetry;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public class TelemetryConstants {
    public static final String ENCHANTMENT_NAMESPACE = "chibi";
    public static final String ENCHANTMENT_NAME = "homing";
    public static final Component ENCHANTMENT_TEXT = Component.text("Homing");
    public static final Key ENCHANTMENT_KEY = Key.key(ENCHANTMENT_NAMESPACE + ":" + ENCHANTMENT_NAME);
}