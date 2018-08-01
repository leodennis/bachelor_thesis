import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to plot graphs.
 * Each graph can be given it's own color.
 *
 * @author Leo Knoll
 */
public class SimpleGraph extends JFrame {
    private final int FRAME_WIDTH = 1200;
    private final int FRAME_HEIGHT = 800;
    private Container drawable;
    private GraphCanvas canvas;

    /**
     * Initializes SimpleGraph object.
     * @param datasList A list of graphs, each graph is an array like [x,y, x,y, x,y ...].
     * @param colors The colors for each graph.
     */
    public SimpleGraph(List<double[]> datasList, List<Color> colors) {
        super("SimpleGraph");
        drawable = getContentPane();
        canvas = new GraphCanvas(datasList, colors);
        drawable.add(canvas);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setLocationRelativeTo(null);
    }

    /**
     * Specify the x-axis values.
     * @param xStart The value at the origin.
     * @param xInterval The interval between two marks.
     */
    public void setXMarkersByValue(double xStart, double xInterval) {
        canvas.adjustXMarkers(xStart, xInterval);
    }

    /**
     * Specify the y-axis values.
     * @param yStart The value at the origin.
     * @param yInterval The interval between two marks.
     */
    public void setYMarkersByValue(double yStart, double yInterval) {
        canvas.adjustYMarkers(yStart, yInterval);
    }


    public class GraphCanvas extends JPanel {
        private List<double[]> datasList;
        private List<double[]> xDatas = new ArrayList<>();
        private List<double[]> yDatas = new ArrayList<>();

        private List<Color> colors;

        private double xOffset = 0;
        private double yOffset = 0;
        private double zoomX = 1.0;
        private double zoomY = 1.0;

        private final int LEGEND_WIDTH = 100;

        private double xMarkerInterval = 50;
        private double yMarkerInterval = 50;

        public GraphCanvas(List<double[]> datasList, List<Color> colors) {
            super();
            this.datasList = datasList;
            this.colors = colors;

            for (double[] data : datasList) {
                double[] xData = new double[data.length/2];
                double[] yData = new double[data.length/2];
                for (int i = 0; i < data.length/2; i++) {
                    xData[i] = data[i * 2];
                    yData[i] = FRAME_HEIGHT - data[i * 2 + 1]; //y-axis inverted
                }
                xDatas.add(xData);
                yDatas.add(yData);
            }

            double minX = min(xDatas);
            double minY = min(yDatas);
            double maxX = max(xDatas);
            double maxY = max(yDatas);

            //determine zoom
            zoomX = (FRAME_WIDTH - LEGEND_WIDTH) / (maxX - minX);
            zoomY = (FRAME_HEIGHT - 100 - LEGEND_WIDTH) / (maxY - minY);

            xOffset = -1 * minX * zoomX + LEGEND_WIDTH;
            yOffset = -1 * minY * zoomY + LEGEND_WIDTH;
        }

        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;

            //activate smooth text
            g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);


            for (double i = LEGEND_WIDTH; i < FRAME_WIDTH; i+=xMarkerInterval) {
                g2.setColor(Color.LIGHT_GRAY);
                int x = (int) (i + 0.5);
                g2.drawLine(x,0, x, FRAME_HEIGHT - LEGEND_WIDTH);
                g2.setColor(Color.BLACK);
                drawMarkerX(g2, i, i == LEGEND_WIDTH);
            }
            for (double i = FRAME_HEIGHT - LEGEND_WIDTH; i > 0; i-=yMarkerInterval) {
                g2.setColor(Color.LIGHT_GRAY);
                int y = (int) (i + 0.5);
                g2.drawLine(LEGEND_WIDTH, y, FRAME_WIDTH, y);
                g2.setColor(Color.BLACK);
                drawMarkerY(g2, i, i == FRAME_HEIGHT - LEGEND_WIDTH);
            }

            g2.setColor(Color.BLACK);
            g2.drawLine(0,FRAME_HEIGHT - LEGEND_WIDTH, FRAME_WIDTH, FRAME_HEIGHT - LEGEND_WIDTH);
            g2.drawLine(LEGEND_WIDTH,0, LEGEND_WIDTH, FRAME_HEIGHT);

            //smooth lines
            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(1.5f));

            for (int p = 0; p < datasList.size(); p++) {
                g2.setColor(colors.get(p));
                double[] xData = xDatas.get(p);
                double[] yData = yDatas.get(p);
                for (int i = 0; i < xData.length - 1; i++) {
                    int x0 = xValueToCoordinate(xData[i]);
                    int x1 = xValueToCoordinate(xData[i + 1]);
                    int y0 = yValueToCoordinate(yData[i]);
                    int y1 = yValueToCoordinate(yData[i + 1]);
                    g2.drawLine(x0, y0, x1, y1);
                }
            }

        }

        private void drawMarkerX(Graphics2D g2, double x, boolean origin) {
            String number = get4DigitNumber(xCoordinateToValue(x));
            int width = g2.getFontMetrics().stringWidth(number);
            int xRounded = (int) (x + 0.5);
            g2.drawLine(xRounded, FRAME_HEIGHT-LEGEND_WIDTH - 5, xRounded, FRAME_HEIGHT-LEGEND_WIDTH + 5);
            if (origin) {
                g2.drawString(number, xRounded - width - 2, FRAME_HEIGHT-LEGEND_WIDTH + 20 + 10);
            } else {
                g2.drawString(number, xRounded - width/2, FRAME_HEIGHT-LEGEND_WIDTH + 20);
            }
        }

        private void drawMarkerY(Graphics2D g2, double y, boolean origin) {
            String number = get4DigitNumber(yCoordinateToValue(y));
            int width = g2.getFontMetrics().stringWidth(number);
            int yRounded = (int) (y + 0.5);
            g2.drawLine(LEGEND_WIDTH - 5, yRounded, LEGEND_WIDTH + 5, yRounded);
            if (origin) {
                g2.drawString(number, LEGEND_WIDTH - width - 10, yRounded + 17);
            } else {
                g2.drawString(number, LEGEND_WIDTH - width - 10, yRounded + 5);
            }
        }

        public int xValueToCoordinate(double value) {
            return (int) (xOffset + value*zoomX + 0.5);
        }

        public int yValueToCoordinate(double value) {
            return (int) (yOffset + value*zoomY + 0.5);
        }

        public double xCoordinateToValue(double x) {
            return (x-xOffset)/zoomX;
        }

        public double yCoordinateToValue(double y) {
            return FRAME_HEIGHT - (y-yOffset)/zoomY;
        }

        public void adjustXMarkers(double xStart, double xInterval) {
            xOffset = -1 * xStart * zoomX + LEGEND_WIDTH;
            xMarkerInterval = xInterval * zoomX;
        }

        public void adjustYMarkers(double yStart, double yInterval) {
            yOffset = FRAME_HEIGHT - LEGEND_WIDTH - (FRAME_HEIGHT - yStart)*zoomY;
            yMarkerInterval = yInterval * zoomY;
        }


        private String get4DigitNumber(double n) {
            //TODO: Round
            //DecimalFormat df = new DecimalFormat("#.###");
            //n = Double.valueOf(df.format(n));

            if (n >= 1000000000 || n <= -1000000000) {
                return "number too big to display";
            } else if (n >= 1000000 || n <= -1000000) { //XXXM or -XXXM
                return (int) (n / 1000000) + "M";
            } else if (n >= 10000 || n <= -10000) {  //XXXk or -XXXk
                return (int) (n / 1000) + "k";
            } else if (n >= 1000 || n <= -1000) {  //XXXX or -XXXX
                return String.valueOf((int) (n));
            } else if (n >= 0.01 || n <= -0.01) { //XXX.X till X.XXX or -X.XX till -XXX.
                String num = String.valueOf(n);
                if (n <= -100) {
                    return num.substring(0, 4);
                } else if (num.length() > 5) {
                    return num.substring(0, 5);
                } else {
                    return num;
                }
            } else if (n >= 0.001) { //X.XXX
                return "." + (int) (1000*n);
            } else { //0
                return "0";
            }
        }

        public double min(List<double[]> data) {
            double min = Double.MAX_VALUE;
            for (double[] d : data) {
                for (int i = 0; i < d.length; i++) {
                    if (d[i] < min) {
                        min = d[i];
                    }
                }
            }
            return min;
        }

        public double max(List<double[]> data) {
            double max = Double.MIN_VALUE;
            for (double[] d : data) {
                for (int i = 0; i < d.length; i++) {
                    if (d[i] > max) {
                        max = d[i];
                    }
                }
            }
            return max;
        }
    }

    public static void main(String[] args) {
        //Example
        double[] d = { 0.0, 0.0,
            30.0, 150.0,
            33.0, 145.0,
            36.0, 162.0,
            39.0, 128.0,
            48.0, 114.0,
            70.0, 240.0,
            81.0, 400.0,
            130.0, 450.0,
            230.0,  85.0,
            255.0,  30.0 };
        double[] d2 = {
            40.0, 200.0,
            55.0, 100.0,
            60.0, 100.0,
            80.0, 120.0,
            82.0, 190.0,
            112.0, 108.0,
            300.0, 250.0 };
        List<double[]> data = new ArrayList<>();
        data.add(d);
        data.add(d2);
        List<Color> colors = new ArrayList<>();
        colors.add(Color.RED);
        colors.add(Color.BLUE);
        SimpleGraph graph = new SimpleGraph(data, colors);
        graph.setVisible(true);
        graph.setXMarkersByValue(50, 50);
        graph.setYMarkersByValue(50, 50);
    }
}
