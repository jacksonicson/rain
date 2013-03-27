package radlab.rain;

import java.util.List;

public class LoadSchedule {

	private List<LoadUnit> loadUnits;

	public LoadSchedule(List<LoadUnit> loadUnits) {
		this.loadUnits = loadUnits;
	}

	public long getMaxGenerators() {
		int i = 0;
		for (LoadUnit unit : loadUnits) {
			i = Math.max(i, unit.getNumberOfUsers());
		}
		return i;
	}

}
