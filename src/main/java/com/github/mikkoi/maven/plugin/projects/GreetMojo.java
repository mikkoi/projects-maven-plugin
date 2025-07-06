package com.github.mikkoi.maven.plugin.projects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Says "Hi" to the user.
 */
@Mojo(name = "greet")
public class GreetMojo extends AbstractMojo {
    /**
     * The greeting to display.
     */
    @Parameter(property = "projecthelper" + ".greet" + ".greeting", defaultValue = "Hello")
    private String greeting;

    /**
     * The target of the greeting (Who to greet).
     */
    @Parameter(property = "projecthelper" + ".greet" + ".target", defaultValue = "World")
    private String target;

    @Override
    public void execute() {
        getLog().info(String.format(getFormatString(), greeting, target));
    }

    String getFormatString() {
        return "%s, %s!";
    }
}