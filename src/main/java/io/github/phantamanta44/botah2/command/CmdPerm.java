package io.github.phantamanta44.botah2.command;

import io.github.phantamanta44.discord4j.core.Discord;
import io.github.phantamanta44.discord4j.data.wrapper.ChannelUser;
import io.github.phantamanta44.discord4j.data.wrapper.User;
import io.github.phantamanta44.discord4j.util.function.Lambdas;

import java.util.function.Predicate;

public enum CmdPerm {

    BOT_OWNER(u -> u.id().equalsIgnoreCase(Discord.ownerId()), u -> u.id().equalsIgnoreCase(Discord.ownerId())),
    SERVER_OWNER(u -> u.guild().owner().equals(u), Lambdas.acceptAll());

    public final Predicate<ChannelUser> test;
    public final Predicate<User> privTest;

    CmdPerm(Predicate<ChannelUser> test, Predicate<User> privTest) {
        this.test = test;
        this.privTest = privTest;
    }

}
