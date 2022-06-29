package cn.lai.jchat.chat;

import cn.lai.jchat.Utils;
import cn.lai.jchat.model.OnlineUser;
import okio.Buffer;
import okio.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class OnlineUserManager {
    private OnlineUser myself;
    private final Map<UUID, OnlineUser> onlineUserMap = new HashMap<>();
    private final AtomicBoolean isAlive = new AtomicBoolean();
    private static final long PUBLISH_INTERVAL = 1000;
    private static final long HEARTBEAT_TIMEOUT = 2000;
    private static final String MULTICAST_HOST = "230.0.0.0";
    private static final int MULTICAST_PORT = 6666;
    private static final int MAX_PACKET_SIZE = 1000;
    private static final int MULTI_PACKET_TIMEOUT = 1000;
    private final Logger logger = LogManager.getLogger(getClass().getSimpleName());
    private MulticastSocket publishSocket;
    private MulticastSocket receiveSocket;

    public synchronized void updateMyself(OnlineUser user) {
        logger.info("更新用户信息 " + user);
        this.myself = user;
    }
    private final ChatManager chatManager;

    public OnlineUserManager(ChatManager chatManager) {
        this.chatManager = chatManager;
    }

    public synchronized Buffer getUserContent() throws IOException {
        if (myself == null) return null;
        return ObjectHelper.serialize(myself);
    }

    public synchronized List<OnlineUser> getOnlineUsers() {
        return new ArrayList<>(onlineUserMap.values());
    }

    public synchronized OnlineUser findOnlineUserById(UUID userId) {
        return onlineUserMap.get(userId);
    }

    private void addUser(OnlineUser user) {
        boolean isNew;
        OnlineUser oldUser;
        synchronized (this) {
            UUID userId = user.getUser().getId();
            if (myself != null && myself.getUser().getId().equals(userId))
                return; // 忽略自己
            user.setLastActiveTime(new Date());
            oldUser = onlineUserMap.get(userId);
            isNew = oldUser == null || !Objects.equals(oldUser.getUser(), user.getUser());
            onlineUserMap.put(userId, user);
        }
        if (isNew) {
            if (oldUser != null) {
                logger.info("用户更新: " + user);
                chatManager.onRemoveOnlineUser(oldUser);
            } else {
                logger.info("用户上线: " + user);
            }
            chatManager.onAddOnlineUser(user);
        }
    }

    private void cleanUserMap() {
        long now = System.currentTimeMillis();
        List<OnlineUser> toRemove = new ArrayList<>();
        synchronized (this) {
            for (OnlineUser user : onlineUserMap.values()) {
                long activeTime = user.getLastActiveTime().getTime();
                if (now - activeTime > HEARTBEAT_TIMEOUT) {
                    toRemove.add(user);
                    logger.info("用户离线: " + user);
                }
            }
            for (OnlineUser onlineUser : toRemove) {
                onlineUserMap.remove(onlineUser.getUser().getId());
            }
        }
        for (OnlineUser user : toRemove) {
            chatManager.onRemoveOnlineUser(user);
        }
    }

    public void start() {
        if (isAlive.get()) return;
        isAlive.set(true);
        new Thread(this::publishLoop).start();
        new Thread(this::receiveLoop).start();
        new Thread(this::onlineUserMapCleanerLoop).start();
    }

    public synchronized void stop() {
        isAlive.set(false);
        Utils.closeQuietly(publishSocket, receiveSocket);
    }

    private void onlineUserMapCleanerLoop() {
        while (isAlive.get()) {
            cleanUserMap();
            try {
                Thread.sleep(HEARTBEAT_TIMEOUT);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static NetworkInterface getDesiredInterface() throws SocketException {
        Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        NetworkInterface first = null;
        while (interfaceEnumeration.hasMoreElements()) {
            NetworkInterface networkInterface = interfaceEnumeration.nextElement();
            if (first == null) first = networkInterface;
            if (!networkInterface.isUp() || networkInterface.isVirtual() || networkInterface.isLoopback()) continue;
            if (networkInterface.getDisplayName().contains("Virtual")) continue;
            if (networkInterface.getDisplayName().contains("lo")) continue;
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                InetAddress address = interfaceAddress.getAddress();
                if (address.isSiteLocalAddress()) // 局域网网卡
                    return networkInterface;
            }
        }
        return first;
    }

    private void publishLoop() {
        try (MulticastSocket socket = new MulticastSocket()) {
            synchronized (this) {
                publishSocket = socket;
            }
            NetworkInterface nif = getDesiredInterface();
            socket.setNetworkInterface(nif);
            logger.info("多播网络接口: " + socket.getNetworkInterface().getDisplayName());
            while (isAlive.get()) {
                try {
                    Buffer buffer = getUserContent();
                    if (buffer != null) {
                        send(socket, buffer.readByteArray());
                    }
                } catch (Exception e) {
                    if (!socket.isClosed())
                        e.printStackTrace();
                }
                try {
                    Thread.sleep(PUBLISH_INTERVAL);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void send(MulticastSocket socket, byte[] contentBuf) throws IOException {
        if (contentBuf == null) return;
        InetAddress group = InetAddress.getByName(MULTICAST_HOST);
        List<MultiPacket> packets = splitToPackets(contentBuf);
        for (MultiPacket packet : packets) {
            if (!isAlive.get()) break;
            byte[] buf = ObjectHelper.serialize(packet).readByteArray();
            DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, group, MULTICAST_PORT);
            socket.send(datagramPacket);
        }
        logger.debug("已发送分包 " + packets.size() + " " + contentBuf.length);
    }

    private List<MultiPacket> splitToPackets(byte[] buf) {
        List<MultiPacket> packets = new ArrayList<>();
        UUID uuid = UUID.randomUUID();
        int i = 0;
        while (i < buf.length) {
            MultiPacket packet = new MultiPacket();
            int e = Math.min(buf.length, i + MAX_PACKET_SIZE);
            byte[] chunk = new byte[e - i];
            System.arraycopy(buf, i, chunk, 0, chunk.length);
            i += chunk.length;
            packet.uuid = uuid;
            packet.bytes = chunk;
            packets.add(packet);
        }
        for (i = 0; i < packets.size(); i++) {
            MultiPacket packet = packets.get(i);
            packet.number = i + 1;
            packet.total = packets.size();
        }
        return packets;
    }

    private void receiveLoop() {
        try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
            synchronized (this) {
                receiveSocket = socket;
            }
            InetAddress group = InetAddress.getByName(MULTICAST_HOST);
            InetSocketAddress inetSocketAddress = new InetSocketAddress(group, MULTICAST_PORT);
            NetworkInterface nif = getDesiredInterface();
            socket.joinGroup(inetSocketAddress, nif);
            byte[] buf = new byte[MAX_PACKET_SIZE * 2];
            PacketInfoHolder holder = new PacketInfoHolder();
            logger.debug("加入分组: " + inetSocketAddress + " " + nif);
            while (isAlive.get()) {
                try {
                    DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length);
                    socket.receive(datagramPacket);
                    MultiPacket packet = (MultiPacket) ObjectHelper.deserialize(
                            ByteString.of(buf, datagramPacket.getOffset(), datagramPacket.getLength()));
                    try {
                        addPacket(holder, packet, datagramPacket.getAddress());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    cleanPackets(holder);
                } catch (Exception e) {
                    if (!socket.isClosed())
                        e.printStackTrace();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void cleanPackets(PacketInfoHolder holder) {
        long now = System.currentTimeMillis();
        if (holder.nextTimeToClean > now) return;
        long minActiveTime = now;
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : holder.firstPacketTime.entrySet()) {
            long time = entry.getValue();
            if (now - time > MULTI_PACKET_TIMEOUT) {
                toRemove.add(entry.getKey());
            } else {
                minActiveTime = Math.min(minActiveTime, time);
            }
        }
        for (UUID id : toRemove) {
            holder.firstPacketTime.remove(id);
            holder.packetMap.remove(id);
        }
        if (toRemove.size() > 0)
            logger.debug("清理超时包数量: " + toRemove.size());
        holder.nextTimeToClean = minActiveTime + MULTI_PACKET_TIMEOUT;
    }

    private void addPacket(PacketInfoHolder holder, MultiPacket packet, InetAddress address) {
        if (packet == null) return;
        if (!holder.firstPacketTime.containsKey(packet.uuid)) {
            holder.firstPacketTime.put(packet.uuid, System.currentTimeMillis());
            holder.packetMap.put(packet.uuid, new ArrayList<>());
//            logger.debug("收到第一个包: " + packet.uuid);
        }
        List<MultiPacket> packets = holder.packetMap.get(packet.uuid);
        packets.add(packet);
        if (packets.size() == packet.total) {
            holder.firstPacketTime.remove(packet.uuid);
            holder.packetMap.remove(packet.uuid);
            byte[][] bytesArray = new byte[packets.size()][];
            for (MultiPacket p : packets) {
                bytesArray[p.number - 1] = p.bytes;
            }
            Buffer buffer = new Buffer();
            for (byte[] bytes : bytesArray) {
                buffer.write(bytes);
            }
            onContentReceived(buffer, address);
        }
    }

    private void onContentReceived(Buffer buffer, InetAddress address) {
        try {
            OnlineUser onlineUser = (OnlineUser) ObjectHelper.deserialize(buffer);
            onlineUser.setHost(address.getHostAddress());
            addUser(onlineUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class PacketInfoHolder {
        final Map<UUID, List<MultiPacket>> packetMap = new HashMap<>();
        final Map<UUID, Long> firstPacketTime = new HashMap<>();
        long nextTimeToClean = 0;
    }

    private static class MultiPacket implements Serializable {
        UUID uuid;
        int number;
        int total;
        byte[] bytes;
    }
}
