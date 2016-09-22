package io.github.phantamanta44.botah2.game.deck;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;

public class BlackCard {

	public final String text;
	public final int pick;

	public BlackCard(String text, int pick) {
		this.text = text.replaceAll("_+", "\uFF3F\uFF3F\uFF3F");
		this.pick = pick;
	}

	public String supplant(Collection<String> cards) {
		StringBuilder val = new StringBuilder();
		Deque<String> parts = new ArrayDeque<>(Arrays.asList(text.split("\uFF3F{3}")));
		Deque<String> args = new ArrayDeque<>(cards);
		while (parts.size() > 0 && args.size() > 0) {
			val.append(String.format("**%s**", parts.pop()))
					.append(parts.size() > 0 ? String.format("__%s__", args.pop().replaceAll("\\.", "")) : " " + args.pop());
		}
		while (parts.size() > 0)
			val.append(String.format("**%s**", parts.pop()));
		while (args.size() > 0)
			val.append(" ").append(args.pop());
		return val.toString();
	}

}
