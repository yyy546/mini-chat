package com.minichat.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class SnowflakeIdWorker {
    // 起始时间戳 2020-01-01 00:00:00 (ms)
    private final long twepoch = 1577836800000L;
    // 数据中心位数
    private final long datacenterIdBits = 5L;
    // 工作节点位数
    private final long workerIdBits = 5L;
    // 序列号位数
    private final long sequenceBits = 12L;

    /**
     * 最大值计算
     */
    private final long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    private final long sequenceMask = -1L ^ (-1L << sequenceBits);
    /**
     * 移位偏移量
     */
    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 成员变量
     */
    private long datacenterId;
    private long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     *
     * @param datacenterId 数据中心ID(0~31)
     * @param workerId     工作节点ID(0~31)
     */
    public SnowflakeIdWorker(long datacenterId, long workerId) {
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("数据中心ID必须在0~%d之间，当前值：%d", maxDatacenterId, datacenterId)
            );
        }
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("工作节点ID必须在0~%d之间，当前值：%d", maxWorkerId, workerId)
            );
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        // 处理时钟回拨
        if(timestamp < lastTimestamp){
            long offset = lastTimestamp - timestamp;
            if(offset <= 5){
                try {
                    Thread.sleep(offset << 1);  //等待2倍回拨时间
                    timestamp = timeGen();
                    if(timestamp < lastTimestamp){
                        throw new RuntimeException(
                                String.format("时钟回拨异常！上次生成ID时间：%d，当前时间：%d，差值：%dms",
                                        lastTimestamp, timestamp, offset)
                        );
                    }
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("生成ID时线程被中断", e);
                }
            }else{
                // 回拨时间>5ms：直接抛异常，避免ID重复
                throw new RuntimeException(
                        String.format("时钟回拨超过5ms，拒绝生成ID！差值：%dms", offset)
                );
            }
        }

        // 处理他同一毫秒的序列号
        if(timestamp == lastTimestamp){
            sequence = (sequence + 1) & sequenceMask;
            // 序列号耗尽，等待下一毫秒
            if(sequence == 0){
                timestamp = tilNextMillis(lastTimestamp);
            }
        }else{
            sequence = 0;
        }

        // 更新最后生成的时间戳
        lastTimestamp = timestamp;

        // 拼装ID返回
        return (timestamp - twepoch) << timestampLeftShift
                | datacenterId << datacenterIdShift
                | workerId << workerIdShift
                | sequence;
    }

    private long timeGen(){
        return System.currentTimeMillis();
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp){
            try {
                TimeUnit.MICROSECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待下一毫秒时线程被中断", e);
            }
            timestamp = timeGen();
        }
        return timestamp;
    }

    public String parseSnowflakeId(long id){
        long timestamp = ( id >> timestampLeftShift ) + twepoch;
        long datacenterId = (id >> datacenterIdShift) & maxDatacenterId;
        long workerId = (id >> workerIdShift) & maxWorkerId;
        long sequence = id & sequenceMask;

        ZoneId beijingZone = ZoneId.of("Asia/Shanghai");
        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalDateTime generateTime = LocalDateTime.ofInstant(instant, beijingZone);

        // 3. 格式化时间，保留毫秒
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String timeStr = generateTime.format(formatter);

        // 拼接解析结果
        return String.format(
                "雪花ID解析结果（北京时间）：\n" +
                        "  生成时间：%s\n" +
                        "  数据中心ID：%d\n" +
                        "  工作节点ID：%d\n" +
                        "  序列号：%d\n" +
                        "  原始时间戳（ms）：%d",
                timeStr, datacenterId, workerId, sequence, timestamp
        );
    }

}

