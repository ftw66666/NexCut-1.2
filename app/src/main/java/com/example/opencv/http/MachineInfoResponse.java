package com.example.opencv.http;


import com.google.gson.annotations.SerializedName;

public class MachineInfoResponse {
    @SerializedName("ftc")
    private FTCStatus ftc;
    @SerializedName("mc")
    private MachineStatus mc;

    //------------- Gson 必须的组成部分 ---------------
    // 1. 必须有默认无参构造器
    public MachineInfoResponse() {
    }

    // 2. 字段要么是 public，要么通过标准 getter/setter 暴露
    public FTCStatus getFtc() {
        return ftc;
    }

    public void setFtc(FTCStatus ftc) {
        this.ftc = ftc;
    }

    public MachineStatus getMc() {
        return mc;
    }

    public void setMc(MachineStatus mc) {
        this.mc = mc;
    }

    //------------- 嵌套类改造 -------------
    // 注意: 需要保持与 JSON 中字段名称匹配
    public static class FTCStatus {
        @SerializedName("Enable") // 如果 JSON 字段名与变量名一致可不加
        private boolean enable;

        // 必须提供 getter/setter
        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }
    }

    public static class MachineStatus {
        @SerializedName("Connect")
        private boolean connect;
        @SerializedName("Coordinate")
        private float[] coordinate;
        @SerializedName("DA")
        private float[] da;
        @SerializedName("DI")
        private boolean[] di;
        @SerializedName("DO") // 处理特殊字段名
        private boolean[] dO;
        @SerializedName("Process")
        private ProcessStatus process;
        @SerializedName("Run")
        private int run;
        @SerializedName("Versions")
        private int versions;
        @SerializedName("Warning")
        private boolean warning;
        @SerializedName("PWM")
        private int pwm;

        @SerializedName("Freq")
        private int freq;

        @SerializedName("Limit")
        private int[] limit;

        // 需要空构造器
        public MachineStatus() {
        }

        // 所有字段的 getter 必须直接返回对象（不能返回副本）
        public boolean isConnect() {
            return connect;
        }

        public void setConnect(boolean connect) {
            this.connect = connect;
        }

        public float[] getCoordinate() {
            return coordinate;
        }

        public void setCoordinate(float[] coordinate) {
            this.coordinate = coordinate;
        }

        public float[] getDa() {
            return da;
        }

        public void setDa(float[] da) {
            this.da = da;
        }

        public boolean[] getDi() {
            return di;
        }

        public void setDi(boolean[] di) {
            this.di = di;
        }

        public boolean[] getDO() {
            return dO;
        } // 特殊处理 dO，使用 @SerializedName

        public void setDO(boolean[] dO) {
            this.dO = dO;
        }

        public ProcessStatus getProcess() {
            return process;
        }

        public void setProcess(ProcessStatus process) {
            this.process = process;
        }

        public int getRun() {
            return run;
        }

        public void setRun(int run) {
            this.run = run;
        }

        public int getVersions() {
            return versions;
        }

        public void setVersions(int versions) {
            this.versions = versions;
        }

        public boolean isWarning() {
            return warning;
        }

        public void setWarning(boolean warning) {
            this.warning = warning;
        }

        public int getPwm() {
            return pwm;
        }

        public void setPwm(int pwm) {
            this.pwm = pwm;
        }

        public int getFreq() {
            return freq;
        }

        public void setFreq(int freq) {
            this.freq = freq;
        }

        public int[] getLimit() {
            return limit;
        }

        public void setLimit(int[] limit) {
            this.limit = limit;
        }

        public static class ProcessStatus {
            @SerializedName("File")
            private String file;
            @SerializedName("Schedule")
            private double schedule;

            public String getFile() {
                return file;
            }

            public void setFile(String file) {
                this.file = file;
            }

            public double getSchedule() {
                return schedule;
            }

            public void setSchedule(double schedule) {
                this.schedule = schedule;
            }
        }
    }
}