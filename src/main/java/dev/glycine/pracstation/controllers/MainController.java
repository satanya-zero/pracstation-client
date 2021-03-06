package dev.glycine.pracstation.controllers;

import com.jfoenix.controls.JFXAlert;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXRadioButton;
import dev.glycine.pracstation.App;
import dev.glycine.pracstation.AppLauncher;
import dev.glycine.pracstation.models.*;
import dev.glycine.pracstation.pb.InitRouteMessage;
import io.grpc.StatusRuntimeException;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Modality;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 程式的主控制器
 */
@Log4j2
public final class MainController {
    @Getter
    private static MainController instance;

    public MainController() {
        instance = this;
    }

    @FXML
    private FlowPane routePane;
    @FXML
    private JFXRadioButton newRouteBtn;
    @FXML
    private JFXRadioButton cancelRouteBtn;

    public void disableCancelRouteBtn() {
        cancelRouteBtn.setDisable(true);
    }

    public void enableCancelRouteBtn() {
        cancelRouteBtn.setDisable(false);
    }

    @FXML
    private JFXRadioButton unlockRouteBtn;
    @FXML
    private ToggleGroup routeToggle;
    @FXML
    private Card routeCard;
    @FXML
    private AnchorPane root;
    @FXML
    private Label localTime;
    @FXML
    private Label stationName;
    @FXML
    private HBox topRightBox;
    @FXML
    private HBox bottomLeftBox;
    @FXML
    private HBox bottomRightBox;

    @Getter
    private StationController stationController;

    private Station station;

    /**
     * 宣告時鐘動畫
     */
    private final Timeline clock = new Timeline(
            new KeyFrame(Duration.ZERO, e -> localTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))),
            new KeyFrame(Duration.seconds(1))
    );

    /**
     * 配置工具
     */
    private void configureToolBox() {
        topRightBox.translateXProperty().bind(root.widthProperty().subtract(topRightBox.widthProperty()));
        bottomLeftBox.translateYProperty().bind(root.heightProperty().subtract(bottomLeftBox.heightProperty()));
        bottomRightBox.translateXProperty().bind(root.widthProperty().subtract(bottomRightBox.widthProperty()));
        bottomRightBox.translateYProperty().bind(root.heightProperty().subtract(bottomRightBox.heightProperty()));
    }

    private void setDefaultCanvas() {
        //將畫布置於窗口的中央
        station.translateXProperty().bind(root.widthProperty().subtract(station.widthProperty()).divide(2));
        station.translateYProperty().bind(root.heightProperty().subtract(station.heightProperty()).divide(2));

        //綁定縱橫軸的縮放倍率
        station.setScaleX(1.5);
        station.setScaleY(1.5);
    }

    /**
     * 配置畫布
     */
    private void configureCanvas() {
        setDefaultCanvas();
        //縮放
        station.setOnScroll(scrollEvent -> {
            if (scrollEvent.isControlDown()) {
                Scale newScale = new Scale();
                var factor = scrollEvent.getDeltaY() > 0 ? 1.05 : 1 / 1.05;
                newScale.setX(factor);
                newScale.setY(factor);
                newScale.setPivotX(scrollEvent.getX());
                newScale.setPivotY(scrollEvent.getY());
                station.getTransforms().add(newScale);
                scrollEvent.consume();
            }
        });

        //平移
        var mouseXProperty = new SimpleDoubleProperty(0.0);
        var mouseYProperty = new SimpleDoubleProperty(0.0);

        station.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.isControlDown()) {
                mouseXProperty.set(mouseEvent.getX());
                mouseYProperty.set(mouseEvent.getY());
                mouseEvent.consume();
            }
        });

        station.setOnMouseDragged(mouseEvent -> {
            if (mouseEvent.isControlDown()) {
                station.setCursor(Cursor.MOVE);
                Translate trans = new Translate(
                        mouseEvent.getX() - mouseXProperty.get(),
                        mouseEvent.getY() - mouseYProperty.get()
                );
                station.getTransforms().add(trans);
                mouseEvent.consume();
            }
        });

        station.setOnMouseReleased(mouseEvent -> {
            if (mouseEvent.isControlDown()) {
                station.setCursor(Cursor.DEFAULT);
                mouseEvent.consume();
            }
        });
    }

    /**
     * 配置時鐘
     */
    void configureClock() {
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    @FXML
    public void initialize() throws IOException {
        if (AppLauncher.getWindowWidth() != 1920 && AppLauncher.getWindowHeight() != 1080) {
            root.setPrefWidth(AppLauncher.getWindowWidth());
            root.setPrefHeight(AppLauncher.getWindowHeight());
        }

        var stationLoader = new FXMLLoader(new File(AppLauncher.getFxmlPath()).toURI().toURL());
        stationController = new StationController();
        stationLoader.setController(stationController);
        station = stationLoader.load();
        Platform.runLater(() -> root.getChildren().add(0, station));

        configureToolBox();
        configureCanvas();
        configureClock();

        stationName.setText(station.getName());

        newRouteBtn.setSelectedColor(AppleColor.GREEN);
        cancelRouteBtn.setSelectedColor(AppleColor.BLUE);
        unlockRouteBtn.setSelectedColor(AppleColor.YELLOW);
        ConsoleController.writeLn(InfoState.INFO, "基本視圖初始化完成");
    }

    @Getter
    private final HashMap<String, Light> routes = new HashMap<>();

    private void addToRouteMap(String str, Light light) {
        routes.put(str, light);
        if (!routes.isEmpty()) {
            cancelRouteBtn.setDisable(false);
            unlockRouteBtn.setDisable(false);
        }
    }

    private void removeFromRouteMap(String str) {
        routes.remove(str);
        if (routes.isEmpty()) {
            cancelRouteBtn.setDisable(true);
            unlockRouteBtn.setDisable(true);
            routeToggle.selectToggle(newRouteBtn);
        }
    }

    public void handleCreateRoute(List<Light> focusedLights) {
        var client = stationController.getStationClient();
        var list = focusedLights.stream().map(Light::getButtonName).collect(Collectors.toList());
        var protectedLight = focusedLights.get(0);
        new Thread(() -> {
            try {
                var response = client.createRoute(list);
                addToRouteMap(response.getRouteId(), protectedLight);
                addToRoutePane(response.getRouteId(), protectedLight);
                ConsoleController.writeLn(InfoState.SUCCESS, "建立進路成功: " + response.getRouteId());
            } catch (StatusRuntimeException e) {
                log.warn(e.getStatus().getDescription());
                ConsoleController.writeLn(InfoState.WARN, "建立進路失敗: " + e.getStatus().getDescription());
            }
        }).start();
        Light.defocusAll();
    }

    public void handleCancelRoute(String routeId) {
        var client = stationController.getStationClient();
        new Thread(() -> {
            try {
                client.cancelRoute(routeId);
                removeFromRoutePane(routeId);
                removeFromRouteMap(routeId);
                ConsoleController.writeLn(InfoState.SUCCESS, "取消進路成功: " + routeId);
            } catch (StatusRuntimeException e) {
                log.warn(e.getStatus().getDescription());
                ConsoleController.writeLn(InfoState.WARN, "取消進路失敗: " + e.getStatus().getDescription());
            }
        }).start();
        Light.defocusAll();
    }

    public void handleClickRouteBadge(MouseEvent mouseEvent) {
        var badge = (RouteBadge) mouseEvent.getSource();
        var alert = new JFXAlert(root.getScene().getWindow());
        alert.initModality(Modality.APPLICATION_MODAL);
        var layout = new JFXDialogLayout();
        var routeIcon = new FontIcon(FontAwesomeSolid.ROUTE);
        routeIcon.setIconColor(AppleColor.WHITE);
        var headingLabel = new Label(badge.getRouteId().replace("-", " 至 "), routeIcon);
        headingLabel.setGraphicTextGap(10);
        headingLabel.setTextFill(AppleColor.WHITE);
        layout.setHeading(headingLabel);
        var bodyLabel = new Label("是否要取消進路: " + badge.getRouteId() + " ?");
        bodyLabel.setTextFill(AppleColor.GRAY);
        layout.setBody(bodyLabel);

        var blur = new GaussianBlur(0); // 55 is just to show edge effect more clearly.
        root.getScene().getRoot().setEffect(blur);
        var timeline1 = new Timeline(new KeyFrame(Duration.millis(160), new KeyValue(blur.radiusProperty(), 10)));
        var timeline2 = new Timeline(new KeyFrame(Duration.millis(160), new KeyValue(blur.radiusProperty(), 0)));

        alert.getDialogPane().setOnMouseClicked(e -> timeline2.play());

        var cancelBtn = new JFXButton("總取消");
        cancelBtn.setTextFill(AppleColor.WHITE);
        var times = new FontIcon(FontAwesomeSolid.TIMES);
        times.setIconColor(AppleColor.WHITE);
        cancelBtn.setButtonType(JFXButton.ButtonType.RAISED);
        cancelBtn.setGraphic(times);
        cancelBtn.setOnAction(event -> {
            alert.hideWithAnimation();
            timeline2.play();
            handleCancelRoute(badge.getRouteId());
        });

        var unlockBtn = new JFXButton("總人解");
        var lock = new FontIcon(FontAwesomeSolid.UNLOCK);
        lock.setIconColor(AppleColor.WHITE);
        unlockBtn.setGraphic(lock);
        unlockBtn.setButtonType(JFXButton.ButtonType.RAISED);
        unlockBtn.setTextFill(AppleColor.WHITE);
        unlockBtn.setOnAction(event -> {
            alert.hideWithAnimation();
            timeline2.play();
        });

        var errorUnlockBtn = new JFXButton("區故解");
        errorUnlockBtn.setTextFill(AppleColor.WHITE);
        var error = new FontIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE);
        error.setIconColor(AppleColor.WHITE);
        errorUnlockBtn.setGraphic(error);
        errorUnlockBtn.setButtonType(JFXButton.ButtonType.RAISED);
        errorUnlockBtn.setOnAction(event -> {
            alert.hideWithAnimation();
            timeline2.play();
        });

        layout.setActions(cancelBtn, unlockBtn, errorUnlockBtn);
        alert.setContent(layout);

        alert.show();
        timeline1.play();
    }

    public void addToRoutePane(String s, Light light) {
        Platform.runLater(() -> {
            var badge = new RouteBadge(s, light);
            var fade = new FadeTransition(Duration.seconds(0.5), badge);
            fade.setFromValue(0);
            fade.setToValue(1);
            routePane.getChildren().add(badge);
            routeCard.resize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
            fade.play();
        });
    }

    public void removeFromRoutePane(String s) {
        Platform.runLater(() -> {
            routePane.getChildren().removeAll(routePane.getChildren().filtered(e -> ((RouteBadge) e).getRouteId().equals(s)));
            routeCard.resize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        });
    }

    public void initRoutePane(List<InitRouteMessage> res) {
        Platform.runLater(() -> {
            routePane.getChildren().clear();
            res.forEach(e -> {
                addToRouteMap(e.getRouteId(), Light.getLightByButtonName(e.getButtonId()));
                addToRoutePane(e.getRouteId(), Light.getLightByButtonName(e.getButtonId()));
                log.debug(2 + e.getButtonId() + ": " + Light.getLightByButtonName(e.getButtonId()));
            });
        });
    }

    public RouteAction getAction() {
        var selectedBtn = (JFXRadioButton) routeToggle.getSelectedToggle();
        if (newRouteBtn.equals(selectedBtn)) return RouteAction.NEW;
        if (cancelRouteBtn.equals(selectedBtn)) return RouteAction.CANCEL;
        if (unlockRouteBtn.equals(selectedBtn)) return RouteAction.MANUAL_UNLOCK;
        return RouteAction.UNKNOWN;
    }
}
