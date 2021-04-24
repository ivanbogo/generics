package test.me;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class Main {
    public static void main(String[] args) {
        PositionInstrument.CashFlow eur = new PositionInstrument.CashFlow("EUR", LocalDate.of(2021, 3, 4));
        PositionInstrument.CashFlow usd = new PositionInstrument.CashFlow("USD", LocalDate.of(2021, 3, 4));
        Position<PositionInstrument.CashFlow> p1 = new Position<>(eur, 1e5);
        Position<PositionInstrument.CashFlow> p2 = new Position<>(usd, -1.2e5);

        FxInstrument.FxSpot eurusd = new FxInstrument.FxSpot("EURUSD");
        Trade<FxInstrument.FxSpot, PositionInstrument.CashFlow> spot = new Trade<>(eurusd, Arrays.asList(p1, p2));

        FxInstrument.Future em1 = new FxInstrument.Future("EURUSD", "6EM1");
        PositionInstrument.Contract eurContract = new PositionInstrument.Contract("EUR", "6EM1");
        PositionInstrument.Contract usdContract = new PositionInstrument.Contract("USD", "6EM1");

        Position<PositionInstrument.Contract> eurContractPos = new Position<>(eurContract, -3.14e5);
        Position<PositionInstrument.Contract> usdContractPos = new Position<>(usdContract, 2.414e5);

        Trade<FxInstrument.Future, PositionInstrument.Contract> future =
                new Trade<>(em1, Arrays.asList(eurContractPos, usdContractPos));


        System.out.println(spot);
        System.out.println(future);

        List<Position<PositionInstrument.Contract>> positions = future.positions();
        System.out.println(positions);

        PositionUpdate update = new PositionUpdate(Arrays.asList(spot, future));

        // TODO: this is not enforced ... try to extract mirror type
        List<Trade<FxInstrument.FxSpot, PositionInstrument.CashFlow>> spotTrades = update.get(FxInstType.SPOT);
        List<Trade<FxInstrument.Future, PositionInstrument.Contract>> futureTrades = update.get(FxInstType.FUTURE);
        System.out.println(spotTrades);
        System.out.println(futureTrades);
    }
}

class PositionUpdate {
    private final List<Object>[] trades = make_array();

    PositionUpdate(Collection<Trade<?, ?>> trades) {
        for (Trade<?, ?> trade : trades) {
            this.trades[trade.instrument().type().ordinal()].add(trade);
        }
    }

    @SuppressWarnings("unchecked")
    <T extends FxInstrument<P>, P extends PositionInstrument> List<Trade<T, P>> get(TypedEnum<FxInstrument<?>> type) {
        List<?> ret = trades[type.ordinal()];
        for (Object t : ret) {
            assert type.clazz().isInstance(t);
        }
        return (List<Trade<T,P>>) ret;
    }

    @SuppressWarnings("unchecked")
    private static List<Object>[] make_array() {
        FxInstType[] types = FxInstType.values();
        List<?>[] ret = new List<?>[types.length];
        for (FxInstType type : types) {
            ret[type.ordinal()] = new ArrayList<>();
        }
        return (List<Object>[]) ret;
    }
}


class Trade<T extends FxInstrument<P>, P extends PositionInstrument> {
    private final T instrument;
    private final List<Position<P>> positions;

    Trade(T instrument, Collection<Position<P>> positions) {
        this.instrument = instrument;
        this.positions = new ArrayList<>(positions);
    }

    T instrument() { return instrument; }

    List<Position<P>> positions() {
        return positions;
    }

    @Override public String toString() {
        return String.format("[%s, %s]", instrument, positions);
    }
}

class Position<T extends PositionInstrument> {
    private final T instrument;
    private final double quantity;

    Position(T instrument, double quantity) {
        this.instrument = instrument;
        this.quantity = quantity;
    }

    @Override public String toString() {
        return String.format("[%s, %,.2f]", instrument, quantity);
    }
}


interface Typed<T extends Typed<T>> {
    TypedEnum<T> type();
}

interface TypedEnum<T extends Typed<T>> {
    Class<? extends T> clazz();
    int ordinal();
    String name();
}

enum FxInstType implements TypedEnum<FxInstrument<?>> {
    SPOT(FxInstrument.FxSpot.class),
    FORWARD(FxInstrument.FxForward.class),
    FUTURE(FxInstrument.Future.class)
    ;

    FxInstType(Class<? extends FxInstrument<?>> clazz) {
        this.clazz = clazz;
    }

    @Override public Class<? extends FxInstrument<?>> clazz() {
        return clazz;
    }

    private final Class<? extends FxInstrument<?>> clazz;
}

enum PosInstType implements TypedEnum<PositionInstrument> {
    CASH(PositionInstrument.CashFlow.class),
    FUTURES(PositionInstrument.Contract.class)
    ;

    private final Class<? extends PositionInstrument> clazz;

    PosInstType(Class<? extends PositionInstrument> clazz) {
        this.clazz = clazz;
    }

    @Override
    public Class<? extends PositionInstrument> clazz() {
        return clazz;
    }
}

abstract class FxInstrument<P extends PositionInstrument> implements Typed<FxInstrument<?>> {
    private final String pair;

    private FxInstrument(String pair) {
        this.pair = pair;
    }

    public String pair() {
        return pair;
    }

    @Override public String toString() {
        return String.format("[%s, %s]", type().name(), pair);
    }

    public abstract TypedEnum<PositionInstrument> positionType();

    public static class FxSpot extends FxInstrument<PositionInstrument.CashFlow> {
        public FxSpot(String pair) {
            super(pair);
        }

        @Override
        public TypedEnum<FxInstrument<?>> type() {
            return FxInstType.SPOT;
        }

        @Override
        public TypedEnum<PositionInstrument> positionType() {
            return PosInstType.CASH;
        }
    }

    public static class FxForward extends FxInstrument<PositionInstrument.CashFlow> {
        public FxForward(String pair) {
            super(pair);
        }

        @Override
        public TypedEnum<FxInstrument<?>> type() {
            return FxInstType.FORWARD;
        }
    }

    public static class Future extends FxInstrument<PositionInstrument.Contract> {
        private final String contract;

        public Future(String pair, String contract) {
            super(pair);
            this.contract = contract;
        }

        @Override
        public TypedEnum<FxInstrument<?>> type() {
            return FxInstType.FUTURE;
        }

        @Override public String toString() {
            return String.format("[%s, %s, %s]", type().name(), pair(), contract);
        }
    }
}

abstract class PositionInstrument implements Typed<PositionInstrument> {
    private final String currency;
    private PositionInstrument(String currency) { this.currency = currency; }
    public String currency() { return currency; }

    @Override public String toString() {
        return String.format("[%s, %s]", getClass().getSimpleName(), currency);
    }

    public static class CashFlow extends PositionInstrument {
        final LocalDate date;
        public CashFlow(String currency, LocalDate date) {
            super(currency);
            this.date = date;
        }

        @Override
        public TypedEnum<PositionInstrument> type() {
            return PosInstType.CASH;
        }
    }

    public static class Contract extends PositionInstrument {
        final String contract;
        public Contract(String currency, String contract) {
            super(currency);
            this.contract = contract;
        }

        @Override
        public TypedEnum<PositionInstrument> type() {
            return PosInstType.FUTURES;
        }
    }
}