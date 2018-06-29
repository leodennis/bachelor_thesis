import javafx.util.Pair;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.factory.Nd4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * This DataSetIterator trains all models on the network.
 *
 * @author Leo Knoll
 */
public class SeparatedDataSetIterator extends RebateDataSetIterator {

    private List<Integer> trainingMiniBatchSizes = new ArrayList<>();
    private int currentIterationElement = 0;
    private int currentIterationIndex = 0;
    private int currentListIndex = 0;

    private Map<Integer, List<RebateData>> separatedData = new HashMap<>();
    private List<Integer> modelList = new ArrayList<>();

    public SeparatedDataSetIterator(String dataFilePath, String delimiter, double splitRatio, String model, boolean addMissingDays, int skipFirstLines, boolean average) {
        super(dataFilePath, delimiter, 1, splitRatio, model, addMissingDays, skipFirstLines, average);
    }

    @Override
    void splitData(double splitRatio) {
        // already happened in readRebateDataFromFile
        // may be optimized here

        test = generateTestDataSet(testDataList);
    }

    /**
     * Saves batch sizes of each week.
     */
    @Override
    void initializeTraining() {
        trainingMiniBatchSizes.clear();
        int model = modelList.get(currentListIndex);
        List<RebateData>  dataList = separatedData.get(model);
        int fullBatches = dataList.size() / MINI_BATCH_SIZE;
        int rest = dataList.size() % MINI_BATCH_SIZE;

        for (int i = 0; i < fullBatches; i++) {
            trainingMiniBatchSizes.add(MINI_BATCH_SIZE);
        }

        if (rest > 0) {
            trainingMiniBatchSizes.add(rest);
        }
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
        return size;
    }

    @Override
    public DataSet next(int actualMiniBatchSize) {
        int model = modelList.get(currentListIndex);
        List<RebateData>  dataList = separatedData.get(model);

        // Nd4j.create(new int[] {MINI-BATCH SIZE, INPUTS, TIME SERIES}, 'f')
        INDArray input = Nd4j.create(new int[] {actualMiniBatchSize, INPUT_VECTOR_SIZE, 1}, 'f');
        INDArray label = Nd4j.create(new int[] {actualMiniBatchSize, OUTPUT_VECTOR_SIZE, 1}, 'f');

        //System.out.println("Training " + actualMiniBatchSize + " of " + model);

        // for all train data in mini-batch size
        for (int i = 0; i < actualMiniBatchSize; i++) {
            RebateData curData = dataList.get(currentIterationElement + i);

            // input (features) //TODO: or {i, 0, i} ???
            input.putScalar(new int[] {i, 0, 0}, (curData.getName() - minArray[0]) / (maxArray[0] - minArray[0]));
            input.putScalar(new int[] {i, 1, 0}, (curData.getYear() - minArray[1]) / (maxArray[1] - minArray[1]));
            input.putScalar(new int[] {i, 2, 0}, (curData.getDate() - minArray[2]) / (maxArray[2] - minArray[2]));
            input.putScalar(new int[] {i, 3, 0}, (curData.getRebate() - minArray[3]) / (maxArray[3] - minArray[3]));

            // output (labels)
            label.putScalar(new int[] {i, 0, 0}, (curData.getSales() - minArray[4]) / (maxArray[4] - minArray[4]));
        }

        currentIterationElement += actualMiniBatchSize;

        if (currentIterationElement == dataList.size()) {
            needsReset = true;
            currentListIndex++;
            if (currentListIndex == modelList.size()) {
                finished = true;
            } else {
                initializeTraining();
            }
        }

        //System.out.println("Labels: " + Arrays.toString(new DataSet(input, label).getLabels().shape()));
        //System.out.println("Features: " + Arrays.toString(new DataSet(input, label).getFeatures().shape()));
        return new DataSet(input, label);
    }

    @Override public int totalExamples() {
        int cnt = 0;
        for (int n : trainingMiniBatchSizes) {
            cnt += n;
        }
        return cnt;
    }

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
        needsReset = false;
        if (currentListIndex == modelList.size()) {
            currentListIndex = 0;
            initializeTraining();
        }
    }

    @Override public int batch() { return separatedData.keySet().size(); }

    @Override public int cursor() { return totalExamples();}

    @Override public int numExamples() { return totalExamples(); }

    @Override public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override public DataSetPreProcessor getPreProcessor() { throw new UnsupportedOperationException("Not Implemented"); }

    @Override public List<String> getLabels() { throw new UnsupportedOperationException("Not Implemented"); }

    @Override public boolean hasNext() { return finished == false; }

    @Override public DataSet next() { return next(getCurrentMiniBatchSize()); }

    public boolean needsReset() {
        return needsReset;
    }

    /**
     * Generates the test data set.
     * @param rebateDataList data list containing the data
     * @return list of test data
     */
    private List<Pair<INDArray, INDArray>> generateTestDataSet (List<RebateData> rebateDataList) {
        List<Pair<INDArray, INDArray>> test = new ArrayList<>();

        System.out.println("Test data size: " + rebateDataList.size());

        // for all test data
        for (int i = 0; i < rebateDataList.size(); i++) {
            INDArray input = Nd4j.create(new int[] {1, INPUT_VECTOR_SIZE, 1}, 'f');
            INDArray label = Nd4j.create(new int[] {1, OUTPUT_VECTOR_SIZE, 1}, 'f');

            RebateData rebateData = rebateDataList.get(i);

            // input (features)
            input.putScalar(new int[] {0, 0, 0}, (rebateData.getName() - minArray[0]) / (maxArray[0] - minArray[0]));
            input.putScalar(new int[] {0, 1, 0}, (rebateData.getYear() - minArray[1]) / (maxArray[1] - minArray[1]));
            input.putScalar(new int[] {0, 2, 0}, (rebateData.getDate() - minArray[2]) / (maxArray[2] - minArray[2]));
            input.putScalar(new int[] {0, 3, 0}, (rebateData.getRebate() - minArray[3]) / (maxArray[3] - minArray[3]));

            // output (labels)
            label.putScalar(new int[] {0, 0, 0}, rebateData.getSales()); //save in {0, 0, 0} for easy recovery

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
    @Override
    List<RebateData> readRebateDataFromFile(String filename, String delimiter, String model, boolean addMissingDays, int skipFirstLines) {
        List<RebateData> allDataList = new ArrayList<>();

        //Read file into list
        try {
            long separationLine = (long) (0.8 * Files.lines(Paths.get(filename)).count());

            for (int i = 0; i < maxArray.length; i++) { // initialize max and min arrays
                maxArray[i] = Double.MIN_VALUE;
                minArray[i] = Double.MAX_VALUE;
            }

            // String replacer replaces each string with a unique number
            StringReplacer modelReplacer = new StringReplacer(INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE,
                new int[] {StringReplacer.TYPE_STRING, StringReplacer.TYPE_NUMBER, StringReplacer.TYPE_NUMBER, StringReplacer.TYPE_NUMBER, StringReplacer.TYPE_NUMBER});

            List<String[]> list = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
            String line;

            //skip first lines
            for (int i = 0; i < skipFirstLines; i++) {
                reader.readLine();
            }
            int lineNumber = skipFirstLines;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] dataSplit = line.split(delimiter, -1); //limit = -1 so that it does not leave out empty elements
                if (dataSplit.length == INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE) {

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

            lineNumber = 1;
            for (String[] arr : list) {
                lineNumber++;
                if (lineNumber >= separationLine) {
                    RebateData data = new RebateData(Integer.valueOf(arr[0]), Integer.valueOf(arr[1]), Long.valueOf(arr[2])/1000, Double.valueOf(arr[3]), Integer.valueOf(arr[4]));
                    if (data.getName() == filterID) {
                        testDataList.add(data);
                    }
                    allDataList.add(data);
                    continue;
                }
                double[] nums = new double[INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE];
                for (int i = 0; i < arr.length; i++) {
                    nums[i] = Double.valueOf(arr[i]);
                    if (i == INDEX_DATE) {
                        nums[i] /= 1000; //because of unix timestamp conversion
                    }
                    if (nums[i] > maxArray[i]) maxArray[i] = nums[i];
                    if (nums[i] < minArray[i]) minArray[i] = nums[i];
                }

                RebateData data = new RebateData(Integer.valueOf(arr[0]), Integer.valueOf(arr[1]), Long.valueOf(arr[2])/1000, Double.valueOf(arr[3]), Integer.valueOf(arr[4]));

                // add to map
                if (separatedData.get(data.getName()) == null) {
                    separatedData.put(data.getName(), new ArrayList<RebateData>());
                    modelList.add(data.getName());
                }
                separatedData.get(data.getName()).add(data);

                allDataList.add(data);
            }

            if (addMissingDays) {
                List<RebateData> missingDates = new ArrayList<>();
                for (Integer m : modelList) {
                    int minDate = (int) minArray[2];
                    int maxDate = (int) maxArray[2];
                    List<RebateData> dateList = separatedData.get(m);
                    if (m != null) {
                        for (RebateData data : dateList) {
                            while (minDate + 24*60*60 <= data.getDate()) {
                                missingDates.add(new RebateData(data.getName(), data.getYear(), minDate, 0, 0));
                                minArray[3] = 0;
                                minArray[4] = 0;
                                minArray[5] = 0;
                                minDate += 24*60*60;
                            }
                        }
                        RebateData data = dateList.get(dateList.size()-1);
                        while (data.getDate() + 24*60*60 <= maxDate) {
                            data = new RebateData(data.getName(), data.getYear(), data.getDate() + 24*60*60, 0, 0);
                            missingDates.add(data);
                        }
                        dateList.addAll(missingDates);
                        missingDates.clear();
                        Collections.sort(dateList, new Comparator<RebateData>() {
                            public int compare(RebateData d1, RebateData d2) {
                                return (int) (d1.getDate() - d2.getDate());
                            }
                        });
                    } else {
                        // should not happen
                        System.err.println("Model " + m + " defined but no data found");
                    }

                }
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
        return allDataList;
    }


    // ---------------------------------------------- Testing ---------------------------------------------------------

    @Override
    void testPrediction(MultiLayerNetwork net) {
        double[] predictedSales = new double[test.size()];
        double[] actualSales = new double[test.size()];

        double min[] = getMinArray();
        double max[] = getMaxArray();

        for (int i = 0; i < test.size(); i++) {
            INDArray predicts = net.rnnTimeStep(test.get(i).getKey());
            INDArray actuals = test.get(i).getValue();
            int day = 0; //actuals.getInt(0, 1, 1); // recover day

            predictedSales[i] = predicts.getDouble(0, 0, day) * (max[0] - min[0]) + min[0];

            actualSales[i] = actuals.getInt(0, 0, 0);
        }

        log.info("Plot...");
        PlotUtil.plot(predictedSales, actualSales, "Total Sales");

    }
}
