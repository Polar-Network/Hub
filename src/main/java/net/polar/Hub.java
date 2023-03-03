package net.polar;

public final class Hub {

    private static final Hub instance = new Hub();

    private Hub() {
        Polaroid.initServer();
        Polaroid.addShutdownTask(this::onDisable);
    }

    private void onDisable() {

    }

    public static void main(String[] args) {
        new Hub();
    }



    public static Hub getInstance() {
        return instance;
    }
}
