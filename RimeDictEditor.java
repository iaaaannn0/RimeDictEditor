import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.regex.*;

public class RimeDictEditor {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton openButton;
    private JButton saveButton;
    private JButton addButton;
    private JButton deleteButton;
    private File currentFile;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RimeDictEditor().createAndShowGUI());
    }

    public void createAndShowGUI() {
        frame = new JFrame("Rime 词典编辑器");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600);
        initComponents();
        frame.setVisible(true);
    }

    private void initComponents() {
        // 创建表格模型
        String[] columnNames = {"词语 (中文)", "拼音 (小写字母)", "权重"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        // 创建带滚动条的表格
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.setShowGrid(true);
        table.setGridColor(new Color(220, 220, 220));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setIntercellSpacing(new Dimension(10, 5));

        // 设置列宽
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(150);
        columnModel.getColumn(1).setPreferredWidth(250);
        columnModel.getColumn(2).setPreferredWidth(80);

        // 设置单元格渲染器
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);

        // 输入验证
        setupCellValidators();

        // 创建带滚动条的面板
        JScrollPane scrollPane = new JScrollPane(table,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建按钮，设置字体为黑色，背景色为默认
        openButton = createStyledButton("打开", e -> openFile());
        saveButton = createStyledButton("保存", e -> saveFile());
        addButton = createStyledButton("添加", e -> addNewRow());
        deleteButton = createStyledButton("删除", e -> deleteSelectedRow());

        buttonPanel.add(openButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);

        // 主布局
        frame.setLayout(new BorderLayout());
        frame.add(buttonPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(100, 35));
        button.setFont(new Font("微软雅黑", Font.BOLD, 14));
        button.setBackground(UIManager.getColor("Button.background"));  // Default background
        button.setForeground(Color.BLACK);  // Set font color to black
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(button.getBackground().darker(), 1),
            new EmptyBorder(5, 15, 5, 15)
        ));
        
        // 鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(button.getBackground().brighter());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(UIManager.getColor("Button.background")); // Reset to default
            }
        });
        button.addActionListener(listener);
        return button;
    }

    private void setupCellValidators() {
        // 中文列验证
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean stopCellEditing() {
                String value = ((JTextField)getComponent()).getText().trim();
                if (!value.matches("[\\u4e00-\\u9fa5]+")) {
                    showError("只能输入中文汉字");
                    return false;
                }
                return super.stopCellEditing();
            }
        });

        // 拼音列验证
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean stopCellEditing() {
                String value = ((JTextField)getComponent()).getText().toLowerCase().trim();
                if (!value.matches("[a-z\\s]+")) {
                    showError("只能输入小写字母和空格");
                    return false;
                }
                ((JTextField)getComponent()).setText(value);
                return super.stopCellEditing();
            }
        });

        // 权重列验证
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean stopCellEditing() {
                String value = ((JTextField)getComponent()).getText().trim();
                if (!value.matches("\\d+")) {
                    showError("权重必须为数字");
                    return false;
                }
                return super.stopCellEditing();
            }
        });
    }

    private void addNewRow() {
        tableModel.addRow(new Object[]{"新词条", "pinyin", "10000"});
        int newRow = tableModel.getRowCount() - 1;
        table.setRowSelectionInterval(newRow, newRow);
        table.scrollRectToVisible(table.getCellRect(newRow, 0, true));
    }

    private void deleteSelectedRow() {
        int row = table.getSelectedRow();
        if (row != -1 && !table.isEditing()) {
            tableModel.removeRow(row);
        }
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser(new File("/Users/yxxiao/Library/Rime/dicts/"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".dict.yaml") || f.isDirectory();
            }
            public String getDescription() {
                return "Rime 字典文件 (*.dict.yaml)";
            }
        });

        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            loadFileContent();
        }
    }

    private void loadFileContent() {
        tableModel.setRowCount(0);
        try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue;
                
                String[] parts = line.split("\t");
                if (parts.length == 3) {
                    tableModel.addRow(new Object[]{
                        parts[0], 
                        parts[1].replace(' ', ' '), // 保留原始空格
                        parts[2]
                    });
                }
            }
            frame.setTitle("Rime 词典编辑器 - " + currentFile.getName());
        } catch (IOException e) {
            showError("文件打开失败: " + e.getMessage());
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            saveAsFile();
            return;
        }

        if (!validateData()) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
            // 写入文件头
            writer.write("# Rime dictionary\n");
            writer.write("# encoding: utf-8\n\n");
            writer.write("---\nname: custom_simple\nversion: \"2024.1.0\"\nsort: by_weight\n...\n\n");

            // 写入数据
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String word = (String) tableModel.getValueAt(i, 0);
                String pinyin = (String) tableModel.getValueAt(i, 1);
                String weight = (String) tableModel.getValueAt(i, 2);
                writer.write(String.join("\t", word, pinyin, weight) + "\n");
            }
            showMessage("保存成功: " + currentFile.getName());
        } catch (IOException e) {
            showError("保存失败: " + e.getMessage());
        }
    }

    private void saveAsFile() {
        JFileChooser fileChooser = new JFileChooser(new File("/Users/yxxiao/Library/Rime/dicts/"));
        fileChooser.setDialogTitle("另存为");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".dict.yaml") || f.isDirectory();
            }
            public String getDescription() {
                return "Rime 字典文件 (*.dict.yaml)";
            }
        });

        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            currentFile = selectedFile.getName().endsWith(".dict.yaml") ? 
                selectedFile : 
                new File(selectedFile.getAbsolutePath() + ".dict.yaml");
            
            if (currentFile.exists()) {
                int choice = JOptionPane.showConfirmDialog(
                    frame,
                    "文件已存在，是否覆盖？",
                    "确认覆盖",
                    JOptionPane.YES_NO_OPTION
                );
                if (choice != JOptionPane.YES_OPTION) return;
            }
            saveFile();
        }
    }

    private boolean validateData() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String word = (String) tableModel.getValueAt(i, 0);
            String pinyin = (String) tableModel.getValueAt(i, 1);
            String weight = (String) tableModel.getValueAt(i, 2);

            if (word == null || word.trim().isEmpty()) {
                showError("第 " + (i+1) + " 行: 词语不能为空");
                return false;
            }
            if (!word.matches("[\\u4e00-\\u9fa5]+")) {
                showError("第 " + (i+1) + " 行: 只能包含中文");
                return false;
            }

            if (pinyin == null || pinyin.trim().isEmpty()) {
                showError("第 " + (i+1) + " 行: 拼音不能为空");
                return false;
            }
            if (!pinyin.matches("[a-z\\s]+")) {
                showError("第 " + (i+1) + " 行: 只能包含小写字母和空格");
                return false;
            }

            if (weight == null || weight.trim().isEmpty()) {
                showError("第 " + (i+1) + " 行: 权重不能为空");
                return false;
            }
            if (!weight.matches("\\d+")) {
                showError("第 " + (i+1) + " 行: 权重必须为数字");
                return false;
            }
        }
        return true;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(frame, message, "操作成功", JOptionPane.INFORMATION_MESSAGE);
    }
}
