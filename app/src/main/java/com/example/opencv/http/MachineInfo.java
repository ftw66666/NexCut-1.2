package com.example.opencv.http;

import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MachineInfo {
    // 主锁控制整个对象访问
    private final ReadWriteLock mainLock = new ReentrantReadWriteLock();
    private FTCStatus ftc;
    private MachineStatus mc;

    //------------------------ 初始化方法 ------------------------
    // 从原始对象构造（深拷贝）
    public MachineInfo(MachineInfoResponse origin) {
        mainLock.writeLock().lock();
        try {
            this.ftc = new FTCStatus(origin.getFtc());
            this.mc = new MachineStatus(origin.getMc());
        } finally {
            mainLock.writeLock().unlock();
        }
    }

    // 空构造器
    public MachineInfo() {
        this.ftc = new FTCStatus();
        this.mc = new MachineStatus();
    }

    //------------------------ 线程安全访问方法 ------------------------
    public FTCStatus getFtc() {
        mainLock.readLock().lock();
        try {
            return new FTCStatus(this.ftc);
        } finally {
            mainLock.readLock().unlock();
        }
    }

    public void setFtc(FTCStatus ftc) {
        mainLock.writeLock().lock();
        try {
            this.ftc = new FTCStatus(ftc);
        } finally {
            mainLock.writeLock().unlock();
        }
    }

    public MachineStatus getMc() {
        mainLock.readLock().lock();
        try {
            return new MachineStatus(this.mc);
        } finally {
            mainLock.readLock().unlock();
        }
    }

    public void setMc(MachineStatus mc) {
        mainLock.writeLock().lock();
        try {
            this.mc = new MachineStatus(mc);
        } finally {
            mainLock.writeLock().unlock();
        }
    }

    //------------------------ 嵌套类 ------------------------
    public static class FTCStatus {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private boolean enable;

        // 深拷贝构造器
        public FTCStatus(FTCStatus origin) {
            lock.writeLock().lock();
            try {
                this.enable = origin.isEnable();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public FTCStatus() {
        }

        public FTCStatus(MachineInfoResponse.FTCStatus ftc) {
            lock.writeLock().lock();
            try {
                this.enable = ftc.isEnable();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public boolean isEnable() {
            lock.readLock().lock();
            try {
                return enable;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void setEnable(boolean enable) {
            lock.writeLock().lock();
            try {
                this.enable = enable;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public static class MachineStatus {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private boolean connect;
        private float[] coordinate;
        private float[] da;
        private boolean[] di;
        private boolean[] dO;
        private ProcessStatus process;
        private int run;
        private int versions;
        private boolean warning;

        // 深拷贝构造器
        public MachineStatus(MachineStatus origin) {
            lock.writeLock().lock();
            try {
                this.connect = origin.isConnect();
                this.coordinate = Arrays.copyOf(origin.getCoordinate(), origin.getCoordinate().length);
                this.da = Arrays.copyOf(origin.getDa(), origin.getDa().length);
                this.di = Arrays.copyOf(origin.getDi(), origin.getDi().length);
                this.dO = Arrays.copyOf(origin.getDO(), origin.getDO().length);
                this.process = new ProcessStatus(origin.getProcess());
                this.run = origin.getRun();
                this.versions = origin.getVersions();
                this.warning = origin.isWarning();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public MachineStatus() {
        }

        public MachineStatus(MachineInfoResponse.MachineStatus mc) {
            lock.writeLock().lock();
            try {
                this.connect = mc.isConnect();
                this.coordinate = Arrays.copyOf(mc.getCoordinate(), mc.getCoordinate().length);
                this.da = Arrays.copyOf(mc.getDa(), mc.getDa().length);
                this.di = Arrays.copyOf(mc.getDi(), mc.getDi().length);
                this.dO = Arrays.copyOf(mc.getDO(), mc.getDO().length);
                this.process = new ProcessStatus(mc.getProcess());
                this.run = mc.getRun();
                this.versions = mc.getVersions();
                this.warning = mc.isWarning();
            } finally {
                lock.writeLock().unlock();
            }
        }

        //------------------------ 线程安全访问方法 ------------------------
        public boolean isConnect() {
            lock.readLock().lock();
            try {
                return connect;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void setConnect(boolean connect) {
            lock.writeLock().lock();
            try {
                this.connect = connect;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public float[] getCoordinate() {
            lock.readLock().lock();
            try {
                return Arrays.copyOf(coordinate, coordinate.length);
            } finally {
                lock.readLock().unlock();
            }
        }

        public void setCoordinate(float[] coordinate) {
            lock.writeLock().lock();
            try {
                this.coordinate = Arrays.copyOf(coordinate, coordinate.length);
            } finally {
                lock.writeLock().unlock();
            }
        }

        // 其他数组字段的访问方法类似...
        public float[] getDa() {
            lock.readLock().lock();
            try {
                return da != null ? Arrays.copyOf(da, da.length) : new float[0];
            } finally {
                lock.readLock().unlock();
            }
        }

        public void setDa(float[] da) {
            lock.writeLock().lock();
            try {
                this.da = da != null ? Arrays.copyOf(da, da.length) : new float[0];
            } finally {
                lock.writeLock().unlock();
            }
        }

        public boolean[] getDi() {
            lock.readLock().lock();
            try {
                return di != null ? Arrays.copyOf(di, di.length) : new boolean[0];
            } finally {
                lock.readLock().unlock();
            }
        }

        public void setDi(boolean[] di) {
            lock.writeLock().lock();
            try {
                this.di = di != null ? Arrays.copyOf(di, di.length) : new boolean[0];
            } finally {
                lock.writeLock().unlock();
            }
        }


        public boolean[] getDO() {
            lock.readLock().lock();
            try {
                return dO != null ? Arrays.copyOf(dO, dO.length) : new boolean[0];
            } finally {
                lock.readLock().unlock();
            }
        }

        public void setDO(boolean[] dO) {
            lock.writeLock().lock();
            try {
                this.dO = dO != null ? Arrays.copyOf(dO, dO.length) : new boolean[0];
            } finally {
                lock.writeLock().unlock();
            }
        }

        public static class ProcessStatus {
            private final ReadWriteLock lock = new ReentrantReadWriteLock();
            private String file;
            private double schedule;

            public ProcessStatus(ProcessStatus origin) {
                lock.writeLock().lock();
                try {
                    this.file = origin.getFile();
                    this.schedule = origin.getSchedule();
                } finally {
                    lock.writeLock().unlock();
                }
            }

            public ProcessStatus() {
            }

            public ProcessStatus(MachineInfoResponse.MachineStatus.ProcessStatus process) {
                lock.writeLock().lock();
                try {
                    this.file = process.getFile();
                    this.schedule = process.getSchedule();
                } finally {
                    lock.writeLock().unlock();
                }
            }

            public String getFile() {
                lock.readLock().lock();
                try {
                    return file;
                } finally {
                    lock.readLock().unlock();
                }
            }

            public void setFile(String file) {
                lock.writeLock().lock();
                try {
                    this.file = file;
                } finally {
                    lock.writeLock().unlock();
                }
            }

            public double getSchedule() {
                lock.readLock().lock();
                try {
                    return schedule;
                } finally {
                    lock.readLock().unlock();
                }
            }

            public void setSchedule(double schedule) {
                lock.writeLock().lock();
                try {
                    this.schedule = schedule;
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }

        public ProcessStatus getProcess() {
            lock.readLock().lock();
            try {
                return process != null ? new ProcessStatus(process) : new ProcessStatus();
            } finally {
                lock.readLock().unlock();
            }
        }

        public void setProcess(ProcessStatus process) {
            lock.writeLock().lock();
            try {
                this.process = process != null ? new ProcessStatus(process) : new ProcessStatus();
            } finally {
                lock.writeLock().unlock();
            }
        }

        public int getRun() {
            lock.readLock().lock();
            try {
                return run;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void setRun(int run) {
            lock.writeLock().lock();
            try {
                this.run = run;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public int getVersions() {
            lock.readLock().lock();
            try {
                return versions;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void setVersions(int versions) {
            lock.writeLock().lock();
            try {
                this.versions = versions;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public boolean isWarning() {
            lock.readLock().lock();
            try {
                return warning;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void setWarning(boolean warning) {
            lock.writeLock().lock();
            try {
                this.warning = warning;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    //------------------------ 与原始类的互操作方法 ------------------------
    public void updateFrom(MachineInfoResponse origin) {
        mainLock.writeLock().lock();
        try {
            this.ftc = new FTCStatus(origin.getFtc());
            this.mc = new MachineStatus(origin.getMc());
        } finally {
            mainLock.writeLock().unlock();
        }
    }
}