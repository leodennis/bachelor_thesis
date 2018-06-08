import com.google.common.collect.EvictingQueue;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.stream.Stream;

class NADIN_Prediction_View extends JFrame implements ActionListener, DocumentListener, ProgressBarView {

    public static final String DELIMITER = ";";
    public static final int FIRST_LINES_TO_SKIP = 1;

    private boolean canceled = false;

    // GUI elements
    private JLabel lblInput = new JLabel("Select input file (CSV-file with ';' as column delimiter and '.' as comma delimiter)");
    private JTextField txtInput = new JTextField();
    private JButton butBrowseInput = new JButton("Browse");
    private JLabel lblEpochs = new JLabel("Training epochs:");
    private JTextField txtEpochs = new JTextField("100");
    private JLabel lblPercent = new JLabel("Training-Testing ratio (0.9 = 90% training, 10% testing):");
    private JTextField txtPercent = new JTextField("0.9");
    private JLabel lblDays = new JLabel("Number of days used to predict next value:");
    private JTextField txtDays = new JTextField("22");
    private JCheckBox cbTrain = new JCheckBox("Train network", true);
    private JCheckBox cbFill = new JCheckBox("Add missing days", false);
    private JLabel lblModel = new JLabel("Select the model you want the prediction for (select input file first):");
    private JComboBox<String> comboModel = new JComboBox<>();
    private JLabel lblIterator = new JLabel("Train network on:");
    private JComboBox<String> comboIterator = new JComboBox<>(new String[]{"Only selected model", "Only model distributed", "Complete dataset"});
    private JLabel lblSave = new JLabel("Location where neural network will be saved:");
    private JTextField txtSave = new JTextField();
    private JButton butBrowseSave = new JButton("Browse");
    private JButton butCancel = new JButton("Cancel");
    private JButton butStart = new JButton("Start");
    private JLabel lblProgress = new JLabel(" ");
    private JProgressBar progressBar = new JProgressBar(0, 100);

    public NADIN_Prediction_View() {
        super.setTitle("NADIN Rebate Prediction Tool");
        super.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        super.setSize(500, 480);
        super.setLocationRelativeTo(null);

        initElements();
    }

    private void initElements() {

        // add listeners
        butBrowseInput.addActionListener(this);
        butBrowseSave.addActionListener(this);
        butStart.addActionListener(this);
        butCancel.addActionListener(this);
        txtInput.getDocument().addDocumentListener(this);

        // position GUI elements
        setPosition(lblInput, 0,0,4,1,1,0);
        setPosition(txtInput, 0,1,3,1,1,0);
        setPosition(butBrowseInput, 3,1,1,1,0,0);
        setPosition(lblEpochs, 0,2,4,1,1,0);
        setPosition(txtEpochs, 0,3,4,1,1,0);
        setPosition(lblPercent, 0,4,4,1,1,0);
        setPosition(txtPercent, 0,5,4,1,1,0);
        setPosition(lblDays, 0,6,4,1,1,0);
        setPosition(txtDays, 0,7,4,1,1,0);
        setPosition(cbTrain, 0,8,4,1,1,0);
        setPosition(cbFill, 0,9,4,1,1,0);
        setPosition(lblModel, 0,10,4,1,1,0);
        setPosition(comboModel, 0,11,4,1,1,0);
        setPosition(lblIterator, 0,12,4,1,1,0);
        setPosition(comboIterator, 0,13,4,1,1,0);
        setPosition(lblSave, 0,14,4,1,1,0);
        setPosition(txtSave, 0,15,3,1,1,0);
        setPosition(butBrowseSave, 3,15,1,1,0,0);
        setPosition(butCancel, 0,16,1,1,0,0);
        setPosition(butStart, 3,16,1,1,0,0);
        setPosition(lblProgress, 0,17,4,1,1,0);
        setPosition(progressBar, 0,18,4,1,1,0);
    }

    /**
     * Updates models combo box based on file read at input path
     */
    private void updateModels() {
        try {
            String path = txtInput.getText();
            if (new File(path).exists()) {
                Stream<String> stream = Files.lines(Paths.get(path));
                String[] choices = stream.map(e -> e.split(DELIMITER)[0]).distinct().sorted().toArray(String[]::new);
                comboModel.removeAllItems();
                if (choices != null) {
                    for (String model : choices) {
                        comboModel.addItem(model);
                    }
                }
                return;
            }
        } catch (Exception e) {
            // do not display error message
        }
        comboModel.removeAllItems();
    }

    private String getNetworkName() {
        StringBuilder name = new StringBuilder();
        if (comboModel.getSelectedItem() != null) {
            name.append(comboModel.getSelectedItem()).append("_");

            if (comboIterator.getSelectedIndex() == 0) {
                name.append("T-MODEL").append("_");
            } else if (comboIterator.getSelectedIndex() == 1) {
                name.append("T-DIST").append("_");
            } else {
                name.append("T-ALL").append("_");
            }

            name.append(txtEpochs.getText()).append("-EPOCHS").append("_");

            if (cbFill.isSelected()) {
                name.append("adding").append("_");
            } else {
                name.append("no-adding").append("_");
            }

            name.append(txtPercent.getText()).append("_");
            name.append(txtDays.getText());
        }
        return name.toString();
    }

    public boolean updateProgress(int progressPercentage, long timeLeft) {
        progressBar.setValue(progressPercentage);
        lblProgress.setText("Estimated finish in: " + getTimeAsString(timeLeft));
        return canceled;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(butBrowseInput)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select input file");
            int success = chooser.showOpenDialog(null);
            if (success == JFileChooser.APPROVE_OPTION) {
                txtInput.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        } else if (e.getSource().equals(butBrowseInput)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select input file");
            int success = chooser.showOpenDialog(null);
            if (success == JFileChooser.APPROVE_OPTION) {
                txtInput.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }  else if (e.getSource().equals(butBrowseSave)) {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select location to save network");
            if (cbTrain.isSelected()) {
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            }
            int success = chooser.showOpenDialog(null);
            if (success == JFileChooser.APPROVE_OPTION) {
                txtSave.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        } else if (e.getSource().equals(butStart)) {
            // check inputs
            // TODO: Check with Preconditions.checkArgument(expression whichg must be true, error string, printf style objects);
            if (txtInput.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,"No input file selected!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if ( ! new File(txtInput.getText()).exists()) {
                JOptionPane.showMessageDialog(this, "Input file does not exists!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (txtSave.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Save file not selected!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (comboModel.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Model not selected!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (txtDays.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter the number of days as input for predicting the next day!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if ( ! txtDays.getText().matches("\\d+")) {
                JOptionPane.showMessageDialog(this, "Please enter a positive integer number for the number of days!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } else if (Integer.valueOf(txtDays.getText()) < 1) {
                JOptionPane.showMessageDialog(this, "The number days must be > 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (cbTrain.isSelected()) {
                if (txtEpochs.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter the number of epochs for training!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else if ( ! txtEpochs.getText().matches("\\d+")) {
                    JOptionPane.showMessageDialog(this, "Please enter a positive integer number as training epochs!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else if (Integer.valueOf(txtEpochs.getText()) <= 0) {
                    JOptionPane.showMessageDialog(this, "The training epochs must be > 0", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (txtPercent.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter the Training-Testing ratio!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else if ( ! txtPercent.getText().matches("\\d+(\\.\\d+)?")) {
                    JOptionPane.showMessageDialog(this, "Training-Testing ratio has wrong input format", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } else if (Double.valueOf(txtPercent.getText()) <= 0 || Double.valueOf(txtPercent.getText()) > 1) {
                    JOptionPane.showMessageDialog(this, "Training-Testing ratio must be > 0 and <= 1", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } else {
                if ( ! new File(txtSave.getText()).exists()) {
                    JOptionPane.showMessageDialog(this, "Could not load network, check saved location!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // disable all but cancel button
            for (Component cp : this.getComponents() ){
                if (cp != butCancel && cp != progressBar) {
                    cp.setEnabled(false);
                }
            }

            new TrainAndTestNetwork(this).execute();

        } else if (e.getSource().equals(butCancel)) {
            canceled = true;
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }

    public void insertUpdate(DocumentEvent e) {
        updateModels();
    }
    public void removeUpdate(DocumentEvent e) {
    }
    public void changedUpdate(DocumentEvent e) {
    }

    public String getTimeAsString(long different){
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long dayDiff = different / daysInMilli;
        different = different % daysInMilli;

        long hDiff = different / hoursInMilli;
        different = different % hoursInMilli;

        long minDiff = different / minutesInMilli;
        different = different % minutesInMilli;

        long secDiff = different / secondsInMilli;

        return dayDiff + " days, " + hDiff + " h, " + minDiff + " min, " + secDiff + " sec";
    }

    // ------------------------------------------- GUI position functions ------------------------------------------ //

    private void setPosition(JComponent component, int gridx, int gridy, int gridwidth,
                             int gridheight, int weightx, int weighty) {
        // place(posGridX,posGidY,GridAnzahlX,GridAnzahlY,WeightX,WeightY);

        if(this.getContentPane().getLayout() == null || !(this.getContentPane().getLayout() instanceof GridBagLayout)) {
            this.setLayout(new GridBagLayout());
        }

        GridBagConstraints gbc = place(gridx, gridy, gridwidth, gridheight, weightx, weighty);
        ((GridBagLayout) this.getContentPane().getLayout()).setConstraints(component, gbc);
        this.getContentPane().add(component);
    }

    private GridBagConstraints place(int gridx, int gridy, int gridwidth,
                                     int gridheight, int weightx, int weighty) {
        // determine positions and dimensions
        GridBagConstraints gbc = new GridBagConstraints(); // take a
        // GridBagConstraints
        // manager object
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        gbc.gridwidth = gridwidth;
        gbc.gridheight = gridheight;
        gbc.insets = new Insets(1, 1, 1, 1);
        gbc.weightx = weightx;
        gbc.weighty = weighty;
        gbc.fill = GridBagConstraints.BOTH;
        return gbc;
    }


    // --------------------------------------- Train and Test worker class------------------------------------------ //

    class TrainAndTestNetwork extends SwingWorker<Void, Void> {

        private Logger log = LoggerFactory.getLogger(TrainAndTestNetwork.class);
        private ProgressBarView listener;

        TrainAndTestNetwork(ProgressBarView listener) {
            this.listener = listener;
        }


        @Override
        protected Void doInBackground() throws IOException{
            String inputPath = txtInput.getText();
            int epochs = Integer.valueOf(txtEpochs.getText());
            double splitRatio = Double.valueOf(txtPercent.getText());
            int inputDays = Integer.valueOf(txtDays.getText());
            boolean addMissingDays = cbFill.isSelected();
            String selectedModel = comboModel.getSelectedItem().toString();
            File locationToSave = new File(txtSave.getText());

            log.info("Selected model: " + selectedModel);

            log.info("Create dataSet iterator...");
            RebateDataSetIterator iterator;
            if (comboIterator.getSelectedIndex() == 0) {
                iterator = new SingleDataSetIterator(inputPath, DELIMITER,inputDays, splitRatio, selectedModel, addMissingDays,FIRST_LINES_TO_SKIP);
            } else if(comboIterator.getSelectedIndex() == 1) {
                iterator = new DistributedDataSetIterator(inputPath, DELIMITER,inputDays, splitRatio, selectedModel, addMissingDays,FIRST_LINES_TO_SKIP);
            } else {
                iterator = new SeparatedDataSetIterator(inputPath, DELIMITER, splitRatio, selectedModel, addMissingDays,
                    FIRST_LINES_TO_SKIP);
            }

            MultiLayerNetwork net;

            if (cbTrain.isSelected()) {
                log.info("Build lstm networks...");
                net = RecurrentNets.buildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes(), iterator.getInputDays());

                log.info("Training...");
                log.info("Using " + iterator.getClass().getName() + " to train " + epochs + " epochs!");

                // TODO: Set calculated finishing time here

                long lastEpochFinishing = Calendar.getInstance().getTimeInMillis();
                EvictingQueue<Double> lastFinishedEpochs = EvictingQueue.create(10);

                for (int i = 0; i < epochs; i++) {
                    System.out.println("Epoch: " + i);
                    while (iterator.hasNext()) {
                        if (iterator.needsReset()) {
                            iterator.reset();
                            net.rnnClearPreviousState(); // clear previous state
                        }
                        net.fit(iterator.next());   // fit model using mini-batch data
                    }

                    iterator.reset(); // reset iterator
                    net.rnnClearPreviousState(); // clear previous state

                    // calculate time for epoch to finish
                    double diff = Calendar.getInstance().getTimeInMillis() - lastEpochFinishing;
                    lastFinishedEpochs.add(diff);
                    // average with last 10 results
                    diff = lastFinishedEpochs.stream().mapToDouble(e -> e).average().orElse(0.0);
                    // compute estimated finish time
                    long completion_in = (long) diff *(epochs - i);

                    if (listener.updateProgress((int) i*100/epochs, completion_in)) {
                        return null; // cancel
                    }
                    lastEpochFinishing = Calendar.getInstance().getTimeInMillis();
                }

                locationToSave = new File(txtSave.getText() + File.separator + getNetworkName() + ".zip");

                log.info("Saving model...");
                log.info("At: " + locationToSave);
                // saveUpdater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this to train your network more in the future
                ModelSerializer.writeModel(net, locationToSave, true);

            }

            log.info("Load model...");
            net = ModelSerializer.restoreMultiLayerNetwork(locationToSave);

            log.info("Testing...");
            iterator.testPrediction(net);

            listener.updateProgress(100, 0);

            log.info("Done...");

            return null;
        }
    }

    // -------------------------------------------------- main ----------------------------------------------------- //


    public static void main(String[] args){
        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // did not work, ignore
        }

        new NADIN_Prediction_View().setVisible(true);
    }

}



