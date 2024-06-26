package client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class Display extends JFrame {
    private JButton recordButton;
    private JButton sendButton;
    private JList<String> recordingList;
    private DefaultListModel<String> listModel;
    private boolean isRecording;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String user;
    private Socket mainSocket;
    private Socket notifySocket;
    private static final int PANEL_WIDTH = 200;
    private static final int LIST_ITEM_HEIGHT = 50;
    private static final int HORIZONTAL_MARGIN = 20;

    public Display(Socket mainSocket, Socket notifySocket, String usr, DataInputStream dis, DataOutputStream dos) {
        super("Voice Board");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 500);
        setLocationRelativeTo(null);

        this.dis = dis;
        this.dos = dos;
        this.user = usr;
        this.mainSocket = mainSocket;
        this.notifySocket = notifySocket;

        initComponents();

        setVisible(true);
    }

    private void initComponents() {
        isRecording = false;
        // リストモデルとリスト
        listModel = new DefaultListModel<>();
        recordingList = new JList<>(listModel);
        recordingList.setCellRenderer(new BubbleListCellRenderer(PANEL_WIDTH, HORIZONTAL_MARGIN));
        recordingList.setFixedCellHeight(LIST_ITEM_HEIGHT);
        JScrollPane scrollPane = new JScrollPane(recordingList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scrollPane.getViewport().setBackground(recordingList.getBackground());

        // ボタンパネル
        JPanel buttonPanel = new JPanel(new FlowLayout());
        recordButton = new JButton("録音");
        sendButton = new JButton("送信");
        sendButton.setEnabled(false);
        buttonPanel.add(recordButton);
        buttonPanel.add(sendButton);

        // メインパネル
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER); // 中央にリストを配置
        mainPanel.add(buttonPanel, BorderLayout.SOUTH); // ボタンパネルを下部に配置
        add(mainPanel);

        // ボタン動作
        recordButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleRecordButton();
                // 録音処理を実装
            }
        });
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listModel.addElement("" + (listModel.getSize() + 1) + ": " + user);
                sendButton.setEnabled(false);
                // 送信処理を実装
                ClientFunc.sendFile(dis, dos, user, "./client/audio.wav");
                sendButton.setEnabled(false);
                isRecording = false;
                reloadComponents(); // 再読み込み
            }
        });

        recordingList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = recordingList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String selectedValue = recordingList.getModel().getElementAt(selectedIndex);
                    System.out.println("再生を開始しました: " + selectedValue);
                    // 再生処理を実装
                    new Thread(() -> {
                        try {
                            String[] parts = selectedValue.split(":");
                            if (parts.length > 0) {
                                String id = parts[0].trim();
                                String filePath = "./client/music/" + id + ".wav"; // IDを使ってファイルパスを指定
                                Path path = Paths.get(filePath);
                                if (Files.exists(path)) {
                                    ClientFunc.playWAV(filePath);
                                } else {
                                    ClientFunc.getFile(dis, dos, Integer.parseInt(id));
                                    ClientFunc.playWAV(filePath);
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                }
            }
        });

        // チャットデータをロードしてリストに表示
        new Thread(() -> loadChatData()).start();

        // ウィンドウリスナー(ウィンドウが閉じられたときにソケットを閉じる)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    mainSocket.close();
                    notifySocket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void loadChatData() {
        ArrayList<HashMap<String, String>> chatData = ClientFunc.getChat(dis, dos);
        SwingUtilities.invokeLater(() -> {
            for (HashMap<String, String> chat : chatData) {
                String entry = chat.get("id") + ": " + chat.get("usr"); // あとで編集
                listModel.addElement(entry);
            }
        });
    }

    private void handleRecordButton() {
        if (!isRecording) {
            startRecording();
        }
    }

    private void startRecording() {
        isRecording = true;
        sendButton.setEnabled(false);
        recordButton.setText("録音中...");
        // 録音処理を実装
        System.out.println("録音を開始しました");
        // ClientFuncを呼び出す
        // 録音処理をメインスレッドで実行
        ClientFunc.makeWAV("./client/audio.wav");

        // 録音処理が終わった後にUIを更新
        recordButton.setText("録音");
        isRecording = false;
        sendButton.setEnabled(true);
        System.out.println("録音を終了しました");
    }

    public void reloadComponents() {
        // コンポーネントの再初期化
        getContentPane().removeAll();
        initComponents();
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        // テスト用
    }
}
