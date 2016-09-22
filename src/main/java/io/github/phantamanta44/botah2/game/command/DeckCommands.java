package io.github.phantamanta44.botah2.game.command;

import io.github.phantamanta44.botah2.CoreModule;
import io.github.phantamanta44.botah2.command.CommandProvider;
import io.github.phantamanta44.botah2.game.GameManager;
import io.github.phantamanta44.botah2.game.deck.DeckManager;
import io.github.phantamanta44.discord4j.core.event.context.IEventContext;
import io.github.phantamanta44.discord4j.data.Permission;
import io.github.phantamanta44.discord4j.data.wrapper.Channel;
import io.github.phantamanta44.discord4j.util.StringUtils;

@CommandProvider(CoreModule.MOD_ID)
public class DeckCommands {

    @CommandProvider.Command(
            name = "adddeck", usage = "adddeck <url> [url...]", aliases = "mkdeck",
            desc = "Attempts to load a deck from the linked JSON file.",
            docs = "Attempts to load a deck from the linked JSON file. Decks should be in CAH-Creator format: https://cahcreator.com",
            dcPerms = Permission.MANAGE_MSG
    )
    public static void cmdAddDeck(String[] args, IEventContext ctx) {
        if (GameManager.getChannel() == null) {
            ctx.send("Bot is not bound to a channel!");
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
        if (args.length < 1) {
            ctx.send("You must specify a valid URL to a deck file!");
            return;
        }
        for (String arg : args) {
            DeckManager.loadDeck(arg).done(deck -> {
                DeckManager.addDeck(deck);
                ctx.send("Loaded deck: %s", deck.getName());
            }).fail(e -> ctx.send("Encountered %s while loading deck: `%s`", e.getClass().getName(), e.getMessage()));
        }
    }

    @CommandProvider.Command(
            name = "deldeck", usage = "deldeck <deck>", aliases = "rmdeck",
            desc = "Removes a deck from the pending match.",
            dcPerms = Permission.MANAGE_MSG
    )
    public static void cmdDelDeck(String[] args, IEventContext ctx) {
        if (GameManager.getChannel() == null) {
            ctx.send("Bot is not bound to a channel!");
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
        if (DeckManager.removeDeck(StringUtils.concat(args)))
            ctx.send("Successfully removed.");
        else
            ctx.send("No such deck!");
    }

    @CommandProvider.Command(
            name = "listdecks", usage = "listdecks", aliases = "lsdeck",
            desc = "Lists currently loaded decks."
    )
    public static void cmdListDecks(String[] args, IEventContext ctx) {
        if (GameManager.getChannel() == null) {
            ctx.send("Bot is not bound to a channel!");
            return;
        }
        Channel chan = GameManager.getChannel();
        if (!chan.id().equalsIgnoreCase(ctx.channel().id())) {
            ctx.send("Bot is currently bound to %s / %s!", chan.guild().name(), chan.name());
            return;
        }
        ctx.send("__**Currently Loaded Decks:**__\n%s", DeckManager.getDecks().stream()
                .map(d -> String.format("- %s", d.getName()))
                .reduce((a, b) -> a.concat("\n").concat(b)).orElse("No decks loaded!"));
    }

    @CommandProvider.Command(
            name = "cardcast", usage = "cardcast <code> [code...]",
            desc = "Attempts to load a deck from a CardCast card set.",
            dcPerms = Permission.MANAGE_MSG
    )
    public static void cmdCardcast(String[] args, IEventContext ctx) {
        if (GameManager.getChannel() == null) {
            ctx.send("Bot is not bound to a channel!");
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
        if (args.length < 1) {
            ctx.send("You must specify a valid Cardcast deck ID!");
            return;
        }
        for (String arg : args) {
            DeckManager.loadCardcast(arg).done(deck -> {
                DeckManager.addDeck(deck);
                ctx.send("Loaded deck: %s", deck.getName());
            }).fail(e -> ctx.send("Encountered %s while loading deck: `%s`", e.getClass().getName(), e.getMessage()));
        }
    }

}
