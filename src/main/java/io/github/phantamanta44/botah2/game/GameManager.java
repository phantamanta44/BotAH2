package io.github.phantamanta44.botah2.game;

import io.github.phantamanta44.botah2.BotAH;
import io.github.phantamanta44.botah2.CoreModule;
import io.github.phantamanta44.botah2.game.deck.BlackCard;
import io.github.phantamanta44.botah2.game.deck.DeckManager;
import io.github.phantamanta44.discord4j.core.event.Events;
import io.github.phantamanta44.discord4j.core.event.Handler;
import io.github.phantamanta44.discord4j.core.event.context.IEventContext;
import io.github.phantamanta44.discord4j.data.wrapper.Channel;
import io.github.phantamanta44.discord4j.data.wrapper.GuildUser;
import io.github.phantamanta44.discord4j.data.wrapper.PrivateChannel;
import io.github.phantamanta44.discord4j.data.wrapper.User;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Handler(CoreModule.MOD_ID)
public class GameManager {

	private static final int CARD_CNT = 7;
	private static final int WIN_LIM = 5;

	private static Channel chan;
	private static int ind = 0, havePlayed = 0;
	private static String[] players;
	private static Hand[] hands;
	private static State state = State.OFF;
	private static BlackCard blackCard;
	private static Map<String, List<String>> plays;
	private static List<Pair<String, List<String>>> choices;

	public static void setChannel(Channel chan) {
		GameManager.chan = chan;
		DeckManager.clearDeck();
		DeckManager.addStandardDecks();
	}

	public static Channel getChannel() {
		return chan;
	}

	public static boolean isPlaying() {
		return state != State.OFF;
	}

	public static void stop() {
		state = State.OFF;
		ind = 0;
		players = null;
		hands = null;
		blackCard = null;
		plays = null;
		choices = null;
	}

	public static void start(int cnt) {
		state = State.WAITING;
		players = new String[cnt];
		chan.send("**A Cards Against Humanity game is starting!**\nType `count me in` to join the fun!");
	}

	private static void procMsg(IEventContext ctx) {
		User sender = ctx.user();
		if (state == State.WAITING) {
			if (ctx.message().body().equalsIgnoreCase("count me in")
					&& Arrays.stream(players).filter(p -> p != null).noneMatch(p -> p.equalsIgnoreCase(ctx.user().id()))) {
				players[ind++] = sender.id();
				ctx.send("**%s** has joined the game! (%d/%d)", sender.tag(), ind, players.length);
			}
			if (ind >= players.length) {
				state = State.PLAYING;
				ind = players.length - 1;
				DeckManager.reshuffle();
				hands = new Hand[players.length];
				plays = new ConcurrentHashMap<>();
				for (int i = 0; i < players.length; i++) {
					hands[i] = new Hand(CARD_CNT);
					plays.put(players[i], new CopyOnWriteArrayList<>());
				}
				nextTurn();
			}
		}
		else if (state == State.JUDGING && players[ind].equalsIgnoreCase(ctx.user().id()))
			judge(ctx);
	}

	private static void nextTurn() {
		ind = (ind + 1) % players.length;
		havePlayed = 0;
		chan.send("```%s```\n**%s is the Card Czar!**", getPlayerState(), resolvePlayer(ind).displayName());
		try {
			blackCard = DeckManager.getBlack();
		} catch (IndexOutOfBoundsException e) {
			chan.send("Reshuffling deck...");
			reshuffle();
			blackCard = DeckManager.getBlack();
		}
		plays.forEach((k, v) -> v.clear());
		chan.send("__**Black Card**__\n`%s`\n*(Pick %d card(s))*", blackCard.text, blackCard.pick);
		distPm();
	}

	private static void distPm() {
		for (int i = 0; i < players.length; i++) {
			if (i == ind)
				continue;
			final int index = i;
			resolvePlayer(i).privateChannel().done(c -> c.send("`%s`\n\n__**Your Hand**__\n%s\n*(Pick %d card(s)) *", blackCard.text, hands[index], blackCard.pick));
		}
	}

	private static void procPm(IEventContext ctx) {
		if (players[ind].equalsIgnoreCase(ctx.user().id())) {
			if (state == State.JUDGING)
				judge(ctx);
			else
				ctx.send("You are the Card Czar! You can't play a card this round.");
		} else {
			if (state == State.PLAYING) {
				List<String> play = plays.get(ctx.user().id());
				if (play.size() < blackCard.pick) {
					try {
						int cardNum = Integer.parseInt(ctx.message().body());
						List<String> hand = hands[indexOf(ctx.user())].cards;
						String card = hand.get(cardNum);
						hand.remove(cardNum);
						play.add(card);
						ctx.send("Played \"%s\". (%d/%d)", card.replaceAll("\\.", ""), play.size(), blackCard.pick);
						if (play.size() >= blackCard.pick)
							onPlayed(ctx.user().of(chan.guild()));
                        else
                            ctx.send("__**Your Hand**__\n%s\n*(Pick %d card(s)) *", hands[indexOf(ctx.user())], blackCard.pick);
					} catch (IndexOutOfBoundsException|NumberFormatException e) {
						ctx.send("Invalid card index!");
					}
				}
				else
					ctx.send("You've can't pick any more cards!");
			}
			else
				ctx.send("The Card Czar is currently choosing a winner!");
		}
	}

	private static void onPlayed(GuildUser player) {
		chan.send("%s played their card(s). (%d/%d)", player.displayName(), ++havePlayed, players.length - 1);
		if (havePlayed >= players.length - 1) {
			state = State.JUDGING;
			choices = plays.entrySet().stream()
					.filter(e -> !e.getValue().isEmpty())
					.map(e -> Pair.of(e.getKey(), e.getValue()))
					.sequential()
					.sorted((a, b) -> (int)Math.floor(Math.random() * 3F - 1F))
					.collect(Collectors.toCollection(CopyOnWriteArrayList::new));
			chan.send("__**Winner Selection**__\n%s", choices.stream()
					.map(c -> String.format("%d | %s", choices.indexOf(c) + 1, blackCard.supplant(c.getValue())))
					.reduce((a, b) -> a.concat("\n").concat(b)).get());
		}
	}

	private static void judge(IEventContext ctx) {
		String msg = ctx.message().body();
		try {
			GuildUser winner = BotAH.bot().user(choices.get(Integer.parseInt(msg) - 1).getKey()).of(ctx.guild()); // TODO Fix NPE if we aren't in judging mode yet
			chan.send("**%s won this round!**", winner.displayName());
			Hand hand = hands[indexOf(winner)];
			hand.win(blackCard);
			if (hand.blackCards.size() >= WIN_LIM) {
				chan.send("**%s reached %d points, winning the game! Congratulations!**", winner.tag(), WIN_LIM);
				stop();
			} else {
				state = State.PLAYING;
				redraw();
				nextTurn();
			}
		} catch (IndexOutOfBoundsException e) {
			ctx.send("Specified choice number does not exist!");
		} catch (NumberFormatException ignored) { }
	}

	private static void redraw() {
		Arrays.stream(hands).forEach(h -> h.drawTo(CARD_CNT));
	}

	private static void reshuffle() {
		DeckManager.reshuffle();
		DeckManager.removeCards(Arrays.stream(hands)
				.flatMap(h -> h.cards.stream())
				.collect(Collectors.toList()));
		DeckManager.removeBlacks(Arrays.stream(hands)
				.flatMap(h -> h.blackCards.stream())
				.collect(Collectors.toList()));
		DeckManager.removeBlack(blackCard);
	}

	private static int indexOf(User user) {
		for (int i = 0; i < players.length; i++) {
			if (players[i].equalsIgnoreCase(user.id()))
				return i;
		}
		return -1;
	}

	public static String getPlayerState() {
		StringBuilder scores = new StringBuilder();
		for (int i = 0; i < players.length; i++)
			scores.append("\n").append(String.format("%s: %d point(s)", resolvePlayer(i).displayName(), hands[i].blackCards.size()));
		return scores.toString();
	}

	private static GuildUser resolvePlayer(int i) {
        return BotAH.bot().user(players[i]).of(chan.guild());
    }

	private static class Hand {

		private final List<String> cards = new CopyOnWriteArrayList<>();
		private final List<BlackCard> blackCards = new CopyOnWriteArrayList<>();

		private Hand(int cards) {
			draw(cards);
		}

		private void draw(int cnt) {
			for (int i = 0; i < cnt; i++) {
				try {
					cards.add(DeckManager.getWhite());
				} catch (IndexOutOfBoundsException e) {
					chan.send("Reshuffling deck...");
					reshuffle();
					cards.add(DeckManager.getWhite());
				}
			}
		}

		private void drawTo(int cnt) {
			draw(Math.max(cnt - cards.size(), 0));
		}

		private void draw(String card) {
			cards.add(card);
		}

		private void win(BlackCard card) {
			blackCards.add(card);
		}

		@Override
		public String toString() {
			return cards.stream()
					.map(c -> String.format("%d | %s", cards.indexOf(c), c))
					.reduce((a, b) -> a.concat("\n").concat(b)).get();
		}

	}

	@Handler.On(Events.MSG_GET)
	public static void onMessage(IEventContext ctx) {
		if (chan != null) {
			if (ctx.channel().id().equalsIgnoreCase(chan.id()))
				procMsg(ctx);
			else if (ctx.channel() instanceof PrivateChannel && Arrays.stream(players).anyMatch(p -> ctx.user().id().equalsIgnoreCase(p)))
				procPm(ctx);
		}
	}

	private enum State {

		OFF, WAITING, PLAYING, JUDGING

	}

}
