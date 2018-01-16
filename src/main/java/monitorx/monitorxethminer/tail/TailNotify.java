package monitorx.monitorxethminer.tail;

/**
 * @author qianlifeng
 */
public interface TailNotify {
    /**
     * LogTail有日志滚动事件产生后调用此方法
     *
     * @return
     */
    public void notifyMsg(String msg);
}
