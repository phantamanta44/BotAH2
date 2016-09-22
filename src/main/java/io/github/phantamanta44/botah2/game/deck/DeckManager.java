package io.github.phantamanta44.botah2.game.deck;

import io.github.phantamanta44.botah2.BotAH;
import io.github.phantamanta44.discord4j.core.Discord;
import io.github.phantamanta44.discord4j.core.StaticInit;
import io.github.phantamanta44.discord4j.data.wrapper.Bot;
import io.github.phantamanta44.discord4j.util.concurrent.deferred.IUnaryPromise;
import io.github.phantamanta44.discord4j.util.concurrent.deferred.UnaryDeferred;
import io.github.phantamanta44.discord4j.util.io.IOUtils;

import javax.net.ssl.SSLContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DeckManager {

	private static final Collection<String> BASE_URLS = Arrays.asList(
			"https://cdn.rawgit.com/Rylius/CardsAgainstEquestria/master/data/decks/b_000_cah.json",
			"https://cdn.rawgit.com/Rylius/CardsAgainstEquestria/master/data/decks/e_000_first_expansion.json",
			"https://cdn.rawgit.com/Rylius/CardsAgainstEquestria/master/data/decks/e_001_second_expansion.json",
			"https://cdn.rawgit.com/Rylius/CardsAgainstEquestria/master/data/decks/e_002_third_expansion.json",
			"https://cdn.rawgit.com/Rylius/CardsAgainstEquestria/master/data/decks/e_003_fourth_expansion.json",
			"https://cdn.rawgit.com/Rylius/CardsAgainstEquestria/master/data/decks/e_004_box_expansion.json",
			"https://cdn.rawgit.com/Rylius/CardsAgainstEquestria/master/data/decks/e_016_misprints.json"
	);
	private static Collection<Deck> BASE_DECKS;

    @StaticInit
	public static void init(Bot bot) {
        BASE_DECKS = new ArrayList<>();
		for (String url : BASE_URLS) {
            loadDeck(url).done(BASE_DECKS::add).fail(e -> {
                Discord.logger().error("Failed to load base deck from {}!", url);
                e.printStackTrace();
            });
        }
	}

	private static Map<String, Deck> decks = new ConcurrentHashMap<>();
	private static Deck deck;

	public static void addDeck(Deck deck) {
		decks.put(deck.getName(), deck);
	}

	public static void addDecks(Collection<Deck> toAdd) {
		toAdd.forEach(DeckManager::addDeck);
	}

	public static boolean removeDeck(String name) {
		boolean didRem = false;
		Iterator<Map.Entry<String, Deck>> iter = decks.entrySet().iterator();
		while (iter.hasNext()) {
			if (BotAH.lenientMatch(iter.next().getKey(), name)) {
				iter.remove();
				didRem = true;
			}
		}
		return didRem;
	}

	public static Deck getDeck() {
		return decks.values().stream()
				.reduce(new Deck(""), (a, b) -> {
					a.addBlacks(b.getBlacks());
					a.addWhites(b.getWhites());
					return a;
				});
	}

	public static Collection<Deck> getDecks() {
		return decks.values();
	}

	public static void clearDeck() {
		decks.clear();
	}

	public static void reshuffle() {
		deck = getDeck();
	}

	public static String getWhite() {
		return deck.getWhite();
	}

	public static BlackCard getBlack() {
		return deck.getBlack();
	}

	public static void addStandardDecks() {
		BASE_DECKS.forEach(DeckManager::addDeck);
	}

	public static IUnaryPromise<Deck> loadDeck(String url) {
		return IOUtils.requestJson(url).map(d -> Deck.parse(d.getAsJsonObject()));
	}

	private static final String CC_DINFO = "https://api.cardcastgame.com/v1/decks/%s", CC_DCARDS = CC_DINFO + "/cards";

	public static IUnaryPromise<Deck> loadCardcast(String deckId) {
        UnaryDeferred<Deck> def = new UnaryDeferred<>();
        IOUtils.requestJson(String.format(CC_DINFO, deckId)).done(i ->
            IOUtils.requestJson(String.format(CC_DCARDS, deckId)).done(c -> {
                try {
                    def.resolve(Deck.parseCardcast(i.getAsJsonObject().get("name").getAsString(), c.getAsJsonObject()));
                } catch (Exception e) {
                    def.reject(e);
                }
            }).fail(def::reject)
        ).fail(def::reject);
        return def.promise();
    }

	public static void removeBlacks(List<BlackCard> cards) {
		deck.removeBlacks(cards);
	}
	
	public static void removeBlack(BlackCard card) {
		deck.removeBlack(card);
	}

	public static void removeCards(List<String> cards) {
		deck.remove(cards);
	}

	public static void removeCard(String card) {
		deck.remove(card);
	}

}
