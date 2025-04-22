package helpers.services.utils;

public interface IHandlerService {
    boolean isActiveNow();
    boolean isPostponed();
    long getTimeUntilNextEvent();
    boolean isEnabled();
}