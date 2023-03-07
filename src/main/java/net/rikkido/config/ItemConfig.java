package net.rikkido.config;

import org.bukkit.configuration.ConfigurationSection;

public class ItemConfig {

    private static String SECTION_ITEM = "item";

    public ZiplineItemConfig ziplineItemconf;

    public ItemConfig() {
        ziplineItemconf = new ZiplineItemConfig();
    }

    public void load(ConfigurationSection sec){
        var inside = sec.getConfigurationSection(SECTION_ITEM);
        ziplineItemconf.load(inside);
    }
}
