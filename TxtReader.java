// TxtReader.java
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;

// 数据模型类
class Chapter {
    private String title;
    private int startLine;
    
    public Chapter(String title, int startLine) {
        this.title = title;
        this.startLine = startLine;
    }
    
    public String getTitle() { return title; }
    public int getStartLine() { return startLine; }
    
    @Override
    public String toString() {
        return title;
    }
}

class Bookmark {
    private String name;
    private int lineNumber;
    private Date createTime;
    
    public Bookmark(String name, int lineNumber, Date createTime) {
        this.name = name;
        this.lineNumber = lineNumber;
        this.createTime = createTime;
    }
    
    public String getName() { return name; }
    public int getLineNumber() { return lineNumber; }
    public Date getCreateTime() { return createTime; }
    
    @Override
    public String toString() {
        return name + " (第" + (lineNumber + 1) + "行)";
    }
}

class LibraryBook {
    private String name;
    private String filePath;
    private Date addTime;
    
    public LibraryBook(String name, String filePath) {
        this.name = name;
        this.filePath = filePath;
        this.addTime = new Date();
    }
    
    public String getName() { return name; }
    public String getFilePath() { return filePath; }
    public Date getAddTime() { return addTime; }
    
    public void setName(String name) { this.name = name; }
}

// 简化的表格模型基类
abstract class SimpleTableModel {
    protected void fireTableRowsInserted(int firstRow, int lastRow) {
        // 简化实现
    }
    
    protected void fireTableRowsDeleted(int firstRow, int lastRow) {
        // 简化实现
    }
    
    public abstract int getRowCount();
    public abstract int getColumnCount();
    public abstract String getColumnName(int column);
    public abstract Object getValueAt(int row, int column);
}

// 书签表格模型
class BookmarkTableModel extends SimpleTableModel {
    private ArrayList<Bookmark> bookmarks;
    private String[] columnNames = {"书签名称", "位置", "创建时间"};
    
    public BookmarkTableModel() {
        bookmarks = new ArrayList<>();
    }
    
    public void addBookmark(Bookmark bookmark) {
        bookmarks.add(bookmark);
        fireTableRowsInserted(bookmarks.size() - 1, bookmarks.size() - 1);
    }
    
    public void removeBookmark(int row) {
        bookmarks.remove(row);
        fireTableRowsDeleted(row, row);
    }
    
    public Bookmark getBookmarkAt(int row) {
        return bookmarks.get(row);
    }
    
    public void clear() {
        int size = bookmarks.size();
        bookmarks.clear();
        fireTableRowsDeleted(0, size - 1);
    }
    
    public int getRowCount() {
        return bookmarks.size();
    }
    
    public int getColumnCount() {
        return columnNames.length;
    }
    
    public String getColumnName(int column) {
        return columnNames[column];
    }
    
    public Object getValueAt(int row, int column) {
        Bookmark bookmark = bookmarks.get(row);
        switch (column) {
            case 0: return bookmark.getName();
            case 1: return "第" + (bookmark.getLineNumber() + 1) + "行";
            case 2: return bookmark.getCreateTime().toString();
            default: return null;
        }
    }
}

// 书库表格模型
class LibraryTableModel extends SimpleTableModel {
    private ArrayList<LibraryBook> books;
    private String[] columnNames = {"书名", "文件路径", "添加时间"};
    
    public LibraryTableModel() {
        books = new ArrayList<>();
    }
    
    public void addBook(LibraryBook book) {
        books.add(book);
        fireTableRowsInserted(books.size() - 1, books.size() - 1);
    }
    
    public void removeBook(int row) {
        books.remove(row);
        fireTableRowsDeleted(row, row);
    }
    
    public LibraryBook getBookAt(int row) {
        return books.get(row);
    }
    
    public void clear() {
        int size = books.size();
        books.clear();
        fireTableRowsDeleted(0, size - 1);
    }
    
    public int getRowCount() {
        return books.size();
    }
    
    public int getColumnCount() {
        return columnNames.length;
    }
    
    public String getColumnName(int column) {
        return columnNames[column];
    }
    
    public Object getValueAt(int row, int column) {
        LibraryBook book = books.get(row);
        switch (column) {
            case 0: return book.getName();
            case 1: return book.getFilePath();
            case 2: return book.getAddTime().toString();
            default: return null;
        }
    }
}

// 自定义表格类
class SimpleTable extends JTable {
    private SimpleTableModel model;
    
    public SimpleTable(SimpleTableModel model) {
        super();
        this.model = model;
    }
    
    public int getRowCount() {
        return model.getRowCount();
    }
    
    public int getColumnCount() {
        return model.getColumnCount();
    }
    
    public String getColumnName(int column) {
        return model.getColumnName(column);
    }
    
    public Object getValueAt(int row, int column) {
        return model.getValueAt(row, column);
    }
}

// 书签管理对话框
class BookmarkManagerDialog extends JDialog {
    private SimpleTable bookmarkTable;
    private BookmarkTableModel tableModel;
    private HashMap<String, Bookmark> bookmarks;
    
    public BookmarkManagerDialog(JFrame parent, HashMap<String, Bookmark> bookmarks) {
        super(parent, "书签管理", true);
        this.bookmarks = bookmarks;
        initComponents();
        pack();
        setSize(500, 300);
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        tableModel = new BookmarkTableModel();
        bookmarkTable = new SimpleTable(tableModel);
        bookmarkTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(bookmarkTable);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton deleteButton = new JButton("删除书签");
        JButton closeButton = new JButton("关闭");
        
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteBookmark();
            }
        });
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        loadBookmarks();
    }
    
    private void loadBookmarks() {
        tableModel.clear();
        for (Bookmark bookmark : bookmarks.values()) {
            tableModel.addBookmark(bookmark);
        }
    }
    
    private void deleteBookmark() {
        int selectedRow = bookmarkTable.getSelectedRow();
        if (selectedRow >= 0) {
            Bookmark bookmark = tableModel.getBookmarkAt(selectedRow);
            bookmarks.values().remove(bookmark);
            tableModel.removeBookmark(selectedRow);
        }
    }
}

// 书库管理对话框
class LibraryManagerDialog extends JDialog {
    private SimpleTable libraryTable;
    private LibraryTableModel tableModel;
    private LibraryManager libraryManager;
    private JButton openButton, renameButton, deleteButton;
    
    public LibraryManagerDialog(JFrame parent, LibraryManager libraryManager) {
        super(parent, "书库管理", true);
        this.libraryManager = libraryManager;
        initComponents();
        pack();
        setSize(500, 400);
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        tableModel = new LibraryTableModel();
        libraryTable = new SimpleTable(tableModel);
        libraryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(libraryTable);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        openButton = new JButton("打开");
        renameButton = new JButton("重命名");
        deleteButton = new JButton("删除");
        JButton closeButton = new JButton("关闭");
        
        openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openBook();
            }
        });
        renameButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                renameBook();
            }
        });
        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteBook();
            }
        });
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        buttonPanel.add(openButton);
        buttonPanel.add(renameButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        loadLibrary();
        
        libraryTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateButtonStates();
            }
        });
        updateButtonStates();
    }
    
    private void loadLibrary() {
        tableModel.clear();
        for (LibraryBook book : libraryManager.getAllBooks()) {
            tableModel.addBook(book);
        }
    }
    
    private void updateButtonStates() {
        boolean hasSelection = libraryTable.getSelectedRow() >= 0;
        openButton.setEnabled(hasSelection);
        renameButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
    }
    
    private void openBook() {
        int selectedRow = libraryTable.getSelectedRow();
        if (selectedRow >= 0) {
            LibraryBook book = tableModel.getBookAt(selectedRow);
            libraryManager.openBook(book);
            dispose();
        }
    }
    
    private void renameBook() {
        int selectedRow = libraryTable.getSelectedRow();
        if (selectedRow >= 0) {
            LibraryBook book = tableModel.getBookAt(selectedRow);
            String newName = JOptionPane.showInputDialog(this, "请输入新名称:", book.getName());
            if (newName != null && !newName.trim().isEmpty()) {
                libraryManager.updateBook(book.getName(), newName.trim());
                loadLibrary();
            }
        }
    }
    
    private void deleteBook() {
        int selectedRow = libraryTable.getSelectedRow();
        if (selectedRow >= 0) {
            LibraryBook book = tableModel.getBookAt(selectedRow);
            int result = JOptionPane.showConfirmDialog(this, 
                "确定要删除《" + book.getName() + "》吗？", "确认删除", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                libraryManager.removeBook(book.getName());
                tableModel.removeBook(selectedRow);
            }
        }
    }
}

// 书库管理类
class LibraryManager {
    private HashMap<String, LibraryBook> books;
    private Preferences prefs;
    private static final String LIBRARY_KEY = "book_library";
    
    public LibraryManager() {
        prefs = Preferences.userNodeForPackage(LibraryManager.class);
        loadLibrary();
    }
    
    public void addBook(String name, String filePath) {
        books.put(name, new LibraryBook(name, filePath));
        saveLibrary();
    }
    
    public void removeBook(String name) {
        books.remove(name);
        saveLibrary();
    }
    
    public void updateBook(String oldName, String newName) {
        LibraryBook book = books.get(oldName);
        if (book != null) {
            books.remove(oldName);
            book.setName(newName);
            books.put(newName, book);
            saveLibrary();
        }
    }
    
    public ArrayList<LibraryBook> getAllBooks() {
        return new ArrayList<>(books.values());
    }
    
    public void openBook(LibraryBook book) {
        File file = new File(book.getFilePath());
        if (file.exists()) {
            // 在实际实现中，需要通过回调或事件通知主程序
            System.out.println("打开书籍: " + book.getName() + ", 路径: " + book.getFilePath());
        }
    }
    
    public void backupLibrary() {
        try {
            String backupDir = System.getProperty("user.home") + File.separator + "txt_reader_backup";
            Files.createDirectories(Paths.get(backupDir));
            
            String backupFile = backupDir + File.separator + "library_backup_" + 
                System.currentTimeMillis() + ".txt";
            
            try (PrintWriter writer = new PrintWriter(backupFile, "UTF-8")) {
                for (LibraryBook book : books.values()) {
                    writer.println(book.getName() + "|" + book.getFilePath() + "|" + book.getAddTime().getTime());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadLibrary() {
        books = new HashMap<>();
        try {
            String libraryData = prefs.get(LIBRARY_KEY, "");
            if (!libraryData.isEmpty()) {
                String[] entries = libraryData.split(";;");
                for (String entry : entries) {
                    String[] parts = entry.split("\\|");
                    if (parts.length >= 2) {
                        books.put(parts[0], new LibraryBook(parts[0], parts[1]));
                    }
                }
            }
        } catch (Exception e) {
            books = new HashMap<>();
        }
    }
    
    private void saveLibrary() {
        StringBuilder sb = new StringBuilder();
        for (LibraryBook book : books.values()) {
            sb.append(book.getName()).append("|")
              .append(book.getFilePath()).append("|")
              .append(book.getAddTime().getTime())
              .append(";;");
        }
        prefs.put(LIBRARY_KEY, sb.toString());
    }
}

// 主程序
public class TxtReader extends JFrame {
    private JTree chapterTree;
    private DefaultTreeModel treeModel;
    private JTextArea textArea;
    private JScrollPane textScrollPane;
    private JSplitPane mainSplitPane;
    private JPanel leftPanel;
    private JButton toggleDirButton;
    private JButton addBookmarkButton;
    private JButton manageLibraryButton;
    
    private File currentFile;
    private ArrayList<String> lines;
    private ArrayList<Chapter> chapters;
    private HashMap<String, Bookmark> bookmarks;
    private LibraryManager libraryManager;
    private Preferences prefs;
    
    private static final String CHAPTER_PATTERN = "^第[零一二三四五六七八九十百千\\d]+[章节回].*";
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    
    public TxtReader() {
        initComponents();
        loadPreferences();
        libraryManager = new LibraryManager();
        bookmarks = new HashMap<>();
    }
    
    private void initComponents() {
        setTitle("TXT文本阅读器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        
        createMenuBar();
        createMainLayout();
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                savePreferences();
            }
        });
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("文件");
        JMenuItem openFileItem = new JMenuItem("打开本地文件");
        JMenuItem openUrlItem = new JMenuItem("打开网络文件");
        JMenuItem exitItem = new JMenuItem("退出");
        
        openFileItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });
        openUrlItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openFromUrl();
            }
        });
        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        
        fileMenu.add(openFileItem);
        fileMenu.add(openUrlItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        JMenu bookmarkMenu = new JMenu("书签");
        JMenuItem addBookmarkItem = new JMenuItem("添加书签");
        JMenuItem manageBookmarksItem = new JMenuItem("管理书签");
        
        addBookmarkItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addBookmark();
            }
        });
        manageBookmarksItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manageBookmarks();
            }
        });
        
        bookmarkMenu.add(addBookmarkItem);
        bookmarkMenu.add(manageBookmarksItem);
        
        JMenu libraryMenu = new JMenu("书库");
        JMenuItem addToLibraryItem = new JMenuItem("添加到书库");
        JMenuItem manageLibraryItem = new JMenuItem("管理书库");
        JMenuItem backupLibraryItem = new JMenuItem("备份书库");
        
        addToLibraryItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addCurrentToLibrary();
            }
        });
        manageLibraryItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manageLibrary();
            }
        });
        backupLibraryItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                backupLibrary();
            }
        });
        
        libraryMenu.add(addToLibraryItem);
        libraryMenu.add(manageLibraryItem);
        libraryMenu.add(backupLibraryItem);
        
        menuBar.add(fileMenu);
        menuBar.add(bookmarkMenu);
        menuBar.add(libraryMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void createMainLayout() {
        leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(250, 0));
        
        JLabel dirLabel = new JLabel("目录");
        dirLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        
        toggleDirButton = new JButton("隐藏目录");
        toggleDirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleDirectory();
            }
        });
        
        JPanel dirHeaderPanel = new JPanel(new BorderLayout());
        dirHeaderPanel.add(dirLabel, BorderLayout.WEST);
        dirHeaderPanel.add(toggleDirButton, BorderLayout.EAST);
        
        treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("目录"));
        chapterTree = new JTree(treeModel);
        chapterTree.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        chapterTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath path = e.getPath();
                if (path != null) {
                    Object lastComponent = path.getLastPathComponent();
                    if (lastComponent instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) lastComponent;
                        Object userObject = node.getUserObject();
                        if (userObject instanceof Chapter) {
                            Chapter chapter = (Chapter) userObject;
                            jumpToPosition(chapter.getStartLine());
                        }
                    }
                }
            }
        });
        
        JScrollPane treeScrollPane = new JScrollPane(chapterTree);
        
        leftPanel.add(dirHeaderPanel, BorderLayout.NORTH);
        leftPanel.add(treeScrollPane, BorderLayout.CENTER);
        
        textArea = new JTextArea();
        textArea.setFont(new Font("宋体", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        
        textScrollPane = new JScrollPane(textArea);
        textScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                updateCurrentPosition();
            }
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addBookmarkButton = new JButton("添加书签");
        manageLibraryButton = new JButton("管理书库");
        
        addBookmarkButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addBookmark();
            }
        });
        manageLibraryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manageLibrary();
            }
        });
        
        buttonPanel.add(addBookmarkButton);
        buttonPanel.add(manageLibraryButton);
        
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, textScrollPane);
        mainSplitPane.setDividerLocation(250);
        mainSplitPane.setResizeWeight(0);
        
        setLayout(new BorderLayout());
        add(mainSplitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("文本文件 (*.txt)", "txt"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            loadFile(currentFile);
        }
    }
    
    private void openFromUrl() {
        String url = JOptionPane.showInputDialog(this, "请输入TXT文件的URL:", "打开网络文件", JOptionPane.QUESTION_MESSAGE);
        if (url != null && !url.trim().isEmpty()) {
            loadFromUrl(url.trim());
        }
    }
    
    private void loadFile(File file) {
        try {
            lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
            textArea.setText(String.join("\n", lines));
            extractChapters();
            loadBookmarks();
            applyBookmark();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "读取文件失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadFromUrl(String urlStr) {
        try {
            URL url = new URI(urlStr).toURL();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                textArea.setText(String.join("\n", lines));
                extractChapters();
                bookmarks.clear();
                updateChapterTree();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "加载网络文件失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void extractChapters() {
        chapters = new ArrayList<>();
        if (lines == null) return;
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.matches(CHAPTER_PATTERN)) {
                chapters.add(new Chapter(line, i));
            }
        }
        updateChapterTree();
    }
    
    private void updateChapterTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("章节列表");
        
        for (Chapter chapter : chapters) {
            DefaultMutableTreeNode chapterNode = new DefaultMutableTreeNode(chapter);
            root.add(chapterNode);
        }
        
        treeModel.setRoot(root);
        chapterTree.expandRow(0);
    }
    
    private void jumpToPosition(int lineNumber) {
        try {
            int position = 0;
            for (int i = 0; i < lineNumber && i < lines.size(); i++) {
                position += lines.get(i).length() + 1; // +1 for newline
            }
            textArea.setCaretPosition(Math.min(position, textArea.getText().length()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void toggleDirectory() {
        if (leftPanel.isVisible()) {
            leftPanel.setVisible(false);
            toggleDirButton.setText("显示目录");
            mainSplitPane.setDividerLocation(0);
        } else {
            leftPanel.setVisible(true);
            toggleDirButton.setText("隐藏目录");
            mainSplitPane.setDividerLocation(250);
        }
    }
    
    private void addBookmark() {
        if (currentFile == null && lines == null) {
            JOptionPane.showMessageDialog(this, "请先打开文件", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String bookmarkName = JOptionPane.showInputDialog(this, "请输入书签名称:", "添加书签", JOptionPane.QUESTION_MESSAGE);
        if (bookmarkName != null && !bookmarkName.trim().isEmpty()) {
            int currentLine = getCurrentLine();
            String key = getBookmarkKey();
            bookmarks.put(key, new Bookmark(bookmarkName.trim(), currentLine, new Date()));
            saveBookmarks();
            JOptionPane.showMessageDialog(this, "书签添加成功", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private int getCurrentLine() {
        int caretPosition = textArea.getCaretPosition();
        String text = textArea.getText();
        int line = 0;
        int currentPos = 0;
        
        for (int i = 0; i < lines.size() && currentPos < caretPosition; i++) {
            currentPos += lines.get(i).length() + 1;
            line = i;
        }
        
        return line;
    }
    
    private void updateCurrentPosition() {
        // 可以在这里实现当前位置显示
    }
    
    private String getBookmarkKey() {
        if (currentFile != null) {
            return currentFile.getAbsolutePath();
        } else {
            return "url_" + textArea.getText().hashCode();
        }
    }
    
    private void loadBookmarks() {
        bookmarks = new HashMap<>();
        // 简化实现：在实际应用中，可以从文件加载书签
    }
    
    private void saveBookmarks() {
        // 简化实现：在实际应用中，可以保存书签到文件
    }
    
    private void applyBookmark() {
        String key = getBookmarkKey();
        Bookmark bookmark = bookmarks.get(key);
        if (bookmark != null) {
            jumpToPosition(bookmark.getLineNumber());
        }
    }
    
    private void manageBookmarks() {
        new BookmarkManagerDialog(this, bookmarks).setVisible(true);
    }
    
    private void addCurrentToLibrary() {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(this, "请先打开本地文件", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String libraryName = JOptionPane.showInputDialog(this, "请输入书库名称:", "添加到书库", JOptionPane.QUESTION_MESSAGE);
        if (libraryName != null && !libraryName.trim().isEmpty()) {
            libraryManager.addBook(libraryName.trim(), currentFile.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "已添加到书库", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void manageLibrary() {
        new LibraryManagerDialog(this, libraryManager).setVisible(true);
    }
    
    private void backupLibrary() {
        libraryManager.backupLibrary();
        JOptionPane.showMessageDialog(this, "书库备份完成", "成功", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void loadPreferences() {
        prefs = Preferences.userNodeForPackage(TxtReader.class);
        // 加载窗口设置
    }
    
    private void savePreferences() {
        // 保存程序设置
    }
    
    public static void main(String[] args) {
        // 使用默认外观，不设置特定外观
        try {
            // 尝试设置系统外观
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // 如果设置失败，使用默认外观
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new TxtReader().setVisible(true);
            }
        });
    }
}