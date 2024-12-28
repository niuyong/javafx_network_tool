package application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
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
    private TextField remotePort;
    @FXML
    private TextArea recvTextArea;
    @FXML
    private TextArea sendTextArea;
    @FXML
    private CheckBox autoRecvCheckBox;
    @FXML
    private Label fileimport;
    @FXML
    private Label remoteIPLabel;
    @FXML
    private Label remotePortLabel;
    @FXML
    private ChoiceBox contentAgreement;
    @FXML
    private Button contentAgreementConfig;
    @FXML
    private Label receiveCountLabel;
    @FXML
    private Label sendCountLabel;
    @FXML
    private Button cleanSendCountBtn;
    @FXML
    private Button cleanReceCountBtn;
    @FXML
    private Button resetNum;
    @FXML
    private VBox clientList;
    @FXML
    private CheckBox sendLoopCheckBox;

    private int receiveCount = 0;
    private int sendCount = 0;

    private String[] agreementArray = new String[]{"TCP Server", "TCP Client", "UDP Server", "UDP Client"};
    private String[] timeUnitArray = new String[]{"毫秒", "秒"};
    private String[] encodingArray = new String[]{"UTF-8", "HEX", "GBK"};
    private String[] contentAgreementArray = new String[]{"无协议", "协议一", "协议二", "协议三"};
    private final SimpleDateFormat sdf = new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss.SSS");

    private static final InetAddressValidator VALIDATOR = InetAddressValidator.getInstance();

    public static boolean isValidIPV4ByValidator(String inetAddress) {
        return VALIDATOR.isValidInet4Address(inetAddress);
    }

    private OutputStream outputStream;
    private InputStream inputStream;
    private ServerSocket serverSocket;
    private Socket socket;

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
            if (portField.getText().trim().isEmpty()) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText("请填写端口号！");
                alert.showAndWait();
                return;
            }
            setFunctionDiabled(true);

            if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[0])) {
                MyServer myServer = new MyServer();
                myServer.start();
            } else if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[1])) {
                MyClient myClient = new MyClient();
                myClient.start();
            }
        });

        closeBtn.setOnAction(actionEvent -> {
            setFunctionDiabled(false);

            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[0])) {//TCP server
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                } else if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[1])) {//TCP client
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        sendBtn.setOnAction(actionEvent -> {
            String str = sendTextArea.getText();
            try {
                byte[] buffer = null;
                if (sendEncoding.getSelectionModel().getSelectedItem().equals(encodingArray[0])) {//UTF-8
                    buffer = str.getBytes(StandardCharsets.UTF_8);
                } else if (sendEncoding.getSelectionModel().getSelectedItem().equals(encodingArray[1])) {//HEX
                    buffer = str.getBytes(StandardCharsets.UTF_8);
                } else {//GBK
                    buffer = str.getBytes("GBK");
                }
                sendCount += buffer.length;
                sendCountLabel.setText("发送：" + sendCount + "字节");
                outputStream.write(buffer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        TextFormatter<String> textFormatter = new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                if (change.getControlNewText().length() <= 5) { // 限制输入长度为10
                    return change; // 允许输入
                }
                return null; // 不允许输入
            }
            return change; // 其他情况允许输入
        });
        portField.setTextFormatter(textFormatter);
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

        cleanReceCountBtn.setOnAction(e -> {
            recvTextArea.setText("");
        });

        cleanSendCountBtn.setOnAction(e -> {
            sendTextArea.setText("");
        });

        resetNum.setOnAction(e -> {
            sendCount = 0;
            sendCountLabel.setText("发送：0字节");
            receiveCount = 0;
            receiveCountLabel.setText("接收：0字节");
        });
    }

    private void setFunctionDiabled(boolean disabled) {
        openBtn.setDisable(disabled);
        closeBtn.setDisable(!disabled);
        portField.setDisable(disabled);
        agreement.setDisable(disabled);
        sendBtn.setDisable(!disabled);
        localIP.setDisable(disabled);
        remoteIP.setDisable(disabled);
//        receiveEncoding.setDisable(disabled);
//        sendEncoding.setDisable(disabled);
    }

    private void getChoice() {
        if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[0])) {//TCP Server
            remoteIPLabel.setDisable(true);
            remotePortLabel.setDisable(true);
            remotePort.setDisable(true);
            remoteIP.setDisable(true);
        } else if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[1])) {//TCP Client
            remoteIPLabel.setDisable(false);
            remotePortLabel.setDisable(false);
            remotePort.setDisable(false);
            remoteIP.setDisable(false);
        }
    }

    public class MyClient extends Thread {

        @Override
        public void run() {
            try {
                Socket socket = new Socket();
                int port = Integer.parseInt(portField.getText());
                SocketAddress socketAddress = new InetSocketAddress(remoteIP.getText(), port);
                socket.connect(socketAddress, 1000);
                SampleController.this.socket = socket;
                new InputThread(socket).start();
            } catch (IOException e) {
                setFunctionDiabled(false);
                e.printStackTrace();
                if (e instanceof SocketTimeoutException) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.WARNING);
                        alert.setTitle("警告");
                        alert.setHeaderText("远程端口未打开！");
                        alert.showAndWait();
                    });
                }
            }
        }
    }

    public class MyServer extends Thread {

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

                    Platform.runLater(() -> {
                        AnchorPane anchorPane = new AnchorPane();
                        // 设置 AnchorPane 的 margin
                        Insets insets = new Insets(5.0, 10.0, 5.0, 10.0);
                        VBox.setMargin(anchorPane, insets);

                        // 创建 CheckBox
                        CheckBox checkBox = new CheckBox();
                        String id = clientSocket.getRemoteSocketAddress().toString();
                        checkBox.setText(id);
                        checkBox.setId(id);
                        checkBox.setMnemonicParsing(false);

                        // 将 CheckBox 添加到 AnchorPane
                        anchorPane.getChildren().add(checkBox);
                        clientList.getChildren().add(anchorPane);
                    });
                    System.out.println(sdf.format(new Date()) + " 客户端连接成功");

                    new InputThread(clientSocket).start();
                }
            } catch (BindException e) {
                setFunctionDiabled(false);
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setTitle("警告");
                    alert.setHeaderText("端口已被占用！");
                    alert.showAndWait();
                });
                e.printStackTrace();
            } catch (Exception e) {
                setFunctionDiabled(false);
                e.printStackTrace();
            }
        }
    }

    public class InputThread extends Thread {

        private final Socket socket;

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
                    if (autoRecvCheckBox.isSelected() && agreement.getSelectionModel().getSelectedItem().equals(agreementArray[0])) {// 如果被选中，则自动回复
                        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
                    }

                    if (contentAgreement.getSelectionModel().getSelectedIndex() == 0) {//无协议
                        String receivedMessage = null;
                        if (receiveEncoding.getSelectionModel().getSelectedItem().equals(encodingArray[0])) {//UTF-8
                            receivedMessage = new String(buf, 0, bytesRead, StandardCharsets.UTF_8);
                        } else if (receiveEncoding.getSelectionModel().getSelectedItem().equals(encodingArray[1])) {//HEX
                            receivedMessage = bytes2hex(buf, bytesRead);
                        } else {//GBK
                            receivedMessage = new String(buf, 0, bytesRead, "GBK");
                        }

                        receiveCount += bytesRead;
                        Platform.runLater(() -> {
                            receiveCountLabel.setText("接收：" + receiveCount + "字节");
                        });
                        recvTextArea.appendText(socketAddress + sdf.format(new Date()) + "\r\n" + receivedMessage + "\r\n");
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
                if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[1])) {//如果是TCP client
                    setFunctionDiabled(false);
                }

                Platform.runLater(() -> {
                    AtomicReference<Node> toRemove = new AtomicReference<>();
                    clientList.getChildren().forEach(node -> {
                        AnchorPane anchorPane = (AnchorPane) node;
                        CheckBox checkBox = (CheckBox) anchorPane.getChildren().get(0);
                        if (checkBox.getId().equals(socketAddress)) {
                            toRemove.set(anchorPane);
                        }
                    });
                    clientList.getChildren().remove(toRemove.get());
                });

            } catch (IOException e) {
                e.printStackTrace();
                if (agreement.getSelectionModel().getSelectedItem().equals(agreementArray[1])) {//如果是TCP client
                    setFunctionDiabled(false);
                }
            }

            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 打印十六进制
     *
     * @param bytes
     * @return
     */
    public static String bytes2hex(byte[] bytes, int len) {
        StringBuilder sb = new StringBuilder();
        String tmp;
        sb.append("[");
        for (int i = 0; i < len; i++) {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            tmp = Integer.toHexString(0xFF & bytes[i]);
            if (tmp.length() == 1) {
                tmp = "0" + tmp;// 只有一位的前面补个0
            }
            sb.append(tmp).append(" ");// 每个字节用空格断开
        }
        sb.delete(sb.length() - 1, sb.length());// 删除最后一个字节后面对于的空格
        sb.append("]");
        return sb.toString();
    }

    private String[] getLocalIp() throws SocketException {
        ArrayList<String> arrayList = new ArrayList<>();
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
                    if (address instanceof Inet4Address) {
                        arrayList.add(address.getHostAddress());
                    }
                }
            }
        }
        return arrayList.toArray(new String[0]);
    }
}
