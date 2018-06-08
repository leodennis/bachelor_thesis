import javafx.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Class to read in data for best rebate prediction. Provides test and training data.
 * @author Leo Knoll
 */
public class MyDataSetIterator implements DataSetIterator {

    // number of inputs (features) for each rebate data
    private final int INPUT_VECTOR_SIZE = 4;
    private final int OUTPUT_VECTOR_SIZE = 2;

    // dataset for training and testing
    private List<RebateData> train;
    private List<Pair<INDArray, INDArray>> test;

    // filter for specific model (name will be converted into int)
    private Integer filterID = null;

    // list containing all mini-batch sizes and variables no navigate through them
    private List<Integer> trainingMiniBatchSizes = new ArrayList<>();
    private int currentIterationElement = 0;
    private int currentIterationIndex = 0;
    private boolean finished = false;

    /** minimal values of each feature in stock dataset */
    private double[] minArray = new double[INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE];
    /** maximal values of each feature in stock dataset */
    private double[] maxArray = new double[INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE];

    /**
     * Initialize new DataSetIterator object.
     * Reads in data and prepares test and train data.
     * @param dataFilePath the path to the data containing the data
     * @param delimiter the delimiter used to separate columns in the data
     * @param splitRatio the percent of data used for training
     * @param model (optional) the model we want to test
     */
    public MyDataSetIterator(String dataFilePath, String delimiter, double splitRatio, String model) {

        // read all input data
        List<RebateData> rebateDataList = readRebateDataFromFile(dataFilePath, delimiter, model);

        System.out.println(Arrays.toString(minArray));
        System.out.println(Arrays.toString(maxArray));

        // split train and test data
        int split = (int) Math.round(rebateDataList.size() * splitRatio);
        train = rebateDataList.subList(0, split);
        test = generateTestDataSet(rebateDataList.subList(split, rebateDataList.size()));

        // split data into mini-batch sizes
        initializeTrainingMiniBatchSizes();
    }

    /**
     * Saves batch sizes of each week.
     */
    void initializeTrainingMiniBatchSizes() {
        int year = -1;
        int week = -1;
        int size = 0;
        for (RebateData data : train) {
            Date date = new Date(data.getDate()*1000); //date time = UNIX-Timestamp*1000
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            if (year < 0) {
                year = calendar.get(Calendar.YEAR);
                week = calendar.get(Calendar.WEEK_OF_YEAR);
            }

            if (year == calendar.get(Calendar.YEAR) && week == calendar.get(Calendar.WEEK_OF_YEAR)) {
                size++;
            } else {
                trainingMiniBatchSizes.add(size);
                size = 1;
                year = calendar.get(Calendar.YEAR);
                week = calendar.get(Calendar.WEEK_OF_YEAR);
            }
        }
        if (size > 0) {
            trainingMiniBatchSizes.add(size);
        } // else: no training data!
    }

    public double[] getMaxArray() {
        double[] arr = new double[2];
        arr[0] = maxArray[4];
        arr[1] = maxArray[5];
        return arr;
    }

    public double[] getMinArray() {
        double[] arr = new double[2];
        arr[0] = minArray[4];
        arr[1] = minArray[5];
        return arr;
    }

    /**
     * Returns the generated test data.
     * @return the data for testing.
     */
    public List<Pair<INDArray, INDArray>> getTestDataSet() { return test; }

    /**
     * Get the next mini-batch size.
     * Also handles if epoch is finished.
     * @return the current mini-batch size to be processed
     */
    private int getCurrentMiniBatchSize() {
        int size = trainingMiniBatchSizes.get(currentIterationIndex);
        currentIterationIndex++;
        if (currentIterationIndex == trainingMiniBatchSizes.size()) {
            finished = true;
        }
        return size;
    }

    @Override
    public DataSet next(int actualMiniBatchSize) {
        // Nd4j.create(new int[] {MINI-BATCH SIZE, INPUTS, TIME SERIES}, 'f')
        INDArray input = Nd4j.create(new int[] {actualMiniBatchSize, INPUT_VECTOR_SIZE, 7}, 'f');
        INDArray label = Nd4j.create(new int[] {actualMiniBatchSize, OUTPUT_VECTOR_SIZE, 7}, 'f');

        // for all train data in mini-batch size
        for (int i = 0; i < actualMiniBatchSize; i++) {
            RebateData curData = train.get(currentIterationElement + i);

            // get date and init calendar
            Date date = new Date(curData.getDate()*1000); //date time = UNIX-Timestamp*1000
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int day = calendar.get(Calendar.DAY_OF_WEEK) - 1; // -1 because it starts at 1

            // input (features)
            input.putScalar(new int[] {i, 0, day}, (curData.getName() - minArray[0]) / (maxArray[0] - minArray[0]));
            input.putScalar(new int[] {i, 1, day}, (curData.getYear() - minArray[1]) / (maxArray[1] - minArray[1]));
            input.putScalar(new int[] {i, 2, day}, (curData.getDate() - minArray[2]) / (maxArray[2] - minArray[2]));
            input.putScalar(new int[] {i, 3, day}, (curData.getRebate() - minArray[3]) / (maxArray[3] - minArray[3]));

            // output (labels)
            label.putScalar(new int[] {i, 0, day}, (curData.getSalesRebate() - minArray[4]) / (maxArray[4] - minArray[4]));
            label.putScalar(new int[] {i, 1, day}, (curData.getSalesWithout() - minArray[5]) / (maxArray[5] - minArray[5]));
        }

        currentIterationElement += actualMiniBatchSize;

        //System.out.println("Labels: " + Arrays.toString(new DataSet(input, label).getLabels().shape()));
        //System.out.println("Features: " + Arrays.toString(new DataSet(input, label).getFeatures().shape()));
        return new DataSet(input, label);
    }

    @Override public int totalExamples() { return train.size(); }

    @Override public int inputColumns() { return INPUT_VECTOR_SIZE; }

    @Override public int totalOutcomes() {
        return OUTPUT_VECTOR_SIZE;
    }

    @Override public boolean resetSupported() { return true; }

    @Override public boolean asyncSupported() { return false; }

    @Override public void reset() {
        currentIterationElement = 0;
        currentIterationIndex = 0;
        finished = false;
    }

    @Override public int batch() { return (int) trainingMiniBatchSizes.stream().mapToInt(e -> e).average().orElse(0); }

    @Override public int cursor() { return totalExamples();}

    @Override public int numExamples() { return totalExamples(); }

    @Override public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override public DataSetPreProcessor getPreProcessor() { throw new UnsupportedOperationException("Not Implemented"); }

    @Override public List<String> getLabels() { throw new UnsupportedOperationException("Not Implemented"); }

    @Override public boolean hasNext() { return finished == false; }

    @Override public DataSet next() { return next(getCurrentMiniBatchSize()); }

    /**
     * Generates the test data set.
     * @param rebateDataList data list containing the data
     * @return list of test data
     */
    private List<Pair<INDArray, INDArray>> generateTestDataSet (List<RebateData> rebateDataList) {
        List<Pair<INDArray, INDArray>> test = new ArrayList<>();

        // for all test data
        for (int i = 0; i < rebateDataList.size(); i++) {
            INDArray input = Nd4j.create(new int[] {1, INPUT_VECTOR_SIZE, 1}, 'f');
            INDArray label = Nd4j.create(new int[] {1, OUTPUT_VECTOR_SIZE, 1}, 'f');

            RebateData rebateData = rebateDataList.get(i);

            // check if we filter for a specific model
            if (filterID != null && rebateData.getName() != filterID) {
                continue;
            }

            // get date and init calendar
            Date date = new Date(rebateData.getDate()*1000); //date time = UNIX-Timestamp*1000
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int day = calendar.get(Calendar.DAY_OF_WEEK) - 1; // -1 because it starts at 1

            day = 0;

            // input (features)
            input.putScalar(new int[] {0, 0, day}, (rebateData.getName() - minArray[0]) / (maxArray[0] - minArray[0]));
            input.putScalar(new int[] {0, 1, day}, (rebateData.getYear() - minArray[1]) / (maxArray[1] - minArray[1]));
            input.putScalar(new int[] {0, 2, day}, (rebateData.getDate() - minArray[2]) / (maxArray[2] - minArray[2]));
            input.putScalar(new int[] {0, 3, day}, (rebateData.getRebate() - minArray[3]) / (maxArray[3] - minArray[3]));

            // output (labels)
            label.putScalar(new int[] {0, 0, 0}, rebateData.getSalesRebate()); //save in {0, 0, 0} for easy recovery
            label.putScalar(new int[] {0, 1, 0}, rebateData.getSalesWithout()); //save in {0, 1, 0} for easy recovery
            //label.putScalar(new int[] {0, 1, 1}, day); // also save day

            test.add(new Pair<>(input, label));
        }
        return test;
    }

    /**
     * Reads in all data and saves it in a list.
     * Also converts strings to numbers.
     * @param filename the path to the file containing the data
     * @param delimiter the delimiter used to separate columns in the data
     * @param model (optional) the model we want to test, saves the associated number in filterID
     * @return all data as list
     */
    private List<RebateData> readRebateDataFromFile(String filename, String delimiter, String model) {
        List<RebateData> stockDataList = new ArrayList<>();

        //Read file into list
        try {

            for (int i = 0; i < maxArray.length; i++) { // initialize max and min arrays
                maxArray[i] = Double.MIN_VALUE;
                minArray[i] = Double.MAX_VALUE;
            }

            // String replacer replaces each string with a unique number
            StringReplacer modelReplacer = new StringReplacer(6,
                new int[] {StringReplacer.TYPE_STRING, StringReplacer.TYPE_NUMBER, StringReplacer.TYPE_NUMBER, StringReplacer.TYPE_NUMBER, StringReplacer.TYPE_NUMBER, StringReplacer.TYPE_NUMBER});

            List<String[]> list = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
            String line = reader.readLine(); //skip first line  // TODO: als parameter uebergeben
            int lineNumber = 1;                                 // TODO: als parameter uebergeben

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] dataSplit = line.split(delimiter, -1); //limit = -1 so that it does not leave out empty elements
                if (dataSplit.length == INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE) {

                    // TODO: for testing only
                    if (model != null && !model.equals(dataSplit[0])) {
                        continue;
                    }

                    String[] replacedData = modelReplacer.replace(dataSplit);   // replace Strings
                    replacedData = modelReplacer.fillNullOrEmpty(replacedData, "0", true); //replace empty number fields

                    list.add(replacedData);
                    if (model != null && filterID == null && model.trim().equals(dataSplit[0].trim())) {
                        filterID = Integer.valueOf(replacedData[0]);
                        System.out.println("Filter for Modell " + model + " represented by " + filterID + ".");
                    }
                } else {
                    // Print Error
                    System.err.println("Wrong number of columns in line " + lineNumber + ": " + line);
                }
            }
            reader.close();

            for (String[] arr : list) {

                double[] nums = new double[INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE];
                for (int i = 0; i < arr.length; i++) {
                    nums[i] = Double.valueOf(arr[i]);
                    if (i == 2) {
                        nums[i] /= 1000; //because of unix timestamp conversion
                    }
                    if (nums[i] > maxArray[i]) maxArray[i] = nums[i];
                    if (nums[i] < minArray[i]) minArray[i] = nums[i];
                }

                stockDataList.add(new RebateData(Integer.valueOf(arr[0]), Integer.valueOf(arr[1]), Long.valueOf(arr[2])/1000, Double.valueOf(arr[3]), Integer.valueOf(arr[4]),
                    Integer.valueOf(arr[5])));

            }

            // important otherwise training impossible because of division by zero
            for (int i = 0; i < minArray.length; i++) {
                if (minArray[i] == maxArray[i]) {
                    minArray[i] = 0;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stockDataList;
    }
}
