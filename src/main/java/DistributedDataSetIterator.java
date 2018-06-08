import java.util.Random;

/**
 * This DataSetIterator trains a specific model (or class)
 * @author Leo Knoll
 */
public class DistributedDataSetIterator extends SingleDataSetIterator{

    public DistributedDataSetIterator(String dataFilePath, String delimiter, int inputDays, double splitRatio, String model, boolean addMissingDays, int skipFirstLines) {
        super(dataFilePath, delimiter, inputDays, splitRatio, model, addMissingDays, skipFirstLines);
    }

    @Override
    void splitData(double splitRatio) {
        // Check if data-size is big enough
        if (allData.size() < inputDays * 2) {
            System.err.println("Data set not big enough! Data set size: " + allData.size());
            System.exit(-1);
        }

        Random random = new Random(1234);
        for (RebateData data : allData) {
            boolean test = random.nextDouble() < 0.5;
            if (test) {
                testDataList.add(data);
            } else {
                trainDataList.add(data);
            }
        }

        test = generateTestDataSet(testDataList);
        System.out.println("Test data set size: " + test.size());
        System.out.println("Training data set size: " + trainDataList.size());
    }
}
