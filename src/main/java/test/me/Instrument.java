package test.me;

public abstract class Instrument {
    private final String pair;

    private Instrument(String pair) {
        this.pair = pair;
    }

    public String pair() {
        return pair;
    }

    public static class Spot extends Instrument {
        private Spot(String pair) {
            super(pair);
        }
    }

    public static class Forward extends Instrument {
        private Forward(String pair) {
            super(pair);
        }
    }
}
