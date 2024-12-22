import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

public class MainFrame extends JFrame {
    private static final int WIDTH = 700;
    private static final int HEIGHT = 500;
    private JFileChooser file1Chooser = null;
    private JFileChooser file2Chooser = null;
    private final JMenuItem resetGraphicsMenuItem;
    private final JCheckBoxMenuItem showAxisMenuItem;
    private final JCheckBoxMenuItem showMarkersMenuItem;
    private final JCheckBoxMenuItem rotateGraphMenuItem;
    private final GraphicsDisplay display = new GraphicsDisplay();
    private JFileChooser fileChooser = null;
    private boolean file1Loaded = false;
    private boolean file2Loaded = false;
    ArrayList<Double[]> graphicsData1 = null;
    ArrayList<Double[]> graphicsData2 = null;
    private final JMenuItem saveToTextMenuItem;

    public MainFrame() {
        super("Построение графиков функций на основе заранее подготовленных файлов");
        this.setSize(700, 500);
        Toolkit kit = Toolkit.getDefaultToolkit();
        this.setLocation((kit.getScreenSize().width - 700) / 2, (kit.getScreenSize().height - 500) / 2);
        this.setExtendedState(6);
        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("Файл");
        menuBar.add(fileMenu);
        Action openGraphicsAction = new AbstractAction("Открыть первый файл с графиком") {
            public void actionPerformed(ActionEvent event) {
                if (MainFrame.this.file1Chooser == null) {
                    MainFrame.this.file1Chooser = new JFileChooser();
                    MainFrame.this.file1Chooser.setCurrentDirectory(new File("."));
                }

                MainFrame.this.file1Chooser.showOpenDialog(MainFrame.this);
                MainFrame.this.openGraphics(MainFrame.this.file1Chooser.getSelectedFile());
            }
        };
        fileMenu.add(openGraphicsAction);
        Action open2GraphicsAction = new AbstractAction("Открыть второй файл с графиком") {
            public void actionPerformed(ActionEvent event) {
                if (MainFrame.this.file2Chooser == null) {
                    MainFrame.this.file2Chooser = new JFileChooser();
                    MainFrame.this.file2Chooser.setCurrentDirectory(new File("."));
                }

                MainFrame.this.file2Chooser.showOpenDialog(MainFrame.this);
                MainFrame.this.open2Graphics(MainFrame.this.file2Chooser.getSelectedFile());
            }
        };
        fileMenu.add(open2GraphicsAction);
        Action resetGraphicsAction = new AbstractAction("Отменить все изменения") {
            public void actionPerformed(ActionEvent event) {
                MainFrame.this.display.reset();
            }
        };
        this.resetGraphicsMenuItem = fileMenu.add(resetGraphicsAction);
        this.resetGraphicsMenuItem.setEnabled(false);
        Action saveToTextAction = new AbstractAction("Сохранить в текстовый файл первый график") {
            public void actionPerformed(ActionEvent event) {
                if (fileChooser==null) {
// Если экземпляр диалогового окна "Открыть файл" ещѐ не создан,
// то создать его
                    fileChooser = new JFileChooser();
// и инициализировать текущей директорией
                    fileChooser.setCurrentDirectory(new File("."));
                }
// Показать диалоговое окно
                if (fileChooser.showSaveDialog(MainFrame.this) ==
                        JFileChooser.APPROVE_OPTION)
// Если результат его показа успешный,
// сохранить данные в текстовый файл
                    display.saveToTextFile(fileChooser.getSelectedFile());
            }
        };
// Добавить соответствующий пункт подменю в меню "Файл"
        saveToTextMenuItem = fileMenu.add(saveToTextAction);
// По умолчанию пункт меню является недоступным (данных ещѐ нет)
        saveToTextMenuItem.setEnabled(false);
        JMenu graphicsMenu = new JMenu("График");
        menuBar.add(graphicsMenu);
        // Создать действие для реакции на активацию элемента "Показывать оси координат"
        Action showAxisAction = new AbstractAction("Показывать оси координат") {
            public void actionPerformed(ActionEvent event) {
// свойство showAxis класса GraphicsDisplay истина, если элемент меню
// showAxisMenuItem отмечен флажком, и ложь - в противном случае
                display.setShowAxis(showAxisMenuItem.isSelected());
            }
        };
        showAxisMenuItem = new JCheckBoxMenuItem(showAxisAction);
// Добавить соответствующий элемент в меню
        graphicsMenu.add(showAxisMenuItem);
// Элемент по умолчанию включен (отмечен флажком)
        showAxisMenuItem.setSelected(true);
// Повторить действия для элемента "Показывать маркеры точек"
        Action showMarkersAction = new AbstractAction("Показывать маркеры точек") {
            public void actionPerformed(ActionEvent event) {
// по аналогии с showAxisMenuItem
                display.setShowMarkers(showMarkersMenuItem.isSelected());
            }
        };
        showMarkersMenuItem = new JCheckBoxMenuItem(showMarkersAction);
        graphicsMenu.add(showMarkersMenuItem);
// Элемент по умолчанию включен (отмечен флажком)
        showMarkersMenuItem.setSelected(true);

        Action rotateGraphAction = new AbstractAction("Повернуть график на 90 градусов") {
            public void actionPerformed(ActionEvent event) {
                display.setRotate(rotateGraphMenuItem.isSelected());
            }
        };
        rotateGraphMenuItem = new JCheckBoxMenuItem(rotateGraphAction);
        graphicsMenu.add(rotateGraphMenuItem);
// Элемент по умолчанию включен (отмечен флажком)
        rotateGraphMenuItem.setSelected(false);

// Зарегистрировать обработчик событий, связанных с меню "График"
        graphicsMenu.addMenuListener(new GraphicsMenuListener());
// Установить GraphicsDisplay в цент граничной компоновки
        getContentPane().add(display, BorderLayout.CENTER);
    }

    protected void openGraphics(File selectedFile) {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(selectedFile));
            ArrayList<Double[]> graphicsData1 = new ArrayList(50);

            while (in.available() > 0) {
                double x = in.readDouble();
                double y = in.readDouble();
                graphicsData1.add(new Double[]{x, y});
            }

            if (!graphicsData1.isEmpty()) {
                this.graphicsData1 = graphicsData1;
                this.file1Loaded = true;
                this.resetGraphicsMenuItem.setEnabled(true);
                this.saveToTextMenuItem.setEnabled(true);
                this.display.displayGraphics(this.graphicsData1, this.graphicsData2);
            }

        } catch (FileNotFoundException var6) {
            JOptionPane.showMessageDialog(this, "Указанный файл не найден", "Ошибка загрузки данных", 2);
        } catch (IOException var7) {
            JOptionPane.showMessageDialog(this, "Ошибка чтения координат точек из файла", "Ошибка загрузки данных", 2);
        }
    }

    protected void open2Graphics(File selectedFile) {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(selectedFile));
            ArrayList<Double[]> graphicsData2 = new ArrayList(50);

            while (in.available() > 0) {
                double x = in.readDouble();
                double y = in.readDouble();
                graphicsData2.add(new Double[]{x, y});
            }

            if (!graphicsData2.isEmpty()) {
                this.graphicsData2 = graphicsData2;
                this.file2Loaded = true;
                this.resetGraphicsMenuItem.setEnabled(true);
                this.display.displayGraphics(graphicsData1, graphicsData2);
            }

        } catch (FileNotFoundException var6) {
            JOptionPane.showMessageDialog(this, "Указанный файл не найден", "Ошибка загрузки данных", 2);
        } catch (IOException var7) {
            JOptionPane.showMessageDialog(this, "Ошибка чтения координат точек из файла", "Ошибка загрузки данных", 2);
        }
    }

    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        frame.setDefaultCloseOperation(3);
        frame.setVisible(true);
    }

    private class GraphicsMenuListener implements MenuListener {
        // Обработчик, вызываемый перед показом меню
        public void menuSelected(MenuEvent e) {
// Доступность или недоступность элементов меню "График" определяется загруженностью данных
            showAxisMenuItem.setEnabled(file1Loaded || file2Loaded);
            showMarkersMenuItem.setEnabled(file1Loaded || file2Loaded);
            rotateGraphMenuItem.setEnabled(file1Loaded || file2Loaded);
        }

        // Обработчик, вызываемый после того, как меню исчезло с экрана
        public void menuDeselected(MenuEvent e) {
        }

        // Обработчик, вызываемый в случае отмены выбора пункта меню (очень редкая ситуация)
        public void menuCanceled(MenuEvent e) {
        }
    }
}