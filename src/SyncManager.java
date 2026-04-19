import java.security.Key;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 同步管理类，包含锁、条件变量
 */
public class SyncManager {
    // 共享锁
    public static final Lock clkandjstlock = new ReentrantLock();
    public static final Lock pstlock = new ReentrantLock();

    public static boolean processThreadReady = false;


    // 条件变量：用于时钟线程和进程调度线程之间的同步
    public static final Condition clkandjstCondition = clkandjstlock.newCondition();  // 时钟线程的条件变量
    public static final Condition pstCondition = pstlock.newCondition();  // 进程调度线程的条件变量

    public static final Lock knlock=new ReentrantLock();//全局锁
    //I/O等待条件
    public static final Condition inputnotempty= knlock.newCondition();
    public static final Condition outputnotempty=knlock.newCondition();
    //暂停控制
    public static final Lock pauseLock = new ReentrantLock();
    public static final Condition pauseCondition = pauseLock.newCondition();
    public static volatile boolean isPaused = false;  // 暂停标志
    public static volatile boolean isRunning = false; // 运行标志
}
