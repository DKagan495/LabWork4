package kagankagan;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.geom.*;
import javax.swing.JPanel;

import static java.lang.Math.abs;

@SuppressWarnings("serial")
public class GraphicsDisplay extends JPanel {
    // Список координат точек для построения графика
    private Double[][] graphicsData;
    // Флаговые переменные, задающие правила отображения графика
    private boolean showAxis = true;
    private boolean showMarkers = true;
    private boolean showGrid = false;
    private boolean toLeftRotate = false;
    private boolean isConcreteValue(double value)
    {
        int toControl = 0;
        int countBadValues = 0;
        do
            {
            toControl = (int)value % 10;
            value /= 10;
            System.out.println(toControl);
            if(toControl%2 != 0)
            {
                countBadValues++;
            }
        } while(value != 0);
        if(countBadValues == 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    // Границы диапазона пространства, подлежащего отображению
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    // Используемый масштаб отображения
    private double scale;
    // Различные стили черчения линий
    private BasicStroke graphicsStroke;
    private BasicStroke axisStroke;
    private BasicStroke markerStroke;
    private BasicStroke gridStroke;
    private BasicStroke delenijaStroke;
    // Различные шрифты отображения надписей
    private Font axisFont;
    private Font gridFont;

    public GraphicsDisplay() {
// Цвет заднего фона области отображения - белый
        setBackground(Color.WHITE);
// Сконструировать необходимые объекты, используемые в рисовании
// Перо для рисования графика
        graphicsStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[] {16, 4, 4, 4, 8, 4, 4, 4, 16, 8}, 0.0f);
// Перо для рисования осей координат
        axisStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
// Перо для рисования контуров маркеров
        markerStroke = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
        //Pero for painting grid lines
        gridStroke = new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, null, 0.0f);
// Шрифт для подписей осей координат
        axisFont = new Font("Serif", Font.BOLD, 36);
        gridFont = new Font("Serif", Font.BOLD, 20);

    }

    // Данный метод вызывается из обработчика элемента меню "Открыть файл с графиком"
    // главного окна приложения в случае успешной загрузки данных
    public void showGraphics(Double[][] graphicsData) {
// Сохранить массив точек во внутреннем поле класса
        this.graphicsData = graphicsData;
// Запросить перерисовку компонента, т.е. неявно вызвать paintComponent()
        repaint();
    }

    // Методы-модификаторы для изменения параметров отображения графика
// Изменение любого параметра приводит к перерисовке области
    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        repaint();
    }

    public void setShowGrid(boolean showGrid)
    {
        this.showGrid = showGrid;
        repaint();
    }
    public void setRotate(boolean toLeftRotate)
    {
        this.toLeftRotate = toLeftRotate;
        repaint();
    }
    // Метод отображения всего компонента, содержащего график
    public void paintComponent(Graphics g) {
        /* Шаг 1 - Вызвать метод предка для заливки области цветом заднего фона
         * Эта функциональность - единственное, что осталось в наследство от
         * paintComponent класса JPanel
         */
        super.paintComponent(g);
// Шаг 2 - Если данные графика не загружены (при показе компонента при запуске программы) - ничего не делать
        if (graphicsData == null || graphicsData.length == 0) return;
// Шаг 3 - Определить минимальное и максимальное значения для координат X и Y
// Это необходимо для определения области пространства, подлежащей отображению
// Еѐ верхний левый угол это (minX, maxY) - правый нижний это(maxX, minY)
        minX = graphicsData[0][0];
        maxX = graphicsData[graphicsData.length - 1][0];
        minY = graphicsData[0][1];
        maxY = minY;
// Найти минимальное и максимальное значение функции
        for (int i = 1; i < graphicsData.length; i++) {
            if (graphicsData[i][1] < minY) {
                minY = graphicsData[i][1];
            }
            if (graphicsData[i][1] > maxY) {
                maxY = graphicsData[i][1];
            }
        }
/* Шаг 4 - Определить (исходя из размеров окна) масштабы по осям X
и Y - сколько пикселов
* приходится на единицу длины по X и по Y
*/
        double scaleX = getSize().getWidth() / (maxX - minX);
        double scaleY = getSize().getHeight() / (maxY - minY);
// Шаг 5 - Чтобы изображение было неискажѐнным - масштаб должен быть одинаков
// Выбираем за основу минимальный
        scale = Math.min(scaleX, scaleY);
// Шаг 6 - корректировка границ отображаемой области согласно выбранному масштабу
        if (scale == scaleX) {
/* Если за основу был взят масштаб по оси X, значит по оси Y
делений меньше,
* т.е. подлежащий визуализации диапазон по Y будет меньше
высоты окна.
* Значит необходимо добавить делений, сделаем это так:
* 1) Вычислим, сколько делений влезет по Y при выбранном
масштабе - getSize().getHeight()/scale
* 2) Вычтем из этого сколько делений требовалось изначально
* 3) Набросим по половине недостающего расстояния на maxY и
minY
*/
            double yIncrement = (getSize().getHeight() / scale - (maxY - minY)) / 2;
            maxY += yIncrement;
            minY -= yIncrement;
        }
        if (scale == scaleY) {
// Если за основу был взят масштаб по оси Y, действовать по аналогии
            double xIncrement = (getSize().getWidth() / scale - (maxX - minX)) / 2;
            maxX += xIncrement;
            minX -= xIncrement;
        }
// Шаг 7 - Сохранить текущие настройки холста
        Graphics2D canvas = (Graphics2D) g;
        Stroke oldStroke = canvas.getStroke();
        Color oldColor = canvas.getColor();
        Paint oldPaint = canvas.getPaint();
        Font oldFont = canvas.getFont();
// Шаг 8 - В нужном порядке вызвать методы отображения элементов графика
// Порядок вызова методов имеет значение, т.к. предыдущий рисунок будет затираться последующим
// Первыми (если нужно) отрисовываются оси координат.
        if (toLeftRotate) {rotateTo90left(canvas);}
        if (showAxis) {paintAxis(canvas);}
// Затем отображается сам график
        paintGraphics(canvas);
// Затем (если нужно) отображаются маркеры точек, по которым строился график.
        if (showMarkers) {paintMarkers(canvas);}
        if(showGrid) {paintGrid(canvas);}
        if (toLeftRotate) {rotateTo90left(canvas);}
        // rotate
// Шаг 9 - Восстановить старые настройки холста
        canvas.setFont(oldFont);
        canvas.setPaint(oldPaint);
        canvas.setColor(oldColor);
        canvas.setStroke(oldStroke);
    }

    // Отрисовка графика по прочитанным координатам
    protected void paintGraphics(Graphics2D canvas) {
// Выбрать линию для рисования графика
        canvas.setStroke(graphicsStroke);
// Выбрать цвет линии
        canvas.setColor(Color.RED);
/* Будем рисовать линию графика как путь, состоящий из множества
сегментов (GeneralPath)
* Начало пути устанавливается в первую точку графика, после чего
прямой соединяется со
* следующими точками
*/
        GeneralPath graphics = new GeneralPath();
        for (int i = 0; i < graphicsData.length; i++) {
// Преобразовать значения (x,y) в точку на экране point
            Point2D.Double point = xyToPoint(graphicsData[i][0],
                    graphicsData[i][1]);
            if (i > 0) {
// Не первая итерация цикла - вести линию в точку point
                graphics.lineTo(point.getX(), point.getY());
            } else {
// Первая итерация цикла - установить начало пути в точку point
                graphics.moveTo(point.getX(), point.getY());
            }
        }
// Отобразить график
        canvas.draw(graphics);
    }

    // Отображение маркеров точек, по которым рисовался график
    protected void paintMarkers(Graphics2D canvas) {
// Шаг 1 - Установить специальное перо для черчения контуров маркеров
        canvas.setStroke(markerStroke);
// Выбрать красный цвета для контуров маркеров
        canvas.setColor(Color.RED);
// Выбрать красный цвет для закрашивания маркеров внутри
        canvas.setPaint(Color.RED);
        GeneralPath lastMarker = null;
// Шаг 2 - Организовать цикл по всем точкам графика
        for (Double[] point : graphicsData) {
// Инициализировать эллипс как объект для представления маркера
            Ellipse2D.Double marker = new Ellipse2D.Double();
/* Эллипс будет задаваться посредством указания координат
его центра
и угла прямоугольника, в который он вписан */
// Центр - в точке (x,y)
            Point2D.Double center = xyToPoint(point[0], point[1]);
            if(isConcreteValue(point[1]))
            {
                canvas.setColor(Color.BLUE);
                canvas.setPaint(Color.BLUE);
            }
            else
            {
                canvas.setColor(Color.RED);
                canvas.setPaint(Color.RED);
            }
            GeneralPath star = new GeneralPath();
            java.awt.geom.Point2D.Double cent = this.xyToPoint(point[0], point[1]);
            star.moveTo(cent.getX(), cent.getY());
            star.lineTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY() - 5.0D);
            star.moveTo(star.getCurrentPoint().getX() - 3.0D, star.getCurrentPoint().getY());
            star.lineTo(star.getCurrentPoint().getX() + 6.0D, star.getCurrentPoint().getY());
            star.moveTo(center.getX(), center.getY());
            star.lineTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY() + 5.0D);
            star.moveTo(star.getCurrentPoint().getX() - 3.0D, star.getCurrentPoint().getY());
            star.lineTo(star.getCurrentPoint().getX() + 6.0D, star.getCurrentPoint().getY());
            star.moveTo(center.getX(), center.getY());
            star.lineTo(star.getCurrentPoint().getX() - 5.0D, star.getCurrentPoint().getY());
            star.moveTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY() - 3.0D);
            star.lineTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY() + 6.0D);
            star.moveTo(center.getX(), center.getY());
            star.lineTo(star.getCurrentPoint().getX() + 5.0D, star.getCurrentPoint().getY());
            star.moveTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY() - 3.0D);
            star.lineTo(star.getCurrentPoint().getX(), star.getCurrentPoint().getY() + 6.0D);

                canvas.draw(star);
                canvas.fill(star);

        }
    }

    // Метод, обеспечивающий отображение осей координат
    protected void paintAxis(Graphics2D canvas) {
// Установить особое начертание для осей
        canvas.setStroke(axisStroke);
// Оси рисуются чѐрным цветом
        canvas.setColor(Color.BLACK);
// Стрелки заливаются чѐрным цветом
        canvas.setPaint(Color.BLACK);
// Подписи к координатным осям делаются специальным шрифтом
        canvas.setFont(axisFont);
// Создать объект контекста отображения текста - для получения характеристик устройства (экрана)
        FontRenderContext context = canvas.getFontRenderContext();
// Определить, должна ли быть видна ось Y на графике
        if (minX <= 0.0 && maxX >= 0.0) {
// Она должна быть видна, если левая граница показываемой области (minX) <= 0.0,
// а правая (maxX) >= 0.0
// Сама ось - это линия между точками (0, maxY) и (0, minY)
            canvas.draw(new Line2D.Double(xyToPoint(0, maxY),
                    xyToPoint(0, minY)));
// Стрелка оси Y
            GeneralPath arrow = new GeneralPath();
// Установить начальную точку ломаной точно на верхний конец оси Y
            Point2D.Double lineEnd = xyToPoint(0, maxY);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
// Вести левый "скат" стрелки в точку с относительными координатами (5,20)
            arrow.lineTo(arrow.getCurrentPoint().getX() + 5,
                    arrow.getCurrentPoint().getY() + 20);
// Вести нижнюю часть стрелки в точку с относительными координатами (-10, 0)
            arrow.lineTo(arrow.getCurrentPoint().getX() - 10,
                    arrow.getCurrentPoint().getY());
// Замкнуть треугольник стрелки
            arrow.closePath();
            canvas.draw(arrow); // Нарисовать стрелку
            canvas.fill(arrow); // Закрасить стрелку
// Нарисовать подпись к оси Y
// Определить, сколько места понадобится для надписи "y"
            Rectangle2D bounds = axisFont.getStringBounds("y", context);
            Point2D.Double labelPos = xyToPoint(0, maxY);
// Вывести надпись в точке с вычисленными координатами
            canvas.drawString("y", (float) labelPos.getX() + 10,
                    (float) (labelPos.getY() - bounds.getY()));
        }
// Определить, должна ли быть видна ось X на графике
        if (minY <= 0.0 && maxY >= 0.0) {
// Она должна быть видна, если верхняя граница показываемой области (maxX) >= 0.0,
// а нижняя (minY) <= 0.0
            canvas.draw(new Line2D.Double(xyToPoint(minX, 0),
                    xyToPoint(maxX, 0)));
// Стрелка оси X
            GeneralPath arrow = new GeneralPath();
// Установить начальную точку ломаной точно на правый конец оси X
            Point2D.Double lineEnd = xyToPoint(maxX, 0);
            arrow.moveTo(lineEnd.getX(), lineEnd.getY());
// Вести верхний "скат" стрелки в точку с относительными координатами (-20,-5)
            arrow.lineTo(arrow.getCurrentPoint().getX() - 20,
                    arrow.getCurrentPoint().getY() - 5);
// Вести левую часть стрелки в точку с относительными координатами (0, 10)
            arrow.lineTo(arrow.getCurrentPoint().getX(),
                    arrow.getCurrentPoint().getY() + 10);
// Замкнуть треугольник стрелки
            arrow.closePath();
            canvas.draw(arrow); // Нарисовать стрелку
            canvas.fill(arrow); // Закрасить стрелку
// Нарисовать подпись к оси X
// Определить, сколько места понадобится для надписи "x"
            Rectangle2D bounds = axisFont.getStringBounds("x", context);
            Point2D.Double labelPos = xyToPoint(maxX, 0);
// Вывести надпись в точке с вычисленными координатами
            canvas.drawString("x", (float) (labelPos.getX() -
                    bounds.getWidth() - 10), (float) (labelPos.getY() + bounds.getY()));
        }
    }
    protected void rotateTo90left(Graphics2D canvas)
    {
        AffineTransform at;
        at = AffineTransform.getRotateInstance(-1.5707963267948966D, this.getSize().getWidth() / 2.0D, this.getSize().getHeight() / 2.0D);
        at.concatenate(new AffineTransform(this.getSize().getHeight() / this.getSize().getWidth(), 0.0D, 0.0D, this.getSize().getWidth() / this.getSize().getHeight(), (this.getSize().getWidth() - this.getSize().getHeight()) / 2.0D, (this.getSize().getHeight() - this.getSize().getWidth()) / 2.0D));
        canvas.setTransform(at);
        System.out.println("qqq");
    }
protected void paintGrid(Graphics2D canvas)
    {
        int numberOfSection = 0;
        canvas.setStroke(gridStroke);
        canvas.setColor(Color.GRAY);
        canvas.setFont(gridFont);
        // горизонтальные линии сетки от 0 вверх
        if(maxY>0);
        for(double i = 0; i <= getToolkit().getScreenSize().height/2; i+=0.05*((int)maxY-(int)minY))
        {


            Line2D.Double horizontalGridLine = new Line2D.Double(xyToPoint(-getToolkit().getScreenSize().width/2, i), xyToPoint(getToolkit().getScreenSize().width/2, i));
            canvas.draw(horizontalGridLine);
            Point2D.Double labelPos = xyToPoint(0, maxY);
        }
        //горизонтальные линии сетки от 0 вниз
        if(minY<0) {
            for (double i = 0; i >= -getToolkit().getScreenSize().height / 2; i -= 0.05 * ((int) maxY - (int) minY)) {

                Line2D.Double horizontalGridLine = new Line2D.Double(xyToPoint(-getToolkit().getScreenSize().width / 2, i), xyToPoint(getToolkit().getScreenSize().width / 2, i));
                canvas.draw(horizontalGridLine);
                Point2D.Double labelPos = xyToPoint(0, maxY);
            }
        }
        // вертикальные линии сетки от 0 влево
        if(minX<0) {
            for (double i = 0; i >= -getToolkit().getScreenSize().width / 2; i -= 0.05 * ((int) maxY - (int) minY)) {

                Line2D.Double horizontalGridLine = new Line2D.Double(xyToPoint(i, -getToolkit().getScreenSize().height / 2), xyToPoint(i, getToolkit().getScreenSize().height / 2));
                canvas.draw(horizontalGridLine);
            }
        }
        //вертикальные линии сетки от 0 вправо
        if(maxX>0) {
            for (double i = 0; i <= getToolkit().getScreenSize().width / 2; i += 0.05 * ((int) maxY - (int) minY)) {

                Line2D.Double horizontalGridLine = new Line2D.Double(xyToPoint(i, -getToolkit().getScreenSize().height / 2), xyToPoint(i, getToolkit().getScreenSize().height / 2));
                canvas.draw(horizontalGridLine);
            }
        }

// вертикальные маленькие линии, идущие горизонтально

        Point2D.Double lPos = xyToPoint(0, 0);
        for(double i = 0; i <= getToolkit().getScreenSize().height/2; i+=0.05*((int)maxY-(int)minY))
        {
            Point2D.Double labelPos = xyToPoint(0, maxY);
        }
        for(double i = 0; i >= minX; i-=0.05*((int)maxY-(int)minY))
        {
            Point2D.Double labelPos = xyToPoint(0, maxY);
        }
        for(double i = 0; i <= getToolkit().getScreenSize().width/2; i+=0.05*((int)maxY-(int)minY))
        {
            Point2D.Double labelPos = xyToPoint(maxX, 0);

        }
        for(double i = 0; i >= minX; i-=0.05*((int)maxY-(int)minY))
        {
            Point2D.Double labelPos = xyToPoint(0, maxY);
        }
        for(double i = 0; i <= maxX; i+=0.05*((int)maxY-(int)minY)) {
            System.out.println(i);
            Line2D.Double horizontalGridLine = new Line2D.Double(xyToPoint((int)minX, i), xyToPoint((int)maxX, i));
            canvas.draw(horizontalGridLine);
            if(minY < 0) {
                for (double j = 0; j > minX; j -= 0.005 * (maxY - minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.007 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.007 * ((int)maxY - (int)minY), j));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.005 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.005 * ((int)maxY - (int)minY), j));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
            else
            {
                for (double j = 0; j < minX; j += 0.005 * ((int)maxY - (int)minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.007 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.007 * ((int)maxY - (int)minY), j));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.005 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.005 * ((int)maxY - (int)minY), j));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
            numberOfSection = 0;
            if(maxX < 0) {
                for (double j = 0; j > maxX; j -= 0.005 * ((int)maxY - (int)minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.007 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.007 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.005 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.005 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
            else
            {
                for (double j = 0; j < maxX; j += 0.005 * ((int)maxY - (int)minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.007 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.007 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.005 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.005 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }

        }
        // 0 to dwn
        for(double i = 0; i > minX; i-=0.05*(maxY-minY)) {
            Line2D.Double horizontalGridLine = new Line2D.Double(xyToPoint((int)minX, i), xyToPoint((int)maxX, i));
            canvas.draw(horizontalGridLine);
            if(minY < 0) {
                for (double j = 0; j > minY; j -= 0.005 * (maxY - minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.007 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.007 * ((int)maxY - (int)minY), j));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.005 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.005 * ((int)maxY - (int)minY), j));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
            else
            {
                for (double j = 0; j < minX; j += 0.005 * ((int)maxY - (int)minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.007 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.007 * ((int)maxY - (int)minY), j));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.005 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.005 * ((int)maxY - (int)minY), j));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
            if(maxX < 0) {
                for (double j = 0; j > maxX; j -= 0.005 * ((int)maxY - (int)minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.007 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.007 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.005 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.005 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
            else
            {
                for (double j = 0; j < maxX; j += 0.005 * ((int)maxY - (int)minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.007 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.007 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.005 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.005 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
        }
        for(double i = 0; i <= maxX; i+=(0.05*(maxY-minY))) {
            Line2D.Double verticalGridLine = new Line2D.Double(xyToPoint(i, (int)minY), xyToPoint(i, (int)maxY));
            canvas.draw(verticalGridLine);
            for(double j = 0; j <= maxY; j+=0.005*(maxY-minY))
            {

                if(numberOfSection%5 == 0)
                {
                    Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.007 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.007 * ((int)maxY - (int)minY), j));
                    canvas.draw(testGridLine);
                }
                else
                {
                    Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.005 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.005 * ((int)maxY - (int)minY), j));
                    canvas.draw(testGridLine);
                }
                numberOfSection++;
            }
            numberOfSection = 0;
            if(minX < 0) {
                for (double j = 0; j > minX; j -= 0.005 * ((int)maxY - (int)minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.007 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.007 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.005 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.005 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
            else
            {
                for (double j = 0; j < minX; j += 0.005 * ((int)maxY - (int)minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.007 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.007 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.005 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.005 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
        }
        for(double i = 0; i >=minX; i-=(0.05*(maxY-minY))) {
            Line2D.Double verticalGridLine = new Line2D.Double(xyToPoint(i, (int)minY), xyToPoint(i, (int)maxY));
            canvas.draw(verticalGridLine);
            for(double j = 0; j <= maxY; j+=0.005*((int)maxY-(int)minY))
            {
                if(numberOfSection%5 == 0)
                {
                    Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.007 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.007 * ((int)maxY - (int)minY), j));
                    canvas.draw(testGridLine);
                }
                else
                {
                    Line2D.Double testGridLine = new Line2D.Double(xyToPoint(i - 0.005 * ((int)maxY - (int)minY), j), xyToPoint(i + 0.005 * ((int)maxY - (int)minY), j));
                    canvas.draw(testGridLine);
                }
                numberOfSection++;
            }
            numberOfSection = 0;
            if(minX < 0) {
                for (double j = 0; j > minX; j -= 0.005 * ((int)maxY - (int)minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.007 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.007 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.005 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.005 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
            else
            {
                for (double j = 0; j < minX; j += 0.005 * ((int)maxY - (int)minY)) {

                    if (numberOfSection % 5 == 0) {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.007 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.007 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    } else {
                        Line2D.Double testGridLine = new Line2D.Double(xyToPoint(j, i - 0.005 * ((int)maxY - (int)minY)), xyToPoint(j, i + 0.005 * ((int)maxY - (int)minY)));
                        canvas.draw(testGridLine);
                    }
                    numberOfSection++;
                }
                numberOfSection = 0;
            }
        }


    }
    /* Метод-помощник, осуществляющий преобразование координат.
    * Оно необходимо, т.к. верхнему левому углу холста с координатами
    * (0.0, 0.0) соответствует точка графика с координатами (minX, maxY),
    где
    * minX - это самое "левое" значение X, а
    * maxY - самое "верхнее" значение Y.
    */
    protected Point2D.Double xyToPoint(double x, double y) {
// Вычисляем смещение X от самой левой точки (minX)
        double deltaX = x - minX;
// Вычисляем смещение Y от точки верхней точки (maxY)
        double deltaY = maxY - y;
        return new Point2D.Double(deltaX * scale, deltaY * scale);
    }
    protected Double oneValueToPointValue(double value)
    {
        double deltaValue = value - minY;
        return deltaValue*scale;
    }
    protected Float oneValueToPointValue(float value)
    {
        float deltaValue = value - (float)minY;
        return deltaValue*(float)scale;
    }

    /* Метод-помощник, возвращающий экземпляр класса Point2D.Double
     * смещѐнный по отношению к исходному на deltaX, deltaY
     * К сожалению, стандартного метода, выполняющего такую задачу, нет.
     */
    protected Point2D.Double shiftPoint(Point2D.Double src, double deltaX,
                                        double deltaY) {
// Инициализировать новый экземпляр точки
        Point2D.Double dest = new Point2D.Double();
// Задать еѐ координаты как координаты существующей точки + заданные смещения
        dest.setLocation(src.getX() + deltaX, src.getY() + deltaY);
        return dest;
    }
}