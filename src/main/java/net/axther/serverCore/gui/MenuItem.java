package net.axther.serverCore.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a single clickable item within a {@link Menu}.
 */
public class MenuItem {

    private final ItemStack displayItem;
    private final Consumer<Player> onClick;
    private final Consumer<Player> onRightClick;
    private final Predicate<Player> viewCondition;

    private MenuItem(ItemStack displayItem, Consumer<Player> onClick,
                     Consumer<Player> onRightClick, Predicate<Player> viewCondition) {
        this.displayItem = displayItem;
        this.onClick = onClick;
        this.onRightClick = onRightClick;
        this.viewCondition = viewCondition;
    }

    public ItemStack getDisplayItem() {
        return displayItem;
    }

    public Consumer<Player> getOnClick() {
        return onClick;
    }

    public Consumer<Player> getOnRightClick() {
        return onRightClick;
    }

    /**
     * Returns whether this item should be visible to the given player.
     * A null view condition means always visible.
     */
    public boolean isVisibleTo(Player player) {
        return viewCondition == null || viewCondition.test(player);
    }

    public static Builder builder(ItemStack displayItem) {
        return new Builder(displayItem);
    }

    public static class Builder {

        private final ItemStack displayItem;
        private Consumer<Player> onClick;
        private Consumer<Player> onRightClick;
        private Predicate<Player> viewCondition;

        private Builder(ItemStack displayItem) {
            this.displayItem = displayItem;
        }

        public Builder onClick(Consumer<Player> onClick) {
            this.onClick = onClick;
            return this;
        }

        public Builder onRightClick(Consumer<Player> onRightClick) {
            this.onRightClick = onRightClick;
            return this;
        }

        public Builder viewCondition(Predicate<Player> viewCondition) {
            this.viewCondition = viewCondition;
            return this;
        }

        public MenuItem build() {
            return new MenuItem(displayItem, onClick, onRightClick, viewCondition);
        }
    }
}
