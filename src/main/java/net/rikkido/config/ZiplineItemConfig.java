package net.rikkido.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

public class ZiplineItemConfig {

    public Config<List<String>> itemshapeConfig;
    public Config<List<Map<String, String>>> itemPair;

    public ZiplineItemConfig(){
        itemshapeConfig = new Config<List<String>>("ItemShape", new ArrayList<String>());
        itemPair = new Config<List<Map<String, String>>>("RecipeItem", new ArrayList<Map<String, String>>());

    }

    public void load(ConfigurationSection sec){
        String SECTION_ITEM_RECIPE_ZIPLINE = "ZiplineRecipe";
        var inside = sec.getConfigurationSection(SECTION_ITEM_RECIPE_ZIPLINE);
        itemshapeConfig.load(inside);
        itemPair.load(inside);
    }
    
}
