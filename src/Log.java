import javax.swing.*;
import java.io.*;
import java.util.*;
public class Log {
    private static StringBuilder logbuffer=new StringBuilder();//缓冲区
    private static GUI gui=null;//gui界面
    public static void setGui(GUI g){
        gui=g;
    }
    public static synchronized void log(String message){
        System.out.println(message);
        logbuffer.append(message+"\n");
        if(gui!=null){//更新gui
            gui.addLog(message);
            gui.updateInterface();//刷新界面
        }
    }
    public static String getlog(){//获取日志内容
      return logbuffer.toString();
    }
    public static void clearlog(){//清空日志
        logbuffer.setLength(0);
    }
    //保存日志到文件
    public static void savelog(String filename){
        try{
            File outd=new File("output");//打开地址
            BufferedWriter writer=new BufferedWriter(new FileWriter(filename));
            writer.write(logbuffer.toString());
            for(PCB pcb:OSKernel.allPCBs){
                if(pcb.getEndTimes()>0){
                    writer.write(pcb.getEndTimes() + ":[进程 ID:" + pcb.getPid() +
                            ":作业请求时间:" + pcb.getInTime() +
                            "+进入时间:" + pcb.getCreateTime() +
                            "+总运行时间:" + (pcb.getEndTimes() - pcb.getCreateTime()) + "]\n");
                }
            }
            writer.close();
        } catch (Exception e) {
            log("保存日志失败：" + e.getMessage());
        }
    }
}

