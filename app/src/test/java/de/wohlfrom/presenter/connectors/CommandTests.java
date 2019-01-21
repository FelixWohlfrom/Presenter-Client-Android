package de.wohlfrom.presenter.connectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * These tests verify that the commands are having proper configuration and all values are valid. 
 */
@RunWith(Parameterized.class)
public class CommandTests {

    /**
     * The list of commands to test
     */
    @Parameterized.Parameters(name = "{0}")
    public static Command[] data() {
        return Command.values();
    }

    /**
     * A single command instance to test
     */
    private final Command command;

    /**
     * The constructor for a single parameter test
     * 
     * @param command The command to test
     */
    public CommandTests(Command command) {
        this.command = command;
    }

    /**
     * Verify that the minimum value is > 0
     */
    @Test
    public void verifyMinValue() {
        assertThat(command.getMinVersion(), is(greaterThan(0)));
    }

    /**
     * Verify that the maximum value is > 0
     */
    @Test
    public void verifyMaxValue() {
        assertThat(command.getMaxVersion(), is(greaterThan(0)));
    }

    /**
     * Verify that the minimum value is smaller than the maximum value
     */
    @Test
    public void verifyMinIsSmallerThanMax() {
        assertThat("The minimum version of command " + command.toString() + " needs to be " +
                "lower or equal than the maximum version", 
                command.getMinVersion(), is(lessThanOrEqualTo(command.getMaxVersion())));
    }
}
