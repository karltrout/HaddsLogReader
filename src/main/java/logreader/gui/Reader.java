package logreader.gui;

/**
 * Created by karltrout on 5/10/17.
 */
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Reader extends Application {

    private Path rootDirectoryPath = Paths.get("/Users/karltrout/Documents/Resources/hadds/");
    private SimpleStringProperty rootDirectoryString = new SimpleStringProperty(rootDirectoryPath.toString());
    private Path outDirectoryPath = Paths.get("/Users/karltrout/Documents/Resources/text/");
    private SimpleStringProperty outDirectoryString = new SimpleStringProperty(outDirectoryPath.toString());

    private XYChart.Series<Number, Number> series = null;

    private final String labelFont = "Helvetica";
    private final int labelFontSize = 14;
    private final static Image writeImg = new Image("images/write_white.png",24,24,true,true, true);
    private final static Image writeHoverImg = new Image("images/write_light_gray.png",24,24,true,true, true);
    private ScheduledExecutorService scheduledPool;

    @Override
    public void stop(){

        if(scheduledPool != null && !scheduledPool.isShutdown() ){
            scheduledPool.shutdownNow();
        }

    }

    @Override
    public void start(Stage primaryStage) {
        Button btn = new Button();
        btn.setText("Process!");
        btn.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                System.out.println("Processing...");
            }
        });

        BorderPane rootLayout = new BorderPane();
        VBox topVBox = new VBox();
        topVBox.setPadding(new Insets(15, 12,5 , 12));
        topVBox.setStyle("-fx-background-color: #336699;");

        HBox rootDirHBox = new HBox();
        HBox outDirHBox = new HBox();

        rootDirHBox.setPadding(new Insets(5, 12,0 ,2));
        rootDirHBox.setAlignment(Pos.CENTER);

        rootDirHBox.setSpacing(5);
        Label rootLbl = new Label("Files:");

        rootLbl.setFont(Font.font(labelFont, FontWeight.BOLD, labelFontSize));
        rootLbl.setTextFill(Color.GHOSTWHITE);
        rootLbl.setLabelFor(rootDirHBox);

        ImageView rootWrtImgView = makeWriteImageView() ;
        rootWrtImgView.setTranslateY(-5.0);
        rootWrtImgView.setOnMouseClicked((mouseEvent) ->{
            Path newDirectory = chooseFile(mouseEvent);
            if (newDirectory != null ) {
                this.rootDirectoryPath = newDirectory;
                this.rootDirectoryString.setValue(rootDirectoryPath.toString());
            }
        });

        Text rootDirectory = new Text();
        rootDirectory.textProperty().bind(rootDirectoryString);
        rootDirectory.setFill(Color.WHITESMOKE);
        rootDirHBox.getChildren().addAll( rootLbl, rootDirectory, rootWrtImgView );

        outDirHBox.setPadding(new Insets(5, 12, 0, 2));
        outDirHBox.setSpacing(10);
        outDirHBox.setAlignment(Pos.CENTER);

        Label outDirLbl = new Label("Save:");
        outDirLbl.setFont(Font.font(labelFont, FontWeight.BOLD, labelFontSize));
        outDirLbl.setTextFill(Color.GHOSTWHITE);
        outDirLbl.setLabelFor(outDirHBox);

        ImageView outWrtImgView = makeWriteImageView();
        outWrtImgView.setTranslateY(-5.0);
        outWrtImgView.setOnMouseClicked((mouseEvent) ->{
            Path newDirectory = chooseFile(mouseEvent);
            if (newDirectory != null ) {
                this.outDirectoryPath = newDirectory;
                this.outDirectoryString.setValue(outDirectoryPath.toString());
            }
        });

        Text outDirectory = new Text();
        outDirectory.textProperty().bind(this.outDirectoryString);
        outDirectory.setFill(Color.WHITESMOKE);
        outDirHBox.getChildren().addAll( outDirLbl, outDirectory, outWrtImgView );

        topVBox.getChildren().addAll( rootDirHBox, outDirHBox );
        rootLayout.setTop(topVBox);

        HBox bottomHBox = new HBox();
        bottomHBox.setAlignment(Pos.BASELINE_RIGHT);
        bottomHBox.getChildren().add(btn);
        bottomHBox.setPadding(new Insets(15, 12, 15, 12));
        bottomHBox.setSpacing(10);
        bottomHBox.setStyle("-fx-background-color: #336699;");
        rootLayout.setBottom(bottomHBox);

        AnchorPane centerPane = new AnchorPane();
        centerPane.setStyle("-fx-background-color: #33669F;");
        AreaChart chart = makeAreaChart();
        AnchorPane.setTopAnchor(chart, 10.0);
        AnchorPane.setBottomAnchor(chart, 10.0);
        AnchorPane.setLeftAnchor(chart, 10.0);
        AnchorPane.setRightAnchor(chart, 10.0);
        chart.setStyle("-fx-background-color: #769FC9");
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        centerPane.getChildren().add(chart);
        rootLayout.setCenter(centerPane);

        Scene scene = new Scene(rootLayout, 600, 450);

        primaryStage.setTitle("Log Reader");
        primaryStage.setScene(scene);
        primaryStage.show();

        startTest();

    }

    private void startTest() {

        AtomicInteger seconds = new AtomicInteger(0);
        Runnable addDataToSeries = () ->{
                int second = seconds.incrementAndGet();
                System.out.println("Seconds :"+second);
               Platform.runLater(()-> { series.getData().add(new XYChart.Data<>(second, Math.random()));});
        };

        scheduledPool = Executors.newScheduledThreadPool(1);
            scheduledPool.scheduleWithFixedDelay(addDataToSeries, 1, 1, TimeUnit.SECONDS);
    }

    private Path chooseFile(Event event) {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Open Resource File");
        File selectedFile = fileChooser.showDialog(((Node)event.getSource()).getScene().getWindow());

        if (selectedFile != null ){
            return Paths.get(selectedFile.toURI());
        }
        return null;
    }

    private ImageView makeWriteImageView(){

        ImageView imgView = new ImageView(writeImg);

        imgView.setOnMouseEntered((mouseEvent) -> {
            ((ImageView)mouseEvent.getTarget()).setImage(writeHoverImg);
            ((ImageView)mouseEvent.getTarget()).getScene().setCursor(Cursor.HAND);});


        imgView.setOnMouseExited((mouseEvent) -> {
            ((ImageView)mouseEvent.getTarget()).setImage(writeImg);
            ((ImageView)mouseEvent.getTarget()).getScene().setCursor(Cursor.DEFAULT);
        });

        return imgView;
    }

    private AreaChart makeAreaChart(){
        final NumberAxis xAxis = new NumberAxis(1, 30, 1);
        final NumberAxis yAxis = new NumberAxis();
        final AreaChart<Number,Number> ac =
                new AreaChart<>(xAxis,yAxis);
        ac.setTitle("Data Monitoring (in Megabytes Mb)");
        ac.setStyle("-fx-text-fill: #FFFFFF;");

        series= new XYChart.Series();
        series.setName("Read");

        series.getData().addListener(
                new ListChangeListener<XYChart.Data<Number, Number>>() {

                    @Override
                    public void onChanged(
                            ListChangeListener.Change<? extends XYChart.Data<Number, Number>> arg0) {
                        ObservableList<XYChart.Data<Number, Number>> data = series
                                .getData();
                        xAxis.setLowerBound(data.get(0).getXValue()
                                .doubleValue());
                        xAxis.setUpperBound(data.get(data.size() - 1)
                                .getXValue().doubleValue());
                    }

                });

        ac.getData().addAll(series);
        return ac;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
