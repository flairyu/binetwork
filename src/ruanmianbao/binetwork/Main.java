package ruanmianbao.binetwork;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main{

	boolean running = false;
	Socket socket;
	ByteOrder order;
	
	public Main(String server, int port, ByteOrder order) throws UnknownHostException, IOException {
		outln("connect to:"+server+":"+port);
		socket = new Socket(server, port);
		outln("connected.");
		this.order = order;
		this.running = true;
		ExecutorService taskExecutor = Executors.newFixedThreadPool(2);
		try {
			taskExecutor.execute(new RecvWorker());
			taskExecutor.execute(new SendWorker());
		} finally {
			taskExecutor.shutdown();
		}
	}

	/**
	 * 发送信息的线程
	 */
	private class SendWorker implements Runnable {
		@Override
		public void run() {
			try {
				Scanner scan = new Scanner(System.in);
				while (running) {
					ByteBuffer outbuffer = ByteBuffer.allocate(4*1024);
					outbuffer.order(order);
					String line = scan.nextLine();
					outln("input:"+line);
					String[] items = line.split("\\|");
					for(int i=0; i<items.length; i++) {
						String[] sp = items[i].split(":");
						if(sp.length!=3) {
							outln("item ["+i+"] format error:"+items[i]);
							continue;
						}
						switch(sp[0]) {
						case "b": //二进制
							putBin(outbuffer, Integer.parseInt(sp[1]), sp[2]);
							break;
						case "o": //八进制
							putOct(outbuffer, Integer.parseInt(sp[1]), sp[2]);
							break;
						case "d": //十进制
							putDec(outbuffer, Integer.parseInt(sp[1]), sp[2]);
							break;
						case "x": //十六进制
							putHex(outbuffer, Integer.parseInt(sp[1]), sp[2]);
							break;
						case "s": //字符串
							putStr(outbuffer, sp[1], sp[2]);
							break;
						}
					}
					outbuffer.flip();
					socket.getOutputStream().write(outbuffer.array(), 0, outbuffer.limit());
					socket.getOutputStream().flush();
					outln("send["+outbuffer.limit()+"]"+toHex(outbuffer.array(), 0, outbuffer.limit()));
				}
				scan.close();
			} catch (Exception e) {
				running = false;
				e.printStackTrace();
			}
		}

		// 二进制
		private void putBin(ByteBuffer outbuffer, int parseInt, String string) {
			putByte(outbuffer, parseInt, string, 2);
		}
		
		// 八进制
		private void putOct(ByteBuffer outbuffer, int parseInt, String string) {
			putByte(outbuffer, parseInt, string, 8);
		}
		
		// 十进制
		private void putDec(ByteBuffer outbuffer, int parseInt, String string) {
			putByte(outbuffer, parseInt, string, 10);
		}
		
		// 十六进制
		private void putHex(ByteBuffer outbuffer, int parseInt, String string) {
			putByte(outbuffer, parseInt, string, 16);
		}
		
		// 字符串
		private void putStr(ByteBuffer outbuffer, String encode, String string) throws UnsupportedEncodingException {
			byte[] data = string.getBytes(encode);
			outbuffer.put(data);
		}
		
		/**
		 * 把一个字符串按指定进制转换成字节，存入指定的缓冲区内。如果指定的长度与字符串拆分后的长度不符，则自动补0。
		 * @param outbuffer 填充的buffer
		 * @param parseInt 单位：1-byte 2-short 4-int 8-long
		 * @param string 待处理字符串
		 * @param radix 进制（2,8,10,16）
		 */
		private void putByte(ByteBuffer outbuffer, int parseInt, String string, int radix) {
			String[] strs = string.split(" ");
			for(int i=0; i<strs.length; i++) {
				if (i<strs.length) {
					switch(parseInt) {
					case 1:
						short b = Short.parseShort(strs[i], radix);
						outbuffer.put((byte)(b&0x00ff));
						break;
					case 2:
						int s = Integer.parseInt(strs[i], radix);
						outbuffer.putShort((short)(s&0x00ffff));
						break;
					case 4:
						long ii = Long.parseLong(strs[i], radix);
						outbuffer.putInt((int)(ii&0x00ffffffff));
						break;
					case 8:
						long l = Long.parseLong(strs[i], radix);
						outbuffer.putLong(l);
						break;
					default:
						outln("unsupport type:"+parseInt);
					}
					
				}
			}
		}
	}
	
	/**
	 * 接收信息的线程
	 */
	private class RecvWorker implements Runnable{
		@Override
		public void run() {
			try {
				byte[] buffer = new byte[4*1024];
				BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
				while (running) {
					int readed = in.read(buffer, 0, buffer.length);
					outln("recv["+readed+"]:"+toHex(buffer, 0, readed));
				}
			} catch (Exception e) {
				running = false;
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 统一输出
	 * @param string 要输出的信息
	 */
	private void outln(String string) {
		Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss]");
		System.out.println(sdf.format(cal.getTime()) + string);
	}

	/**
	 * 入口
	 * @param args host ip [le|be]
	 */
	public static void main(String[] args) {
		String server = "localhost";
		int port = 8080;
		ByteOrder order = ByteOrder.LITTLE_ENDIAN;
		
		if (args.length>=1) {
			server = args[0];
			if (server == "help" || server=="-h" || server=="?" || server=="--help") {
				printHelp("java ruanmianbao.binetwork.Main ");
				System.exit(0);
			}
		}
		
		if (args.length>=2) {
			port = Integer.parseInt(args[1]);
		}
		
		if (args.length>=3) {
			order = args[2]=="be"?
					ByteOrder.BIG_ENDIAN
					:ByteOrder.LITTLE_ENDIAN;
		}
		Main main = null;
		try {
			main = new Main(server, port, order);
		} catch (Exception e) {
			if (main!=null) main.running = false;
			e.printStackTrace();
		}
		
	}

	private static void printHelp(String cmd) {
		System.out.println("usage: check 'readme.md' for more info.\n\n" +
				cmd + "[host or ip] [port] [le|be]\n" +
				"   -host or ip: the host name or ip (default: localhost)\n"+
				"   -port: the port of server (default: 8080) \n" +
				"   -le: little endian (default)\n"+
				"   -be: big endian \n\n" +
				"example:\n"+
				"    "+ cmd + "127.0.0.1 8080 le\n");
	}
	
	/**
	 * 把一个数组用16进制格式输出
	 * @param buf 需要转换的数据
	 * @param start 起始位置
	 * @param length 长度
	 * @return 16进制字符串
	 */
	private String toHex(byte[] buf, int start, int length) { 
		if (buf == null)
			return "";
		StringBuffer result = new StringBuffer(2 * length);
		for (int i = start; (( i < start+length) && (i<buf.length)); i++) {
			appendHex(result, buf[i]);
		}
		return result.toString();
	}
	private final static String HEX = "0123456789ABCDEF";
	private static void appendHex(StringBuffer sb, byte b) {
		sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
	}
}
