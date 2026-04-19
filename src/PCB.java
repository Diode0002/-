public class PCB extends Job {

    private int pid;
    private int pc;          // 程序计数器
    private int state;       // 0=READY,1=RUNNING,2=BLOCKED
    private int InstructionCount;//指令条数

    private int memorySize;  // 占用内存
    private int baseAddress; // 起始地址
    private int priorityLevel;   // 当前所在队列级别
    private int timeSliceLeft;   // 剩余时间片
    private int blockedSince;    // 阻塞开始时的仿真时间
    private int EndTimes;      // 进程结束时间
    private int createTime;      // 进程创建时间
    private int needA;           // 所需设备A数量
    private int needB;           // 所需设备B数量
    private int wakeupTime;     //唤醒时间

    private int lastReadyTime;  // 最近一次进入就绪队列的时间
    private int lastBlockTime;  // 最近一次进入阻塞队列的时间
    private int lastRunTime;    // 最近一次开始运行的时间

    public PCB(int jobId, int inTime, int instructionCount, int pid) {
        super(jobId, inTime,0,0,0, instructionCount);//job，三个0用于占位，后续采用set更新
        this.pid = pid;
        this.pc = 0;
        this.state = 0; // READY
        this.InstructionCount=instructionCount;
        //用指令数转换为内训大小
        this.memorySize = instructionCount;
        this.baseAddress = -1; // 初始未分配
        this.priorityLevel = 1;   // 新进程默认进入一级队列
        this.timeSliceLeft = 0;//当前时间片余量，初始为0
        this.blockedSince = -1;//进入阻塞的时间点，初始-1为未阻塞
        this.EndTimes = -1;//结束时间，同上
        this.createTime=-1;//创建时间，同上
        this.needA = 0;//所需设备A的数量
        this.needB = 0;//B
    }
    //get/set
    public int getPid() {
        return pid;
    }
    public void setPid(int pid){this.pid=pid;}

    public int getInstructionCount(){
        return InstructionCount;
    }

    public int getMemorySize() {
        return memorySize;
    }
    public void setMemorySize(int memorySize){this.memorySize=memorySize;}

    public int getPc(){return pc;}
    public void setPc(int pc){this.pc=pc;}

    public int getBaseAddress() {
        return baseAddress;
    }
    public void setBaseAddress(int baseAddress) {
        this.baseAddress = baseAddress;
    }

    public void setState(int state) {
        this.state = state;
    }
    public int getState() {
        return state;
    }

    public int getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(int level) { this.priorityLevel = level; }

    public int getTimeSliceLeft() { return timeSliceLeft; }
    public void setTimeSliceLeft(int t) { this.timeSliceLeft = t; }

    public int getBlockedSince() { return blockedSince; }
    public void setBlockedSince(int t) { this.blockedSince = t; }

    public int getEndTimes() { return EndTimes; }
    public void setEndTimes(int t) { this.EndTimes = t; }

    public int getCreateTime() { return createTime; }
    public void setCreateTime(int createTime) { this.createTime = createTime; }

    public int getNeedA() { return needA; }
    public void setNeedA(int needA) { this.needA = needA; }

    public int getNeedB() { return needB; }
    public void setNeedB(int needB) { this.needB = needB; }

    public int getWakeupTime(){
        return wakeupTime;
    }
    public void setWakeupTime(int wakeupTime){
        this.wakeupTime=wakeupTime;
    }

    public int getLastReadyTime() { return lastReadyTime; }
    public void setLastReadyTime(int t) { this.lastReadyTime = t; }

    public int getLastBlockTime() { return lastBlockTime; }
    public void setLastBlockTime(int t) { this.lastBlockTime = t; }

    public int getLastRunTime() { return lastRunTime; }
    public void setLastRunTime(int t) { this.lastRunTime = t; }

    public String getStateDesc() {
        switch (state) {
            case 0: return "就绪";
            case 1: return "运行";
            case 2: return "阻塞";
            case 3: return "终止";
            default: return "未知";
        }
    }
}
