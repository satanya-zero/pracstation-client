package dev.glycine.pracstation.models;

import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
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

    public String getButtonName(){
        return signal.sid+buttonName;
    }

    @Getter
    private static final List<Light> focusedLight = new ArrayList<>();

    private boolean focused = false;

    private void focus() {
        focusedLight.add(this);
        setStroke(AppleColor.GREEN);
        setStrokeWidth(FOCUSED_STROKE_WIDTH);
        focused = true;
    }

    private void defocus() {
        focusedLight.remove(this);
        setStroke(DEFAULT_STROKE_FILL);
        setStrokeWidth(DEFAULT_STROKE_WIDTH);
        focused = false;
    }

    public static void defocusAll() {
        focusedLight.forEach(e -> {
            e.setStroke(DEFAULT_STROKE_FILL);
            e.setStrokeWidth(DEFAULT_STROKE_WIDTH);
            e.focused = false;
        });
        focusedLight.clear();
    }

    private void handleClick(MouseEvent mouseEvent) {
        if (focused) defocus();
        else focus();
    }

    Light(SignalBase signal, String buttonName) {
        super(DEFAULT_CIRCLE_RADIUS);
        setStroke(DEFAULT_STROKE_FILL);
        setStrokeWidth(DEFAULT_STROKE_WIDTH);
        setSignalState(DEFAULT_SIGNAL_STATE);
        signalStateProperty.addListener((observable, oldvalue, newvalue) -> setFill(newvalue.getColor()));
        setOnMouseClicked(this::handleClick);
        this.signal = signal;
        this.buttonName = buttonName;
    }

    @Getter
    private final SignalStateProperty signalStateProperty = new SignalStateProperty();

    public void setSignalState(SignalState signalState) {
        this.signalStateProperty.set(signalState);
    }
}
