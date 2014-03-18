package stsc.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import stsc.algorithms.EodAlgorithm;
import stsc.algorithms.StockAlgorithm;

public final class AlgorithmNamesStorage {

	private String containerPackageName = "stsc.algorithm";

	private HashMap<String, Class<? extends StockAlgorithm>> stockNames = new HashMap<>();
	private HashMap<String, Class<? extends EodAlgorithm>> eodNames = new HashMap<>();

	public AlgorithmNamesStorage() throws ClassNotFoundException, IOException {
		loadAlgorithms();
	}

	public AlgorithmNamesStorage(final String containerPackageName) throws ClassNotFoundException, IOException {
		this.containerPackageName = containerPackageName;
		loadAlgorithms();
	}

	private void loadAlgorithms() throws ClassNotFoundException, IOException {
		for (ClassInfo e : ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses()) {
			final String eName = e.getName().toLowerCase();
			if (eName.contains("$") || eName.contains("test"))
				continue;
			else if (eName.startsWith(containerPackageName)) {
				final Class<?> classType = Class.forName(e.getName());
				if (classType.getSuperclass() == StockAlgorithm.class) {
					final Class<? extends StockAlgorithm> stockAlgorithm = classType.asSubclass(StockAlgorithm.class);
					addStockAlgorithm(stockAlgorithm);
				}
				if (classType.getSuperclass() == EodAlgorithm.class) {
					final Class<? extends EodAlgorithm> eodAlgorithm = classType.asSubclass(EodAlgorithm.class);
					addEodAlgorithm(eodAlgorithm);
				}
			}
		}

	}

	public void addStockAlgorithm(Class<? extends StockAlgorithm> algorithmClass) {
		add(algorithmClass, stockNames);
	}

	public void addEodAlgorithm(Class<? extends EodAlgorithm> algorithmClass) {
		add(algorithmClass, eodNames);
	}

	private final <T> void add(Class<? extends T> classType, HashMap<String, Class<? extends T>> algorithmsMap) {
		final String className = classType.getName().toLowerCase();
		if (!algorithmsMap.containsKey(className))
			algorithmsMap.put(className, classType);
	}

	public Class<? extends EodAlgorithm> getEod(final String algorithmName) {
		return getAlgorithmClass(algorithmName, eodNames);
	}

	public Class<? extends StockAlgorithm> getStock(final String algorithmName) {
		return getAlgorithmClass(algorithmName, stockNames);
	}

	private final <T> Class<? extends T> getAlgorithmClass(final String algorithmName,
			HashMap<String, Class<? extends T>> algorithmsMap) {
		final String lowCase = algorithmName.toLowerCase();
		for (Map.Entry<String, Class<? extends T>> i : algorithmsMap.entrySet()) {
			if (i.getKey().contains(lowCase))
				return i.getValue();
		}
		return null;
	}
}
