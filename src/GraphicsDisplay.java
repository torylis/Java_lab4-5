import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JPanel;

public class GraphicsDisplay extends JPanel {
    private ArrayList<Double[]> graphicsData1;
    private ArrayList<Double[]> graphicsData2;
    private ArrayList<Double[]> originalData1;
    private ArrayList<Double[]> originalData2;
    private int selectedMarker = -1;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double[][] viewport = new double[2][2];
    private final ArrayList<double[][]> undoHistory = new ArrayList(5);
    private double scaleX;
    private double scaleY;
    private boolean showAxis = true;
    private boolean showMarkers = true;
    private boolean rotate = false;
    private final BasicStroke axisStroke;
    private final BasicStroke gridStroke;
    private BasicStroke markerStroke;
    private final BasicStroke selectionStroke;
    private final Font axisFont;
    private final Font labelsFont;
    private static final DecimalFormat formatter = (DecimalFormat)NumberFormat.getInstance();
    private boolean scaleMode = false;
    private boolean changeMode = false;
    private double[] originalPoint = new double[2];
    private final Rectangle2D.Double selectionRect = new Rectangle2D.Double();

    public GraphicsDisplay() {
        this.setBackground(Color.WHITE);
        this.axisStroke = new BasicStroke(2.0F, 0, 0, 10.0F, (float[])null, 0.0F);
        this.gridStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[]{4.0F, 4.0F}, 0.0F);
        this.markerStroke = new BasicStroke(1.0F, 0, 0, 10.0F, (float[])null, 0.0F);
        this.selectionStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[]{10.0F, 10.0F}, 0.0F);
        this.axisFont = new Font("Serif", 1, 36);
        this.labelsFont = new Font("Serif", 0, 10);
        formatter.setMaximumFractionDigits(5);
        this.addMouseListener(new MouseHandler());
        this.addMouseMotionListener(new MouseMotionHandler());
    }

    public void displayGraphics(ArrayList<Double[]> graphicsData1, ArrayList<Double[]> graphicsData2) {
        Iterator var3 = null;
        Iterator var4 = null;

        if (graphicsData1 != null && graphicsData1.size() != 0) {
            this.graphicsData1 = graphicsData1;
            this.originalData1 = new ArrayList(graphicsData1.size());
            var3 = graphicsData1.iterator();
            while(var3.hasNext()) {
                Double[] point = (Double[])var3.next();
                Double[] newPoint = new Double[]{point[0], point[1]};
                this.originalData1.add(newPoint);
            }
            this.minX = ((Double[])graphicsData1.get(0))[0];
            this.maxX = ((Double[])graphicsData1.get(graphicsData1.size() - 1))[0];
            this.minY = ((Double[])graphicsData1.get(0))[1];
            this.maxY = this.minY;
            for(int i = 1; i < graphicsData1.size(); ++i) {
                if (((Double[])graphicsData1.get(i))[1] < this.minY) {
                    this.minY = ((Double[])graphicsData1.get(i))[1];
                }

                if (((Double[])graphicsData1.get(i))[1] > this.maxY) {
                    this.maxY = ((Double[])graphicsData1.get(i))[1];
                }
            }
        }

        if (graphicsData2 != null && graphicsData2.size() != 0) {
            this.graphicsData2 = graphicsData2;
            this.originalData2 = new ArrayList(graphicsData2.size());
            var4 = graphicsData2.iterator();
            while(var4.hasNext()) {
                Double[] point = (Double[])var4.next();
                Double[] newPoint = new Double[]{point[0], point[1]};
                this.originalData2.add(newPoint);
            }
            if (((Double[])graphicsData2.get(0))[0] < this.minX) {
                this.minX = ((Double[])graphicsData2.get(0))[0];
            }

            if (((Double[])graphicsData2.get(graphicsData2.size() - 1))[0] > this.maxX) {
                this.maxX = ((Double[])graphicsData2.get(graphicsData2.size() - 1))[0];
            }
            for(int i = 1; i < graphicsData2.size(); ++i) {
                if (((Double[])graphicsData2.get(i))[1] < this.minY) {
                    this.minY = ((Double[])graphicsData2.get(i))[1];
                }

                if (((Double[])graphicsData2.get(i))[1] > this.maxY) {
                    this.maxY = ((Double[])graphicsData2.get(i))[1];
                }
            }
        }

        this.zoomToRegion(this.minX, this.maxY, this.maxX, this.minY);
    }

    public void zoomToRegion(double x1, double y1, double x2, double y2) {
        this.viewport[0][0] = x1;
        this.viewport[0][1] = y1;
        this.viewport[1][0] = x2;
        this.viewport[1][1] = y2;
        this.repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (rotate) {
            this.scaleX = this.getSize().getHeight() / (this.viewport[1][0] - this.viewport[0][0]); // меняем ось X
            this.scaleY = this.getSize().getWidth() / (this.viewport[0][1] - this.viewport[1][1]);  // меняем ось Y
        } else {
            this.scaleX = this.getSize().getWidth() / (this.viewport[1][0] - this.viewport[0][0]);
            this.scaleY = this.getSize().getHeight() / (this.viewport[0][1] - this.viewport[1][1]);
        }

        if (this.graphicsData1 != null && this.graphicsData1.size() != 0 || this.graphicsData2 != null && this.graphicsData2.size() != 0) {
            Graphics2D canvas = (Graphics2D)g;
            if (rotate) {
                // Осуществляем поворот на 90 градусов влево, относительно центра
                canvas.translate(this.getWidth() / 2, this.getHeight() / 2);  // Перемещаем систему координат в центр
                canvas.rotate(Math.toRadians(-90)); // Поворот на 90 градусов против часовой стрелки
                canvas.translate(-this.getHeight() / 2, -this.getWidth() / 2);  // Сдвигаем обратно
            }
            this.paintGrid(canvas);
            if (showAxis) paintAxis(canvas);
            this.paintLabels(canvas);
            this.paintSelection(canvas);
            if (this.graphicsData1 != null && this.graphicsData1.size() != 0) {
                this.paintGraphics1(canvas);
                if (showMarkers) paintMarkers1(canvas);
            }
            if (this.graphicsData2 != null && this.graphicsData2.size() != 0) {
                this.paintGraphics2(canvas);
                if (showMarkers) paintMarkers2(canvas);
            }
        }
    }

    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    public void setRotate(boolean rotate) {
        this.rotate = rotate;
        repaint();
    }

    private void paintSelection(Graphics2D canvas) {
        if (this.scaleMode) {
            canvas.setStroke(this.selectionStroke);
            canvas.setColor(Color.BLACK);
            canvas.draw(this.selectionRect);
        }
    }

    private void paintGraphics1(Graphics2D canvas) {
        BasicStroke lineStroke = new BasicStroke(2.0F, 0, 0, 10.0F, new float[]
                {10.0F, 2.0F, 10.0F, 2.0F, 10.0F, 2.0F, 2.0F, 2.0F, 2.0F, 2.0F, 2.0F, 2.0F}, 0.0F);
        canvas.setStroke(lineStroke);
        canvas.setColor(Color.RED);
        Double currentX = null;
        Double currentY = null;
        Iterator var5 = this.graphicsData1.iterator();

        while(var5.hasNext()) {
            Double[] point = (Double[])var5.next();
            if (!(point[0] < this.viewport[0][0]) && !(point[1] > this.viewport[0][1]) && !(point[0] > this.viewport[1][0]) && !(point[1] < this.viewport[1][1])) {
                if (currentX != null && currentY != null) {
                    canvas.draw(new Line2D.Double(this.translateXYtoPoint(currentX, currentY), this.translateXYtoPoint(point[0], point[1])));
                }

                currentX = point[0];
                currentY = point[1];
            }
        }

    }

    private void paintGraphics2(Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.CYAN);
        Double currentX = null;
        Double currentY = null;
        Iterator var5 = this.graphicsData2.iterator();

        while(var5.hasNext()) {
            Double[] point = (Double[])var5.next();
            if (!(point[0] < this.viewport[0][0]) && !(point[1] > this.viewport[0][1]) && !(point[0] > this.viewport[1][0]) && !(point[1] < this.viewport[1][1])) {
                if (currentX != null && currentY != null) {
                    canvas.draw(new Line2D.Double(this.translateXYtoPoint(currentX, currentY), this.translateXYtoPoint(point[0], point[1])));
                }

                currentX = point[0];
                currentY = point[1];
            }
        }

    }

    private void paintMarkers1(Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.RED);
        canvas.setPaint(Color.RED);
        int i = -1;
        Iterator var5 = this.graphicsData1.iterator();

        while(var5.hasNext()) {
            Double[] point = (Double[])var5.next();
            ++i;
            if (!(point[0] < this.viewport[0][0]) && !(point[1] > this.viewport[0][1]) && !(point[0] > this.viewport[1][0]) && !(point[1] < this.viewport[1][1])) {
                byte size = 5;

                Point2D center = this.translateXYtoPoint(point[0], point[1]);

                double ydValue = point[1];
                int yValue = (int) ydValue;
                int sumOfDigits = 0;
                while (yValue != 0) {
                    sumOfDigits += Math.abs(yValue % 10);
                    yValue /= 10;
                }

                if (sumOfDigits < 10) {
                    canvas.setColor(Color.BLUE);
                    canvas.setPaint(Color.BLUE);
                } else {
                    canvas.setColor(Color.RED);
                    canvas.setPaint(Color.RED);
                }


                canvas.drawLine((int)center.getX() - size, (int)center.getY(), (int)center.getX() + size, (int)center.getY()); // горизонтальная линия
                canvas.drawLine((int)center.getX() - 2, (int)center.getY() + size, (int)center.getX() + 2, (int)center.getY() + size);
                canvas.drawLine((int)center.getX() - 2, (int)center.getY() - size, (int)center.getX() + 2, (int)center.getY() - size);
                canvas.drawLine((int)center.getX(), (int)center.getY() - size, (int)center.getX(), (int)center.getY() + size); // вертикальная линия
                canvas.drawLine((int)center.getX() - size, (int)center.getY() - 2, (int)center.getX() - size, (int)center.getY() + 2);
                canvas.drawLine((int)center.getX() + size, (int)center.getY() - 2, (int)center.getX() + size, (int)center.getY() + 2);

                canvas.drawLine((int)center.getX() - size, (int)center.getY() + 2, (int)center.getX() - 2, (int)center.getY() + size);
                canvas.drawLine((int)center.getX() + 2, (int)center.getY() + size, (int)center.getX() + size, (int)center.getY() + 2);
                canvas.drawLine((int)center.getX() + size, (int)center.getY() - 2, (int)center.getX() + 2, (int)center.getY() - size);
                canvas.drawLine((int)center.getX() - 2, (int)center.getY() - size, (int)center.getX() - size, (int)center.getY() - 2);
            }
        }
    }

    private void paintMarkers2(Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.CYAN);
        canvas.setPaint(Color.CYAN);
        int i = -1;
        Iterator var5 = this.graphicsData2.iterator();

        while(var5.hasNext()) {
            Double[] point = (Double[])var5.next();
            ++i;
            if (!(point[0] < this.viewport[0][0]) && !(point[1] > this.viewport[0][1]) && !(point[0] > this.viewport[1][0]) && !(point[1] < this.viewport[1][1])) {
                byte radius = 2;

                Ellipse2D.Double marker = new Ellipse2D.Double();
                Point2D center = this.translateXYtoPoint(point[0], point[1]);
                Point2D corner = new Point2D.Double(((Point2D)center).getX() + (double)radius, ((Point2D)center).getY() + (double)radius);
                marker.setFrameFromCenter(center, corner);
                canvas.draw(marker);
                canvas.fill(marker);
            }
        }
    }

    private void paintLabels(Graphics2D canvas) {
        canvas.setColor(Color.BLACK);
        canvas.setFont(this.labelsFont);
        FontRenderContext context = canvas.getFontRenderContext();
        double labelYPos;
        if (this.viewport[1][1] < 0.0 && this.viewport[0][1] > 0.0) {
            labelYPos = 0.0;
        } else {
            labelYPos = this.viewport[1][1];
        }

        double labelXPos;
        if (this.viewport[0][0] < 0.0 && this.viewport[1][0] > 0.0) {
            labelXPos = 0.0;
        } else {
            labelXPos = this.viewport[0][0];
        }

        double pos = this.viewport[0][0];

        double step;
        Point2D.Double point;
        String label;
        Rectangle2D bounds;
        for(step = (this.viewport[1][0] - this.viewport[0][0]) / 10.0; pos < this.viewport[1][0]; pos += step) {
            point = this.translateXYtoPoint(pos, labelYPos);
            label = formatter.format(pos);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + 5.0), (float)(point.getY() - bounds.getHeight()));
        }

        pos = this.viewport[1][1];

        for(step = (this.viewport[0][1] - this.viewport[1][1]) / 10.0; pos < this.viewport[0][1]; pos += step) {
            point = this.translateXYtoPoint(labelXPos, pos);
            label = formatter.format(pos);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + 5.0), (float)(point.getY() - bounds.getHeight()));
        }

        if (this.selectedMarker >= 0) {
            point = this.translateXYtoPoint(((Double[])this.graphicsData1.get(this.selectedMarker))[0], ((Double[])this.graphicsData1.get(this.selectedMarker))[1]);
            label = "X=" + formatter.format(((Double[])this.graphicsData1.get(this.selectedMarker))[0]) + ", Y=" + formatter.format(((Double[])this.graphicsData1.get(this.selectedMarker))[1]);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.setColor(Color.BLUE);
            canvas.drawString(label, (float)(point.getX() + 5.0), (float)(point.getY() - bounds.getHeight()));
        }

    }

    private void paintGrid(Graphics2D canvas) {
        canvas.setStroke(this.gridStroke);
        canvas.setColor(Color.GRAY);
        double pos = this.viewport[0][0];

        double step;
        for(step = (this.viewport[1][0] - this.viewport[0][0]) / 10.0; pos < this.viewport[1][0]; pos += step) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(pos, this.viewport[0][1]), this.translateXYtoPoint(pos, this.viewport[1][1])));
        }

        canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[1][0], this.viewport[0][1]),
                this.translateXYtoPoint(this.viewport[1][0], this.viewport[1][1])));
        pos = this.viewport[1][1];

        for(step = (this.viewport[0][1] - this.viewport[1][1]) / 10.0; pos < this.viewport[0][1]; pos += step) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], pos), this.translateXYtoPoint(this.viewport[1][0], pos)));
        }

        canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], this.viewport[0][1]), this.translateXYtoPoint(this.viewport[1][0],
                this.viewport[0][1])));
    }

    private void paintAxis(Graphics2D canvas) {
        canvas.setStroke(this.axisStroke);
        canvas.setColor(Color.BLACK);
        canvas.setFont(this.axisFont);
        FontRenderContext context = canvas.getFontRenderContext();
        Rectangle2D bounds;
        Point2D.Double labelPos;
        if (this.viewport[0][0] <= 0.0 && this.viewport[1][0] >= 0.0) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(0.0, this.viewport[0][1]), this.translateXYtoPoint(0.0, this.viewport[1][1])));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(-(this.viewport[1][0] - this.viewport[0][0]) * 0.0025, this.viewport[0][1] - (this.viewport[0][1] - this.viewport[1][1]) * 0.015), this.translateXYtoPoint(0.0, this.viewport[0][1])));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint((this.viewport[1][0] - this.viewport[0][0]) * 0.0025, this.viewport[0][1] - (this.viewport[0][1] - this.viewport[1][1]) * 0.015), this.translateXYtoPoint(0.0, this.viewport[0][1])));
            bounds = this.axisFont.getStringBounds("y", context);
            labelPos = this.translateXYtoPoint(0.0, this.viewport[0][1]);
            canvas.drawString("y", (float)labelPos.x + 10.0F, (float)(labelPos.y + bounds.getHeight() / 2.0));
        }

        if (this.viewport[1][1] <= 0.0 && this.viewport[0][1] >= 0.0) {
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], 0.0), this.translateXYtoPoint(this.viewport[1][0], 0.0)));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[1][0] - (this.viewport[1][0] - this.viewport[0][0]) * 0.01, (this.viewport[0][1] - this.viewport[1][1]) * 0.005), this.translateXYtoPoint(this.viewport[1][0], 0.0)));
            canvas.draw(new Line2D.Double(this.translateXYtoPoint(this.viewport[1][0] - (this.viewport[1][0] - this.viewport[0][0]) * 0.01, -(this.viewport[0][1] - this.viewport[1][1]) * 0.005), this.translateXYtoPoint(this.viewport[1][0], 0.0)));
            bounds = this.axisFont.getStringBounds("x", context);
            labelPos = this.translateXYtoPoint(this.viewport[1][0], 0.0);
            canvas.drawString("x", (float)(labelPos.x - bounds.getWidth() - 10.0), (float)(labelPos.y - bounds.getHeight() / 2.0));
        }

    }

    protected Point2D.Double translateXYtoPoint(double x, double y) {
        double deltaX = x - this.viewport[0][0];
        double deltaY = this.viewport[0][1] - y;
        return new Point2D.Double(deltaX * this.scaleX, deltaY * this.scaleY);
    }

    protected double[] translatePointToXY(int x, int y) {
        return new double[]{this.viewport[0][0] + (double)x / this.scaleX, this.viewport[0][1] - (double)y / this.scaleY};
    }

    protected int findSelectedPoint(int x, int y) {
        if (this.graphicsData1 == null) {
            return -1;
        } else {
            int pos = 0;

            for(Iterator var5 = this.graphicsData1.iterator(); var5.hasNext(); ++pos) {
                Double[] point = (Double[])var5.next();
                Point2D.Double screenPoint = this.translateXYtoPoint(point[0], point[1]);
                double distance = (screenPoint.getX() - (double)x) * (screenPoint.getX() - (double)x) + (screenPoint.getY() - (double)y) * (screenPoint.getY() - (double)y);
                if (distance < 100.0) {
                    return pos;
                }
            }

            return -1;
        }
    }

    public void reset() {
        this.displayGraphics(this.originalData1, this.originalData2);
    }

    protected void saveToTextFile(File selectedFile) {
        try (PrintStream out = new PrintStream(selectedFile)) {
            // Проходим по всем точкам данных
            for (Double[] point : graphicsData1) {
                // Записываем значения X и Y, разделяя их запятой
                out.printf("%f,%f%n", point[0], point[1]);
            }
        } catch (FileNotFoundException e) {

        }
    }

    public class MouseHandler extends MouseAdapter {
        public MouseHandler() {
        }

        public void mouseClicked(MouseEvent ev) {
            if (ev.getButton() == 3) {
                if (GraphicsDisplay.this.undoHistory.size() > 0) {
                    GraphicsDisplay.this.viewport = (double[][])GraphicsDisplay.this.undoHistory.get(GraphicsDisplay.this.undoHistory.size() - 1);
                    GraphicsDisplay.this.undoHistory.remove(GraphicsDisplay.this.undoHistory.size() - 1);
                } else {
                    GraphicsDisplay.this.zoomToRegion(GraphicsDisplay.this.minX, GraphicsDisplay.this.maxY, GraphicsDisplay.this.maxX, GraphicsDisplay.this.minY);
                }

                GraphicsDisplay.this.repaint();
            }

        }

        public void mousePressed(MouseEvent ev) {
            if (ev.getButton() == 1) {
                GraphicsDisplay.this.selectedMarker = GraphicsDisplay.this.findSelectedPoint(ev.getX(), ev.getY());
                GraphicsDisplay.this.originalPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());
                if (GraphicsDisplay.this.selectedMarker >= 0) {
                    GraphicsDisplay.this.changeMode = true;
                    GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(8));
                } else {
                    GraphicsDisplay.this.scaleMode = true;
                    GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(5));
                    GraphicsDisplay.this.selectionRect.setFrame((double)ev.getX(), (double)ev.getY(), 1.0, 1.0);
                }

            }
        }

        public void mouseReleased(MouseEvent ev) {
            if (ev.getButton() == 1) {
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(0));
                if (GraphicsDisplay.this.changeMode) {
                    GraphicsDisplay.this.changeMode = false;
                } else {
                    GraphicsDisplay.this.scaleMode = false;
                    double[] finalPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());
                    GraphicsDisplay.this.undoHistory.add(GraphicsDisplay.this.viewport);
                    GraphicsDisplay.this.viewport = new double[2][2];
                    GraphicsDisplay.this.zoomToRegion(GraphicsDisplay.this.originalPoint[0], GraphicsDisplay.this.originalPoint[1], finalPoint[0], finalPoint[1]);
                    GraphicsDisplay.this.repaint();
                }

            }
        }
    }

    public class MouseMotionHandler implements MouseMotionListener {
        public MouseMotionHandler() {
        }

        public void mouseMoved(MouseEvent ev) {
            GraphicsDisplay.this.selectedMarker = GraphicsDisplay.this.findSelectedPoint(ev.getX(), ev.getY());
            if (GraphicsDisplay.this.selectedMarker >= 0) {
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(8));
            } else {
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(0));
            }

            GraphicsDisplay.this.repaint();
        }

        public void mouseDragged(MouseEvent ev) {
            if (GraphicsDisplay.this.changeMode) {
                double[] currentPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());
                double newY = ((Double[])GraphicsDisplay.this.graphicsData1.get(GraphicsDisplay.this.selectedMarker))[1]
                        + (currentPoint[1] - ((Double[])GraphicsDisplay.this.graphicsData1.get(GraphicsDisplay.this.selectedMarker))[1]);
                if (newY > GraphicsDisplay.this.viewport[0][1]) {
                    newY = GraphicsDisplay.this.viewport[0][1];
                }

                if (newY < GraphicsDisplay.this.viewport[1][1]) {
                    newY = GraphicsDisplay.this.viewport[1][1];
                }

                ((Double[])GraphicsDisplay.this.graphicsData1.get(GraphicsDisplay.this.selectedMarker))[1] = newY;
                GraphicsDisplay.this.repaint();
            } else {
                double width = (double)ev.getX() - GraphicsDisplay.this.selectionRect.getX();
                if (width < 5.0) {
                    width = 5.0;
                }

                double height = (double)ev.getY() - GraphicsDisplay.this.selectionRect.getY();
                if (height < 5.0) {
                    height = 5.0;
                }

                GraphicsDisplay.this.selectionRect.setFrame(GraphicsDisplay.this.selectionRect.getX(), GraphicsDisplay.this.selectionRect.getY(), width, height);
                GraphicsDisplay.this.repaint();
            }

        }
    }
}
