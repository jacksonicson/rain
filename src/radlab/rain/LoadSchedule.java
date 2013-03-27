package radlab.rain;

import java.util.List;

public class LoadSchedule {

	private List<LoadUnit> loadUnits;

	public LoadSchedule(List<LoadUnit> loadUnits) {
		this.loadUnits = loadUnits;
	}

	public LoadUnit get(int index) {
		return loadUnits.get(index);
	}

	public int size() {
		return loadUnits.size();
	}
	
	public long getMaxGenerators() {
		int i = 0;
		for (LoadUnit unit : loadUnits) {
			i = Math.max(i, unit.getNumberOfUsers());
		}
		return i;
	}
}
