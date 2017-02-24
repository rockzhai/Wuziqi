package com.rockzhai.ai;


/**
 *  主界面和业务逻辑
*/

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.Exception;
import java.lang.Math;
import java.lang.SecurityException;
import java.lang.String;
import java.lang.System;
import java.lang.Thread;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileFilter;

public final class DrawingPanel extends FileFilter
    implements ActionListener, MouseMotionListener,  WindowListener {   
    // 常量
    public static final String HEADLESS_PROPERTY   = "my.headless";
    public static final String MULTIPLE_PROPERTY   =  "my.multiple";
    public static final String SAVE_PROPERTY       = "my.save";
    private static final String TITLE              = "五子棋";
    private static final Color GRID_LINE_COLOR     = new Color(64, 64, 64, 128);
    private static final int GRID_SIZE             = 10;      // 10px 网格间距
    private static final int DELAY                 = 100;     // 延时delay between repaints in millis
    private static final int MAX_SIZE              = 10000;   // max width/height
    private static final boolean DEBUG             = true; 	  // DeBug 开关
    private static final boolean SAVE_SCALED_IMAGES = true;   // true panel放大缩小时, 保留放大状态下的图片
    private static int instances = 0;
    private static Thread shutdownThread = null;
    

    private static boolean hasProperty(String name) {
        try {
            return System.getProperty(name) != null;
        } catch (SecurityException e) {
        	// 读值异常
            if (DEBUG) System.out.println("Security exception when trying to read " + name);
            return false;
        }
    }
    
    // 返回主线程是否在运行 main is active
    private static boolean mainIsActive() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        int activeCount = group.activeCount();
        
        // 在线程组中寻找主线程
        Thread[] threads = new Thread[activeCount];
        group.enumerate(threads);
        for (int i = 0; i < threads.length; i++) {
            Thread thread = threads[i];
            String name = ("" + thread.getName()).toLowerCase();
            if (name.indexOf("main") >= 0 || 
                name.indexOf("testrunner-assignmentrunner") >= 0) {
                // 找到主线程
                // (TestRunnerApplet's main runner also counts as "main" thread)
                return thread.isAlive();
            }
        }
        
        // 没有找到主线程
        return false;
    }
    
    // 自定义一个ImagePanel
    private class ImagePanel extends JPanel {
        private static final long serialVersionUID = 0;
        private Image image;
        
        public ImagePanel(Image image) {
            setImage(image);
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(image.getWidth(this), image.getHeight(this)));
            setAlignmentX(0.0f);
        }
        
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            if (currentZoom != 1) {
                g2.scale(currentZoom, currentZoom);
            }
            g2.drawImage(image, 0, 0, this);
            
            // 为了调试方便加入的网格线
            if (gridLines) {
                g2.setPaint(GRID_LINE_COLOR);
                for (int row = 1; row <= getHeight() / GRID_SIZE; row++) {
                    g2.drawLine(0, row * GRID_SIZE, getWidth(), row * GRID_SIZE);
                }
                for (int col = 1; col <= getWidth() / GRID_SIZE; col++) {
                    g2.drawLine(col * GRID_SIZE, 0, col * GRID_SIZE, getHeight());
                }
            }
        }
        
        public void setImage(Image image) {
            this.image = image;
            repaint();
        }
    }

    // 控件
    private int width, height;             // 窗口 frame 的大小
    private JFrame frame;                  // 总窗口的 frame
    private JPanel panel;                  // 总的画布面板
    private ImagePanel imagePanel;         // 真正的绘画面板
    private BufferedImage image;           // 记录绘图的情况
    private Graphics2D g2;                 // 2D绘图 graphics context
    private JLabel statusBar;              // 状态栏显示鼠标移动的位置
    private JFileChooser chooser;          // 保存选项 file chooser
    private Timer timer;                   // 绘制的动画时间
    private Color backgroundColor = Color.WHITE;
    private boolean PRETTY = true;         // 消除锯齿操作true to anti-alias
    private boolean gridLines = false;		//是否网格线
    private int currentZoom = 1;
    private int initialPixel;              // 初始化每个像素点
    
    // 根据width和height绘制一个panel
    public DrawingPanel(int width, int height) {
        if (width < 0 || width > MAX_SIZE || height < 0 || height > MAX_SIZE) {
            throw new IllegalArgumentException("Illegal width/height: " + width + " x " + height);
        }
        //synchronized保证在同一时刻最多只有一个线程执行该段代码       
        synchronized (getClass()) {
            instances++;
            if (shutdownThread == null) {
                shutdownThread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            while (true) {
                                //完成执行主线程已经挂掉
                                if ((instances == 0 || shouldSave()) && !mainIsActive()) {
                                    try {
                                        System.exit(0);
                                    } catch (SecurityException sex) {}
                                }

                                Thread.sleep(250);
                            }
                        } catch (Exception e) {}
                    }
                });
                shutdownThread.setPriority(Thread.MIN_PRIORITY);
                shutdownThread.start();
            }
        }
        this.width = width;
        this.height = height;
        
        if (DEBUG) System.out.println("w=" + width + ",h=" + height +  ",graph=" + isGraphical() + ",save=" + shouldSave());
        
        if (shouldSave()) {
            // 图像不能超过256中颜色
            image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED);
            PRETTY = false;   // 关闭抗锯齿，节省调色板颜色
            
            // 用初始化的背景色填充frame，因为它不会透明显示ARGB图像
            Graphics g = image.getGraphics();
            g.setColor(backgroundColor);
            // 加上1，防止width或height为0
            g.fillRect(0, 0, width + 1, height + 1);
        } else {
        	//ARGB
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        initialPixel = image.getRGB(0, 0);        
        g2 = (Graphics2D) image.getGraphics();
        g2.setColor(Color.BLACK);
        if (PRETTY) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
            
        if (isGraphical()) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            
            statusBar = new JLabel(" ");
            statusBar.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            panel.setBackground(backgroundColor);
            panel.setPreferredSize(new Dimension(width, height));
            imagePanel = new ImagePanel(image);
            imagePanel.setBackground(backgroundColor);
            panel.add(imagePanel);
            
            // 监听鼠标事件
            panel.addMouseMotionListener(this);
            
            // 主界面窗格
            frame = new JFrame(TITLE);
            frame.addWindowListener(this);
            JScrollPane center = new JScrollPane(panel);
            frame.getContentPane().add(center);
            frame.getContentPane().add(statusBar, "South");
            frame.setBackground(Color.WHITE);

            // 菜单栏
            setupMenuBar();
            
            frame.pack();
            center(frame);
            frame.setVisible(true);
            if (!shouldSave()) {
                toFront(frame);
            }        
            // 重绘update
            timer = new Timer(DELAY, this);
            timer.start();
        }
    }
    
    // 文件保存格式可以为png和gif
    public boolean accept(File file) {
        return file.isDirectory() ||
            (file.getName().toLowerCase().endsWith(".png") || 
             file.getName().toLowerCase().endsWith(".gif"));
    }
    
    //初始化UI组件
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof Timer) {
            // 重绘
            panel.repaint();
        } else if (e.getActionCommand().equals("退出")) {
            exit();
        } else if (e.getActionCommand().equals("保留截图")) {
            saveAs();
        } else if (e.getActionCommand().equals("放大")) {
            zoom(currentZoom + 1);
        } else if (e.getActionCommand().equals("缩小")) {
            zoom(currentZoom - 1);
        } else if (e.getActionCommand().equals("正常大小 (100%)")) {
            zoom(1);
        } else if (e.getActionCommand().equals("调试网格线")) {
            setGridLines(((JCheckBoxMenuItem) e.getSource()).isSelected());
        } else if (e.getActionCommand().equals("关于")) {
            JOptionPane.showMessageDialog(frame,
                    "五子棋\n" + 
                    "技术涉及：\n" +
                    "Alpha-Beta剪枝算法\n" +
                    "博弈树\n" +
                    "Swing业务逻辑实现\n" +
                    "\n"+"--阿宅（http：//imzhai.com)",
                   
                    "关于\n",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    public void addKeyListener(KeyListener listener) {
        frame.addKeyListener(listener);
    }
    
    public void addMouseListener(MouseListener listener) {
        panel.addMouseListener(listener);
    }
    
    public void addMouseListener(MouseMotionListener listener) {
        panel.addMouseMotionListener(listener);
    }
    
    public void addMouseMotionListener(MouseMotionListener listener) {
        panel.addMouseMotionListener(listener);
    }
    
    public void addMouseListener(MouseInputListener listener) {
        panel.addMouseListener(listener);
        panel.addMouseMotionListener(listener);
    }
    
    // 清除所有的线/颜色
    public void clear() {
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = initialPixel;
        }
        image.setRGB(0, 0, width, height, pixels, 0, 1);
    }
  
    // 文件格式png或者gif
    public String getDescription() {
        return "Image files (*.png; *.gif)";
    }
    
    // 获得Graphics2D对象
    public Graphics2D getGraphics() {
        return g2;
    }
    
    // 返回Height
    public int getHeight() {
        return height;
    }
     
    // 用Dimension对象返回width和height
    public Dimension getSize() {
        return new Dimension(width, height);
    }
    
    // 返回Width
    public int getWidth() {
        return width;
    }
    
    // 返回目前的缩放倍数
    public int getZoom() {
        return currentZoom;
    }
    
    // 监听鼠标行为并将坐标显示在statusbar上
    public void mouseDragged(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {
        int x = e.getX() / currentZoom;
        int y = e.getY() / currentZoom;
        setStatusBarText("(" + x + ", " + y + ")");
    }
    
   
    // 保存文件image
    public void save(String filename) throws IOException {
        BufferedImage image2 = getImage();
        
        // 如果缩放了，恢复再保存
        if (SAVE_SCALED_IMAGES && currentZoom != 1) {
            BufferedImage zoomedImage = new BufferedImage(width * currentZoom, height * currentZoom, image.getType());
            Graphics2D g = (Graphics2D) zoomedImage.getGraphics();
            g.setColor(Color.BLACK);
            if (PRETTY) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            }
            g.scale(currentZoom, currentZoom);
            g.drawImage(image2, 0, 0, imagePanel);
            image2 = zoomedImage;
        }
        int lastDot = filename.lastIndexOf(".");
        String extension = filename.substring(lastDot + 1);
        ImageIO.write(image2, extension, new File(filename));
    }  
    // 设置背景颜色
    public void setBackground(Color c) {
        backgroundColor = c;
        if (isGraphical()) {
            panel.setBackground(c);
            imagePanel.setBackground(c);
        }
    }
    
    // 图像的顶部网格线的绘图帮助
    // 使用调试尺寸和坐标
    public void setGridLines(boolean gridLines) {
        this.gridLines = gridLines;
        imagePanel.repaint();
    }
    
    // 通过给定值height 程序必须再次调用getGraphics()，重新获取上下文来进行绘图
    public void setHeight(int height) {
        setSize(getWidth(), height);
    }
     
    public void setSize(int width, int height) {
        // 替换绘图的BufferedImage
        BufferedImage newImage = new BufferedImage(width, height, image.getType());
        imagePanel.setImage(newImage);
        newImage.getGraphics().drawImage(image, 0, 0, imagePanel);
        this.width = width;
        this.height = height;
        image = newImage;
        g2 = (Graphics2D) newImage.getGraphics();
        g2.setColor(Color.BLACK);
        if (PRETTY) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        zoom(currentZoom);
        if (isGraphical()) {
            frame.pack();
        }
    }
    
    // frame可见不可见
    public void setVisible(boolean visible) {
        if (isGraphical()) {
            frame.setVisible(visible);
        }
    }
    
    // 设置窗口最前（强迫症）=-=..
    public void toFront() {
        toFront(frame);
    }
    
    // 关闭，退出
    public void windowClosing(WindowEvent event) {
        frame.setVisible(false);
        synchronized (getClass()) {
            instances--;
        }
        frame.dispose();
    }
    
    // 实现WindowListener必须的方法（这些方法目前未使用）
    public void windowActivated(WindowEvent event) {}
    public void windowClosed(WindowEvent event) {}
    public void windowDeactivated(WindowEvent event) {}
    public void windowDeiconified(WindowEvent event) {}
    public void windowIconified(WindowEvent event) {}
    public void windowOpened(WindowEvent event) {}

    // 根据factor进行放大缩小
    // factor >= 1
    public void zoom(int zoomFactor) {
        currentZoom = Math.max(1, zoomFactor);
        if (isGraphical()) {
            Dimension size = new Dimension(width * currentZoom, height * currentZoom);
            imagePanel.setPreferredSize(size);
            panel.setPreferredSize(size);
            imagePanel.validate();
            imagePanel.revalidate();
            panel.validate();
            panel.revalidate();
            frame.getContentPane().validate();
            imagePanel.repaint();
            setStatusBarText(" ");
            // resize
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            if (size.width <= screen.width || size.height <= screen.height) {
                frame.pack();
            }
        }
    }
    
    // 把主窗口放到屏幕中间
    private void center(Window frame) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screen = tk.getScreenSize();
        
        int x = Math.max(0, (screen.width - frame.getWidth()) / 2);
        int y = Math.max(0, (screen.height - frame.getHeight()) / 2);
        frame.setLocation(x, y);
    }   
    // 如果有必要，构造并初始化JFileChooser对象
    private void checkChooser() {
        if (chooser == null) {
            // TODO: fix security on applet mode
            chooser = new JFileChooser(System.getProperty("user.dir"));
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileFilter(this);
        }
    }
        
    // 退出程序
    private void exit() {
        if (isGraphical()) {
            frame.setVisible(false);
            frame.dispose();
        }
        try {
            System.exit(0);
        } catch (SecurityException e) {
        }
    }
  
    //获取image
    private BufferedImage getImage() {
        BufferedImage image2;    
            image2 = new BufferedImage(width, height, image.getType());
        Graphics g = image2.getGraphics();
        if (DEBUG) System.out.println("getImage setting background to " + backgroundColor);
        g.setColor(backgroundColor);
        g.fillRect(0, 0, width, height);
        g.drawImage(image, 0, 0, panel);
        return image2;
    }
    
    private boolean isGraphical() {
        return !hasProperty(SAVE_PROPERTY) && !hasProperty(HEADLESS_PROPERTY);
    }
    
    // 点击保存图片的时候调用
    private void saveAs() {
        String filename = saveAsHelper("png");
        if (filename != null) {
            try {
                save(filename);  // 保存
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Unable to save image:\n" + ex);
            }
        }
    }
    
    private String saveAsHelper(String extension) {
        // 使用文件选择对话框，获得文件名和保存格式
        checkChooser();
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return null;
        }     
        File selectedFile = chooser.getSelectedFile();
        String filename = selectedFile.toString();
        if (!filename.toLowerCase().endsWith(extension)) {
            // =-=..丫的不加.都不行，调半天还以为出bug了，windows太傻逼了！！！
            filename += "." + extension;
        }

        // 如果有，是否覆盖
        if (new File(filename).exists() && JOptionPane.showConfirmDialog(
                frame, "文件存在.  是否Overwrite?", "Overwrite?",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return null;
        }

        return filename;
    }
    
    // 底部的状态栏显示放大倍数
    private void setStatusBarText(String text) {
        if (currentZoom != 1) {
            text += " (current zoom: " + currentZoom + "x" + ")";
        }
        statusBar.setText(text);
    }
    
    // 初始化UI控件
    private void setupMenuBar() {
        boolean secure = (System.getSecurityManager() != null);
        
        JMenuItem saveAs = new JMenuItem("保留截图", 'A');
        saveAs.addActionListener(this);
        saveAs.setAccelerator(KeyStroke.getKeyStroke("ctrl S"));
        saveAs.setEnabled(!secure);
        
        
        JMenuItem zoomIn = new JMenuItem("放大", 'I');
        zoomIn.addActionListener(this);
        zoomIn.setAccelerator(KeyStroke.getKeyStroke("ctrl EQUALS"));
        
        JMenuItem zoomOut = new JMenuItem("缩小", 'O');
        zoomOut.addActionListener(this);
        zoomOut.setAccelerator(KeyStroke.getKeyStroke("ctrl MINUS"));
        
        JMenuItem zoomNormal = new JMenuItem("正常大小 (100%)", 'N');
        zoomNormal.addActionListener(this);
        zoomNormal.setAccelerator(KeyStroke.getKeyStroke("ctrl 0"));
        
        JMenuItem gridLinesItem = new JCheckBoxMenuItem("调试网格线");
        gridLinesItem.setMnemonic('G');
        gridLinesItem.addActionListener(this);
        gridLinesItem.setAccelerator(KeyStroke.getKeyStroke("ctrl G"));
        
        JMenuItem exit = new JMenuItem("退出", 'x');
        exit.addActionListener(this);
        
        JMenuItem about = new JMenuItem("关于", 'A');
        about.addActionListener(this);
        
        JMenu file = new JMenu("File");
        file.setMnemonic('F');
        file.addSeparator();
        file.add(saveAs);
        file.addSeparator();
        file.add(exit);
        
        JMenu view = new JMenu("View");
        view.setMnemonic('V');
        view.add(zoomIn);
        view.add(zoomOut);
        view.add(zoomNormal);
        view.addSeparator();
        view.add(gridLinesItem);
        
        JMenu help = new JMenu("Help");
        help.setMnemonic('H');
        help.add(about);
        
        JMenuBar bar = new JMenuBar();
        bar.add(file);
        bar.add(view);
        bar.add(help);
        frame.setJMenuBar(bar);
    }  
    private boolean shouldSave() {
        return hasProperty(SAVE_PROPERTY);
    }
    
    // 窗口放到最前（置顶）
    private void toFront(final Window window) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (window != null) {
                    window.toFront();
                    window.repaint();
                }
            }
        });
    }

}

