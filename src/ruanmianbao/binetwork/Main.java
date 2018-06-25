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
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main{

	boolean running = false;
	Socket socket;
	ByteOrder order;
	String script = "";
	
	public Main(String[] args) throws Exception {
		String server = "localhost";
		int port = 8788;
		ByteOrder order = ByteOrder.LITTLE_ENDIAN;
		
		
		for(String a: args) {
			String[] a2 = a.split("=",2);
			if (a2.length==2) {
				switch(a2[0]) {
				case "-s":
				case "--server":
					server = a2[1];
					break;
				case "-p":
				case "--port":
					port = Integer.parseInt(a2[1]);
					break;
				case "-e":
				case "--endian":
					order = a2[1].equalsIgnoreCase("be")?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN;
					break;
				case "-r":
				case "--run":
					script = a2[1];
					break;
				}
			}
		}
		
		Main.log("connect to:"+server+":"+port);
		socket = new Socket(server, port);
		socket.setSoTimeout(15000);
		Main.log("connected.");
		this.order = order;
		
		if (this.script.isEmpty()) {
			// read from input
			this.running = true;
			ExecutorService taskExecutor = Executors.newFixedThreadPool(2);
			try {
				taskExecutor.execute(new RecvWorker());
				taskExecutor.execute(new SendWorker());
			} finally {
				taskExecutor.shutdown();
			}
		} else {
			this.running = false;
			this.RunWorker();
		}
		
	}

	/**
	 * 进行一次测试
	 * @throws Exception 
	 */
	private void RunWorker() throws Exception {
		if(script.startsWith("'") && script.endsWith("'"))
			script = script.substring(1, script.length()-1);
		Main.log("run script:" + script);
		sendData(script);
		recvData();
	}
	
	private String recvData() throws IOException {
		int buffer_size = 4096;
		byte[] buffer = new byte[buffer_size];
		BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
		int readed = 0, pos = 0;
		
		readed = in.read(buffer, pos, buffer.length-pos);
		if (readed>0) pos+=readed;
		
		String msg = toHex(buffer,0,pos);
		Main.log("recv["+pos+"]:"+msg);
		return msg;
	}
	
	private void sendData(String line) throws IOException {
		ByteBuffer outbuffer = ByteBuffer.allocate(4*1024);
		outbuffer.order(order);
		String[] items = line.split("\\|");
		for(int i=0; i<items.length; i++) {
			String[] sp = items[i].split(":");
			if(sp.length!=3) {
				Main.log("item ["+i+"] format error:"+items[i]);
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
		Main.log("send["+outbuffer.limit()+"]"+toHex(outbuffer.array(), 0, outbuffer.limit()));
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
					Main.log("unsupport type:"+parseInt);
				}
				
			}
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
					String line = scan.nextLine();
					Main.log("input:"+line);
					sendData(line);
				}
				scan.close();
			} catch (Exception e) {
				running = false;
				e.printStackTrace();
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
					Main.log("recv["+readed+"]:"+toHex(buffer, 0, readed));
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
	static void log(String msg) {
		Date dNow = new Date( );
	    SimpleDateFormat ft = new SimpleDateFormat ("yyyy.MM.dd HH:mm:ss");
		System.out.println("["+ft.format(dNow)+"] "+msg);
	}

	/**
	 * 入口
	 * @param args host ip [le|be]
	 */
	public static void main(String[] args) {
	
		if (args.length==0 
				|| args[0].equalsIgnoreCase("help")
				|| args[0].equalsIgnoreCase("-h")
				|| args[0].equalsIgnoreCase("?")
				|| args[0].equalsIgnoreCase("--help")
				) {
			printHelp("binetwork");
			System.exit(0);
		}

		try {
			new Main(args);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}

	private static void printHelp(String cmd) {
		System.out.println("usage: check 'readme.md' for more info.\n\n" +
				cmd + "[-s|--server]=<host> [-p|--port]=<port> [-e|-endian]=<le|be>\n" +
				"   -host or ip: the host name or ip (default: localhost)\n"+
				"   -port: the port of server (default: 8080) \n" +
				"   -le: little endian (default)\n"+
				"   -be: big endian \n\n" +
				"example:\n"+
				"    "+ cmd + "-s=127.0.0.1 -p=8080 -e=le -r=\"x:1:fa fb fc fd\"\n");
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
