package art.chibi.telemetry;

import java.util.logging.Logger;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;

public class TelemetryBootstrap implements PluginBootstrap {
    private static final Logger LOGGER = Logger.getLogger("Telemetry");

    @Override
    public void bootstrap(BootstrapContext context) {
        LOGGER.info("Telemetry bootstrapping...");
        // Register a new handler for the freeze lifecycle event on the enchantment
        // registry
        // context.getLifecycleManager().registerEventHandler(RegistryEvents.ENCHANTMENT.freeze().newHandler(event
        // -> {
        // event.registry().register(
        // EnchantmentKeys.create(HomingEnchantmentConstants.ENCHANTMENT_KEY),
        // b -> b.description(HomingEnchantmentConstants.ENCHANTMENT_TEXT)
        // .supportedItems(RegistrySet.keySet(RegistryKey.ITEM, ItemTypeKeys.CROSSBOW,
        // ItemTypeKeys.BOW))
        // .anvilCost(1)
        // .maxLevel(25)
        // .weight(10)
        // .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(1, 1))
        // .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(3, 1))
        // .activeSlots(EquipmentSlotGroup.ANY));
        // }));
    }
}