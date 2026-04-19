import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.table.DefaultTableCellRenderer;

public class GUI extends JFrame {
    //表格
    private JLabel clockLabel;//时钟标签
    private JTable backupQueueTable;//后备队列表
    private JTable memoryTable;//内存表
    private JTable readyQueue1Table;//就绪队列一
    private JTable readyQueue2Table;//就绪队列二
    private JTable runningTable;//运行作业表
    private JTable inputBlockTable;//输入阻塞
    private JTable outputBlockTable;//输出阻塞
    private JTextArea logTextArea;    // 调度过程显示
    private JScrollPane logScroll;    // 滚动面板

    // 按钮
    private JButton startButton;//执行
    private JButton pauseButton;//暂停
    private JButton saveButton;//保存
    private JButton realtimeButton;//实时

    // 表格模型
    private DefaultTableModel backupModel;//后背
    private DefaultTableModel memoryModel;//内存
    private JPanel memoryGridPanel;   // 位示图
    private DefaultTableModel ready1Model;//就绪队列一
    private DefaultTableModel ready2Model;//就绪队列二
    private DefaultTableModel runningModel;//运行
    private DefaultTableModel inputBlockModel;//输入阻塞
    private DefaultTableModel outputBlockModel;//输出堵塞

    // 控制标志
    private volatile boolean isRunning = false;//是否正在运行
    private volatile boolean isPaused = false;//是否暂停

    // 日志收集
    private StringBuilder logBuffer = new StringBuilder();

    //GUI
    public GUI() {
        setTitle("操作系统课程设计-B");
        setSize(1400, 950);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        Log.setGui(this);//注册gui实例到日志
        initComponents();//初始化组件
        startUpdateThread();//更新线程
    }

    //初始化组件
    private void initComponents() {
        setLayout(new BorderLayout());

        //顶部按钮栏
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        buttonPanel.setBackground(new Color(240, 240, 240));
        buttonPanel.setPreferredSize(new Dimension(1400, 70));
        Font buttonFont = new Font("微软雅黑", Font.BOLD, 15);
        //按钮
        startButton = createButton("执行", new Color(16, 165, 16), buttonFont);
        pauseButton = createButton("暂停", new Color(174, 133, 16), buttonFont);
        saveButton = createButton("保存", new Color(19, 103, 186), buttonFont);
        realtimeButton = createButton("实时", new Color(255, 103, 207), buttonFont);
        //事件监听
        startButton.addActionListener(e -> startSimulation());
        pauseButton.addActionListener(e -> pauseSimulation());
        saveButton.addActionListener(e -> saveLog());
        realtimeButton.addActionListener(e -> createRealtimeJob());
        buttonPanel.add(startButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(realtimeButton);
        add(buttonPanel, BorderLayout.NORTH);

        //中央内容面板
        JPanel bigPanel=new JPanel(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);

        // 时钟面板
        JPanel upPanel=new JPanel(new GridLayout(1,5));
        JPanel clockPanel = new JPanel();
        clockPanel.setBackground(Color.BLACK);
        JLabel clockTitle = new JLabel("时钟：");
        clockTitle.setForeground(Color.WHITE);
        clockTitle.setFont(new Font("微软雅黑", Font.BOLD, 20));
        clockLabel = new JLabel("0 秒");
        clockLabel.setForeground(Color.WHITE);
        clockLabel.setFont(new Font("微软雅黑", Font.BOLD, 28));
        clockPanel.add(clockTitle);
        clockPanel.add(clockLabel);
        upPanel.add(clockPanel);
        upPanel.add(new JPanel());
        upPanel.add(new JPanel());
        upPanel.add(new JPanel());
        upPanel.add(new JPanel());
        contentPanel.add(upPanel);

        // 作业请求区
        JPanel backupPanel = new JPanel(new BorderLayout());
        backupPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "作业请求区",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                Color.BLACK
        ));
        backupPanel.setPreferredSize(new Dimension(0, 100));
        backupPanel.setBackground(Color.WHITE);
        JScrollPane backupScrollPane = new JScrollPane(createBackupQueueTable());
        backupScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        backupScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        backupPanel.add(backupScrollPane, BorderLayout.CENTER);
        contentPanel.add(backupPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // 用户内存区
        JPanel memoryPanel = new JPanel(new BorderLayout());
        memoryPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "用户内存区",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                Color.BLACK
        ));
        memoryPanel.setPreferredSize(new Dimension(0, 200));
        JPanel splitMemoryPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        splitMemoryPanel.setBackground(Color.WHITE);

        // 位示图
        memoryGridPanel = new JPanel(new GridLayout(5, 10));
        memoryGridPanel.setBackground(Color.WHITE);
        for (int i = 0; i < 50; i++) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            cell.setBackground(Color.WHITE);
            memoryGridPanel.add(cell);
        }
        splitMemoryPanel.add(memoryGridPanel);

        // 地址变化
        JScrollPane memoryTableScrollPane = new JScrollPane(createMemoryTable());
        memoryTableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        memoryTableScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        splitMemoryPanel.add(memoryTableScrollPane);

        memoryPanel.add(splitMemoryPanel, BorderLayout.CENTER);
        contentPanel.add(memoryPanel);

        //进程就绪区
        JPanel readyPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        readyPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "进程就绪区",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                Color.BLACK
        ));
        readyPanel.setPreferredSize(new Dimension(1400, 180));
        readyPanel.setBackground(Color.WHITE);
        JPanel ready1Panel = createQueueSubPanel("一级就绪队列(时间片1s)", createReadyQueue1Table());
        JPanel ready2Panel = createQueueSubPanel("二级就绪队列(时间片2s)", createReadyQueue2Table());
        readyPanel.add(ready1Panel);
        readyPanel.add(ready2Panel);
        contentPanel.add(readyPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // 进程运行区
        JPanel runningPanel = new JPanel(new BorderLayout());
        runningPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "进程运行区",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                Color.BLACK
        ));
        runningPanel.setPreferredSize(new Dimension(0, 100));
        runningPanel.setBackground(Color.WHITE);
        JScrollPane runningScrollPane = new JScrollPane(createRunningTable());
        runningScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        runningScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        runningPanel.add(runningScrollPane, BorderLayout.CENTER);
        contentPanel.add(runningPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // 进程阻塞区
        JPanel blockPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        blockPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "进程阻塞区",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                Color.BLACK
        ));
        blockPanel.setPreferredSize(new Dimension(1400, 180));
        blockPanel.setBackground(Color.WHITE);
        JPanel inputBlockPanel = createQueueSubPanel("键盘输入阻塞队列", createInputBlockTable());
        JPanel outputBlockPanel = createQueueSubPanel("屏幕输出阻塞队列", createOutputBlockTable());
        blockPanel.add(inputBlockPanel);
        blockPanel.add(outputBlockPanel);
        contentPanel.add(blockPanel);

        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setPreferredSize(new Dimension(280, 0));
        logPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "作业请求与进程调度全过程",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                new Color(0,0,0)
        ));
        logTextArea = new JTextArea();
        //logTextArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        //logTextArea.setLineWrap(false);
        logScroll = new JScrollPane(logTextArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        bigPanel.add(contentPanel,BorderLayout.CENTER);
        bigPanel.add(logPanel,BorderLayout.EAST);

        add(bigPanel, BorderLayout.CENTER);


    }

    //创建按钮
    private JButton createButton(String text, Color bgColor, Font font) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(100, 40));
        return button;
    }

    //带子标题的面板显示表格
    private JPanel createQueueSubPanel(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                title,
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 12),
                Color.DARK_GRAY
        ));
        panel.setBackground(Color.WHITE);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    // 后备队列表
    private JTable createBackupQueueTable() {
        String[] columns = {"作业 ID", "到达时间", "指令数量", "需要A", "需要B", "状态"};
        backupModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        backupQueueTable = new JTable(backupModel);
        backupQueueTable.setRowHeight(25);
        backupQueueTable.setSelectionBackground(new Color(255, 255, 200));
        return backupQueueTable;
    }

    // 内存表
    private JTable createMemoryTable() {
        String[] columns = {"起始地址", "大小 (B)", "状态", "作业 ID"};
        memoryModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        memoryTable = new JTable(memoryModel);
        memoryTable.setRowHeight(30);
        memoryTable.setDefaultRenderer(Object.class, new MemoryCellRenderer());
        return memoryTable;
    }

    // 一级就绪队列表
    private JTable createReadyQueue1Table() {
        String[] columns = {"进入时间", "进程 ID", "作业 ID", "优先级", "剩余时间片", "未执行指令"};
        ready1Model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        readyQueue1Table = new JTable(ready1Model);
        readyQueue1Table.setRowHeight(25);
        readyQueue1Table.setSelectionBackground(new Color(200, 255, 200));
        return readyQueue1Table;
    }
    // 二级就绪队列表
    private JTable createReadyQueue2Table() {
        String[] columns = {"进入时间", "进程 ID", "作业 ID", "优先级", "剩余时间片", "未执行指令"};
        ready2Model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        readyQueue2Table = new JTable(ready2Model);
        readyQueue2Table.setRowHeight(25);
        readyQueue2Table.setSelectionBackground(new Color(200, 255, 200));
        return readyQueue2Table;
    }

    // 运行区表
    private JTable createRunningTable() {
        String[] columns = {"进入运行时间", "作业 ID", "进程 ID", "指令编号", "优先级", "剩余指令数"};
        runningModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        runningTable = new JTable(runningModel);
        runningTable.setRowHeight(25);
        runningTable.setSelectionBackground(new Color(150, 255, 150));
        return runningTable;
    }

    // 输入阻塞表
    private JTable createInputBlockTable() {
        String[] columns = {"进入阻塞时间", "进程 ID", "作业 ID", "未执行指令", "状态"};
        inputBlockModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        inputBlockTable = new JTable(inputBlockModel);
        inputBlockTable.setRowHeight(25);
        inputBlockTable.setDefaultRenderer(Object.class, new BlockCellRenderer());
        return inputBlockTable;
    }

    // 输出阻塞表
    private JTable createOutputBlockTable() {
        String[] columns = {"进入阻塞时间", "进程 ID", "作业 ID", "未执行指令", "状态"};
        outputBlockModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        outputBlockTable = new JTable(outputBlockModel);
        outputBlockTable.setRowHeight(25);
        outputBlockTable.setDefaultRenderer(Object.class, new BlockCellRenderer());
        return outputBlockTable;
    }

    //更新页面
    public void updateInterface() {
        if (!isRunning) return;
        SwingUtilities.invokeLater(() -> {
            // 更新时钟
            clockLabel.setText(ClockAndJobSchedulingThread.simulationTime + " 秒");
            SyncManager.knlock.lock();
            try {
                //作业请求区
                backupModel.setRowCount(0);
                for (Object[] row : OSKernel.backupHistory) {
                    backupModel.addRow(row);
                }
                JScrollPane backupScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, backupQueueTable);
                if (backupScroll != null) {
                    JScrollBar vertical = backupScroll.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
                //后备队列队头高亮显示
                Job headJob = OSKernel.backupQueue.peek();
                backupQueueTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                                                                   boolean isSelected, boolean hasFocus, int row, int column) {
                        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        int jobId = (int) table.getValueAt(row, 0);
                        Job headJob = OSKernel.backupQueue.peek();
                        boolean isHead = (headJob != null && headJob.getJobId() == jobId);
                        if (isHead) {
                            c.setBackground(new Color(255, 255, 200));
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                        c.setForeground(Color.BLACK);
                        return c;
                    }
                });

                //内存区
                memoryModel.setRowCount(0);
                if (OSKernel.memory != null) {
                    for (Memory.Block block : OSKernel.memory.getBlocks()) {
                        memoryModel.addRow(new Object[]{
                                block.start,
                                block.size,
                                block.free ? "空闲" : "已分配",
                                block.free ? "-" : getJobIdByAddress(block.start)
                        });
                    }
                }
                Component[] cells = memoryGridPanel.getComponents();
                for (int i = 0; i < cells.length; i++) {
                    JPanel cell = (JPanel) cells[i];
                    cell.setBackground(Color.WHITE);
                    cell.removeAll();
                    cell.repaint();
                }
                if (OSKernel.memory != null) {
                    for (Memory.Block block : OSKernel.memory.getBlocks()) {
                        if (!block.free) {
                            int startAddr = block.start;
                            int endAddr = startAddr + block.size - 1;
                            int startBlock = startAddr / 100;
                            int endBlock = endAddr / 100;
                            int jobId = getJobIdByAddress(startAddr);
                            Color color = new Color((jobId * 50) % 200 + 50,
                                    (jobId * 70) % 200 + 50,
                                    (jobId * 90) % 200 + 50);
                            for (int i = startBlock; i <= endBlock && i < 50; i++) {
                                if (i < cells.length) {
                                    JPanel cell = (JPanel) cells[i];
                                    cells[i].setBackground(color);
                                    JLabel label = new JLabel(String.valueOf(jobId));
                                    label.setFont(new Font("微软雅黑", Font.BOLD, 12));
                                    label.setHorizontalAlignment(SwingConstants.CENTER);
                                    cell.add(label);
                                }
                            }
                        }
                    }
                }
                memoryGridPanel.revalidate();
                memoryGridPanel.repaint();

                //一级就绪队列
                ready1Model.setRowCount(0);
                for (Object[] row : OSKernel.readyQueue1History) {
                    ready1Model.addRow(row);
                }
                JScrollPane readyqueue1Scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, readyQueue1Table);
                if (readyqueue1Scroll != null) {
                    JScrollBar vertical = readyqueue1Scroll.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
                //每个进程的最新进入时间
                Map<Integer, Integer> lastTimeMap1 = new HashMap<>();
                for (Object[] row : OSKernel.readyQueue1History) {
                    int pid = (int) row[1];
                    int time = (int) row[0];
                    Integer old = lastTimeMap1.get(pid);
                    if (old == null || time > old) {
                        lastTimeMap1.put(pid, time);
                    }
                }
                //高亮一级队列中最近一次进入的进程
                readyQueue1Table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                                                                   boolean isSelected, boolean hasFocus, int row, int column) {
                        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        int pid = (int) table.getValueAt(row, 1);
                        int time = (int) table.getValueAt(row, 0);
                        boolean isCurrent = false;
                        // 检查该进程是否在当前队列中
                        for (PCB pcb : OSKernel.readyQueue1) {
                            if (pcb.getPid() == pid) {
                                // 进一步检查该行是否是该进程的最新进入记录
                                if (lastTimeMap1.get(pid) == time) {
                                    isCurrent = true;
                                }
                                break;
                            }
                        }
                        if (isCurrent) {
                            c.setBackground(new Color(255, 255, 200));
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                        return c;
                    }
                });
                //二级就绪队列
                ready2Model.setRowCount(0);
                for (Object[] row : OSKernel.readyQueue2History) {
                    ready2Model.addRow(row);
                }
                JScrollPane readyqueue2Scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, readyQueue2Table);
                if (readyqueue2Scroll != null) {
                    JScrollBar vertical = readyqueue2Scroll.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
                Map<Integer, Integer> lastTimeMap2 = new HashMap<>();
                for (Object[] row : OSKernel.readyQueue2History) {
                    int pid = (int) row[1];
                    int time = (int) row[0];
                    Integer old = lastTimeMap2.get(pid);
                    if (old == null || time > old) {
                        lastTimeMap2.put(pid, time);
                    }
                }
                //显示二级队列中等待的进程
                readyQueue2Table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                                                                   boolean isSelected, boolean hasFocus, int row, int column) {
                        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        int pid = (int) table.getValueAt(row, 1);
                        int time = (int) table.getValueAt(row, 0);
                        boolean isCurrent = false;
                        for (PCB pcb : OSKernel.readyQueue2) {
                            if (pcb.getPid() == pid) {
                                if (lastTimeMap2.get(pid) == time) {
                                    isCurrent = true;
                                }
                                break;
                            }
                        }
                        if (isCurrent) {
                            c.setBackground(new Color(255, 255, 200));
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                        return c;
                    }
                });

                //输入阻塞队列
                inputBlockModel.setRowCount(0);
                for (Object[] row : OSKernel.inputBlockHistory) {
                    inputBlockModel.addRow(row);
                }
                JScrollPane inputscroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, inputBlockTable);
                if (inputscroll != null) {
                    JScrollBar vertical = inputscroll.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
                inputBlockTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                                                                   boolean isSelected, boolean hasFocus, int row, int column) {
                        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        String state = (String) table.getValueAt(row, 4); // 状态列
                        if ("唤醒".equals(state)) {
                            c.setBackground(new Color(100, 150, 255));
                            c.setForeground(Color.WHITE);
                        } else {
                            int pid = (int) table.getValueAt(row, 1);
                            int time = (int) table.getValueAt(row, 0);
                            boolean isCurrentBlock = false;
                            for (PCB pcb : OSKernel.inputblockQueue) {
                                if (pcb.getPid() == pid && pcb.getBlockedSince() == time) {
                                    isCurrentBlock = true;
                                    break;
                                }
                            }
                            if (isCurrentBlock) {
                                c.setBackground(new Color(255, 200, 200));
                            } else {
                                c.setBackground(Color.WHITE);
                            }
                            c.setForeground(Color.BLACK);
                        }
                        return c;
                    }
                });

                //输出阻塞队列
                outputBlockModel.setRowCount(0);
                for (Object[] row : OSKernel.outputBlockHistory) {
                    outputBlockModel.addRow(row);
                }
                JScrollPane outputScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, outputBlockTable);
                if (outputScroll != null) {
                    JScrollBar vertical = outputScroll.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
                outputBlockTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                                                                   boolean isSelected, boolean hasFocus, int row, int column) {
                        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        String state = (String) table.getValueAt(row, 4);
                        if ("唤醒".equals(state)) {
                            c.setBackground(new Color(100, 150, 255));
                            c.setForeground(Color.WHITE);
                        } else {
                            int pid = (int) table.getValueAt(row, 1);
                            int time = (int) table.getValueAt(row, 0);
                            boolean isCurrentBlock = false;
                            for (PCB pcb : OSKernel.outputblockQueue) {
                                if (pcb.getPid() == pid && pcb.getBlockedSince() == time) {
                                    isCurrentBlock = true;
                                    break;
                                }
                            }
                            if (isCurrentBlock) {
                                c.setBackground(new Color(255, 200, 200));
                            } else {
                                c.setBackground(Color.WHITE);
                            }
                            c.setForeground(Color.BLACK);
                        }
                        return c;
                    }
                });

                //运行区
                runningModel.setRowCount(0);
                for (Object[] row : OSKernel.runningHistory) {
                    runningModel.addRow(row);
                }
                JScrollPane runningScroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, runningTable);
                if (runningScroll != null) {
                    JScrollBar vertical = runningScroll.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
                runningTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                                                                   boolean isSelected, boolean hasFocus, int row, int column) {
                        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        int pid = (int) table.getValueAt(row, 2);     // 进程ID列索引
                        int time = (int) table.getValueAt(row, 0);    // 时间列索引
                        boolean isCurrentRun = (OSKernel.currentRunningProcess != null &&
                                OSKernel.currentRunningProcess.getPid() == pid &&
                                OSKernel.currentRunningProcess.getLastRunTime() == time);
                        if (isCurrentRun) {
                            c.setBackground(new Color(150, 255, 150));
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                        return c;
                    }
                });

            } finally {
                SyncManager.knlock.unlock();
            }
        });
        // 日志自动滚动到底部
        SwingUtilities.invokeLater(() -> {
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }
    //通过地址查找作业ID
    private int getJobIdByAddress(int address) {
        for (PCB pcb : OSKernel.allPCBs) {
            if (pcb.getBaseAddress() == address) {
                return pcb.getJobId();
            }
        }
        return -1;
    }

    // 执行按钮
    private void startSimulation() {
        isRunning = true;
        isPaused = false;
        startButton.setEnabled(false);
        pauseButton.setEnabled(true);
        pauseButton.setText("暂停");
        ClockAndJobSchedulingThread.startSimulation();
        Log.log(ClockAndJobSchedulingThread.simulationTime + ":[开始执行]");
        addLog(ClockAndJobSchedulingThread.simulationTime + ":[开始执行]");
    }

    // 暂停/继续按钮
    private void pauseSimulation() {
        if (isRunning) {
            isPaused = !isPaused;
            if (isPaused) {
                pauseButton.setText("继续");
                ClockAndJobSchedulingThread.pauseSimulation();
                Log.log(ClockAndJobSchedulingThread.simulationTime + ":[暂停]");
                addLog(ClockAndJobSchedulingThread.simulationTime + ":[暂停]");
            } else {
                pauseButton.setText("暂停");
                ClockAndJobSchedulingThread.resumeSimulation();
                Log.log(ClockAndJobSchedulingThread.simulationTime + ":[继续]");
                addLog(ClockAndJobSchedulingThread.simulationTime + ":[继续]");
            }
        }
    }

    // 保存日志
    private void saveLog() {
        try {
            String fileName = "output/ProcessResults-" + ClockAndJobSchedulingThread.simulationTime + "-B.txt";
            Log.savelog(fileName);
            JOptionPane.showMessageDialog(this, "日志已保存到：" + fileName, "保存成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "保存失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 实时作业
    private void createRealtimeJob() {
        int currentTime = ClockAndJobSchedulingThread.simulationTime;
        int jobId = OSKernel.allJobs.size() + 1;

        // 生成指令序列
        List<Instruction> instructions = new ArrayList<>();
        int calcCount = 0, inputCount = 0, outputCount = 0;
        for (int i = 1; i <= 20; i++) {
            int state;
            if (inputCount < 3 && i >= 6 && i <= 8) {
                state = 1;
                inputCount++;
            } else if (outputCount < 2 && i >= 19) {
                state = 2;
                outputCount++;
            } else {
                state = 0;
                calcCount++;
            }
            instructions.add(new Instruction(i, state));
        }

        // 创建作业（5参数构造函数：jobId, inTime, priority, needA, needB, instructionCount）
        Job realtimeJob = new Job(jobId, currentTime,  0, 3,2, 20);
        realtimeJob.setInstructions(instructions);
        OSKernel.allJobs.add(realtimeJob);
        OSKernel.backupQueue.offer(realtimeJob);


        Log.log(currentTime + ":[新增实时作业：作业ID=" + jobId + ",指令数量=20]");

        // 保存实时作业文件
        try {
            File outputDir = new File("output");
            if (!outputDir.exists()) outputDir.mkdirs();
            String fileName = "output/" + currentTime + ".txt";
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            for (Instruction inst : instructions) {
                writer.write(inst.getId() + "," + inst.getState() + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JOptionPane.showMessageDialog(this, "实时作业已创建：作业 ID=" + jobId + "\n指令：15 计算 + 3 输入 + 2 输出", "实时作业", JOptionPane.INFORMATION_MESSAGE);
    }

    // 定时刷新界面
    private void startUpdateThread() {
        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(200);
                    if (isRunning && !isPaused) {
                        updateInterface();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    // 内存块渲染
    class MemoryCellRenderer extends JLabel implements TableCellRenderer {
        public MemoryCellRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (column == 2) { // 状态列
                if ("空闲".equals(value)) {
                    setBackground(Color.WHITE);
                    setForeground(Color.BLACK);
                } else {
                    int jobId = getJobIdFromTable(table, row);
                    Color color = new Color((jobId * 50) % 200 + 50,
                            (jobId * 70) % 200 + 50,
                            (jobId * 90) % 200 + 50);
                    setBackground(color);
                    setForeground(Color.WHITE);
                }
            } else {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
            }
            setText(value == null ? "" : value.toString());
            return this;
        }

        private int getJobIdFromTable(JTable table, int row) {
            Object value = table.getValueAt(row, 3);
            if (value != null && !"-".equals(value.toString())) {
                try {
                    return Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
            return 1;
        }
    }

    // 阻塞队列渲染
    class BlockCellRenderer extends JLabel implements TableCellRenderer {
        public BlockCellRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (column == 4 && "已唤醒".equals(value)) {
                setBackground(new Color(100, 150, 255));
                setForeground(Color.WHITE);
            } else {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
            }
            setText(value == null ? "" : value.toString());
            return this;
        }
    }

    public void addLog(String log) {
        logBuffer.append(log).append("\n");
        // 输出到右侧面板 + 自动滚动到底部
        if (logTextArea != null) {
            logTextArea.append(log + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        }
    }
    public static GUI instance = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            instance = new GUI();
            instance.setVisible(true);
        });
    }
}