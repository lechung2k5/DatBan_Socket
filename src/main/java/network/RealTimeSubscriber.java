package network;

import java.util.function.Consumer;

/**
 * Giao diện dành cho các Controller muốn nhận thông báo Real-time.
 * Giúp quản lý vòng đời của bộ lắng nghe (Listener) để tránh rò rỉ bộ nhớ.
 */
public interface RealTimeSubscriber {
    /**
     * Trả về bộ lắng nghe sự kiện của Controller này.
     */
    Consumer<RealTimeEvent> getRealTimeListener();

    /**
     * Dọn dẹp listener khi Controller không còn hiển thị.
     */
    default void cleanupRealTime() {
        if (getRealTimeListener() != null) {
            RealTimeClient.getInstance().removeListener(getRealTimeListener());
        }
    }
}
