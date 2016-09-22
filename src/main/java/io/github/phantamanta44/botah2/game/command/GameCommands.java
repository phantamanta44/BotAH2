package io.github.phantamanta44.botah2.game.command;

import io.github.phantamanta44.botah2.BotAH;
import io.github.phantamanta44.botah2.CoreModule;
import io.github.phantamanta44.botah2.command.CommandDispatcher;
import io.github.phantamanta44.botah2.command.CommandProvider;
import io.github.phantamanta44.botah2.game.GameManager;
import io.github.phantamanta44.botah2.game.deck.DeckManager;
import io.github.phantamanta44.discord4j.core.event.context.IEventContext;
import io.github.phantamanta44.discord4j.data.Permission;
import io.github.phantamanta44.discord4j.data.wrapper.Channel;
import io.github.phantamanta44.discord4j.data.wrapper.PrivateChannel;

@CommandProvider(CoreModule.MOD_ID)
public class GameCommands {
    
    @CommandProvider.Command(
            name = "join", usage = "join", aliases = "bind",
            desc = "Binds the bot to a channel.",
            dcPerms = Permission.MANAGE_MSG
    )
    public static void cmdJoin(String[] args, IEventContext ctx) {
        if (ctx.channel() instanceof PrivateChannel) {
            ctx.send("You can't do this in a private channel!");
            return;
        }
        if (GameManager.getChannel() != null) {
            Channel chan = GameManager.getChannel();
            ctx.send("Bot is currently bound to %s / %s!", chan.guild().name(), chan.name());
            return;
        }
        GameManager.setChannel(ctx.channel());
        ctx.send("Bot bound to %s / %s.", ctx.guild().name(), ctx.channel().name());
    }

    @CommandProvider.Command(
            name = "leave", usage = "leave", aliases = "unbind",
            desc = "Unbinds the bot from a channel.",
            dcPerms = Permission.MANAGE_MSG
    )
    public static void cmdLave(String[] args, IEventContext ctx) {
        if (ctx.channel() instanceof PrivateChannel) {
            ctx.send("You can't do this in a private channel!");
            return;
        }
        if (GameManager.getChannel() == null) {
            ctx.send("Bot is not currently bound to a channel!");
            return;
        }
        Channel chan = GameManager.getChannel();
        if (!chan.id().equalsIgnoreCase(ctx.channel().id())) {
            ctx.send("Bot is currently bound to %s / %s!", chan.guild().name(), chan.name());
            return;
        }
        if (GameManager.isPlaying()) {
            ctx.send("There is a game in progress!");
            return;
        }
        GameManager.setChannel(null);
        ctx.send("Bot unbound.");
    }

    @CommandProvider.Command(
            name = "start", usage = "start <#players>",
            desc = "Starts a Cards Against Humanity match with the specified number of players.",
            dcPerms = Permission.MANAGE_MSG
    )
    public static void cmdStart(String[] args, IEventContext ctx) {
        if (GameManager.getChannel() == null) {
            ctx.send("You must bind the bot to a channel with `join` before playing!");
            return;
        }
        Channel chan = GameManager.getChannel();
        if (!chan.id().equalsIgnoreCase(ctx.channel().id())) {
            ctx.send("Bot is currently bound to %s / %s!", chan.guild().name(), chan.name());
            return;
        }
        if (GameManager.isPlaying()) {
            ctx.send("A game is already in progress!");
            return;
        }
        if (DeckManager.getDecks().size() < 1) {
            ctx.send("You need to add at least one deck to play!");
            return;
        }
        if (DeckManager.getDecks().stream().noneMatch(d -> d.getWhites().size() > 0)
                || DeckManager.getDecks().stream().noneMatch(d -> d.getBlacks().size() > 0)) {
            ctx.send("Your decks must have at least one black and one white card!");
            return;
        }
        try {
            int players = Integer.parseInt(args[0]);
            if (players < 2)
                throw new NumberFormatException();
            GameManager.start(players);
        } catch (IndexOutOfBoundsException|NumberFormatException e) {
            ctx.send("Must specify a valid number of players!");
        }
    }

    @CommandProvider.Command(
            name = "stop", usage = "stop",
            desc = "Stops a Cards Against Humanity match, if one is in progress.",
            dcPerms = Permission.MANAGE_MSG
    )
    public static void cmdStop(String[] args, IEventContext ctx) {
        if (GameManager.getChannel() == null) {
            ctx.send("Bot isn't bound to a channel!");
            return;
        }
        Channel chan = GameManager.getChannel();
        if (!chan.id().equalsIgnoreCase(ctx.channel().id())) {
            ctx.send("Bot is currently bound to %s / %s!", chan.guild().name(), chan.name());
            return;
        }
        if (!GameManager.isPlaying()) {
            ctx.send("There is no game in progress!");
            return;
        }
        GameManager.stop();
        ctx.send("Game cancelled.");
    }
    
}
