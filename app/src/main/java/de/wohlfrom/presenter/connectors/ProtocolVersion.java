package de.wohlfrom.presenter.connectors;

/**
 * Contains the minimum and maximum protocol version that is supported either by server or client.
 */
public class ProtocolVersion {
    /**
     * The minimum protocol version that is supported.
     */
    private final int minVersion;

    /**
     * The maximum protocol version that is supported.
     */
    private final int maxVersion;

    /**
     * Creates a new protocol version object.
     *
     * @param minVersion The minimum protocol version to be supported.
     * @param maxVersion The maximum protocol version to be supported.
     */
    public ProtocolVersion(int minVersion, int maxVersion) {
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
    }

    /**
     * Returns the minimum protocol version to be supported.
     *
     * @return The minimum protocol version.
     */
    public int getMinVersion() {
        return minVersion;
    }

    /**
     * Returns the maximum protocol version to be supported.
     *
     * @return The maximum protocol version.
     */
    public int getMaxVersion() {
        return maxVersion;
    }

    @Override
    public String toString() {
        return "{ " +
                "\"minVersion\": \"" + getMinVersion() + "\"," +
                "\"maxVersion\": \"" + getMaxVersion() + "\"" +
                "}";
    }
}
