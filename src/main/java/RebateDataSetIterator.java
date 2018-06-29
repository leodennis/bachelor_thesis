import javafx.util.Pair;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract Class to read in data for best rebate prediction. Provides test and training data.
 * @author Leo Knoll
 */
public abstract class RebateDataSetIterator implements DataSetIterator {

    protected static Logger log = LoggerFactory.getLogger(RebateDataSetIterator.class);

    // number of inputs (features) and outputs (labels) for each rebate data
    protected final int INPUT_VECTOR_SIZE = 4;
    protected final int OUTPUT_VECTOR_SIZE = 1;
    protected final int INDEX_MODEL = 0;
    protected final int INDEX_YEAR = 1;
    protected final int INDEX_DATE = 2;
    protected final int INDEX_REBATE = 3;
    protected final int INDEX_SALES = 4;

    // data lists
    protected List<RebateData> allData;
    protected  List<RebateData> trainDataList = new ArrayList<>();
    protected  List<RebateData> testDataList = new ArrayList<>();

    // dataset for training and testing
    protected List<Pair<INDArray, INDArray>> test;

    // filter for specific model (name will be converted into int)
    protected Integer filterID = null;

    protected final int MINI_BATCH_SIZE = 64;

    protected boolean finished = false;

    protected boolean needsReset = false;

    protected int inputDays;

    protected boolean average;

    /** minimal values of each feature in stock dataset */
    protected double[] minArray = new double[INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE];
    /** maximal values of each feature in stock dataset */
    protected double[] maxArray = new double[INPUT_VECTOR_SIZE + OUTPUT_VECTOR_SIZE];

    /**
     * Initialize new DataSetIterator object.
     * Reads in data and prepares test and train data.
     * @param dataFilePath the path to the data containing the data
     * @param delimiter the delimiter used to separate columns in the data
     * @param inputDays the number of days used to predict the next day
     * @param splitRatio the percent of data used for training
     * @param model the model we want to test
     * @param addMissingDays if missing dates should be added with zeros
     * @param skipFirstLines the number of lines which have to be skipped at the beginning
     */
    public RebateDataSetIterator(String dataFilePath, String delimiter, int inputDays, double splitRatio, String model, boolean addMissingDays, int skipFirstLines, boolean average) {
        Objects.requireNonNull(dataFilePath, "Path to data cannot be null!");
        Objects.requireNonNull(delimiter, "Delimiter cannot be null!");
        Objects.requireNonNull(model, "Model is null, you must select a model or class for testing!");

        this.inputDays = inputDays;
        this.average = average;

        // read all input data
        allData = readRebateDataFromFile(dataFilePath, delimiter, model, addMissingDays, skipFirstLines);

        // split data in train and test data
        splitData(splitRatio);

        // initialize training
        initializeTraining();
    }

    /**
     * Splits data into training and test data.
     * @param splitRatio the percentage of data to be used for training
     */
    abstract void splitData(double splitRatio);

    /**
     * Here the training process can be initialized. Eg. initializing mini-batches offsets.
     */
    abstract void initializeTraining();

    protected double[] getMaxArray() {
        double[] arr = new double[1];
        arr[0] = maxArray[INDEX_SALES];
        return arr;
    }

    protected double[] getMinArray() {
        double[] arr = new double[1];
        arr[0] = minArray[INDEX_SALES];
        return arr;
    }

    /**
     * Returns the generated test data.
     * @return the data for testing.
     */
    protected List<Pair<INDArray, INDArray>> getTestDataSet() { return test; }

    @Override public int inputColumns() { return INPUT_VECTOR_SIZE; }

    @Override public int totalOutcomes() {
        return OUTPUT_VECTOR_SIZE;
    }

    @Override public boolean resetSupported() { return true; }

    @Override public boolean asyncSupported() { return false; }

    @Override public int cursor() { return totalExamples();}

    @Override public int numExamples() { return totalExamples(); }

    @Override public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override public DataSetPreProcessor getPreProcessor() { throw new UnsupportedOperationException("Not Implemented"); }

    @Override public List<String> getLabels() { throw new UnsupportedOperationException("Not Implemented"); }

    @Override public boolean hasNext() { return finished == false; }

    public int getInputDays() {
        return inputDays;
    }

    public boolean needsReset() {
        return needsReset;
    }

    /**
     * Reads in all data and saves it in a list.
     * Also converts strings to numbers.
     * @param filename the path to the file containing the data
     * @param delimiter the delimiter used to separate columns in the data
     * @param model  the model we want to test, saves the associated number in filterID
     * @param addMissingDays if missing dates should be added with zeros
     * @param skipFirstLines the number of lines which have to be skipped at the beginning
     * @return all data as list
     */
    abstract List<RebateData> readRebateDataFromFile(String filename, String delimiter, String model, boolean addMissingDays, int skipFirstLines);


    // ---------------------------------------------- Testing ---------------------------------------------------------

    abstract void testPrediction(MultiLayerNetwork net);
}
