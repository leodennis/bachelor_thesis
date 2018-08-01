import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to create averages over a time span of some days.
 */
public class CustomAverager {
    private int days;
    private List<List<Double>> savedNumbers;

    public CustomAverager(int days) {
        this.days = days;
        savedNumbers = new ArrayList<>(this.days);
    }

    public void addDay(int day, double number) {
        if (day >= savedNumbers.size()) {
            List<Double> list = new ArrayList<>();
            list.add(number);
            savedNumbers.add(list);
        } else {
            savedNumbers.get(day).add(number);
        }
    }

    public double getAverage() {
        double avg = 0;
        List<Double> list = savedNumbers.get(0);
        for (double number : list) {
            avg += number;
        }
        return avg / list.size();
    }

    public void nextIteration() {
        savedNumbers.remove(0);
    }

}
