package com.jsh.erp.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public abstract class ComputerInfo {
    private static String macAddressStr = null;
    private static String computerName = System.getenv().get("COMPUTERNAME");

    /**
     * 通过 java.net.NetworkInterface 获取稳定的 MAC 地址
     * 排除 loopback、virtual interface，取第一个物理网卡
     */
    public static String getMacAddress() {
        if (macAddressStr != null && !macAddressStr.isEmpty()) {
            return macAddressStr;
        }
        try {
            List<String> macList = getMacAddressList();
            for (String mac : macList) {
                // 排除虚拟网卡地址
                if (!"0000000000E0".equals(mac) && !"00-00-00-00-00-E0".equals(mac)
                        && !"00-00-00-00-00-00".equals(mac)) {
                    macAddressStr = mac;
                    return macAddressStr;
                }
            }
            // 如果所有地址都被过滤，取第一个
            if (!macList.isEmpty()) {
                macAddressStr = macList.get(0);
            }
        } catch (Exception ignored) {
        }
        return macAddressStr != null ? macAddressStr : "";
    }

    private static List<String> getMacAddressList() throws SocketException {
        List<String> macList = new ArrayList<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces == null) {
            return macList;
        }
        List<NetworkInterface> sorted = Collections.list(interfaces);
        // 优先选择非 loopback、非 virtual 的物理网卡
        sorted.sort((a, b) -> {
            boolean aVirtual = a.isLoopback() || a.isVirtual() || !a.isUp();
            boolean bVirtual = b.isLoopback() || b.isVirtual() || !b.isUp();
            if (aVirtual != bVirtual) return aVirtual ? 1 : -1;
            return 0;
        });
        for (NetworkInterface ni : sorted) {
            if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                continue;
            }
            byte[] mac = ni.getHardwareAddress();
            if (mac != null && mac.length == 6) {
                macList.add(formatMac(mac));
            }
        }
        return macList;
    }

    private static String formatMac(byte[] mac) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X", mac[i]));
            if (i < mac.length - 1) {
                sb.append("-");
            }
        }
        return sb.toString();
    }

    /**
     * 获取电脑名
     */
    public static String getComputerName() {
        if (computerName == null || computerName.isEmpty()) {
            computerName = System.getenv().get("COMPUTERNAME");
        }
        return computerName;
    }

    /**
     * 获取客户端IP地址
     */
    public static String getIpAddrAndName() throws Exception {
        return InetAddress.getLocalHost().toString();
    }

    /**
     * 获取客户端IP地址
     */
    public static String getIpAddr() throws Exception {
        return InetAddress.getLocalHost().getHostAddress();
    }

    private ComputerInfo() {
    }
}
