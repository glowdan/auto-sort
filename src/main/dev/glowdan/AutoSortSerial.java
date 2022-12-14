package dev.glowdan;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AutoSortForSerial {
    private static final Map<String, Float> SortScoreMap = new ConcurrentHashMap<>();
    private static final Map<String, TaskRunStatic> taskStatics = new ConcurrentHashMap<>();

    public static <T extends Named> List<T> reCalScore(List<T> originList) {
        return originList.stream()
                .sorted(Comparator.comparing(o -> SortScoreMap.getOrDefault(o.getFilterName(), Float.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    public static void updateStatic(String taskName, int itemNum, int filterNum, long timeCost) {
        taskStatics.computeIfAbsent(taskName, TaskRunStatic::new).update(itemNum, filterNum, timeCost);
    }

    private static class TaskRunStatic {
        private int count = 0;
        private int allCount = 0;
        private int filterCount = 0;
        private long cost = 0;
        private final String taskName;
        private int updateSteps = 1;

        public TaskRunStatic(String taskName) {
            this.taskName = taskName;
        }

        public synchronized void update(int itemNum, int fCount, long timeCost) {
            count++;
            allCount += itemNum;
            filterCount += fCount;
            cost += timeCost;
            if (count > updateSteps) {
                Float unitCost = 1f * allCount / cost;
                Float keepAbility = 1f * (allCount - filterCount) / allCount;
                //保留能力弱并且用时短的最优
                SortScoreMap.put(this.taskName, unitCost * keepAbility);
                count = 0;
                allCount = 0;
                filterCount = 0;
                cost = 0;
                updateSteps *= 2;
                if (updateSteps > 1024) {
                    updateSteps = 1024;
                }
            }
        }

    }
}
