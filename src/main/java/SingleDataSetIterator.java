import com.google.common.collect.EvictingQueue;
import com.opencsv.CSVReader;
import javafx.util.Pair;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This DataSetIterator trains a specific model (or class)
 * @author Leo Knoll
 */
public class SingleDataSetIterator extends RebateDataSetIterator{

    // mini-batch-size offsets
    private LinkedList<Integer> exampleStartOffsets = new LinkedList<>();

    public SingleDataSetIterator(String dataFilePath, String delimiter, int inputDays, double splitRatio, String model, boolean addMissingDays, int skipFirstLines, boolean average) {
        super(dataFilePath, delimiter, inputDays, splitRatio, model, addMissingDays, skipFirstLines, average);
    }

    @Override
    void splitData(double splitRatio) {
        // Check if data-size is big enough
        if (allData.size() < inputDays) {
            System.err.println("Data set not big enough! Data set size: " + allData.size());
            System.exit(-1);
        }

        // split into train and test data
        int split = (int) Math.round(allData.size() * splitRatio);
        trainDataList = allData.subList(0, split);
        test = generateTestDataSet(allData.subList(split - (inputDays-1), allData.size()));
        System.out.println("Test data set size: " + test.size());
        System.out.println("Training data set size: " + trainDataList.size());
    }

    @Override
    void initializeTraining () {
        // initialize the mini-batch-size offsets
        exampleStartOffsets = new LinkedList<>();
        int window = inputDays + 1;
        for (int i = 0; i < trainDataList.size() - window; i++) { exampleStartOffsets.add(i); }
    }

    public List<Pair<INDArray, INDArray>> getTestDataSet() { return test; }

    @Override
    public DataSet next(int miniBatchSize) {
        if (exampleStartOffsets.size() == 0) throw new NoSuchElementException();
        int actualMiniBatchSize = Math.min(miniBatchSize, exampleStartOffsets.size());
        INDArray input = Nd4j.create(new int[] {actualMiniBatchSize, INPUT_VECTOR_SIZE, inputDays}, 'f');
        INDArray label = Nd4j.create(new int[] {actualMiniBatchSize, OUTPUT_VECTOR_SIZE, inputDays}, 'f');

        for (int index = 0; index < actualMiniBatchSize; index++) {
            int startIdx = exampleStartOffsets.removeFirst();
            int endIdx = startIdx + inputDays;
            RebateData curData = trainDataList.get(startIdx);

            for (int i = startIdx; i < endIdx; i++) {
                int c = i - startIdx;
                input.putScalar(new int[] {index, 0, c}, (curData.getName() - minArray[0]) / (maxArray[0] - minArray[0]));
                input.putScalar(new int[] {index, 1, c}, (curData.getYear() - minArray[1]) / (maxArray[1] - minArray[1]));
                input.putScalar(new int[] {index, 2, c}, (curData.getDate() - minArray[2]) / (maxArray[2] - minArray[2]));
                input.putScalar(new int[] {index, 3, c}, (curData.getRebate() - minArray[3]) / (maxArray[3] - minArray[3]));

                curData = trainDataList.get(i + 1);

                label.putScalar(new int[] {index, 0, c}, (curData.getSales() - minArray[4]) / (maxArray[4] - minArray[4]));
            }

            if (exampleStartOffsets.size() == 0) break;
        }
        return new DataSet(input, label);
    }


    @Override public boolean hasNext() { return exampleStartOffsets.size() > 0; }

    @Override public DataSet next() { return next(MINI_BATCH_SIZE); }

    @Override
    public int totalExamples() {
        return trainDataList.size();
    }

    @Override
    public void reset() {
        initializeTraining();
    }

    @Override
    public int batch() {
        return MINI_BATCH_SIZE;
    }

    protected List<Pair<INDArray, INDArray>> generateTestDataSet (List<RebateData> stockDataList) {
    	int window = inputDays + 1;
    	List<Pair<INDArray, INDArray>> test = new ArrayList<>();

        //EvictingQueue<Double> averages = EvictingQueue.create(inputDays);

        for (int i = 0; i < stockDataList.size() - window; i++) {
    		INDArray input = Nd4j.create(new int[] {inputDays, INPUT_VECTOR_SIZE}, 'f');
    		for (int j = i; j < i + inputDays; j++) {
    			RebateData stock = stockDataList.get(j);
    			input.putScalar(new int[] {j - i, 0}, (stock.getName() - minArray[0]) / (maxArray[0] - minArray[0]));
    			input.putScalar(new int[] {j - i, 1}, (stock.getYear() - minArray[1]) / (maxArray[1] - minArray[1]));
    			input.putScalar(new int[] {j - i, 2}, (stock.getDate() - minArray[2]) / (maxArray[2] - minArray[2]));
    			input.putScalar(new int[] {j - i, 3}, (stock.getRebate() - minArray[3]) / (maxArray[3] - minArray[3]));
    		}
            RebateData stock = stockDataList.get(i + inputDays);
            INDArray label;
            label = Nd4j.create(new int[]{OUTPUT_VECTOR_SIZE}, 'f'); // ordering is set as 'f', faster construct
            label.putScalar(new int[] {0}, stock.getSales());

    		test.add(new Pair<>(input, label));
    	}
    	return test;
    }

    @Override
    List<RebateData> readRebateDataFromFile(String filename, String delimiter, String model, boolean addMissingDays, int skipFirstLines) {

        List<RebateData> rebateDataList = new ArrayList<>();
        try {
            for (int i = 0; i < maxArray.length; i++) { // initialize max and min arrays
                maxArray[i] = Double.MIN_VALUE;
                minArray[i] = Double.MAX_VALUE;
            }

            // String replacer replaces each string with a unique number
            StringReplacer modelReplacer = new StringReplacer(INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE,
                new int[] {StringReplacer.TYPE_STRING, StringReplacer.TYPE_NUMBER, StringReplacer.TYPE_NUMBER, StringReplacer.TYPE_NUMBER, StringReplacer.TYPE_NUMBER});

            List<String[]> list = new CSVReader(new FileReader(filename), ';').readAll(); // load all elements in a list
            RebateData lastData = null;
            for (String[] arr : list) {
                if (!arr[INDEX_MODEL].equals(model)) continue;

                String[] replacedData = modelReplacer.replace(arr);   // replace Strings
                replacedData = modelReplacer.fillNullOrEmpty(replacedData, "0", true); //replace empty number fields

                double[] nums = new double[INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE];
                for (int i = 0; i < replacedData.length; i++) {
                    nums[i] = Double.valueOf(replacedData[i]);
                    if (i == INDEX_DATE) {
                        nums[i] /= 1000;
                    }
                    if (nums[i] > maxArray[i]) maxArray[i] = nums[i];
                    if (nums[i] < minArray[i]) minArray[i] = nums[i];
                }

                if (lastData != null) {
                    // if there are two rebates in one day
                    if (lastData.getName() == Integer.valueOf(replacedData[INDEX_MODEL]) && lastData.getDate() == Long.valueOf(replacedData[INDEX_DATE])/1000) {
                        lastData.setRebate((lastData.getRebate() + Double.valueOf(replacedData[INDEX_REBATE])) / 2);    // average
                        lastData.setSales(lastData.getSales() + Integer.valueOf(replacedData[INDEX_SALES]));            // add sales
                        continue;
                    }
                }

                lastData = new RebateData(Integer.valueOf(replacedData[INDEX_MODEL]), Integer.valueOf(replacedData[INDEX_YEAR]), Long.valueOf(replacedData[INDEX_DATE])/1000,
                    Double.valueOf(replacedData[INDEX_REBATE]), Integer.valueOf(replacedData[INDEX_SALES]));

                rebateDataList.add(lastData);
            }

            if (addMissingDays) {
                int missedDates = 0;
                List<RebateData> missingDates = new ArrayList<>();
                long minDate = (long) minArray[2 - 1];
                long maxDate = (long) maxArray[2 - 1];
                final int UNIX_TIMESTAMP_DAY = 24*60*60; // 86400 sec.
                for (RebateData data : rebateDataList) {
                    while (minDate + UNIX_TIMESTAMP_DAY <= data.getDate()) {
                        missingDates.add(new RebateData(data.getName(), data.getYear(), minDate, 0, 0));
                        minArray[INDEX_SALES] = 0;
                        minDate += UNIX_TIMESTAMP_DAY;
                        missedDates++;
                    }
                    minDate = data.getDate() + UNIX_TIMESTAMP_DAY;
                }
                RebateData data = rebateDataList.get(rebateDataList.size()-1);
                while (data.getDate() + UNIX_TIMESTAMP_DAY <= maxDate) {
                    data = new RebateData(data.getName(), data.getYear(), data.getDate() + UNIX_TIMESTAMP_DAY, 0, 0);
                    missingDates.add(data);
                    missedDates++;
                }
                // add and sort
                rebateDataList.addAll(missingDates);
                missingDates.clear();
                Collections.sort(rebateDataList, new Comparator<RebateData>() {
                    public int compare(RebateData d1, RebateData d2) {
                        return (int) (d1.getDate() - d2.getDate());
                    }
                });

                System.out.println("Added " + missedDates + " missed dates!");

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // important otherwise training impossible because of division by zero
        for (int i = 0; i < minArray.length; i++) {
            if (minArray[i] == maxArray[i]) {
                maxArray[i] = 0;    // also possible minArray[i] = 0
            }
        }

        return rebateDataList;
    }

    @Override
    public int inputColumns() {
        return super.inputColumns();
    }

    // ---------------------------------------------- Testing ---------------------------------------------------------

    @Override
    void testPrediction(MultiLayerNetwork net, boolean average, boolean showResult) {
        INDArray[][] predicts = new INDArray[test.size()][inputDays];
        INDArray[] actuals = new INDArray[test.size()];

        INDArray min = Nd4j.create(getMinArray());
        INDArray max = Nd4j.create(getMaxArray());

        CustomAverager avg = new CustomAverager(inputDays);

        for (int i = 0; i < test.size(); i++) {
            for (int j = 0; j < inputDays; j++) {
                predicts[i][j] = net.rnnTimeStep(test.get(i).getKey()).getRow(j).mul(max.sub(min)).add(min);
            }
            actuals[i] = test.get(i).getValue();
        }

        if (showResult) {
            log.info("Print out Predictions and Actual Values...");
            log.info("Predict\tActual");
            for (int i = 0; i < predicts.length; i++) log.info(predicts[i] + "\t" + actuals[i]);

            log.info("Plot...");
            double[] pred = new double[predicts.length];
            double[] actu = new double[actuals.length];
            if (average) {
                int offsetCounter = 0;
                for (int i = 0; i < predicts.length + inputDays-1; i++) { // -1 ????
                    if (i < predicts.length) {
                        for (int j = 0; j < inputDays; j++) {
                            avg.addDay(j, predicts[i][j].getDouble(0));
                        }
                    }
                    if (offsetCounter == inputDays-1) {
                        pred[i - offsetCounter] = avg.getAverage();
                    } else {
                        offsetCounter++;
                    }
                    avg.nextIteration();
                    if (i < predicts.length) {
                        actu[i] = actuals[i].getDouble(0);
                    }
                }
            } else {
                for (int i = 0; i < predicts.length; i++) {
                    pred[i] = predicts[i][inputDays - 1].getDouble(0);
                    actu[i] = actuals[i].getDouble(0);
                }
            }
            log.info("Showing Plot!");
            PlotUtil.plot(pred, actu, "Total Sales" + ((average)? " - AVG" : ""), "Test Results");
        }
    }
}
