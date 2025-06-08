import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

/**
 * 计算机网络课程设计：Ping程序设计与实现 (Java)
 *
 * 本程序模拟Ping命令的基本功能，通过Java的InetAddress.isReachable()方法
 * 检测主机可达性，并测量往返时间（RTT）。
 * 注意：由于Java标准API的限制，无法直接获取ICMP报头中的TTL字段。
 *
 * 用法:
 * java PingUtility <主机名/IP地址> [次数] [超时时间(ms)]
 * java PingUtility <起始IP-结束IP> [次数] [超时时间(ms)]
 *
 * 示例:
 * java PingUtility [www.google.com](https://www.google.com) 4 2000
 * java PingUtility 192.168.1.1 5
 * java PingUtility 192.168.1.100-192.168.1.105 3
 */
public class PingUtility {

    private static final int DEFAULT_PING_COUNT = 4; // 默认Ping次数
    private static final int DEFAULT_TIMEOUT_MS = 2000; // 默认超时时间（毫秒）

    // 用于解析IP范围的正则表达式
    private static final Pattern IP_RANGE_PATTERN = Pattern.compile(
            "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.)(\\d{1,3})-(\\d{1,3})"
    );

    public static void main(String[] args) {
        // --- 1. 命令行参数解析 ---
        if (args.length == 0) {
            displayUsage(); // 显示使用说明
            return;
        }

        String target = args[0]; // 目标主机名、IP地址或IP范围
        int count = DEFAULT_PING_COUNT;
        int timeout = DEFAULT_TIMEOUT_MS;

        // 解析Ping次数
        if (args.length >= 2) {
            try {
                count = Integer.parseInt(args[1]);
                if (count <= 0) {
                    System.out.println("错误: Ping次数必须是正整数。");
                    displayUsage();
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("错误: Ping次数格式不正确。");
                displayUsage();
                return;
            }
        }

        // 解析超时时间
        if (args.length >= 3) {
            try {
                timeout = Integer.parseInt(args[2]);
                if (timeout <= 0) {
                    System.out.println("错误: 超时时间必须是正整数。");
                    displayUsage();
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("错误: 超时时间格式不正确。");
                displayUsage();
                return;
            }
        }

        List<String> targetHosts = new ArrayList<>();
        Matcher matcher = IP_RANGE_PATTERN.matcher(target);

        // --- 2. 判断是单个IP/主机名还是IP范围 ---
        if (matcher.matches()) {
            // 是IP范围
            String baseIp = matcher.group(1); // 提取IP地址前三段 (e.g., "192.168.1.")
            int start = Integer.parseInt(matcher.group(2)); // 起始段 (e.g., 100)
            int end = Integer.parseInt(matcher.group(3)); // 结束段 (e.g., 105)

            if (start > end || start < 0 || end > 255) {
                System.out.println("错误: IP范围不合法。");
                displayUsage();
                return;
            }

            for (int i = start; i <= end; i++) {
                targetHosts.add(baseIp + i);
            }
        } else {
            // 是单个主机名或IP地址
            targetHosts.add(target);
        }

        // --- 3. 循环对每个目标进行Ping操作 ---
        System.out.println("\n正在Ping " + target + "，请稍候...");
        for (String host : targetHosts) {
            System.out.println("\n----------------------------------------");
            System.out.println("目标主机: " + host);
            pingHost(host, count, timeout);
        }
        System.out.println("\n----------------------------------------");
        System.out.println("Ping操作完成。");
    }

    /**
     * 对单个主机执行Ping操作并显示结果。
     * @param host 要Ping的主机名或IP地址。
     * @param count Ping的次数。
     * @param timeout 超时时间（毫秒）。
     */
    private static void pingHost(String host, int count, int timeout) {
        int sent = 0;
        int received = 0;
        long totalRtt = 0;
        long minRtt = Long.MAX_VALUE;
        long maxRtt = Long.MIN_VALUE;

        try {
            InetAddress address = InetAddress.getByName(host); // 获取目标IP地址对象
            System.out.println("解析IP地址: " + address.getHostAddress());

            for (int i = 0; i < count; i++) {
                sent++; // 发送计数器递增
                long startTime = System.nanoTime(); // 记录开始时间
                boolean reachable = address.isReachable(timeout); // 调用isReachable进行探测
                long endTime = System.nanoTime(); // 记录结束时间

                if (reachable) {
                    received++; // 接收计数器递增
                    long rttMs = (endTime - startTime) / 1_000_000; // 计算RTT（毫秒）
                    totalRtt += rttMs;
                    minRtt = Math.min(minRtt, rttMs);
                    maxRtt = Math.max(maxRtt, rttMs);

                    // 模拟Ping输出格式
                    // 注意：Java的isReachable()无法直接获取IP包头中的TTL，这里仅作占位说明。
                    System.out.printf("来自 %s 的回复: 时间=%dms TTL=无法获取(Java API限制)\n",
                            address.getHostAddress(), rttMs);
                } else {
                    System.out.printf("来自 %s 的请求超时。\n", address.getHostAddress());
                }

                // 每次Ping之间暂停一小段时间，避免请求过于频繁
                try {
                    Thread.sleep(500); // 暂停500毫秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // 恢复中断状态
                    System.out.println("Ping过程被中断。");
                    return;
                }
            }

            // --- 4. 显示统计结果 ---
            System.out.println("\n" + address.getHostAddress() + " 的 Ping 统计信息:");
            System.out.printf("    数据包: 已发送 = %d，已接收 = %d，丢失 = %d (%.0f%% 丢失)\n",
                    sent, received, (sent - received), ((double)(sent - received) / sent) * 100);

            if (received > 0) {
                System.out.printf("往返行程的估计时间(以毫秒为单位):\n");
                System.out.printf("    最短 = %dms，最长 = %dms，平均 = %.0fms\n",
                        minRtt, maxRtt, (double)totalRtt / received);
            }

        } catch (UnknownHostException e) {
            System.out.println("错误: 未知主机 " + host + "。请检查主机名或IP地址。");
        } catch (IOException e) {
            // isReachable可能抛出IOException (例如，网络不可达或权限问题)
            System.out.println("错误: Ping过程中发生I/O错误: " + e.getMessage());
            System.out.println("请检查网络连接或程序运行权限。");
        }
    }

    /**
     * 显示程序的使用说明。
     */
    private static void displayUsage() {
        System.out.println("用法:");
        System.out.println("  java PingUtility <主机名/IP地址> [次数] [超时时间(ms)]");
        System.out.println("  java PingUtility <起始IP-结束IP> [次数] [超时时间(ms)]");
        System.out.println("\n示例:");
        System.out.println("  java PingUtility [www.google.com](https://www.google.com) 4 2000");
        System.out.println("  java PingUtility 192.168.1.1 5");
        System.out.println("  java PingUtility 192.168.1.100-192.168.1.105 3 1500");
        System.out.println("\n参数说明:");
        System.out.println("  <主机名/IP地址> : 必填，要Ping的目标主机名或IP地址。");
        System.out.println("  <起始IP-结束IP> : 必填，Ping一个IP地址范围（仅支持IPv4最后一段）。");
        System.out.println("  [次数]          : 可选，Ping的次数，默认 " + DEFAULT_PING_COUNT + " 次。");
        System.out.println("  [超时时间(ms)]  : 可选，每次Ping的超时时间（毫秒），默认 " + DEFAULT_TIMEOUT_MS + " ms。");
    }
}
