import java.awt.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.geom.Rectangle2D;
import java.awt.event.*;
import java.io.*;
//интерфейс - класс в кототором методы не реализованы оверрайд - переопределение метода
//implement -реализация интерфейса экстенс - наследование класса
//Класс FractalExplorer позволяет исследовать различные части фрактала с помощью
// создания и отображения графического интерфейса Swing и обработки событий,
// вызванных различными взаимодействиями с пользователем.

//Класс для отображения фрактала
public class FractalExplorer {
    /** Целочисленный размер отображения - это ширина и высота отображения в пикселях. **/
   private int displaySize;
   private int rowsRemaining; //чтобы узнать, когда будет завершена перерисовка.
    //Чтение и запись этого значения будет происходить в потоке обработки событий, чтобы не было параллельного доступа к этому элементу.
    // Если взаимодействие с ресурсом будет происходить только из одного потока, то не возникнет ошибок параллелизма.
   //Константы, хардкоженные строки
   private static final String TITLE = "Навигатор фракталов";
   private static final String RESET = "Сброс";
   private static final String SAVE = "Сохранить";
   private static final String CHOOSE = "Выбрать фрактал :";
   private static final String COMBOBOX_CHANGE = "comboBoxChanged";
   private static final String SAVE_ERROR = "Ошибка при сохранении изображения";
    /**
     * Ссылка JImageDisplay для обновления отображения с помощью различных методов как
     * фрактал вычислен.
     */
   private JImageDisplay display;
    /** Объект FractalGenerator для каждого типа фрактала.
     * Будет использоваться ссылка на базовый класс
     * для отображения других видов фракталов в будущем.
     **/
    private FractalGenerator fractal;
    /**
     * Объект Rectangle2D.Double, который определяет диапазон
     * то, что мы в настоящее время показываем.
     */
   private Rectangle2D.Double range;
   private JComboBox comboBox;
   private JButton resetButton;//
   private JButton saveButton;//

   //Имплементируем интерфейс ActionListener для обработки событий
    /**Имплементируем интерфейс ActionListener для кнопки сброса
     * Обработка событий от кнопки сброса
     * Обработчик должен сбросить диапазон к начальному,
     * определенному генератором, а затем перерисовать фрактал.
     //сверху селектор снизу сохранение
     */
   class ActionsHandler implements ActionListener {
      public void actionPerformed(ActionEvent e) {
          String command = e.getActionCommand();
          if(command.equals(RESET)){
              fractal.getInitialRange(range);
              drawFractal();
          } else if (command.equals(COMBOBOX_CHANGE)) {
              JComboBox source = (JComboBox) e.getSource();
              fractal = (FractalGenerator) source.getSelectedItem();
              fractal.getInitialRange(range);
              display.clearImage();
              drawFractal();
          } else if (command.equals(SAVE)) {//сохраняем файл в пнг
              JFileChooser fileChooser = new JFileChooser();
              FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Images", "png");
              fileChooser.setFileFilter(filter);
              fileChooser.setAcceptAllFileFilterUsed(false);
              if(fileChooser.showSaveDialog(display) == JFileChooser.APPROVE_OPTION) {
                  File file = fileChooser.getSelectedFile();//получение настроек файла
                  String path = file.toString();//выбор пути
                  if(path.length() == 0) return;
                  if(!path.contains(".png")){
                      file = new File(path + ".png");
                  }
                  try {
                      javax.imageio.ImageIO.write(display.getImage(), "png", file);//запись изображения в файл
                  } catch (Exception exception) {//при возникновении ошибки (ловит ошибки)
                      JOptionPane.showMessageDialog(display, exception.getMessage(), SAVE_ERROR, JOptionPane.ERROR_MESSAGE);
                  }
              }
          }
      }
   }
   //Наследуем MouseAdapter для обработки событий мыши
    /**
     * Класс для обработки событий MouseListener с дисплея.
     * Когда обработчик получает событие щелчка мыши, он отображает пиксель-
     * координаты щелчка в области фрактала, который
     * отображается, а затем вызывает функцию RecenterAndZoomRange () генератора
     * метод с координатами, по которым был выполнен щелчок, и масштабом 0,5
     * -происходит увеличение при нажатии
     */
   class MouseHandler extends MouseAdapter {
      @Override
      public void mouseClicked(MouseEvent e) {
         if(rowsRemaining != 0) return;
         display.clearImage();
          /** Получаем координату x области отображения щелчка мыши. **/
          int x = e.getX();
          double xCoord = FractalGenerator.getCoord(range.x, range.x + range.width, displaySize, x);
          /** Получаем координату y области отображения щелчка мышью. **/
         int y = e.getY();
         double yCoord = FractalGenerator.getCoord(range.y, range.y + range.height, displaySize, y);
          /**
           * Вызывааем метод генератора RecenterAndZoomRange() с помощью
           * координаты, по которым был выполнен щелчок, и шкала 0,5.
           */
         fractal.recenterAndZoomRange(range, xCoord, yCoord, 0.5);
          /**
           * Перерисовываем фрактал после изменения отображаемой области.
           */
         drawFractal();
      }
   }

//В этой лабораторной работе нужно изменить программу так,
// чтобы она использовала один или несколько фоновых потоков для вычисления фрактала.
   class FractalWorker extends javax.swing.SwingWorker<Object, Object> {//SwingWorker - абстрактный класс
       //отвечает за генерацию данных строки и за рисование этой строки
      private int y;//целочисленная y- координата вычисляемой строки
      private int[] rgb;//массив чисел типа int для хранения вычисленных значений RGB для каждого пикселя в строке
      public FractalWorker(int y){
          this.y = y;
      }//отвечает за вычисление значений цвета для одной строки фрактала
    //Конструктор получает y-координату в качестве параметра и сохраняет это.
      @Override
      protected Object doInBackground(){//- метод выполняет фоновые операции.
          //добавляем пиксели в массив
          //вычисляем строки для фракталов, заполняет массив rgb
          //Swing вызывает этот метод в фоновом потоке, а не в потоке обработки событий, отвечает за выполнение длительной задачи.
          //Вместо того, чтобы рисовать изображение в окне, цикл должен будет сохранить каждое значение RGB
          rgb = new int[displaySize];
          int color;
          /**Проходим через каждый пиксель на дисплее **/
          for(int x = 0; x < displaySize; x++){
              /**
               * Находим соответствующие координаты xCoord и yCoord
               * в области отображения фрактала.
               */
              double xCoord = fractal.getCoord(range.x, range.x + range.width, displaySize, x);
              double yCoord = fractal.getCoord(range.y, range.y + range.height, displaySize, y);
              /**
               * Вычисляем количество итераций для координат в
               * область отображения фрактала.
               */
              int iteration = fractal.numIterations(xCoord, yCoord);
              //пиксель в черный цвет
              color = 0;
              if(iteration > 0){//т.е. точка не выходит за границы
                  /**
                   * выбираем значение оттенка на основе числа итераций.
                   * цветовое пространство HSV: поскольку значение цвета варьируется от 0 до 1,
                   * получается плавная последовательность цветов от красного к желтому, зеленому, синему,
                   * фиолетовому и затем обратно к красному!
                   */
                    float hue = 0.7f + (float) iteration / 200f;
                    color = Color.HSBtoRGB(hue, 1f, 1f);
              }
              //Вместо того, чтобы рисовать изображение в окне, цикл должен будет сохранить каждое значение RGB
              rgb[x] = color;
          }
         return null;
      }
      @Override
      protected void done() {//- этот метод вызывается, когда фоновая задача завершена.
          //перерисовывает пиксели
          // Он вызывается в потоке обработки событий, поэтому методу разрешено взаимодействовать с пользовательским интерфейсом.
          //вызывается из потока обработки событий Swing - можем модифицировать компоненты Swing на наш вкус
          for(int x = 0; x < displaySize; x++){
              display.drawPixel(x, y, rgb[x]);
          }
          display.repaint(0, 0, y, displaySize, 1);//позволяет нам указать область для перерисовки
          rowsRemaining--;
          //уменьшаем значение «rows remaining» на 1, как последний шаг данной операции.
          if(rowsRemaining == 0) enableUI(true);
          //если после уменьшения значение «rows remaining» равно 0, вызовите метод enableUI (true).
      }
   }

   //Точка входа в программу
    /**
     * Статический метод main() для запуска FractalExplorer. Инициализирует новый
     * Экземпляр FractalExplorer с размером дисплея 800, вызывает
     * createAndShowGU () в объекте проводника, а затем вызывает
     * drawFractal() в проводнике, чтобы увидеть исходный вид.
     */
   public static void main(String[] args){
       FractalExplorer fractalExplorer = new FractalExplorer(800);
       fractalExplorer.createAndShowGUI();
   }

   //Конструктор класса
    /**
     * Конструктор, который принимает размер отображения, сохраняет его и
     * инициализирует объекты диапазона и фрактал-генератора.
     */
   public FractalExplorer(int displaySize){
       /** Размер дисплея  **/
       this.displaySize = displaySize;
       /** Инициализирует фрактальный генератор и объекты диапазона. **/
       fractal = new Mandelbrot();
       range = new Rectangle2D.Double();
       fractal.getInitialRange(range);
   }

   //Метод для инициализации графического интерфейса Swing
    /**
     * Этот метод инициализирует графический интерфейс Swing с помощью JFrame, содержащего
     * Объект JImageDisplay и кнопку для очистки дисплея
     */
   public void createAndShowGUI(){
        ActionsHandler actionsHandler = new ActionsHandler();
        //Frame
        JFrame frame = new JFrame(TITLE);
       /** Вызываем операцию закрытия фрейма по умолчанию на "выход".. **/
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Display
        display = new JImageDisplay(displaySize, displaySize);
       /** Установите для frame использование java.awt.BorderLayout для своего содержимого. **/
       /** Добавьте объект отображения изображения в BorderLayout.CENTER position.*/
        frame.add(display, BorderLayout.CENTER);

        //Panels
        JPanel topPanel = new JPanel();
        JPanel bottomPanel = new JPanel();

        //label
        JLabel label = new JLabel(CHOOSE);
        topPanel.add(label);

        //ComboBox
        comboBox = new JComboBox();
        comboBox.addItem(new Mandelbrot());
        comboBox.addItem(new Tricorn());
        comboBox.addItem(new BurningShip());
        comboBox.addActionListener(actionsHandler);
        topPanel.add(comboBox, BorderLayout.NORTH);


        //Save Button
        saveButton = new JButton(SAVE);
        saveButton.addActionListener(actionsHandler);
        bottomPanel.add(saveButton, BorderLayout.WEST);

        //Reset Button
       /** Создаем кнопку очистки. **/
        resetButton = new JButton(RESET);
        resetButton.addActionListener(actionsHandler);
        bottomPanel.add(resetButton, BorderLayout.EAST);

        /** Добавьте объект отображения кнопки в BorderLayout.SOUTH position.*/
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.add(topPanel, BorderLayout.NORTH);

        //Mouse Handler
        MouseHandler click = new MouseHandler();
       /** Экземпляр MouseHandler в компоненте фрактального отображения. **/
        display.addMouseListener(click);

        //Misc
       /**
        * Данные операции правильно разметят содержимое окна
        * Размещаем содержимое фрейма, делаем его видимым и
        * запрещаем изменение размера окна.
        */
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);
        drawFractal();
   }
   private void enableUI(boolean val){
       //во время перерисовки включает или отключает кнопки с выпадающим списком в пользовательском интерфейсе на основе указанного параметра.
       //Для включения или отключения этих компонентов
       //метод обновляет состояние кнопки сохранения, кнопки сброса и выпадающего списка.
       comboBox.setEnabled(val);
       resetButton.setEnabled(val);
       saveButton.setEnabled(val);
   }

   //Метод для отрисовки фрактала
   private void drawFractal(){
       //для каждой строки в отображении создать отдельный рабочий объект, а затем вызвать для него метод execute ().
       enableUI(false);//чтобы отключить все элементы пользовательского интерфейса во время рисования.
       rowsRemaining = displaySize;
       for(int y = 0; y < displaySize; y++){
           FractalWorker worker = new FractalWorker(y);
           worker.execute();
       }
   }
}
