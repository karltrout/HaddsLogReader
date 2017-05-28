package logreader.gui;

/**
 * Created by karltrout on 5/10/17.
 * This is the Gui Interface to HADDS Binary Log File to Ascii File Processor.
 */

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
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
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import logreader.HaddsLogFileParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class Reader extends Application {

    private Path rootDirectoryPath = Paths.get("/Users/karltrout/Documents/Resources/hadds/");
    private SimpleStringProperty rootDirectoryString = new SimpleStringProperty(rootDirectoryPath.toString());
    private Path outDirectoryPath = Paths.get("/Users/karltrout/Documents/Resources/text/");
    private SimpleStringProperty outDirectoryString = new SimpleStringProperty(outDirectoryPath.toString());

    private XYChart.Series<Number, Number> series = null;

    private final String labelFont = "Helvetica";
    private final int labelFontSize = 14;
    private final int numberFontSize = 9;
    private final static Image writeImg = new Image("images/write_white.png",24,24,true,true, true);
    private final static Image writeHoverImg = new Image("images/write_light_gray.png",24,24,true,true, true);
    private ScheduledExecutorService scheduledPool;
    private SimpleStringProperty filePattern = new SimpleStringProperty("nas_[0-9]{2}\\.log");
    private boolean editingPattern = false;
    private VBox fileLvlsVBox;
    private VBox mbsLvlsVBox;
    //Counters
    private Number incrementingMbsCount = 0;
    private SimpleStringProperty totalMbsTxt = new SimpleStringProperty("40.0");
    private SimpleStringProperty mbsProcessedTxt = new SimpleStringProperty("----");
    private Number incrementingFilesCnt = 0;
    private SimpleStringProperty totalFilesTxt = new SimpleStringProperty("60.0");
    private SimpleStringProperty filesProcessedTxt = new SimpleStringProperty("----");
    private Button processBtn;
    private Button loadBtn;

    private List<Path> files = new ArrayList<>();

    private AtomicLong totalBytes = new AtomicLong(0);
    private AtomicInteger totalFilesRead = new AtomicInteger(0);
    private ExecutorService executorService;

    @Override
    public void stop(){

        if(scheduledPool != null && !scheduledPool.isShutdown() ){
            scheduledPool.shutdownNow();
        }

        if (executorService != null && !executorService.isShutdown() ){
            executorService.shutdownNow();
        }


    }

    @Override
    public void start(Stage primaryStage) {

        buildGui(primaryStage);

    }

    private void buildGui(Stage primaryStage) {
        processBtn = new Button();
        processBtn.setText("Process!");
        processBtn.setOnAction(event -> {
            System.out.println("Processing...");
            processFiles();
        });
        processBtn.setDisable(true);

        loadBtn = new Button("Load");
        loadBtn.setOnAction( event -> {
            if(this.rootDirectoryString.getValue().isEmpty()){
                //add message dialog here
                System.out.println("Please select a root Directory to search for files.");
                return;
            }
            initializeData();
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
        bottomHBox.getChildren().addAll(loadBtn,processBtn);
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
    }

    private void initializeData() {

        resetData();
        int maxDepth = 10;
        Number totalBytesCollected;
        String pattern = filePattern.getValue();

        try (Stream<Path> stream = Files.walk(rootDirectoryPath, maxDepth)) {
            files = stream
                    .filter(path -> path.getFileName().toString().matches(pattern))
                    .collect(toList());
            totalBytesCollected =
                    files.stream().mapToDouble(
                            path -> {
                                try {
                                    return (Double.valueOf(Files.size(path)) / 1024 / 1024);
                                } catch (IOException ioe) {
                                    System.out.println("::Warning:: Missing File: " + path.toString());
                                    return 0;
                                }
                            }).sum();

            System.out.println(String.format("Total file size to process: %.2f Mb", totalBytesCollected.doubleValue()));
            this.totalMbsTxt.set(String.format("%.2f", totalBytesCollected.doubleValue()));
            this.totalFilesTxt.set(String.format("%d", files.size()));

            this.processBtn.setDisable(false);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
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

        VBox mbsCntVbox = new VBox();
        mbsCntVbox.setAlignment(Pos.CENTER);
        mbsCntVbox.setMinWidth(40);
        Text mbLvlTxt = new Text("# Mbs");
        mbLvlTxt.setFill(Color.WHITE);
        Text mbsTotalsTxt = new Text("----");
        mbsTotalsTxt.setFont(Font.font(labelFont, FontWeight.LIGHT, numberFontSize));
        mbsTotalsTxt.textProperty().bind(this.totalMbsTxt);
        mbsTotalsTxt.setFill(Color.WHITE);
        Text mbsProcessedTxt = new Text("----");
        mbsProcessedTxt.setFont(Font.font(labelFont, FontWeight.LIGHT, numberFontSize));
        mbsProcessedTxt.textProperty().bind(this.mbsProcessedTxt);
        mbsProcessedTxt.setFill(Color.WHITE);
        mbsCntVbox.getChildren().addAll(mbLvlTxt, mbsTotalsTxt,mbsProcessedTxt);

        VBox fileCntVbox = new VBox();
        fileCntVbox.setAlignment(Pos.CENTER);
        fileCntVbox.setMinWidth(40);
        Text fileCntTxt = new Text("# Files");
        fileCntTxt.setFill(Color.WHITE);
        Text fileCntTotalTxt = new Text("----");
        fileCntTotalTxt.setFont(Font.font(labelFont, FontWeight.LIGHT, numberFontSize));
        fileCntTotalTxt.textProperty().bind(this.totalFilesTxt);
        fileCntTotalTxt.setFill(Color.WHITE);
        Text filesCountedTxt = new Text("----");
        filesCountedTxt.setFont(Font.font(labelFont, FontWeight.LIGHT, numberFontSize));
        filesCountedTxt.textProperty().bind(filesProcessedTxt);
        filesCountedTxt.setFill(Color.WHITE);
        fileCntVbox.getChildren().addAll(fileCntTxt, fileCntTotalTxt, filesCountedTxt);

        levelsTxt.setSpacing(5);
        levelsTxt.getChildren().addAll(fileCntVbox, mbsCntVbox);

        HBox levelsHbox = new HBox();
        levelsHbox.minWidth(100);
        levelsHbox.setFillHeight(true);
        levelsHbox.setSpacing(4);
        levelsHbox.setStyle("-fx-background-color:WHITE;");

        fileLvlsVBox = new VBox();
        mbsLvlsVBox = new VBox();
        fileLvlsVBox.setSpacing(1);
        mbsLvlsVBox.setSpacing(1);
        mbsLvlsVBox.setFillWidth(true);
        fileLvlsVBox.setFillWidth(true);
        fileLvlsVBox.setStyle("-fx-background-color:#475058;");
        mbsLvlsVBox.setStyle("-fx-background-color: #475058;");
        fileLvlsVBox.setAlignment(Pos.BOTTOM_CENTER);
        mbsLvlsVBox.setAlignment(Pos.BOTTOM_CENTER);

        IntStream.range(1,16).forEach( (i) -> {

            Rectangle fileLvl1 = new Rectangle(0, 0);
            fileLvl1.setWidth(38);
            fileLvl1.setHeight(15);
            fileLvl1.setFill(Color.valueOf("475058"));
            fileLvl1.setStrokeWidth(.5);
            fileLvl1.setStroke(Color.BLACK);
            fileLvlsVBox.getChildren().add(fileLvl1);

        });

        IntStream.range(1,16).forEach((i1) ->{
            Rectangle mbsLvl1 = new Rectangle(0, 0);
            mbsLvl1.setWidth(38);
            mbsLvl1.setHeight(15);
            mbsLvl1.setFill(Color.valueOf("475058"));
            mbsLvl1.setStrokeWidth(.5);
            mbsLvl1.setStroke(Color.BLACK);
            mbsLvlsVBox.getChildren().add(mbsLvl1);
        });

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

//    private void startTest() {
//
//        resetData();
//        AtomicInteger seconds = new AtomicInteger(0);
//        Runnable addDataToSeries = () ->{
//                int second = seconds.incrementAndGet();
//                System.out.println("Seconds :"+second);
//               Platform.runLater(()-> {
//                   Number random = Math.random();
//                   series.getData().add(new XYChart.Data<>(second, random));
//                   updateFileCnt(second);
//                   updateMbsProcessed(random);
//
//               });
//        };
//
//        scheduledPool = Executors.newScheduledThreadPool(1);
//            scheduledPool.scheduleWithFixedDelay(addDataToSeries, 1, 1, TimeUnit.SECONDS);
//    }

    private void resetData() {

        series.getData().clear();
        incrementingMbsCount = 0;
        fillmbsLvlBox(0.0);
        incrementingFilesCnt = 0;

        fillFileLvlMeter(0.0);
        mbsProcessedTxt.set(String.format("%.2f",incrementingMbsCount.floatValue()));
        filesProcessedTxt.set(String.format("%d", incrementingFilesCnt.intValue()));

        totalFilesRead.set(0);
        totalBytes.set(0);

    }


    private void updateMbsProcessed(Number currentMbsProcessed) {

        incrementingMbsCount = currentMbsProcessed;
        this.mbsProcessedTxt.set(String.format("%.2f",incrementingMbsCount.floatValue()));
        Double lvlReached = incrementingMbsCount.intValue()/Double.valueOf(this.totalMbsTxt.get())*15;
        fillmbsLvlBox(lvlReached);

    }

    private void fillmbsLvlBox(Double lvlReached) {
        int counter = 0;
        List<Node> levelBars = this.mbsLvlsVBox.getChildren();

        for( int i = levelBars.size()-1; i >=0; i-- ){

            Node rec = levelBars.get(i);

            if (counter < lvlReached) {
                ((Rectangle) rec).setFill(Color.ORANGERED);
            }
            else {
                ((Rectangle) rec).setFill(Color.valueOf("475058"));
            }
            counter++;
        }
    }

    private void updateFileCnt(int numberOfFilesProcessed) {

        incrementingFilesCnt = numberOfFilesProcessed; //tst purposes
        filesProcessedTxt.set(String.format("%d", incrementingFilesCnt.intValue()));
        Number lvlReached = ((incrementingFilesCnt.doubleValue()/Double.valueOf(this.totalFilesTxt.get())) * 15.0);
        fillFileLvlMeter(lvlReached);


    }

    private void fillFileLvlMeter(Number lvlReached) {
        int counter = 0;

        List<Node> levelbars = this.fileLvlsVBox.getChildren();

        for (int i = levelbars.size() - 1; i >= 0; i--) {

            Node rec = levelbars.get(i);
            if (counter < lvlReached.intValue()) {
                ((Rectangle) rec).setFill(Color.LIMEGREEN);
            }
            else {
                ((Rectangle) rec).setFill(Color.valueOf("475058"));
            }
            counter++;

        }
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
        ac.setTitle("Data Processed(Mb)");
        ac.setStyle("-fx-text-fill: #FFFFFF;");

        series= new XYChart.Series<>();
        series.setName("Read");

        series.getData().addListener(
                (ListChangeListener<XYChart.Data<Number, Number>>) arg0 -> {
                    ObservableList<XYChart.Data<Number, Number>> data = series
                            .getData();
                    if (data.size() > 0) {
                        xAxis.setLowerBound(data.get(0).getXValue()
                                .doubleValue());
                        xAxis.setUpperBound(data.get(data.size() - 1)
                                .getXValue().doubleValue());
                    }
                });

        ac.getData().addAll(series);
        return ac;
    }


    private  void processFiles() {

        resetData();
        processBtn.setDisable(true);
        loadBtn.setDisable(true);


        executorService = Executors.newFixedThreadPool(4);

        //executorService = Executors.newSingleThreadExecutor();
//        executorService =
//                new ThreadPoolExecutor(
//                        4,
//                        4,
//                        5000L,
//                        TimeUnit.MILLISECONDS,
//                        new LinkedBlockingDeque<>());

        LocalTime start = LocalTime.now();

        for(Path p : files){
            try {
                Path covertedFileDirectory = outDirectoryPath.resolve(rootDirectoryPath.relativize(p)).getParent();
                HaddsLogFileParser hLogParser = new HaddsLogFileParser(p,covertedFileDirectory, totalBytes, totalFilesRead);
                hLogParser.setTesting(false);
                executorService.submit(hLogParser);
            }catch (IOException ioe){
                ioe.printStackTrace();
            }

        }

        try{
            final AtomicInteger seconds = new AtomicInteger(0);
            final AtomicLong lastRead = new AtomicLong(0);
            Runnable addDataToSeries = () ->{
                int second = seconds.incrementAndGet();
                long localLastRead = lastRead.get();
                long currentRead = totalBytes.get() - localLastRead;
                lastRead.set( localLastRead + currentRead);
                double mbsProcessed = this.totalBytes.doubleValue()/1024/1024;
                this.updateMbsProcessed(mbsProcessed);
                this.updateFileCnt(totalFilesRead.get());
                double mbsProcessedRate = (Double.valueOf(currentRead)/1024/1024)*10;
                System.out.println(
                        String.format("Running for : %.2f Secs. Mbs read: %.2f of %s from %d file(s) | Mb/sec. : %.2f",
                                second/10.0,
                                mbsProcessed,
                                this.totalMbsTxt,
                                totalFilesRead.get(),
                                mbsProcessedRate));

                Platform.runLater(()-> {
                   ObservableList<XYChart.Data<Number, Number>> data = series.getData();
                   if (series.getData().size() >= 300){
                        series.getData().remove(0);
                   }
                    series.getData().add(new XYChart.Data<>((second/10.0), mbsProcessedRate));
                });

                if (incrementingFilesCnt.intValue() >= Integer.valueOf(this.totalFilesTxt.get())
                        && !scheduledPool.isShutdown()){

                    try {
                        scheduledPool.awaitTermination(100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    scheduledPool.shutdown();

                    System.out.println("shutingDown the scheduled Thread.");
                    loadBtn.setDisable(false);

                }

            };

            scheduledPool = Executors.newScheduledThreadPool(1);
            scheduledPool.scheduleWithFixedDelay(addDataToSeries, 100, 100, TimeUnit.MILLISECONDS);

        } catch (Exception e){
            e.printStackTrace();
        }

        Duration duration = Duration.between(start, LocalTime.now());
        System.out.println("Total Duration to read files: "+duration);

    }

    public static void main(String[] args) {
        launch(args);
    }

}
