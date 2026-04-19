import java.util.ArrayList;
import java.util.List;

public class Memory {
    //内存块
    public static class Block{
        public int start, size;
        public boolean free;
        Block(int start, int size, boolean free) {
            this.start = start;//起始位置
            this.size = size;//大小
            this.free = free;//是否空闲
        }
    }
    private int totalMemory;     // 总内存
    private int usedMemory;      // 已使用内存
    private List<Block> blocks;//内存块

    public Memory(int totalMemory) {//有参构造函数
        this.totalMemory = totalMemory;
        this.usedMemory = 0;
        blocks=new ArrayList<>();
        blocks.add(new Block(0,totalMemory,true));
    }
    public List<Block> getBlocks() {
        return new ArrayList<>(blocks);
    }

    // =========================
    // 1. 判断是否可以分配
    // =========================
    public boolean canAllocate(int size) {
        //return (totalMemory - usedMemory) >= size;
        int free = 0;
        for (Block b : blocks) if (b.free) free += b.size;
        return free >= size;
    }

    // =========================
    // 2. 分配内存
    // =========================
    public int allocate(int size) {
    //    if (canAllocate(size)) {
    //        usedMemory += size;
    //        return true;
    //    }
    //    return false;
        //采用最佳适应算法分配
        Block best = null;
        for (Block b : blocks) {//遍历寻找最佳适应的内存块
            if (b.free && b.size >= size) {//可分配
                if (best == null || b.size < best.size) {//是否存在更小的
                    best = b;
                }
            }
        }
        if (best == null) return -1;//没找到QAQ
        int addr = best.start;//初始地址
        if (best.size == size) {//分配
            best.free = false;
        } else {
            // 拆分
            Block newBlock = new Block(best.start + size, best.size - size, true);
            blocks.add(blocks.indexOf(best) + 1, newBlock);
            best.size = size;
            best.free = false;
        }
        return addr;
    }

    // =========================
    // 3. 释放内存
    // =========================
    public void free(int size,int start) {
        //usedMemory -= size;
        //if (usedMemory < 0) {
        //    usedMemory = 0;
        //}
        for (Block b : blocks) {
            if (!b.free && b.start == start && b.size == size) {
                b.free = true;
                //合并相邻空闲块
                List<Block> merged = new ArrayList<>();
                for (Block fb : blocks) {
                    if (merged.isEmpty()) {
                        merged.add(fb);
                    } else {
                        Block last = merged.get(merged.size() - 1);
                        if (last.free && fb.free && last.start + last.size == fb.start) {
                            last.size += fb.size;
                        } else {
                            merged.add(fb);
                        }
                    }
                }
                blocks.clear();
                blocks.addAll(merged);
                return;
            }
        }
    }

    // =========================
    // 4. 查看剩余内存
    // =========================
    public int getFreeMemory() {
        //return totalMemory - usedMemory;
        int free = 0;
        for (Block b : blocks) {
            if (b.free) free += b.size;
        }
        return free;
    }

    public int getUsedMemory() {
        return totalMemory-getFreeMemory();
    }
}
