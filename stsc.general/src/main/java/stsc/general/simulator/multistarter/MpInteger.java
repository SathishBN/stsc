package stsc.general.simulator.multistarter;

public class MpInteger extends MpIterator<Integer> {

	private final int from;
	private final int to;
	private final int step;
	private int iterator;

	public MpInteger(String name, int from, int to, int step) throws BadParameterException {
		super(name);
		this.from = from;
		this.to = to;
		if (from >= to)
			throw new BadParameterException("Integer from should be smaller than to for " + name);
		this.step = step;
		this.iterator = 0;
	}

	@Override
	public MpIterator<Integer> clone() {
		return new MpInteger(getName(), from, to, step, true);
	}

	private MpInteger(String name, int from, int to, int step, boolean privateBoolean) {
		super(name);
		this.from = from;
		this.to = to;
		this.step = step;
		this.iterator = 0;
	}

	@Override
	public long size() {
		return Math.round((to - from) / step);
	}

	@Override
	public String toString() {
		return getName() + ":" + String.valueOf(current()) + " from (" + String.valueOf(step) + "|" + String.valueOf(from) + ":" + String.valueOf(to) + ")";
	}

	@Override
	public boolean hasNext() {
		return current() < to;
	}

	@Override
	public void reset() {
		iterator = 0;
	}

	@Override
	public void increment() {
		iterator += 1;
	}

	@Override
	public Integer current() {
		return Integer.valueOf(from + iterator * step);
	}

	@Override
	public Integer next() {
		final int result = current();
		increment();
		return result;
	}

	@Override
	public void remove() {
		iterator = 0;
	}

	@Override
	public Integer parameter(int index) {
		return Integer.valueOf(from + step * index);
	}

}
