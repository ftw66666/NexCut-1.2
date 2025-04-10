package com.example.opencv;

public class Constant {
    public static final int DeviceStartAddr = 1000;

    public static final int DeviceRegCount = 64;

    public static final String[] DeviceRegnName = new String[]{
            "程序标识", "软件版本", "日期", "时间",
            "输入状态", "输出状态", "报警状态 1", "报警状态 2",
            "运行状态", "NULL", "DA1", "DA2", "NULL", "PWM频率",
            "PWM占空比", "NULL", "NULL", "NULL",
            "从站设备信息", "加工状态", "加工位置", "扩展输入状态",
            "扩展输出状态", "NULL", "NULL", "通电时间", "通讯时间", "开光时间",
            "双驱反馈派偏差", "NULL", "NULL", "参数状态"
    };

    public static final int MachineAddr = 800;

    public static final int MachineRegCount = 3;

    public static final String[] AxisRegnName = {
            "NULL", "速度",
            "脉冲位置", "NULL",
            "加工停止时位置",
            "NULL", "累计行程",
    };

    public static final int AxisStartAddr = 2000;

    public static final int AxisRegCount = 28;

    public static final int CommandAddr = 101;

    public static final int Stop = 1;

    public static final int Back = 2;

    public static final int AxisRun = 3;

    public static final int DO = 4;

    public static final int DA = 5;

    public static final int FileStart = 7;

    public static final int FileStop = 8;

    public static final int Ftc = 9;

    public static final int OfflineProcess = 12;

    public static final int WorkBroder = 13;

    public static int PlatformWidth = 400;

    public static volatile int PlatformHeight = 400;

    public static volatile int ProcessState = 0xFF;

    public static int PrintWidth = 50;
    public static int PrintHeight = 50;
    public static double PrintStartX = 175;
    public static double PrintStartY = 175;



}
