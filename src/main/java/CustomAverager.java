import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to create averages over a time span of some days.
 */
public class CustomAverager {
    private int days;
    private List<List<Integer>> savedNumbers;

    public CustomAverager(int days) {
        this.days = days;
        savedNumbers = new ArrayList<>();
    }

    public void addDay(int day, int number) {
        if (day > savedNumbers.size()) {
            List<Integer> list = new ArrayList<>();
            list.add(number);
            savedNumbers.add(list);
        }
    }

    public double getAverage() {
        double avg = 0;
        List<Integer> list = savedNumbers.get(days - 1);
        for (int number : list) {
            avg += number;
        }
        return avg / list.size();
    }

}
