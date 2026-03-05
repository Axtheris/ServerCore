package net.axther.serverCore.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
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
    private final boolean dynamic;
    private final List<MenuAction> actions;
    private final List<MenuAction> rightActions;
    private final List<ItemStack> cycleItems;
    private final int cycleInterval;
    private final String nameTemplate;
    private final List<String> loreTemplates;

    private MenuItem(ItemStack displayItem, Consumer<Player> onClick,
                     Consumer<Player> onRightClick, Predicate<Player> viewCondition,
                     boolean dynamic, List<MenuAction> actions, List<MenuAction> rightActions,
                     List<ItemStack> cycleItems, int cycleInterval,
                     String nameTemplate, List<String> loreTemplates) {
        this.displayItem = displayItem;
        this.onClick = onClick;
        this.onRightClick = onRightClick;
        this.viewCondition = viewCondition;
        this.dynamic = dynamic;
        this.actions = actions;
        this.rightActions = rightActions;
        this.cycleItems = cycleItems;
        this.cycleInterval = cycleInterval;
        this.nameTemplate = nameTemplate;
        this.loreTemplates = loreTemplates;
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

    public boolean isDynamic() {
        return dynamic;
    }

    public List<MenuAction> getActions() {
        return actions;
    }

    public List<MenuAction> getRightActions() {
        return rightActions;
    }

    public List<ItemStack> getCycleItems() {
        return cycleItems;
    }

    public int getCycleInterval() {
        return cycleInterval;
    }

    public String getNameTemplate() {
        return nameTemplate;
    }

    public List<String> getLoreTemplates() {
        return loreTemplates;
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
        private boolean dynamic = false;
        private List<MenuAction> actions = List.of();
        private List<MenuAction> rightActions = List.of();
        private List<ItemStack> cycleItems = List.of();
        private int cycleInterval = 20;
        private String nameTemplate = null;
        private List<String> loreTemplates = List.of();

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

        public Builder dynamic(boolean dynamic) {
            this.dynamic = dynamic;
            return this;
        }

        public Builder actions(List<MenuAction> actions) {
            this.actions = actions;
            return this;
        }

        public Builder rightActions(List<MenuAction> rightActions) {
            this.rightActions = rightActions;
            return this;
        }

        public Builder cycleItems(List<ItemStack> cycleItems) {
            this.cycleItems = cycleItems;
            return this;
        }

        public Builder cycleInterval(int cycleInterval) {
            this.cycleInterval = cycleInterval;
            return this;
        }

        public Builder nameTemplate(String nameTemplate) {
            this.nameTemplate = nameTemplate;
            return this;
        }

        public Builder loreTemplates(List<String> loreTemplates) {
            this.loreTemplates = loreTemplates;
            return this;
        }

        public MenuItem build() {
            return new MenuItem(displayItem, onClick, onRightClick, viewCondition,
                    dynamic, actions, rightActions, cycleItems, cycleInterval,
                    nameTemplate, loreTemplates);
        }
    }
}
