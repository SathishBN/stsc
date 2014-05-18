package stsc.simulator;

import org.apache.commons.lang3.Validate;

import stsc.trading.TradeProcessorInit;

public class SimulatorSettings {

	private long id;
	private final TradeProcessorInit tradeProcessorInit;

	public SimulatorSettings(final long id, TradeProcessorInit tradeProcessorInit) {
		Validate.notNull(tradeProcessorInit);
		Validate.isTrue(id >= 0);
		this.id = id;
		this.tradeProcessorInit = tradeProcessorInit;
	}

	public TradeProcessorInit getInit() {
		return tradeProcessorInit;
	}

	public String stringHashCode() {
		return tradeProcessorInit.stringHashCode();
	}

	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return tradeProcessorInit.toString();
	}

}