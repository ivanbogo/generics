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

        Trade<FxInstrument.Future, PositionInstrument.Contract> future = new Trade<>(em1, Arrays.asList(eurContractPos, usdContractPos));

        System.out.println(spot);
        System.out.println(future);

        List<Position<PositionInstrument.Contract>> positions = future.positions();
        System.out.println(positions);

        PositionUpdate update = new PositionUpdate(Arrays.asList(spot, future));

        // TODO: this is not enforced ... try to extract mirror type
        List<Trade<FxInstrument.FxSpot, PositionInstrument.CashFlow>> spotTrades = update.get(FxInstrument.SPOT);
        List<Trade<FxInstrument.Future, PositionInstrument.Contract>> futureTrades = update.get(FxInstrument.FUTURE);
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

    <T extends FxInstrument<P>, P extends PositionInstrument> List<Trade<T, P>> get(Type<T> type) {
        List<?> ret = trades[type.enumType().ordinal()];
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
    int ordinal();
    String name();

    Type<? extends T> type();
}

class Type<T> {
    private final Class<T> clazz;
    private final TypedEnum<? super T> enumType;

    protected Type(Class<T> clazz, TypedEnum<? super T> enumType) {
        this.clazz = clazz;
        this.enumType = enumType;
    }

    public Class<T> clazz() {
        return clazz;
    }

    public TypedEnum<? super T> enumType() {
        return enumType;
    }
}

enum FxInstType implements TypedEnum<FxInstrument<?>> {
    SPOT(FxInstrument.SPOT),
    FORWARD(FxInstrument.FORWARD),
    FUTURE(FxInstrument.FUTURE)
    ;

    private final Type<? extends FxInstrument<?>> type;

    FxInstType(Type<? extends FxInstrument<?>> type) {
        this.type = type;
    }

    public Type<? extends FxInstrument<?>> type() {
        return type;
    }
}

enum PosInstType implements TypedEnum<PositionInstrument> {
    CASH(PositionInstrument.CASH_FLOW),
    FUTURES(PositionInstrument.CONTRACT)
    ;

    private final Type<? extends PositionInstrument> type;

    PosInstType(Type<? extends PositionInstrument> type) {
        this.type = type;
    }

    @Override
    public Type<? extends PositionInstrument> type() {
        return type;
    }
}

abstract class FxInstrument<P extends PositionInstrument> implements Typed<FxInstrument<?>> {
    public static Type<FxInstrument.FxSpot> SPOT = new Type<>(FxInstrument.FxSpot.class, FxInstType.SPOT);
    public static Type<FxInstrument.FxForward> FORWARD = new Type<>(FxInstrument.FxForward.class, FxInstType.FORWARD);
    public static Type<FxInstrument.Future> FUTURE = new Type<>(FxInstrument.Future.class, FxInstType.FUTURE);

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

    public abstract Type<P> positionType();

    public static class FxSpot extends FxInstrument<PositionInstrument.CashFlow> {
        public FxSpot(String pair) {
            super(pair);
        }

        @Override
        public TypedEnum<FxInstrument<?>> type() {
            return FxInstType.SPOT;
        }

        @Override
        public Type<PositionInstrument.CashFlow> positionType() {
            return PositionInstrument.CASH_FLOW;
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

        @Override
        public Type<PositionInstrument.CashFlow> positionType() {
            return PositionInstrument.CASH_FLOW;
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

        @Override
        public Type<PositionInstrument.Contract> positionType() {
            return PositionInstrument.CONTRACT;
        }

        @Override public String toString() {
            return String.format("[%s, %s, %s]", type().name(), pair(), contract);
        }
    }
}

abstract class PositionInstrument implements Typed<PositionInstrument> {
    public static final Type<CashFlow> CASH_FLOW = new Type<>(CashFlow.class, PosInstType.CASH);
    public static final Type<Contract> CONTRACT = new Type<>(Contract.class, PosInstType.FUTURES);

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
