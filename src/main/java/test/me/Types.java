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

    <T extends FxInstrument<P>, P extends PositionInstrument> List<Trade<T, P>> get(MarkerType<T> type) {
        List<?> ret = trades[type.type().ordinal()];
        for (Object t : ret) {
            // TODO assert this during construction
            assert type.clazz().isInstance(t);
        }
        return (List<Trade<T,P>>) ret;
    }

    @SuppressWarnings("unchecked")
    private static List<Object>[] make_array() {
        FxInstrumentType[] types = FxInstrumentType.values();
        List<?>[] ret = new List<?>[types.length];
        for (FxInstrumentType type : types) {
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



class MarkerType<T> {
    final Class<T> clazz;
    final EnumType<?, ? super T> type;

    MarkerType(Class<T> clazz, EnumType<?, ? super T> type) {
        this.clazz = clazz;
        this.type = type;
    }

    public Class<T> clazz() {
        return clazz;
    }

    public EnumType<?, ? super T> type() {
        return type;
    }
}

interface EnumType<T extends Enum<T>, P> {
    int ordinal();
    MarkerType<? extends P> marker();
}


enum FxInstrumentType implements EnumType<FxInstrumentType, FxInstrument<?>> {
    SPOT(FxInstrument.SPOT),
    FORWARD(FxInstrument.FORWARD),
    FUTURE(FxInstrument.FUTURE);

    private final MarkerType<? extends FxInstrument<?>> marker;

    FxInstrumentType(MarkerType<? extends FxInstrument<?>> marker) {
        this.marker = marker;
    }

    public MarkerType<? extends FxInstrument<?>> marker() {
        return marker;
    }
}

enum PositionInstrumentType implements EnumType<PositionInstrumentType, PositionInstrument> {
    ForwardCashFlow(PositionInstrument.CASH_FLOW),
    Contract(PositionInstrument.CONTRACT)
    ;

    private final MarkerType<? extends PositionInstrument> marker;

    PositionInstrumentType(MarkerType<? extends PositionInstrument> marker) {
        this.marker = marker;
    }

    @Override
    public MarkerType<? extends PositionInstrument> marker() {
        return marker;
    }
}

abstract class FxInstrument<P extends PositionInstrument> {
    public static MarkerType<FxSpot> SPOT = new MarkerType<>(FxInstrument.FxSpot.class, FxInstrumentType.SPOT);
    public static MarkerType<FxForward> FORWARD = new MarkerType<>(FxInstrument.FxForward.class, FxInstrumentType.FORWARD);
    public static MarkerType<Future> FUTURE = new MarkerType<>(FxInstrument.Future.class, FxInstrumentType.FUTURE);

    private final String pair;

    private FxInstrument(String pair) {
        this.pair = pair;
    }

    public String pair() {
        return pair;
    }

    public abstract FxInstrumentType type();

    @Override public String toString() {
        return String.format("[%s, %s]", type(), pair);
    }

    public static class FxSpot extends FxInstrument<PositionInstrument.CashFlow> {
        public FxSpot(String pair) {
            super(pair);
        }

        @Override
        public FxInstrumentType type() {
            return FxInstrumentType.SPOT;
        }
    }

    public static class FxForward extends FxInstrument<PositionInstrument.CashFlow> {
        public FxForward(String pair) {
            super(pair);
        }

        @Override
        public FxInstrumentType type() {
            return FxInstrumentType.FORWARD;
        }
    }

    public static class Future extends FxInstrument<PositionInstrument.Contract> {
        private final String contract;

        public Future(String pair, String contract) {
            super(pair);
            this.contract = contract;
        }

        @Override
        public FxInstrumentType type() {
            return FxInstrumentType.FUTURE;
        }

        @Override public String toString() {
            return String.format("[%s, %s, %s]", type(), pair(), contract);
        }
    }
}

abstract class PositionInstrument {
    public static final MarkerType<CashFlow> CASH_FLOW = new MarkerType<>(CashFlow.class, PositionInstrumentType.ForwardCashFlow);
    public static final MarkerType<Contract> CONTRACT = new MarkerType<>(Contract.class, PositionInstrumentType.Contract);

    private final String currency;
    private PositionInstrument(String currency) { this.currency = currency; }
    public String currency() { return currency; }
    public abstract PositionInstrumentType type();

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
        public PositionInstrumentType type() {
            return PositionInstrumentType.ForwardCashFlow;
        }
    }

    public static class Contract extends PositionInstrument {
        final String contract;
        public Contract(String currency, String contract) {
            super(currency);
            this.contract = contract;
        }

        @Override
        public PositionInstrumentType type() {
            return PositionInstrumentType.Contract;
        }
    }
}
