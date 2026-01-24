#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
TCP IM 客户端测试脚本
用于测试 TCP 长连接、注册和心跳功能

协议格式：
+--------+----------------+
| Length |    Payload     |
| 4 bytes|   N bytes      |
+--------+----------------+

消息格式（JSON）：
{
    "code": <int>,
    "token": "<string>",
    "data": <object>,
    "deviceType": "<string>",
    "requestId": "<string>",
    "timestamp": <long>
}

消息类型 (code):
- 0: REGISTER (注册)
- 1: REGISTER_SUCCESS (注册成功)
- 2: HEART_BEAT (心跳)

使用方法:
    python tcp_client.py --host 127.0.0.1 --port 9000 --token YOUR_JWT_TOKEN
"""

import argparse
import json
import socket
import struct
import threading
import time
import uuid
from datetime import datetime


class TCPIMClient:
    """TCP IM 客户端"""

    # 消息类型常量
    REGISTER = 0
    REGISTER_SUCCESS = 1
    HEART_BEAT = 2

    def __init__(self, host: str, port: int, token: str, device_type: str = "PYTHON"):
        self.host = host
        self.port = port
        self.token = token
        self.device_type = device_type
        self.socket = None
        self.connected = False
        self.running = False
        self.heartbeat_thread = None
        self.receive_thread = None

    def connect(self) -> bool:
        """建立 TCP 连接"""
        try:
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
            self.socket.connect((self.host, self.port))
            self.connected = True
            self.running = True
            print(f"[{self._now()}] ✓ 已连接到服务器 {self.host}:{self.port}")
            return True
        except Exception as e:
            print(f"[{self._now()}] ✗ 连接失败: {e}")
            return False

    def disconnect(self):
        """断开连接"""
        self.running = False
        self.connected = False
        if self.socket:
            try:
                self.socket.close()
            except:
                pass
        print(f"[{self._now()}] 已断开连接")

    def send_message(self, msg: dict) -> bool:
        """发送消息（自动添加长度头）"""
        if not self.connected:
            print(f"[{self._now()}] ✗ 未连接到服务器")
            return False

        try:
            # 序列化为 JSON
            payload = json.dumps(msg, ensure_ascii=False).encode('utf-8')
            # 添加 4 字节长度头（大端序）
            length = len(payload)
            frame = struct.pack('>I', length) + payload

            self.socket.sendall(frame)
            print(f"[{self._now()}] → 发送: code={msg.get('code')}, len={length}")
            return True
        except Exception as e:
            print(f"[{self._now()}] ✗ 发送失败: {e}")
            self.connected = False
            return False

    def receive_message(self) -> dict | None:
        """接收消息（读取长度头后读取 payload）"""
        if not self.connected:
            return None

        try:
            # 先读取 4 字节长度头
            length_data = self._recv_exact(4)
            if not length_data:
                return None

            length = struct.unpack('>I', length_data)[0]

            # 读取 payload
            payload_data = self._recv_exact(length)
            if not payload_data:
                return None

            msg = json.loads(payload_data.decode('utf-8'))
            return msg
        except Exception as e:
            print(f"[{self._now()}] ✗ 接收失败: {e}")
            self.connected = False
            return None

    def _recv_exact(self, n: int) -> bytes | None:
        """精确读取 n 字节"""
        data = b''
        while len(data) < n:
            packet = self.socket.recv(n - len(data))
            if not packet:
                return None
            data += packet
        return data

    def register(self) -> bool:
        """发送注册消息"""
        msg = {
            "code": self.REGISTER,
            "token": self.token,
            "deviceType": self.device_type,
            "deviceName": "Python TCP Client",
            "requestId": str(uuid.uuid4()),
            "timestamp": int(time.time() * 1000)
        }
        return self.send_message(msg)

    def heartbeat(self) -> bool:
        """发送心跳消息"""
        msg = {
            "code": self.HEART_BEAT,
            "token": self.token,
            "requestId": str(uuid.uuid4()),
            "timestamp": int(time.time() * 1000)
        }
        return self.send_message(msg)

    def start_heartbeat(self, interval: int = 25):
        """启动心跳线程"""
        def heartbeat_loop():
            while self.running and self.connected:
                time.sleep(interval)
                if self.running and self.connected:
                    self.heartbeat()

        self.heartbeat_thread = threading.Thread(target=heartbeat_loop, daemon=True)
        self.heartbeat_thread.start()
        print(f"[{self._now()}] 心跳线程已启动, 间隔 {interval}s")

    def start_receive(self):
        """启动接收线程"""
        def receive_loop():
            while self.running and self.connected:
                msg = self.receive_message()
                if msg:
                    self._handle_message(msg)
                else:
                    if self.running:
                        print(f"[{self._now()}] 连接已断开")
                        self.connected = False
                    break

        self.receive_thread = threading.Thread(target=receive_loop, daemon=True)
        self.receive_thread.start()
        print(f"[{self._now()}] 接收线程已启动")

    def _handle_message(self, msg: dict):
        """处理接收到的消息"""
        code = msg.get('code')
        message = msg.get('message', '')

        if code == self.REGISTER_SUCCESS:
            print(f"[{self._now()}] ← 注册成功: {message}")
            metadata = msg.get('metadata', {})
            if metadata:
                print(f"              metadata: {metadata}")
        elif code == self.HEART_BEAT:
            print(f"[{self._now()}] ← 心跳响应")
        else:
            print(f"[{self._now()}] ← 收到消息: code={code}, message={message}")
            if msg.get('data'):
                print(f"              data: {msg.get('data')}")

    def _now(self) -> str:
        """获取当前时间字符串"""
        return datetime.now().strftime('%H:%M:%S')

    def run(self, heartbeat_interval: int = 25):
        """运行客户端（连接 -> 注册 -> 心跳循环）"""
        if not self.connect():
            return

        # 启动接收线程
        self.start_receive()

        # 发送注册消息
        time.sleep(0.1)  # 短暂等待确保连接稳定
        if not self.register():
            self.disconnect()
            return

        # 启动心跳
        self.start_heartbeat(heartbeat_interval)

        # 主线程保持运行
        print(f"\n[{self._now()}] 客户端运行中... 按 Ctrl+C 退出\n")
        try:
            while self.running and self.connected:
                time.sleep(1)
        except KeyboardInterrupt:
            print(f"\n[{self._now()}] 收到退出信号")
        finally:
            self.disconnect()


def main():
    parser = argparse.ArgumentParser(
        description='TCP IM 客户端测试工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog='''
示例:
  python tcp_client.py --host 127.0.0.1 --port 9000 --token eyJhbGciOiJIUzI1NiJ9...

消息类型:
  0: REGISTER (注册)
  1: REGISTER_SUCCESS (注册成功)  
  2: HEART_BEAT (心跳)
        '''
    )
    parser.add_argument('--host', '-H', default='127.0.0.1', help='服务器地址 (默认: 127.0.0.1)')
    parser.add_argument('--port', '-p', type=int, default=9000, help='服务器端口 (默认: 9000)')
    parser.add_argument('--token', '-t', required=True, help='JWT Token (必需)')
    parser.add_argument('--device', '-d', default='PYTHON', help='设备类型 (默认: PYTHON)')
    parser.add_argument('--heartbeat', '-b', type=int, default=25, help='心跳间隔秒数 (默认: 25)')

    args = parser.parse_args()

    print("""
╔══════════════════════════════════════════╗
║       TCP IM Client - Python v1.0        ║
╚══════════════════════════════════════════╝
    """)

    client = TCPIMClient(
        host=args.host,
        port=args.port,
        token=args.token,
        device_type=args.device
    )

    client.run(heartbeat_interval=args.heartbeat)


if __name__ == '__main__':
    main()

