package dev.glowdan;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AutoSort {
    private static final Map<String, Float> SortScoreMap = new ConcurrentHashMap<>();
    private static final Map<String, LoaderStat> OriginTimeCost = new ConcurrentHashMap<>();

    public static <T> List<T> sort(Map<String, T> originMaps) {
        return originMaps.keySet().stream()
                .map(name -> new SortStruct<>(SortScoreMap.getOrDefault(name, 0.0f), originMaps.get(name)))
                .sorted()
                .map(SortStruct::getValue)
                .collect(Collectors.toList());
    }

    public static <T extends Named> List<T> sort(List<T> originList) {
        return originList.stream()
                .map(one -> new SortStruct<>(SortScoreMap.getOrDefault(one.getName(), 0.0f), one))
                .sorted()
                .map(SortStruct::getValue)
                .collect(Collectors.toList());
    }

    public static void updateTimeCost(String source, long timeCost) {
        LoaderStat stat = OriginTimeCost.get(source);
        if (stat == null) {
            stat = new LoaderStat(source);
            OriginTimeCost.put(source, stat);
        }
        stat.update(timeCost);
    }

    private static class SortStruct<T> implements Comparable<SortStruct<T>> {
        Float score;
        T value;

        public SortStruct(Float score, T value) {
            this.score = score;
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        @Override
        public int compareTo(SortStruct o) {
            return o.score.compareTo(this.score);
        }
    }

    private static class LoaderStat {
        private int preCount = 0;
        private long preCost = 0;
        private final String loaderName;

        public LoaderStat(String loaderName) {
            this.loaderName = loaderName;
        }

        public synchronized void update(long timecost) {
            preCost += timecost;
            preCount++;
            if (preCount > 50) { //大概半分到1分钟
                SortScoreMap.put(loaderName, 1f * preCost / preCount);
                preCount = 0;
                preCost = 0;
            }
        }

    }
}

