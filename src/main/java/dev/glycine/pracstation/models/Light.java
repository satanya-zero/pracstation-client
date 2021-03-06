package dev.glycine.pracstation.models;

import dev.glycine.pracstation.controllers.MainController;
import javafx.application.Platform;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Log4j2
public class Light extends Circle {
    private static final Double DEFAULT_STROKE_WIDTH = 0.75;
    public static final Double FOCUSED_STROKE_WIDTH = 1.5;
    private static final Double DEFAULT_CIRCLE_RADIUS = 5.0;
    private static final Paint DEFAULT_STROKE_FILL = AppleColor.WHITE;
    private static final SignalState DEFAULT_SIGNAL_STATE = SignalState.UNKNOWN;

    private final SignalBase signal;
    private final String buttonName;

    public String getButtonName() {
        return signal.sid + buttonName;
    }

    private static final HashMap<String, Light> lights = new HashMap<>();

    public static Light getLightByButtonName(String buttonName) {
        return lights.get(buttonName);
    }

    @Getter
    private final SignalStateProperty signalStateProperty = new SignalStateProperty();

    public void setSignalState(SignalState signalState) {
        this.signalStateProperty.set(signalState);
    }

    @Getter
    private static final List<Light> focusedLight = new ArrayList<>();

    @Getter
    private final FocusedProperty focusedProperty = new FocusedProperty(false);

    public boolean isLightFocused() {
        return focusedProperty.get();
    }

    private void focus() {
        focusedLight.add(this);
        setStroke(AppleColor.GREEN);
        setStrokeWidth(FOCUSED_STROKE_WIDTH);
        focusedProperty.set(true);
    }

    private void defocus() {
        focusedLight.remove(this);
        setStroke(DEFAULT_STROKE_FILL);
        setStrokeWidth(DEFAULT_STROKE_WIDTH);
        focusedProperty.set(false);
    }

    public static void defocusAll() {
        focusedLight.forEach(e -> {
            e.setStroke(DEFAULT_STROKE_FILL);
            e.setStrokeWidth(DEFAULT_STROKE_WIDTH);
            e.focusedProperty.set(false);
        });
        focusedLight.clear();
    }

    Light(SignalBase signal, String buttonName) {
        super(DEFAULT_CIRCLE_RADIUS);
        setStroke(DEFAULT_STROKE_FILL);
        setStrokeWidth(DEFAULT_STROKE_WIDTH);
        setSignalState(DEFAULT_SIGNAL_STATE);
        signalStateProperty.addListener((observable, oldvalue, newvalue) -> setFill(newvalue.getColor()));
        setOnMouseClicked(e -> {
            if (focusedProperty.get()) defocus();
            else focus();
            //若多於兩個則嘗試創建進路
            var controller = MainController.getInstance();
            switch (MainController.getInstance().getAction()) {
                //新建進路
                case NEW -> {
                    if (focusedLight.size() >= 2) {
                        controller.handleCreateRoute(focusedLight);
                    }
                }
                //取消進路
                case CANCEL -> controller.getRoutes().forEach((k, v) -> {
                    if (v.equals(this)) controller.handleCancelRoute(k);
                });
                //總人解
                case MANUAL_UNLOCK, ERROR_UNLOCK -> {
                    //TODO
                }
                //區故解
            }
        });
        this.signal = signal;
        this.buttonName = buttonName;
        Platform.runLater(() -> {
            lights.put(getButtonName(), this);
        });
    }
}
