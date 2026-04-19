import java.io.*;
import java.util.*;

public class Main{
    public static void main(String[] args) {

        //此部分为第一次实验完成内容，功能为系统启动前预先加载作业
        String jobsFilePath = "input/jobs-input.txt";
        String instructionsFolderPath = "input/";

        try {
            // 调用加载作业和指令的封装方法，并获取返回的作业列表
            List<Job> jobs = JobandInstructionLoader.loadAllJobsAndInstructions(jobsFilePath, instructionsFolderPath);

            System.out.println("\n所有作业加载完成：");
            for (Job job : jobs) {
                System.out.println("作业ID: " + job.getJobId() + ", 到达时间: " + job.getInTime() + ", 指令数量: " + job.getInstructionCount());
            }
            OSKernel.allJobs=jobs;//将作业列表存入OSkernel
        } catch (IOException e) {
            System.err.println("加载作业和指令时出错: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 启动 GUI
        //GUI.main(args);
        GUI gui=new GUI();
        gui.setVisible(true);

        // 此部分为第二次实验，时钟作业调度线程和进程调度线程的依次执行
        Thread processSchedulingHandlerThread = new ProcessSchedulingHandlerThread();
        processSchedulingHandlerThread.start();


        // 确保进程调度线程启动后再启动时钟作业调度线程
        SyncManager.pstlock.lock();
        try {
            while (!SyncManager.processThreadReady) {
                SyncManager.pstCondition.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            SyncManager.pstlock.unlock();
        }

        // 启动时钟作业调度线程
        Thread clockAndJobInterruptHandlerThread = new ClockAndJobSchedulingThread();
        clockAndJobInterruptHandlerThread.start();

        // 启动I/O线程
        Thread inputThread = new InputBlockThread();
        inputThread.start();
        Thread outputThread = new OutputBlockThread();
        outputThread.start();


    }
}
