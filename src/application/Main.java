package application;
	
import java.net.URL;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
			URL location = getClass().getResource("Sample.fxml");
	        FXMLLoader fxmlLoader = new FXMLLoader();
	        fxmlLoader.setLocation(location);
	        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());
	        Parent root = fxmlLoader.load();
	        //如果使用 Parent root = FXMLLoader.load(...) 静态读取方法，无法获取到Controller的实例对象
	        primaryStage.setTitle("网络工具");
	        Scene scene = new Scene(root, 1190, 761);
	        //加载css样式
	        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
	        primaryStage.setScene(scene);
	        SampleController controller = fxmlLoader.getController();   //获取Controller的实例对象
	        //Controller中写的初始化方法
	        controller.init(scene);
	        primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}