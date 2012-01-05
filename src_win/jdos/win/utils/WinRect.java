package jdos.win.utils;

import jdos.hardware.Memory;

public class WinRect {
    public int left;
    public int top;
    public int right;
    public int bottom;

    public WinRect(int address) {
        left = Memory.mem_readd(address);
        top = Memory.mem_readd(address+4);
        right = Memory.mem_readd(address+8);
        bottom = Memory.mem_readd(address+12);
    }

    public static void write(int address, int left, int top, int right, int bottom) {
        Memory.mem_writed(address, left);
        Memory.mem_writed(address+4, top);
        Memory.mem_writed(address+8, right);
        Memory.mem_writed(address+12, bottom);
    }
}
