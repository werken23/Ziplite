package net.rikkido;

import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

public interface IItemBase extends Listener {

    ItemStack createItem();

    ShapedRecipe createRecipe(ItemStack item);

    void dropItem(Location loc, int amount);

    void setAmount(ItemStack items, int amount);

    boolean isItem(ItemStack items);

}
