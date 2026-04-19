import java.util.Random;
public class ProcessSchedulingHandlerThread extends Thread {
    //时间片
    public static int timeSlice = 0;
    @Override
    public void run() {
        SyncManager.pstlock.lock();
        try {
            SyncManager.processThreadReady = true;
            SyncManager.pstCondition.signal();
        } finally {
            SyncManager.pstlock.unlock();
        }
        // 持续运行线程，模拟进程调度
        while (true) {
            //暂停检查
            SyncManager.pauseLock.lock();
            try {
                while (!SyncManager.isRunning) {
                    SyncManager.pauseCondition.await();
                }
                while (SyncManager.isPaused) {
                    SyncManager.pauseCondition.await();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                SyncManager.pauseLock.unlock();
            }
            // 获取锁，确保线程同步
            SyncManager.pstlock.lock();// 获取锁
            try {
                SyncManager.pstCondition.await();


                //任务需求：
                // 1.根据策略是否进行进程调度（1.判断当前CPU进程是否为空，为空调度。2. 时间片为0则进行调度分配时间片。3.判断是否有更高优先级的进程，采用的是可剥夺的多级反馈队列。进程调度完成分配时间片）

                // 2.CPU执行.进程状态应该从就绪态（Ready）转换到运行态（Running）。（1.进程执行完毕则转到终止态（Terminated），并初始化时间片为0。2.时间片归0则重新放入就绪队列。）

                //重要提醒，由于作业调度线程和进程调度线程并行执行，在使用就绪队列需要加锁避免同时使用就绪队列，以及自定义的一些两个线程都要用到的资源都需要加锁避免访问时出错

                // 模拟进程调度完成后的输出
                SyncManager.knlock.lock();
                try {
                    // 1. 执行当前进程
                    if (OSKernel.currentRunningProcess != null) {//当前cpu进程有正在运行
                        PCB cur = OSKernel.currentRunningProcess;//当前运行的进程控制块
                        boolean terminated = CPU.runInstruction(cur);//是否终止
                        if (terminated) {//执行完所有指令，终止
                            cur.setEndTimes(ClockAndJobSchedulingThread.simulationTime);
                            OSKernel.memory.free(cur.getMemorySize(), cur.getBaseAddress());
                            cur.setState(3); // 终止
                            OSKernel.currentRunningProcess = null;
                            Log.log(ClockAndJobSchedulingThread.simulationTime + ":[终止进程：" + cur.getPid()+"]");
                        } else if (cur.getState() == 2) { // 阻塞
                            OSKernel.currentRunningProcess = null;
                        } else {
                            // 减少时间片
                            cur.setTimeSliceLeft(cur.getTimeSliceLeft() - 1);
                            if (cur.getTimeSliceLeft() <= 0) {
                                cur.setState(0); // 就绪
                                if (cur.getPriorityLevel() == 1) {
                                    cur.setPriorityLevel(2);
                                    cur.setTimeSliceLeft(2);
                                    OSKernel.readyQueue2.offer(cur);
                                    cur.setLastReadyTime(ClockAndJobSchedulingThread.simulationTime);
                                    OSKernel.readyQueue2History.add(new Object[]{
                                            ClockAndJobSchedulingThread.simulationTime,
                                            cur.getPid(),
                                            cur.getJobId(),
                                            cur.getPriorityLevel(),
                                            cur.getTimeSliceLeft(),
                                            cur.getInstructionCount() - cur.getPc()
                                    });
                                    Log.log(ClockAndJobSchedulingThread.simulationTime + ":[进程降级：" + cur.getPid()+"]");
                                } else {//重新返回二级就绪
                                    cur.setTimeSliceLeft(2);
                                    OSKernel.readyQueue2.offer(cur);
                                    cur.setLastReadyTime(ClockAndJobSchedulingThread.simulationTime);
                                    OSKernel.readyQueue2History.add(new Object[]{
                                            ClockAndJobSchedulingThread.simulationTime,
                                            cur.getPid(),
                                            cur.getJobId(),
                                            cur.getPriorityLevel(),
                                            cur.getTimeSliceLeft(),
                                            cur.getInstructionCount() - cur.getPc()
                                    });
                                    Log.log(ClockAndJobSchedulingThread.simulationTime + ":[进程重入队：" + cur.getPid()+"]");
                                }
                                OSKernel.currentRunningProcess = null;
                            }
                        }
                    }

                    // 2. 调度新进程
                    if (OSKernel.currentRunningProcess == null) {//cpu空闲
                        PCB next = null;//下一条指令
                        if (!OSKernel.readyQueue1.isEmpty()) {//先从一级队列中取
                            next = OSKernel.readyQueue1.poll();
                            next.setPriorityLevel(1);
                            next.setTimeSliceLeft(1);
                        } else if (!OSKernel.readyQueue2.isEmpty()) {//二级取
                            next = OSKernel.readyQueue2.poll();
                            next.setPriorityLevel(2);
                            next.setTimeSliceLeft(2);
                        }
                        if (next != null) {//运行下一条指令
                            next.setState(1); // 运行
                            OSKernel.currentRunningProcess = next;
                            next.setLastRunTime(ClockAndJobSchedulingThread.simulationTime);
                            OSKernel.runningHistory.add(new Object[]{
                                    ClockAndJobSchedulingThread.simulationTime,
                                    next.getJobId(),          // 作业ID
                                    next.getPid(),            // 进程ID
                                    next.getPc() + 1,         // 正在执行的指令编号
                                    next.getPriorityLevel(),  // 优先级
                                    next.getInstructionCount() - next.getPc()  // 剩余指令数
                            });
                            Log.log(ClockAndJobSchedulingThread.simulationTime + ":[进程调度：" + next.getPid()+","+ next.getPriorityLevel()+","+ next.getTimeSliceLeft()+"]");//进程id,优先级,剩余时间片
                        } else {
                            Log.log(ClockAndJobSchedulingThread.simulationTime + ":[CPU空闲]");
                        }
                    } else {
                        // 3. 抢占：当前进程在二级，一级队列非空
                        PCB cur = OSKernel.currentRunningProcess;
                        if (cur.getPriorityLevel() == 2 && !OSKernel.readyQueue1.isEmpty()) {
                            cur.setState(0);
                            OSKernel.readyQueue2.offer(cur);
                            cur.setLastReadyTime(ClockAndJobSchedulingThread.simulationTime);
                            OSKernel.readyQueue2History.add(new Object[]{
                                    ClockAndJobSchedulingThread.simulationTime,
                                    cur.getPid(),
                                    cur.getJobId(),
                                    cur.getPriorityLevel(),
                                    cur.getTimeSliceLeft(),
                                    cur.getInstructionCount() - cur.getPc()
                            });
                            PCB next = OSKernel.readyQueue1.poll();
                            next.setPriorityLevel(1);
                            next.setTimeSliceLeft(1);
                            next.setState(1);
                            OSKernel.currentRunningProcess = next;
                            next.setLastRunTime(ClockAndJobSchedulingThread.simulationTime);
                            // 获取即将执行的指令信息
                            int pc = next.getPc();
                            String instType = "计算";
                            int physAddr = next.getBaseAddress() + pc * 20;
                            if (pc < next.getInstructions().size()) {
                                int state = next.getInstructions().get(pc).getState();
                                if (state == 1) instType = "键盘输入";
                                else if (state == 2) instType = "屏幕输出";
                            }
                            OSKernel.runningHistory.add(new Object[]{
                                    ClockAndJobSchedulingThread.simulationTime,
                                    next.getJobId(),          // 作业ID
                                    next.getPid(),            // 进程ID
                                    next.getPc() + 1,         // 正在执行的指令编号
                                    next.getPriorityLevel(),  // 优先级
                                    next.getInstructionCount() - next.getPc()  // 剩余指令数
                            });
                            Log.log(ClockAndJobSchedulingThread.simulationTime + ":[进程抢占：" + cur.getPid() + "被" + next.getPid()+"抢占]");
                        }
                    }
                } finally {
                    SyncManager.knlock.unlock();
                }

                SyncManager.clkandjstlock.lock();
                SyncManager.clkandjstCondition.signal();
                SyncManager.clkandjstlock.unlock();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            } finally {
                SyncManager.pstlock.unlock();
            }
        }
    }
}