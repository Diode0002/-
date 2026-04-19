import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CPU {
    private int pc; // 程序计数器
    private int ir; // 指令寄存器
    private int psw; // 程序状态字
    private Map<String, Integer> registerBackup; // 寄存器备份，用于进程切换时保存寄存器状态
    private static CPU instance = null; // 单例模式实例
    public static Process currentProcess; // 当前正在执行的进程
    public static int instructionPointer; // 当前进程的指令指针

    // 私有构造函数，防止外部实例化
    private CPU() {
        this.pc = 0;
        this.ir = 0;
        this.psw = 0;
        this.registerBackup = new HashMap<>();
        currentProcess = null;
        instructionPointer = 0;
    }

    public void runProcess() {

    }
    //执行一条指令
    public static boolean runInstruction(PCB pcb) {
        List<Instruction> insts = pcb.getInstructions();
        int pc = pcb.getPc();
        if (pc >= insts.size()) {
            return true; // 进程终止
        }
        Instruction inst = insts.get(pc);
        int type = inst.getState();
        int currentTime = ClockAndJobSchedulingThread.simulationTime;

        switch (type) {
            case 0: // 计算
                pcb.setPc(pc + 1);
                Log.log(currentTime + ":[运行进程：" + pcb.getJobId() + "," + pcb.getPid() + ":" + inst.getId() + ",计算," + (pcb.getBaseAddress() + pc * 20) + ",20]");//作业 ID,进程 ID:指令编号,指令类型,物理地址，数据大小
                break;
            case 1: // 键盘输入
                Log.log(currentTime + ":[运行进程：" + pcb.getJobId() + "," + pcb.getPid() + ":" + inst.getId() + ",键盘输入," + (pcb.getBaseAddress() + pc * 20) + ",20]");
                // 尝试分配设备A
                if (OSKernel.deviceA.canAllocate(pcb)) {
                    pcb.setPc(pc + 1);
                    // 获得设备，进入I/O阻塞队列
                    pcb.setState(2); // 阻塞
                    pcb.setBlockedSince(currentTime);
                    pcb.setLastBlockTime(currentTime);
                    OSKernel.inputblockQueue.offer(pcb);
                    // 构造阻塞队列所有进程ID列表
                    StringBuilder pidList = new StringBuilder();
                    for (PCB p : OSKernel.inputblockQueue) {
                        pidList.append(p.getPid()).append(",");
                    }
                    // 添加阻塞历史
                    OSKernel.inputBlockHistory.add(new Object[]{
                            currentTime,
                            pcb.getPid(),
                            pcb.getJobId(),
                            pcb.getInstructionCount() - pcb.getPc(),
                            "阻塞"
                    });
                    // 唤醒输入线程
                    synchronized (SyncManager.inputnotempty) {
                        SyncManager.inputnotempty.signal();
                    }
                    if (pidList.length() > 0) pidList.setLength(pidList.length() - 1);
                    Log.log(currentTime + ":[阻塞进程:1," + pidList.toString()+"]");
                } else {
                    // 设备忙，进程已加入设备等待队列
                    Log.log(currentTime + ":[等待设备A：" + pcb.getPid()+"]");
                }
                break;
            case 2: // 屏幕输出
                Log.log(currentTime + ":[运行进程：" + pcb.getJobId() + "," + pcb.getPid() + ":" + inst.getId() + ",屏幕输出," + (pcb.getBaseAddress() + pc * 20) + ",20]");
                // 尝试分配设备B
                if (OSKernel.deviceB.canAllocate(pcb)) {
                    pcb.setPc(pc + 1);
                    pcb.setState(2);
                    pcb.setBlockedSince(currentTime);
                    pcb.setLastBlockTime(currentTime);
                    OSKernel.outputblockQueue.offer(pcb);
                    // 构造阻塞队列所有进程ID列表
                    StringBuilder pidList = new StringBuilder();
                    for (PCB p : OSKernel.outputblockQueue) {
                        pidList.append(p.getPid()).append(",");
                    }
                    OSKernel.outputBlockHistory.add(new Object[]{
                            currentTime,
                            pcb.getPid(),
                            pcb.getJobId(),
                            pcb.getInstructionCount() - pcb.getPc(),
                            "阻塞"
                    });
                    synchronized (SyncManager.outputnotempty) {
                        SyncManager.outputnotempty.signal();
                    }
                    if (pidList.length() > 0) pidList.setLength(pidList.length() - 1);
                    Log.log(currentTime + ":[阻塞进程:2," + pidList.toString()+"]");
                } else {
                    Log.log(currentTime + ":[等待设备B：" + pcb.getPid()+"]");
                }
                break;
            default:
                pcb.setPc(pc + 1);
        }
        return pcb.getPc() >= insts.size(); // 是否执行完
    }
    //CPU现场保护
    private void CPU_PRO() {

    }

    //CPU现场恢复
    private void CPU_REC() {

    }
}
