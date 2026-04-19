import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;



public class OSKernel {

    // 后备队列
    public static Queue<Job> backupQueue = new LinkedList<>();

    // 就绪队列
    public static Queue<PCB> readyQueue1 = new LinkedList<>();//一级队列
    public static Queue<PCB> readyQueue2 = new LinkedList<>();//二级队列
    //阻塞队列
    public static Queue<PCB> inputblockQueue = new LinkedList<>();//输入阻塞
    public static Queue<PCB> outputblockQueue = new LinkedList<>();//输出阻塞
    // 内存管理
    public static Memory memory = new Memory(5000); // 总内存5000
    //已加载的作业列表
    public  static List<Job> allJobs=new ArrayList<>();
    //已创建的进程
    public static List<PCB> allPCBs=new ArrayList<>();
    //当前运行的进程
    public static PCB currentRunningProcess=null;
    // 外部设备
    public static Device deviceA = new Device(2,1,inputblockQueue,"键盘输入设备A"); // 设备A 2个
    public static Device deviceB = new Device(1,2,outputblockQueue,"屏幕输出设备B"); // 设备B 1个
    //历史记录列表
    public static List<Object[]> readyQueue1History = new ArrayList<>();   //一级就绪队列
    public static List<Object[]> readyQueue2History = new ArrayList<>();   //二级就绪队列
    public static List<Object[]> inputBlockHistory = new ArrayList<>();     //输入阻塞队列
    public static List<Object[]> outputBlockHistory = new ArrayList<>();    //输出阻塞队列
    public static List<Object[]> runningHistory = new ArrayList<>();        //运行态队列
    public static List<Object[]> backupHistory = new ArrayList<>(); //请求作业

}
