import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Class representing the neural network.
 */
public class RecurrentNets {

    private static final double learningRate = 0.05;
    private static final int iterations = 1;
    private static final int seed = 12345; //TODO: make random

    private static final int lstmLayer1Size = 256;
    private static final int lstmLayer2Size = 256;
    private static final int denseLayerSize = 32;
    private static final double dropoutRatio = 0.2;

    public static MultiLayerNetwork buildLstmNetworks(int nIn, int nOut, int truncatedBPTTLength) {
        MultiLayerConfiguration conf;
        NeuralNetConfiguration.ListBuilder lb = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .iterations(iterations)
            .learningRate(learningRate)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .weightInit(WeightInit.XAVIER)
            .updater(Updater.RMSPROP)
            .regularization(true)
            .l2(1e-4)
            .list()
            .layer(0, new GravesLSTM.Builder()
                .nIn(nIn)
                .nOut(lstmLayer1Size)
                .activation(Activation.TANH)
                .gateActivationFunction(Activation.HARDSIGMOID)
                .dropOut(dropoutRatio)
                .build())
            .layer(1, new GravesLSTM.Builder()
                .nIn(lstmLayer1Size)
                .nOut(lstmLayer2Size)
                .activation(Activation.TANH)
                .gateActivationFunction(Activation.HARDSIGMOID)
                .dropOut(dropoutRatio)
                .build())
            .layer(2, new DenseLayer.Builder()
                .nIn(lstmLayer2Size)
                .nOut(denseLayerSize)
                .activation(Activation.RELU)
                .build())
            .layer(3, new RnnOutputLayer.Builder()
                .nIn(denseLayerSize)
                .nOut(nOut)
                .activation(Activation.IDENTITY)
                .lossFunction(LossFunctions.LossFunction.MSE)
                .build());

        if (truncatedBPTTLength > 1) {
            lb.backpropType(BackpropType.TruncatedBPTT)
                .tBPTTForwardLength(truncatedBPTTLength)
                .tBPTTBackwardLength(truncatedBPTTLength);
        }

        conf = lb.pretrain(false)
            .backprop(true)
            .build();


        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(1));
        return net;
    }
}
