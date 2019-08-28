package io.kakashi.graphics;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    interface IPaintable {
        void paint(Graphics2D graphics2D);
    }

    interface IUpdatable {
        void update(int deltaMillis, float deltaSecond);
    }

    interface IPositionable {
        Point getCoord();

        float getAngle();
    }

    interface IObject extends IPaintable, IUpdatable {
    }

    static class RandomUtil {
        private static ThreadLocal<Random> random = new ThreadLocal<Random>() {
            @Override
            protected Random initialValue() {
                return new Random();
            }
        };

        public static int randomBetween(int min, int max) {
            if (min == max) {
                return min;
            }
            if (min > max) {
                int tmp = min;
                min = max;
                max = tmp;
            }
            return min + random.get().nextInt(max - min);
        }

        public static Point randomPoint(Point min, Point max) {
            return new Point(randomBetween(min.x, max.x), randomBetween(min.y, max.y));
        }

        public static Color randomColor(Color black, Color white) {
            int r = randomBetween(black.getRed(), white.getRed());
            int g = randomBetween(black.getGreen(), white.getGreen());
            int b = randomBetween(black.getBlue(), white.getBlue());
            return new Color(r, g, b);
        }
    }

    static class RandomMove implements IUpdatable, IPositionable {

        private Point min;
        private Point max;
        private float speed;

        private Point position;
        private Point srcPosition;
        private Point dstPosition;

        private double reachSec;
        private double movedSec;
        private float rotateSpeed;
        private float angle;

        public RandomMove(Point min, Point max, float speed, float rotateSpeed) {
            this.rotateSpeed = rotateSpeed;
            this.min = min;
            this.max = max;
            this.speed = speed;
            position = RandomUtil.randomPoint(min, max);
            nextPosition();
        }

        public RandomMove(Point min, Point max) {
            this(min, max, RandomUtil.randomBetween(300, 450), RandomUtil.randomBetween(40, 70));
        }

        private void nextPosition() {
            srcPosition = new Point(position);
            dstPosition = RandomUtil.randomPoint(min, max);
            if (!isZero(speed)) {
                reachSec = dstPosition.distance(position) / speed;
                movedSec = 0;
            }
        }

        private boolean isZero(double v) {
            return Math.abs(v) < 0.00001;
        }

        @Override
        public void update(int deltaMillis, float deltaSecond) {
            angle += deltaSecond * rotateSpeed;
            if (isZero(speed)) {
                return;
            }
            if (isZero(position.distance(dstPosition))) {
                nextPosition();
            }
            if (isZero(reachSec)) {
                position = dstPosition;
                return;
            }
            movedSec += deltaSecond;
            double r = Math.min(1, movedSec / reachSec);
            position.setLocation(srcPosition.x + (dstPosition.x - srcPosition.x) * r, srcPosition.y + (dstPosition.y - srcPosition.y) * r);
        }

        @Override
        public Point getCoord() {
            return position;
        }

        @Override
        public float getAngle() {
            return angle;
        }
    }

    static class Quad implements IPaintable {
        IPositionable positionable;

        private int size;
        private int halfSize;
        private Color color;

        public Quad(IPositionable positionable, Color color, int size) {
            this.positionable = positionable;
            this.color = color;
            this.size = size;
            this.halfSize = size / 2;
        }

        public Quad(IPositionable positionable) {
            this(positionable, RandomUtil.randomColor(new Color(50,50,50), new Color(200,200,200)), RandomUtil.randomBetween(30, 80));
        }

        @Override
        public void paint(Graphics2D graphics2D) {
            int x = positionable.getCoord().x - halfSize;
            int y = positionable.getCoord().y - halfSize;
            double rotate = positionable.getAngle() * Math.PI / 180;
            graphics2D.rotate(rotate, x + halfSize, y + halfSize);
            graphics2D.setColor(color);
            graphics2D.drawRect(x, y, size, size);
            graphics2D.rotate(-rotate, x + halfSize, y + halfSize);
        }
    }

    static class Circle implements IPaintable {
        IPositionable positionable;

        private int size;
        private int halfSize;
        private Color color;

        public Circle(IPositionable positionable, Color color, int size) {
            this.positionable = positionable;
            this.color = color;
            this.size = size;
            this.halfSize = size / 2;
        }

        public Circle(IPositionable positionable) {
            this(positionable, RandomUtil.randomColor(new Color(50,50,50), new Color(200,200,200)), RandomUtil.randomBetween(30, 80));
        }

        @Override
        public void paint(Graphics2D graphics2D) {
            graphics2D.setColor(color);
            graphics2D.drawOval(positionable.getCoord().x - halfSize, positionable.getCoord().y - halfSize, size, size);
        }
    }

    static class GObject implements IObject {

        private IPaintable paintable;
        private IUpdatable updatable;

        public GObject(IPaintable paintable, IUpdatable updatable) {
            this.paintable = paintable;
            this.updatable = updatable;
        }

        @Override
        public void update(int deltaMillis, float deltaSecond) {
            updatable.update(deltaMillis, deltaSecond);
        }

        @Override
        public void paint(Graphics2D graphics2D) {
            paintable.paint(graphics2D);
        }
    }

    static class GraphicsPanel extends JPanel implements Runnable {

        private static DecimalFormat decimalFormat = new DecimalFormat(".00");

        private List<IObject> objects = new ArrayList<IObject>();
        private int spfMillis;
        private long lastUpdateMillis = -1;
        private float deltaSecond;

        public GraphicsPanel(Dimension dimension, int fps) {
            synchronized (this) {
                this.spfMillis = 1000 / fps;
                setPreferredSize(dimension);
                Point min = new Point(0, 0);
                Point max = new Point(dimension.width, dimension.height);
                for (int i = 0; i < 50; i++) {
                    RandomMove move = new RandomMove(min, max);
                    addObjects(new GObject(i % 2 == 0 ? new Quad(move) : new Circle(move), move));
                }
            }
            new Thread(this).start();
        }

        private void addObjects(IObject object) {
            if (object == null) {
                return;
            }
            objects.add(object);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D graphics2D = (Graphics2D) g;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.drawString("FPS:" + decimalFormat.format(1f/deltaSecond), 10, 20);
            for (IObject obj : objects) {
                obj.paint(graphics2D);
            }
        }
        
        @Override
        public void run() {
            lastUpdateMillis = System.currentTimeMillis();

            while (true) {
                int deltaMillis = (int) (System.currentTimeMillis() - lastUpdateMillis);
                deltaSecond = deltaMillis / 1000f;
                lastUpdateMillis = System.currentTimeMillis();

                for (IObject obj : objects) {
                    obj.update(deltaMillis, deltaSecond);
                }
                repaint();

                try {
                    Thread.sleep(spfMillis);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("graphics");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new GraphicsPanel(new Dimension(1280, 720), 60), BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
