import java.util.LinkedList;
import java.util.Queue;
public class Device {
    private int total; // 总资源数
    private int used;  // 已使用
    private String name;//设备名称
    private Queue<PCB> waitQueue;//等待使用设备
    private Queue<PCB> blockQueue;   // I/O阻塞队列
    private int deviceType;

    public Device(int total, int deviceType, Queue<PCB> blockQueue,String name) {
        this.total = total;
        this.used = 0;
        this.waitQueue = new LinkedList<>();
        this.deviceType = deviceType;
        this.blockQueue = blockQueue;
        this.name=name;
    }
    //分配
    public synchronized boolean canAllocate(PCB pcb) {
        if (used < total) {
            used++;
            Log.log(ClockAndJobSchedulingThread.simulationTime + ":[设备分配:" + name + " 分配给进程" + pcb.getPid()
                    + name+"=" + (total - used)+"]");
            return true;
        } else {
            waitQueue.offer(pcb);
            pcb.setState(2);
            Log.log(ClockAndJobSchedulingThread.simulationTime + ":[设备等待:" + name + " 忙，进程" + pcb.getPid() + " 加入等待队列]");
            return false;
        }
    }
    //释放资源
    public synchronized void release() {
        if (used > 0) used--;
        PCB pcb = waitQueue.poll();
        if (pcb != null) {
            pcb.setState(0);// 就绪
            pcb.setPriorityLevel(1); // 放入一级队列
            pcb.setTimeSliceLeft(1);
            OSKernel.readyQueue1.offer(pcb);
            Log.log(ClockAndJobSchedulingThread.simulationTime + ":[设备唤醒:" +name+ " 唤醒进程" + pcb.getPid()+"]");
        }
    }
    //空闲资源数
    public int getFree() {
        return total - used;
    }
    //已使用资源数
    public int getUsed() {
        return used;
    }
}