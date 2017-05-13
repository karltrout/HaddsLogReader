package logreader.gui;

/**
 * Created by karltrout on 5/10/17.
 */
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
import java.util.stream.IntStream;

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
    private SimpleStringProperty filePattern = new SimpleStringProperty("nas_[0-9]{2}\\.log");
    private boolean editingPattern = false;

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

        HBox rootDirHBox = getRootDirectoryHBox();
        HBox outDirHBox =  getOutDirectoryHBox();

        topVBox.getChildren().addAll( rootDirHBox, outDirHBox );
        rootLayout.setTop(topVBox);

        HBox bottomHBox = new HBox();
        bottomHBox.setAlignment(Pos.BASELINE_RIGHT);
        bottomHBox.getChildren().add(btn);
        bottomHBox.setPadding(new Insets(15, 12, 15, 12));
        bottomHBox.setSpacing(10);
        bottomHBox.setStyle("-fx-background-color: #336699;");

        rootLayout.setBottom(bottomHBox);

        AnchorPane leftPane = new AnchorPane();
        leftPane.setStyle("-fx-background-color: #336699;");

        VBox leftLvlsHBox = getLevelsHBox();
        leftLvlsHBox.setAlignment(Pos.TOP_CENTER);
        AnchorPane.setTopAnchor(leftLvlsHBox,10.0);
        AnchorPane.setBottomAnchor(leftLvlsHBox, 10.0);
        AnchorPane.setLeftAnchor(leftLvlsHBox, 10.0);
        AnchorPane.setRightAnchor(leftLvlsHBox, 0.0);

        leftPane.getChildren().add(leftLvlsHBox);
        rootLayout.setLeft(leftPane);

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

        Scene scene = new Scene(rootLayout, 700, 500);

        primaryStage.setTitle("Log Reader");
        primaryStage.setScene(scene);
        primaryStage.show();

        rootLayout.requestFocus();

        startTest();

    }

    private VBox getLevelsHBox() {

        VBox levelsVbox = new VBox();
        levelsVbox.setMaxWidth(100.0);
        levelsVbox.setMinWidth(100.0);
        levelsVbox.setSpacing(20.0);
        levelsVbox.setPadding(new Insets(5,15,5,15));
        levelsVbox.setStyle("-fx-background-color: white;");
        levelsVbox.setAlignment(Pos.CENTER);

        HBox levelsTxt = new HBox();

        VBox mbsVbox = new VBox();
        mbsVbox.setAlignment(Pos.CENTER);
        mbsVbox.setMinWidth(40);
        Text mbLvlTxt = new Text("# Mbs");
        mbLvlTxt.setFill(Color.WHITE);
        Text mbsTotalsTxt = new Text("----");
        mbsTotalsTxt.setFill(Color.WHITE);
        Text mbsProcessedTxt = new Text("----");
        mbsProcessedTxt.setFill(Color.WHITE);
        mbsVbox.getChildren().addAll(mbLvlTxt, mbsTotalsTxt,mbsProcessedTxt);

        VBox fileCntVbox = new VBox();
        fileCntVbox.setAlignment(Pos.CENTER);
        fileCntVbox.setMinWidth(40);
        Text fileCntTxt = new Text("# Files");
        fileCntTxt.setFill(Color.WHITE);
        Text fileCntTotalTxt = new Text("----");
        fileCntTotalTxt.setFill(Color.WHITE);
        Text filesCountedTxt = new Text("----");
        filesCountedTxt.setFill(Color.WHITE);
        fileCntVbox.getChildren().addAll(fileCntTxt, fileCntTotalTxt, filesCountedTxt);

        levelsTxt.setSpacing(5);
        levelsTxt.getChildren().addAll(mbsVbox, fileCntVbox);

        HBox levelsHbox = new HBox();
        levelsHbox.minWidth(100);
        levelsHbox.setFillHeight(true);
        levelsHbox.setSpacing(4);
        levelsHbox.setStyle("-fx-background-color:WHITE;");

        VBox fileLvlsVBox = new VBox();
        VBox mbsLvlsVBox = new VBox();
        fileLvlsVBox.setSpacing(1);
        mbsLvlsVBox.setSpacing(1);
        mbsLvlsVBox.setFillWidth(true);
        fileLvlsVBox.setFillWidth(true);
        fileLvlsVBox.setStyle("-fx-background-color:#475058;");
        mbsLvlsVBox.setStyle("-fx-background-color: #475058;");
        fileLvlsVBox.setAlignment(Pos.BOTTOM_CENTER);
        mbsLvlsVBox.setAlignment(Pos.BOTTOM_CENTER);

        for (int i : IntStream.range(1,16).toArray()) {

            Rectangle fileLvl1 = new Rectangle(0, 0);
            fileLvl1.setWidth(38);
            fileLvl1.setHeight(15);
            fileLvl1.setFill(Color.valueOf("475058"));
            fileLvl1.setStrokeWidth(.5);
            fileLvl1.setStroke(Color.BLACK);
            fileLvlsVBox.getChildren().add(fileLvl1);
        }
        for (int i : IntStream.range(1,16).toArray()) {
            Rectangle mbsLvl1 = new Rectangle(0, 0);
            mbsLvl1.setWidth(38);
            mbsLvl1.setHeight(15);
            mbsLvl1.setFill(Color.valueOf("475058"));
            mbsLvl1.setStrokeWidth(.5);
            mbsLvl1.setStroke(Color.BLACK);
            mbsLvlsVBox.getChildren().add(mbsLvl1);
        }

        levelsHbox.getChildren().addAll(fileLvlsVBox, mbsLvlsVBox);

        levelsVbox.setAlignment(Pos.TOP_CENTER);
        levelsVbox.getChildren().addAll(levelsTxt, levelsHbox);
        levelsVbox.setStyle("-fx-background-color: black; ");
        return levelsVbox;

    }

    private HBox getOutDirectoryHBox() {
        HBox outDirHBox = new HBox() ;
        outDirHBox.setPadding(new Insets(5, 12, 0, 2));
        outDirHBox.setSpacing(5);
        outDirHBox.setAlignment(Pos.CENTER_LEFT);

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
        outDirectory.setWrappingWidth(300);
        outDirectory.textProperty().bind(this.outDirectoryString);
        outDirectory.setFill(Color.WHITESMOKE);
        outDirHBox.getChildren().addAll( outDirLbl, outDirectory, outWrtImgView );

        return outDirHBox;

    }

    private HBox getRootDirectoryHBox() {
        HBox rootDirHBox = new HBox();

        rootDirHBox.setPadding(new Insets(5, 12,0 ,2));
        rootDirHBox.setAlignment(Pos.CENTER_LEFT);

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

        rootDirectory.setWrappingWidth(300);

        rootDirectory.textProperty().bind(rootDirectoryString);
        rootDirectory.setFill(Color.WHITESMOKE);

        Label patternLbl = new Label("Pattern:");
        patternLbl.setPadding(new Insets(0,0,0,20));

        patternLbl.setFont(Font.font(labelFont, FontWeight.BOLD, labelFontSize));
        patternLbl.setTextFill(Color.GHOSTWHITE);
        patternLbl.setLabelFor(rootDirHBox);

        TextField patternText = new TextField();
        patternText.setEditable(false);
        patternText.textProperty().bind(filePattern);
        patternText.setStyle("-fx-background-color:#336699; -fx-text-fill: white;");

        ImageView patternWrtImgView = makeWriteImageView() ;

        patternWrtImgView.setTranslateY(-5.0);
        patternWrtImgView.setOnMouseClicked((mouseEvent) ->{

            if(editingPattern){

                editingPattern = false;
                patternText.setEditable(false);
                patternText.setStyle("-fx-background-color:#336699; -fx-text-fill: WHITE;");
                patternText.getParent().requestFocus();

            }
            else{
                editingPattern = true;
                patternText.setEditable(true);
                patternText.setStyle("-fx-background-color:WHITE; -fx-text-fill: #336699;");
                patternText.requestFocus();
            }

        });


        rootDirHBox.getChildren().addAll(
                rootLbl,
                rootDirectory,
                rootWrtImgView,
                patternLbl,
                patternText,
                patternWrtImgView );

        return rootDirHBox;

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
