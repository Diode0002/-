import java.util.Iterator;

public class InputBlockThread extends Thread {
    @Override
    public void run() {
        while (true) {
            // 暂停检查
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

            SyncManager.knlock.lock();
            try {
                Iterator<PCB> iterator = OSKernel.inputblockQueue.iterator();
                while (iterator.hasNext()) {
                    PCB pcb = iterator.next();
                    int waitTime = ClockAndJobSchedulingThread.simulationTime - pcb.getBlockedSince();
                    if (waitTime >= 2) {
                        // 达到2秒，唤醒
                        iterator.remove();  // 从阻塞队列移除
                        pcb.setState(0);    // 就绪态
                        pcb.setPriorityLevel(1); // 回到一级队列
                        pcb.setBlockedSince(-1);
                        OSKernel.deviceA.release();//释放A
                        OSKernel.inputBlockHistory.add(new Object[]{
                                ClockAndJobSchedulingThread.simulationTime,
                                pcb.getPid(),
                                pcb.getJobId(),
                                pcb.getInstructionCount() - pcb.getPc(),
                                "唤醒"
                        });
                        OSKernel.readyQueue1.offer(pcb);
                        pcb.setTimeSliceLeft(1);
                        pcb.setLastReadyTime(ClockAndJobSchedulingThread.simulationTime);
                        OSKernel.readyQueue1History.add(new Object[]{
                                ClockAndJobSchedulingThread.simulationTime,
                                pcb.getPid(),
                                pcb.getJobId(),
                                pcb.getPriorityLevel(),          // 优先级
                                pcb.getTimeSliceLeft(),          // 剩余时间片
                                pcb.getInstructionCount() - pcb.getPc()
                        });
                        Log.log(ClockAndJobSchedulingThread.simulationTime + ":[I/O完成重新进入就绪队列：" + pcb.getPid() + "," + (pcb.getInstructionCount() - pcb.getPc())+"]");//进程ID列表,剩余指令数列表
                    }
                }
                // 如果队列为空，则等待信号
                if (OSKernel.inputblockQueue.isEmpty()) {
                    SyncManager.inputnotempty.await();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                SyncManager.knlock.unlock();
            }

            // 短暂休眠，避免CPU空转
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}