package radlab.rain.load;

import java.util.List;

public class LoadSchedule {

	private List<LoadDefinition> loadUnits;

	public LoadSchedule(List<LoadDefinition> loadUnits) {
		this.loadUnits = loadUnits;
	}

	public LoadDefinition get(int index) {
		return loadUnits.get(index);
	}

	public int size() {
		return loadUnits.size();
	}

	public long getMaxAgents() {
		long i = 0;
		for (LoadDefinition load : loadUnits)
			i = Math.max(i, load.getNumberOfUsers());
		return i;
	}
}
