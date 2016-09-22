package io.github.phantamanta44.botah2.command;

import io.github.phantamanta44.discord4j.data.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandProvider {

    String value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Command {

        String name();

        String usage();

        String desc() default "No description provided.";

        String docs() default "";

        String[] aliases() default {};

        CmdPerm[] perms() default {};

        Permission[] dcPerms() default {};

    }

}
