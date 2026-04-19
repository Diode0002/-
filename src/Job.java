import java.util.List;

public class Job {
    private int jobId;               // 作业的唯一标识符
    private int inTime;              // 作业进入系统的时间
    private int priority;            //优先级
    private int instructionCount;    // 作业包含的指令数量
    private List<Instruction> instructions; // 与作业关联的指令列表
    private int needA;//使用设备A
    private int needB;//设备B

    public Job(int jobId, int inTime, int priority,int needA,int needB,int instructionCount) {
        this.jobId = jobId;
        this.inTime = inTime;
        this.priority=priority;
        this.instructionCount = instructionCount;
        this.needA=needA;
        this.needB=needB;
        System.out.println("作业 " + jobId + " 读入成功，到达时间: " + inTime + "，指令数量: " + instructionCount);  // 每加载一个作业时打印
    }

    public int getJobId() {
        return jobId;
    }
    public int getInTime() {
        return inTime;
    }
    public int getInstructionCount() {
        return instructionCount;
    }
    public void decInstructionCount() {
        this.instructionCount--;
    }
    public int getPriority(){
        return priority;
    }
    public void setPriority(int priority){
        this.priority=priority;
    }
    public List<Instruction> getInstructions() {
        return instructions;
    }
    public int getNeedA() { return needA; }
    public int getNeedB() { return needB; }
    public void setNeedA(int needA){
        this.needA=needA;
    }
    public void setNeedB(int needB){
        this.needB=needB;
    }
    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
    }
    public void setInstructionCount(int instructionCount){
        this.instructionCount=instructionCount;
    }

    @Override
    public String toString() {
        return jobId + "," + inTime + "," + instructionCount;
    }
}