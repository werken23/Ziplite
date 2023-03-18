package net.rikkido.config;

import org.bukkit.configuration.ConfigurationSection;

public class ItemConfig {

    public ZiplineItemConfig ziplineItemconf;

    public ItemConfig() {
        ziplineItemconf = new ZiplineItemConfig();
    }

    public void load(ConfigurationSection sec){
        String SECTION_ITEM = "item";
        var inside = sec.getConfigurationSection(SECTION_ITEM);
        ziplineItemconf.load(inside);
    }
}
