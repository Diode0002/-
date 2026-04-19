import java.util.Iterator;
import java.util.List;

public class ClockAndJobSchedulingThread extends Thread {

    public static int simulationTime = 0;//当前时间
    public static int milliseconds = 1000;//时钟间隔
    private int nextJobIndex=0;//下一条作业索引
    //无参构造函数
    public ClockAndJobSchedulingThread(){
        setName("clockthread");
    }
    @Override
    public void run() {//实现暂停与继续的控制
        while (true) {
            SyncManager.pauseLock.lock();
            try {
                //检查是否运行
                while (!SyncManager.isRunning) {
                    // 还没开始执行，等待
                    SyncManager.pauseCondition.await();
                }
                // 检查是否暂停
                while (SyncManager.isPaused) {
                    // 暂停状态，等待
                    SyncManager.pauseCondition.await();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            } finally {
                SyncManager.pauseLock.unlock();
            }
            //时钟线程主循环
            // 获取锁，确保线程同步
            SyncManager.clkandjstlock.lock(); // 获取锁
            try {
                // =====================
                // 1. 时钟推进
                // =====================
                simulateTimePassing(milliseconds);
                // =====================
                // 2. 作业调度
                // =====================
                jobScheduling();
                //唤醒调度线程
                SyncManager.pstlock.lock(); // 获取锁
                SyncManager.pstCondition.signal();  // 唤醒进程调度线程
                SyncManager.pstlock.unlock(); // 释放锁

                // 等待时钟作业进程调度线程完成
                SyncManager.clkandjstCondition.await();

            } catch (InterruptedException e) {
                // 捕获异常，如果线程被中断，则打印异常信息
                // 请在此处处理线程中断异常
                Thread.currentThread().interrupt();
                e.printStackTrace();
            } finally {
                // 释放锁，允许其他线程访问共享资源
                SyncManager.clkandjstlock .unlock(); // 释放锁
            }
        }
    }

    // =====================
    // 获取当前时间
    // =====================
    public static int getCurrentTime() {
        return simulationTime;
    }

    // =====================
    // 模拟时间流逝
    // =====================
    public static void simulateTimePassing(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
            simulationTime++;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // =====================
    // 作业调度逻辑
    // =====================
    public void jobScheduling() {

        // TODO:
        // 1. 判断是否有新作业到达加入后备队列
        if (simulationTime % 2 == 0) {//每2秒检查
            while (nextJobIndex < OSKernel.allJobs.size()) {
                Job job = OSKernel.allJobs.get(nextJobIndex);
                if (job.getInTime() <= simulationTime) {
                    OSKernel.backupQueue.offer(job);
                    // 添加历史记录
                    OSKernel.backupHistory.add(new Object[]{
                            job.getJobId(),
                            job.getInTime(),
                            job.getInstructionCount(),
                            job.getNeedA(),
                            job.getNeedB(),
                            "后备"
                    });
                    Log.log(simulationTime + ":[新增作业：" + job.getJobId() + "," + job.getInTime() + "," + job.getInstructionCount()+"]");//作业 ID,请求时间,指令数量
                    nextJobIndex++;
                } else {
                    break;
                }
            }

            // 2. 判断内存资源是否满足 → 分配内存资源 → 创建进程 → 放入就绪队列
            SyncManager.knlock.lock();//加锁保护
            try {
                if (!OSKernel.backupQueue.isEmpty()) {//后备队列有进程，开始判断
                    Job job = OSKernel.backupQueue.peek();
                    //计算所需内存
                    int ccount = 0;
                    for (Instruction instruction : job.getInstructions()) {
                        if (instruction.getState() == 0) ccount++;//查找计算类指令条数
                    }
                    //内存=计算类指令条数*20
                    int need = ccount * 20;
                    //检查设备资源
                    if (OSKernel.memory.canAllocate(need)) {//分配内存
                        int address = OSKernel.memory.allocate(need);
                        if (address != -1) {
                            int pid = OSKernel.allPCBs.size() + 1;
                            PCB pcb = new PCB(job.getJobId(), job.getInTime(),
                                    job.getInstructions().size(), pid);
                            //修改pcb各个属性
                            pcb.setMemorySize(need);
                            pcb.setBaseAddress(address);
                            pcb.setInstructions(job.getInstructions());
                            pcb.setNeedA(job.getNeedA());
                            pcb.setNeedB(job.getNeedB());
                            pcb.setCreateTime(simulationTime); // 记录创建时间

                            OSKernel.allPCBs.add(pcb);
                            OSKernel.readyQueue1.offer(pcb);
                            pcb.setTimeSliceLeft(1);
                            pcb.setLastReadyTime(simulationTime);
                            OSKernel.readyQueue1History.add(new Object[]{
                                    simulationTime,
                                    pcb.getPid(),
                                    pcb.getJobId(),
                                    pcb.getPriorityLevel(),          // 优先级
                                    pcb.getTimeSliceLeft(),          // 剩余时间片
                                    pcb.getInstructionCount() - pcb.getPc()
                            });
                            OSKernel.backupQueue.poll(); // 移除队头作业

                            Log.log(simulationTime + ":[创建进程：" + job.getJobId() + "," + pid + "," + address + "," + need+"]");//作业 ID,进程 ID,PCB 内存块始地址,分配内存大小
                        } else {
                            Log.log(simulationTime + ":[内存不足：" + job.getJobId() + " 需内存:" + need+"]");
                        }
                    } else {
                        Log.log(simulationTime + ":[资源不足：" + job.getJobId() + " 需设备A=" + job.getNeedA() + " 设备B=" + job.getNeedB()+"]");
                    }
                }
            } finally {
                SyncManager.knlock.unlock();
            }
        }
    }

    //引入外部控制，是的gui界面的按钮可以暂停进程
    //开始
    public static void startSimulation() {
        SyncManager.pauseLock.lock();
        try {
            SyncManager.isRunning = true;
            SyncManager.isPaused = false;
            SyncManager.pauseCondition.signalAll();
        } finally {
            SyncManager.pauseLock.unlock();
        }
    }
    //暂停
    public static void pauseSimulation() {
        SyncManager.pauseLock.lock();
        try {
            SyncManager.isPaused = true;
        } finally {
            SyncManager.pauseLock.unlock();
        }
    }
    //恢复
    public static void resumeSimulation() {
        SyncManager.pauseLock.lock();
        try {
            SyncManager.isPaused = false;
            SyncManager.pauseCondition.signalAll();
        } finally {
            SyncManager.pauseLock.unlock();
        }
    }
    //停止
    public static void stopSimulation() {
        SyncManager.pauseLock.lock();
        try {
            SyncManager.isRunning = false;
            SyncManager.isPaused = false;
            SyncManager.pauseCondition.signalAll();
        } finally {
            SyncManager.pauseLock.unlock();
        }
    }
}
