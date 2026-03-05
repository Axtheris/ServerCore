package net.axther.serverCore.npc.dialogue;

import net.axther.serverCore.npc.NPC;
import net.axther.serverCore.npc.dialogue.action.DialogueAction;
import net.axther.serverCore.npc.dialogue.condition.DialogueCondition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class DialogueSession {

    private final Player player;
    private final NPC npc;
    private final DialogueTree tree;
    private DialogueNode currentNode;
    private List<DialogueChoice> visibleChoices;
    private net.axther.serverCore.hologram.DialogueHologram dialogueHologram;

    public DialogueSession(Player player, NPC npc, DialogueTree tree) {
        this.player = player;
        this.npc = npc;
        this.tree = tree;
    }

    public void start() {
        DialogueNode startNode = tree.getStartNode();
        if (startNode == null) {
            player.sendMessage(Component.text("This NPC has nothing to say.", NamedTextColor.GRAY));
            return;
        }
        displayNode(startNode);
    }

    public void selectChoice(int index) {
        if (visibleChoices == null || index < 0 || index >= visibleChoices.size()) {
            return;
        }

        DialogueChoice choice = visibleChoices.get(index);

        // Execute actions
        for (DialogueAction action : choice.getActions()) {
            action.execute(player);
        }

        // Advance to next node or end
        String nextNodeId = choice.getNextNodeId();
        if (nextNodeId == null) {
            end();
            return;
        }

        DialogueNode nextNode = tree.getNode(nextNodeId);
        if (nextNode == null) {
            player.sendMessage(Component.text("(Dialogue ended)", NamedTextColor.GRAY));
            end();
            return;
        }

        displayNode(nextNode);
    }

    public void displayNode(DialogueNode node) {
        this.currentNode = node;
        MiniMessage mm = MiniMessage.miniMessage();

        // Separator
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("--- ", NamedTextColor.DARK_GRAY)
                .append(mm.deserialize(npc.getDisplayName()))
                .append(Component.text(" ---", NamedTextColor.DARK_GRAY)));

        // NPC text lines
        for (String line : node.getText()) {
            player.sendMessage(mm.deserialize(line));
        }

        // Update dialogue hologram above NPC head
        if (dialogueHologram != null) {
            StringBuilder holoText = new StringBuilder();
            for (int i = 0; i < node.getText().size(); i++) {
                if (i > 0) holoText.append("\n");
                holoText.append(node.getText().get(i));
            }
            if (dialogueHologram.isSpawned()) {
                dialogueHologram.updateText(holoText.toString());
            } else {
                dialogueHologram.spawn(holoText.toString());
            }
        }

        // Build visible choices (filter by conditions)
        visibleChoices = new ArrayList<>();
        for (DialogueChoice choice : node.getChoices()) {
            boolean passesConditions = true;
            for (DialogueCondition condition : choice.getConditions()) {
                if (!condition.test(player)) {
                    passesConditions = false;
                    break;
                }
            }
            if (passesConditions) {
                visibleChoices.add(choice);
            }
        }

        // Display clickable choices
        if (visibleChoices.isEmpty()) {
            player.sendMessage(Component.text("[End of conversation]", NamedTextColor.GRAY));
            end();
            return;
        }

        player.sendMessage(Component.empty());
        for (int i = 0; i < visibleChoices.size(); i++) {
            DialogueChoice choice = visibleChoices.get(i);
            Component choiceText = Component.text(" [" + (i + 1) + "] ", NamedTextColor.DARK_GRAY)
                    .append(mm.deserialize(choice.getLabel()))
                    .clickEvent(ClickEvent.runCommand("/npc _dialogue " + i))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to select", NamedTextColor.YELLOW)));
            player.sendMessage(choiceText);
        }
    }

    public void setDialogueHologram(net.axther.serverCore.hologram.DialogueHologram hologram) {
        this.dialogueHologram = hologram;
    }

    public void end() {
        if (dialogueHologram != null) {
            dialogueHologram.despawn();
            dialogueHologram = null;
        }
        this.currentNode = null;
        this.visibleChoices = null;
    }

    public boolean isActive() {
        return currentNode != null;
    }

    public Player getPlayer() { return player; }
    public NPC getNpc() { return npc; }
    public DialogueTree getTree() { return tree; }
    public DialogueNode getCurrentNode() { return currentNode; }
}
