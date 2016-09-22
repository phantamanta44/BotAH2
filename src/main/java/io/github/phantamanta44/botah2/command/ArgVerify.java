package io.github.phantamanta44.botah2.command;

import io.github.phantamanta44.discord4j.core.event.context.IEventContext;
import io.github.phantamanta44.discord4j.data.wrapper.PrivateChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiPredicate;

public class ArgVerify {

    public static final ArgVerify GUILD = new ArgVerify().server();
    public static final ArgVerify ONE = new ArgVerify().count(1);
    public static final ArgVerify TWO = new ArgVerify().count(2);
    public static final ArgVerify GUILD_ONE = new ArgVerify().server().count(1);
	public static final ArgVerify GUILD_TWO = new ArgVerify().server().count(2);

    private Collection<ArgTest> tests = new ArrayList<>();

    public ArgVerify count(int num) {
        tests.add(new ArgTest((a, c) -> a.length >= num, "Not enough parameters supplied!"));
        return this;
    }

    public ArgVerify server() {
        tests.add(new ArgTest((a, c) -> !(c.channel() instanceof PrivateChannel), "This command can only be used in a server!"));
        return this;
    }

    public boolean verify(String[] args, IEventContext ctx) {
        for (ArgTest test : tests) {
            if (!test.test(args, ctx))
                return false;
        }
        return true;
    }

    private static class ArgTest {

        private final BiPredicate<String[], IEventContext> test;
        private final String failure;

        ArgTest(BiPredicate<String[], IEventContext> test, String failure) {
            this.test = test;
            this.failure = failure;
        }

        public boolean test(String[] args, IEventContext ctx) {
            if (test.test(args, ctx))
                return true;
            ctx.send("%s: %s", ctx.user().tag(), failure);
            return false;
        }

    }

}
