package application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import javax.swing.JButton;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.validator.routines.InetAddressValidator;

public class SampleController {
    @FXML
    private ChoiceBox agreement;
    @FXML
    private ChoiceBox timeUnit;
    @FXML
    private ChoiceBox sendEncoding;
    @FXML
    private ChoiceBox receiveEncoding;
    @FXML
    private ChoiceBox localIP;
    @FXML
    private Button openBtn;
    @FXML
    private Button sendBtn;
    @FXML
    private Button closeBtn;
    @FXML
    private TextField portField;
    @FXML
    private TextField remoteIP;
    @FXML
    private TextArea recvTextArea;
    @FXML
    private TextArea sendTextArea;
    @FXML
    private CheckBox autoRecvCheckBox;
    @FXML
    private Label fileimport;
    @FXML
    private Label ipLabel;
    @FXML
    private Label portLabel;
    @FXML
    private ChoiceBox contentAgreement;
    @FXML
    private Button contentAgreementConfig;

    private String[] agreementArray = new String[]{"TCP Server", "TCP Client"};
    private String[] timeUnitArray = new String[]{"毫秒", "秒"};
    private String[] encodingArray = new String[]{"UTF-8", "HEX"};
    private String[] contentAgreementArray = new String[]{"无协议", "协议一", "协议二", "协议三"};

    private static final InetAddressValidator VALIDATOR = InetAddressValidator.getInstance();
    public static boolean isValidIPV4ByValidator(String inetAddress) {
        return VALIDATOR.isValidInet4Address(inetAddress);
    }

    private OutputStream outputStream;
    private ServerSocket serverSocket;

    public void init(Scene scene) {
        agreement.getItems().addAll(agreementArray);
        agreement.getSelectionModel().select(0);

        timeUnit.getItems().addAll(timeUnitArray);
        timeUnit.getSelectionModel().select(0);

        sendEncoding.getItems().addAll(encodingArray);
        sendEncoding.getSelectionModel().select(0);

        receiveEncoding.getItems().addAll(encodingArray);
        receiveEncoding.getSelectionModel().select(0);

        contentAgreement.getItems().addAll(contentAgreementArray);
        contentAgreement.getSelectionModel().select(0);

        try {
            localIP.getItems().addAll(getLocalIp());
            localIP.getSelectionModel().select(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        openBtn.setOnAction(actionEvent -> {
            if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[1]) && !isValidIPV4ByValidator(remoteIP.getText())) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("请填写正确的服务器IP地址！");
                alert.showAndWait();
                return;
            }
            if (portField.getText().trim().length() == 0) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("请填写端口号！");
                alert.showAndWait();
                return;
            }
            openBtn.setDisable(true);
            closeBtn.setDisable(false);
            portField.setEditable(false);
            agreement.setDisable(true);
            sendBtn.setDisable(false);
            localIP.setDisable(true);
            remoteIP.setDisable(true);

            if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[0])) {
                MyServer myServer = new MyServer();
                myServer.start();
            } else if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[1])) {
                MyClient myClient = new MyClient();
                myClient.start();
            }
        });

        closeBtn.setOnAction(actionEvent -> {
            openBtn.setDisable(false);
            closeBtn.setDisable(true);
            portField.setEditable(true);
            agreement.setDisable(false);
            sendBtn.setDisable(true);
            localIP.setDisable(false);
            remoteIP.setDisable(false);

            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        sendBtn.setOnAction(actionEvent -> {
            String str = sendTextArea.getText();
            try {
                outputStream.write(str.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        portField.setOnKeyTyped(e -> {
            String character = e.getCharacter();
            if (!character.matches("\\d")) {
                e.consume();// 如果不是数字则取消
            }
        });

        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File("./")); // 设置初始路径，默认为我的电脑
        chooser.setTitle("打开文件"); // 设置窗口标题，默认为“打开”
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("TXT", "*.txt"),
                new FileChooser.ExtensionFilter("LOG", "*.log"));

        fileimport.setOnMouseReleased(e -> {
            Stage stage = (Stage) scene.getWindow();
            chooser.showOpenDialog(stage);
        });

        agreement.setOnAction(e -> getChoice());
    }

    private void getChoice() {
        if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[0])) {//TCP Server
            ipLabel.setText("本地IP地址");
            portLabel.setText("本地端口号");
            localIP.setVisible(true);
            remoteIP.setVisible(false);
        } else if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[1])) {//TCP Client
            ipLabel.setText("服务器IP地址");
            portLabel.setText("服务器端口号");
            localIP.setVisible(false);
            remoteIP.setVisible(true);
        }
    }

    public class MyClient extends Thread {

        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public void run() {
            try {
                Socket socket = new Socket();
                int port = Integer.parseInt(portField.getText());
                SocketAddress socketAddress = new InetSocketAddress(remoteIP.getText(), port);
                socket.connect(socketAddress, 1000);
                new InputThread(socket).start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class MyServer extends Thread {

        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        @Override
        public void run() {
            try {
                int port = Integer.parseInt(portField.getText());
                serverSocket = new ServerSocket(port, 0,
                        InetAddress.getByName(localIP.getSelectionModel().getSelectedItem().toString()));
                System.out.println("服务器已启动，等待客户端连接...");

                while (true) {
                    // 等待客户端连接
                    Socket clientSocket = serverSocket.accept();
                    System.out.println(sdf.format(new Date()) + " 客户端连接成功");

                    new InputThread(clientSocket).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class InputThread extends Thread {

        private Socket socket;

        public InputThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            boolean end = false;
            int endindex = 0;
            boolean start = false;
            int startindex = 0;

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024 * 100);
            InputStream inputStream = null;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String socketAddress = socket.getRemoteSocketAddress().toString();

            byte[] buf = new byte[1024];
            int bytesRead = 0;

            try {
                while ((bytesRead = inputStream.read(buf)) != -1) {
                    // 读取客户端发送的数据
                    String text = sendTextArea.getText();
                    if (autoRecvCheckBox.isSelected()) {// 如果被选中，则自动回复
                        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
                    }

                    if (contentAgreement.getSelectionModel().getSelectedIndex() == 0) {//无协议
                        String receivedMessage = new String(buf, 0, bytesRead, StandardCharsets.UTF_8);
                        recvTextArea.appendText(socketAddress + "\r\n" + receivedMessage + "\r\n");
                        recvTextArea.positionCaret(recvTextArea.getText().length());
                    } else {//自定义协议
                        for (int i = 0; i < buf.length; i++) {
                            // System.out.print(String.format("%02X ", buf[i]));
                            if ((buf[i] & 0xFF) == 0xF5) {
                                start = true;
                                startindex = i;
                            } else if ((buf[i] & 0xFF) == 0xF6) {
                                end = true;
                                endindex = i;
                            }
                        }
                        System.out.print(bytesRead + "(" + Integer.toHexString(buf[bytesRead - 1] & 0xFF) + ")，");
                        byteBuffer.put(buf, 0, bytesRead);
                        if (end) {
                            String receivedMessage = null;
                            byte[] buffer = byteBuffer.array();
                            System.out.println(
                                    "___xxxxxxxxxxx_____ " + byteBuffer.position() + " _______xxxxxxxxxxxxxx______");
                            receivedMessage = new String(buffer, 6, byteBuffer.position() - 7, StandardCharsets.UTF_8);
                            // System.out.println(receivedMessage);
                            recvTextArea.appendText(socketAddress + "\r\n" + receivedMessage + "\r\n");
                            recvTextArea.positionCaret(recvTextArea.getText().length());
                            byteBuffer.clear();
                            end = false;
                            endindex = 0;
                            start = false;
                            startindex = 0;
                        }
                    }
                }
                System.out.println("对方关闭了socket通道！");
                closeBtn.setDisable(true);
                openBtn.setDisable(false);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String[] getLocalIp() throws SocketException {
        ArrayList<String> arrayList = new ArrayList<String>();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            // System.out.println("当前系统是 Linux");
        } else {
            // System.out.println("当前系统是 Windows");
        }
        Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
        while (allNetInterfaces.hasMoreElements()) {
            NetworkInterface netInterface = allNetInterfaces.nextElement();
            if (!netInterface.isLoopback() && !netInterface.isVirtual() && netInterface.isUp()) {
                // if (netInterface.isUp()) {
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (null != address && address instanceof Inet4Address) {
                        arrayList.add(address.getHostAddress());
                    }
                }
            }
        }
        return arrayList.toArray(new String[0]);
    }
}
