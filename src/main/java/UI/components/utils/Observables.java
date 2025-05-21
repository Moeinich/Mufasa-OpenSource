package UI.components.utils;

import javafx.beans.property.*;

import static utils.Constants.IS_WINDOWS_USER;

public class Observables {
    public static BooleanProperty DEBUGGING_ENABLED = new SimpleBooleanProperty();
    public static BooleanProperty DEVLOGS_ENABLED = new SimpleBooleanProperty();
    public static BooleanProperty IS_DEVUI_OPEN = new SimpleBooleanProperty();
    public static BooleanProperty IS_PAINT_ENABLED = new SimpleBooleanProperty(true);
    public static BooleanProperty USE_DIRECT_CAPTURE = new SimpleBooleanProperty(false);
    public static BooleanProperty USE_PW_CAPTURE = new SimpleBooleanProperty(false);

    public static DoubleProperty WIDTH_OBSERVABLE = new SimpleDoubleProperty();
    public static DoubleProperty HEIGHT_OBSERVABLE = new SimpleDoubleProperty();

    public static IntegerProperty GAME_REFRESHRATE = new SimpleIntegerProperty(IS_WINDOWS_USER ? 200 : 500);}