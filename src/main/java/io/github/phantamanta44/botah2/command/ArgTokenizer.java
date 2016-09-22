package io.github.phantamanta44.botah2.command;

import io.github.phantamanta44.botah2.BotAH;
import io.github.phantamanta44.discord4j.data.wrapper.Channel;
import io.github.phantamanta44.discord4j.data.wrapper.Guild;
import io.github.phantamanta44.discord4j.data.wrapper.Role;
import io.github.phantamanta44.discord4j.data.wrapper.User;

import java.util.NoSuchElementException;

public class ArgTokenizer {

	private String[] parts;
	private int pos;
	
	public ArgTokenizer(String[] args) {
		this.parts = args;
		this.pos = 0;
	}
	
	public ArgTokenizer(String str) {
		this(str.split("\\s"));
	}
	
	public ArgTokenizer reset() {
		pos = 0;
		return this;
	}
	
	public boolean hasNext() {
		return pos < parts.length;
	}
	
	public String nextString() {
		if (!hasNext())
			throw new NoSuchElementException("No such token available!");
		return parts[pos++];
	}
	
	public String nextInlineCode() {
		StringBuilder sb = new StringBuilder(nextString());
		if (sb.charAt(0) != '`') {
			pos--;
			throw new NoSuchElementException("No such token available!");
		}
		final int start = pos - 1;
		while (!(
				sb.charAt(sb.length() - 1) == '`'
				&& (sb.charAt(sb.length() - 3) == '\\' || sb.charAt(sb.length() - 2) != '\\')
		)) {
			if (hasNext())
				sb.append(' ').append(nextString());
			else {
				pos = start;
				throw new NoSuchElementException("No such token available!");
			}
		}
		return sb.substring(1, sb.length() - 1);
	}
	
	public String nextBlockCode() {
		StringBuilder sb = new StringBuilder(nextString());
		if (!sb.substring(0, 3).equals("```")) {
			pos--;
			throw new NoSuchElementException("No such token available!");
		}
		final int start = pos - 1, str0Len = sb.length();
		while (!sb.substring(sb.length() - 3, sb.length()).equals("```")) {
			if (hasNext())
				sb.append(' ').append(nextString());
			else {
				pos = start;
				throw new NoSuchElementException("No such token available!");
			}
		}
		return sb.substring(str0Len, sb.length() - 3).trim();
	}
	
	public int nextInt() {
		try {
			String str = nextString();
			return Integer.parseInt(str);
		} catch (NumberFormatException e) {
			pos--;
			throw new NoSuchElementException("No such token available!");
		}
	}
	
	public User nextUserTag() {
		String tag = nextString();
		if (!tag.startsWith("<@") || !tag.endsWith(">") || tag.charAt(2) == '&') {
			pos--;
			throw new NoSuchElementException("No such token available!");
		}
		if (tag.charAt(2) == '!')
			tag = tag.substring(3, tag.length() - 1);
		else
			tag = tag.substring(2, tag.length() - 1);
		User user = BotAH.bot().user(tag);
		if (user == null) {
			pos--;
			throw new NoSuchElementException("Unknown user!");
		}
		return user;
	}
	
	public Channel nextChannelTag() {
		String tag = nextString();
		if (!tag.startsWith("<#") || !tag.endsWith(">")) {
			pos--;
			throw new NoSuchElementException("No such token available!");
		}
		Channel chan = BotAH.bot().channel(tag.substring(2, tag.length() - 1));
		if (chan == null) {
			pos--;
			throw new NoSuchElementException("Unknown channel!");
		}
		return chan;
	}

	public Role nextRoleTag(Guild guild) {
		String tag = nextString();
		if (!tag.startsWith("<@&") || !tag.endsWith(">")) {
			pos--;
			throw new NoSuchElementException("No such token available!");
		}
		Role role = guild.role(tag.substring(3, tag.length() - 1));
		if (role == null) {
			pos--;
			throw new NoSuchElementException("Unknown role!");
		}
		return role;
	}
	
}
