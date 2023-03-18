package net.rikkido.config;

import org.bukkit.configuration.ConfigurationSection;


public class ZiplineConfig {

    public Config<Double> Speed; // block per tick
    public Config<Double> MaxRadius; // negative value means infinity

    public ZiplineConfig() {
        Speed = new Config<Double>("speed", 1.0);
        MaxRadius = new Config<Double>("max_radius", -1.0);
    }

    public void load(ConfigurationSection sec) {
        String SECTION_ZIPLINE = "zipline";
        var insideSec = sec.getConfigurationSection(SECTION_ZIPLINE);
        Speed.load(insideSec);
        MaxRadius.load(insideSec);
    }
}
