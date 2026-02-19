package com.otr.lotto.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lottery")
public class LotteryProperties {
    private String firstPrizePhone;
    private String firstPrizeNumbers;

    public String getFirstPrizePhone() {
        return firstPrizePhone;
    }

    public void setFirstPrizePhone(String firstPrizePhone) {
        this.firstPrizePhone = firstPrizePhone;
    }

    public String getFirstPrizeNumbers() {
        return firstPrizeNumbers;
    }

    public void setFirstPrizeNumbers(String firstPrizeNumbers) {
        this.firstPrizeNumbers = firstPrizeNumbers;
    }

    public List<Integer> getWinningNumbers() {
        List<Integer> numbers = parseNumbers(firstPrizeNumbers);
        validateNumbers(numbers);
        return numbers;
    }

    public String getWinningNumbersCsv() {
        List<Integer> numbers = getWinningNumbers();
        List<Integer> sorted = new ArrayList<>(numbers);
        Collections.sort(sorted);
        return joinNumbers(sorted);
    }

    private List<Integer> parseNumbers(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] parts = value.split(",");
        List<Integer> numbers = new ArrayList<>(parts.length);
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                numbers.add(Integer.valueOf(trimmed));
            }
        }
        return numbers;
    }

    private void validateNumbers(List<Integer> numbers) {
        if (numbers.size() != 6) {
            throw new IllegalStateException("firstPrizeNumbers must contain 6 numbers");
        }

        Set<Integer> unique = new HashSet<>(numbers);
        if (unique.size() != 6) {
            throw new IllegalStateException("firstPrizeNumbers must be unique");
        }

        for (int number : numbers) {
            if (number < 1 || number > 45) {
                throw new IllegalStateException("firstPrizeNumbers must be between 1 and 45");
            }
        }
    }

    private String joinNumbers(List<Integer> numbers) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numbers.size(); i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(numbers.get(i));
        }
        return builder.toString();
    }
}
