package de.wohlfrom.presenter.connectors;

/**
 * The commands that can be sent to the presenter server.
 */
public enum Command {
    /**
     * Request switch to previous slide.
     */
    PREV_SLIDE("prevSlide", 1, 2),

    /**
     * Request switch to next slide.
     */
    NEXT_SLIDE("nextSlide", 1, 2),

    /**
     * Request to start the presentation.
     */
    START_PRESENTATION("startPresentation", 2, 2),

    /**
     * Request to stop the presentation.
     */
    STOP_PRESENTATION("stopPresentation", 2, 2);

    /**
     * The command to send.
     */
    private final String command;

    /**
     * The minimum protocol version that supports this command.
     */
    private final int minVersion;

    /**
     * The maximum protocol version that supports this command.
     */
    private final int maxVersion;

    /**
     * Create a new command.
     *
     * @param command The command to send.
     * @param minVersion The minimum protocol version that supports this command.
     * @param maxVersion The maximum protocol version that supports this command.
     */
    Command(String command, int minVersion, int maxVersion) {
        this.command = command;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
    }

    /**
     * Return the command to send.
     *
     * @return The command to send.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Returns the minimum protocol version that supports this command.
     *
     * @return The minimum protocol version.
     */
    public int getMinVersion() {
        return minVersion;
    }

    /**
     * Returns the maximum protocol version that supports this command.
     *
     * @return The maximum protocol version.
     */
    public int getMaxVersion() {
        return maxVersion;
    }
}
