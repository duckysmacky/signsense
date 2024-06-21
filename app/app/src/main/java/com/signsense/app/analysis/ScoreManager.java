package com.signsense.app.analysis;

import java.util.*;

public class ScoreManager {
    private String[] dictionary;
    private float[] scores;

    public ScoreManager (String[] dictionary, float[] scores) {
        this.dictionary = dictionary;
        this.scores = scores;
    }

    public Map<Object, Integer> mostCommon(Iterable<?> iterable) {
        Map<Object, Integer> items = new HashMap<>();

        for (Object item : iterable) {
            items.put(item, items.get(item) != null ? items.get(item) + 1 : 1);
        }

        return items;
    }

    public Map<String, Float> getScores() {
        Map<String, Float> scoreMap = new HashMap<>();
        for (int i = 0; i < scores.length; i++) scoreMap.put(dictionary[i], scores[i]);
        return sortByValue(scoreMap);
    }

    public float getBiggestScore() {
        float biggestScore = Float.MIN_VALUE;
        for (Float score : scores) if (score > biggestScore) biggestScore = score;
        return biggestScore + 60;
    }

    public String getLetter() {
        int letterIndex = -1;
        float biggestScore = Float.MIN_VALUE;

        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > biggestScore) {
                biggestScore = scores[i];
                letterIndex = i;
            }
        }

        return letterIndex != -1 ? dictionary[letterIndex] : "";
    }

    private Map<String, Float> sortByValue(Map<String, Float> scoreMap) {
        List<Map.Entry<String, Float>> entryList = new ArrayList<>(scoreMap.entrySet());
        Map<String, Float> sortedMap = new HashMap<>();

        entryList.sort(Collections.reverseOrder(Map.Entry.comparingByValue()));

        for (Map.Entry<String, Float> entry : entryList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    private String mostCommonSign(List<String> signs) {
        Map<String, Integer> occurrences = new HashMap<>();
        final String[] commonSign = new String[1];

        for (String sign : signs) {
            if (occurrences.containsKey(sign)) {
                occurrences.put(sign, occurrences.get(sign) + 1);
            } else {
                occurrences.putIfAbsent(sign, 1);
            }
        }
        int maxOcc = occurrences.values().stream()
                .max(Integer::compare)
                .get();

        occurrences.forEach((key, value) -> {
            if (value == maxOcc) {
                commonSign[0] = key;
            }
        });

        return commonSign[0];
    }
}
