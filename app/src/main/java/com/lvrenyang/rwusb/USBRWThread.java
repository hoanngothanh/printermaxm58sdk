package com.lvrenyang.rwusb;

import java.io.IOException;

import com.lvrenyang.callback.RecvCallBack;
import com.lvrenyang.rwbuf.RxBuffer;
import com.lvrenyang.rwusb.PL2303Driver.TTYTermios;
import com.lvrenyang.rwusb.USBDriver.RTNCode;
import com.lvrenyang.rwusb.USBDriver.USBPort;
import com.lvrenyang.utils.FileUtils;

//import android.hardware.usb.UsbRequest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * 这只是单纯处理读数据的线程，如果需要执行工作，还需要另外的线程
 * 这里面启动心跳线程，构造函数的时候new，start的时候也start子线程，quit的时候也
 * 
 * @author Administrator
 * 
 */
public class USBRWThread extends Thread {
	private static final String TAG = "USBRWThread";
	// private static final Lock SLOCK = new ReentrantLock(); // 读写互斥锁，内部使用。
	private static volatile USBRWThread usbrwThread = null;
	// private static boolean bThreadWait = false;
	// private static boolean bReadPaused = false;
	// private static final Object oThreadControl = new Object();

	private static final int RWHANDLER_READ = 1000;

	private static Handler usbrwHandler = null;
	private static Looper mLooper = null;
	private static boolean threadInitOK = false;

	private static PL2303Driver pl2303 = new PL2303Driver();
	private static USBPort port = null;
	private static TTYTermios serial = null;
	private static boolean isOpened = false;

	private static RecvCallBack callBack = null;
	private static final Object NULLLOCK = new Object();
	public static RxBuffer USBRXBuffer = new RxBuffer(0x1000);

	private USBRWThread() {
		threadInitOK = false;
	}

	public static USBRWThread InitInstant() {
		if (usbrwThread == null) {
			synchronized (USBRWThread.class) {
				if (usbrwThread == null) {
					usbrwThread = new USBRWThread();
				}
			}
		}
		return usbrwThread;
	}

	@Override
	public void start() {
		super.start();
		while (!threadInitOK)
			;
	}

	@Override
	public void run() {
		Looper.prepare();
		mLooper = Looper.myLooper();
		usbrwHandler = new RWHandler();
		threadInitOK = true;
		Looper.loop();
	}

	private static class RWHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case RWHANDLER_READ: {
				byte[] buffer = new byte[0x20];
				int rec = 0;
				Log.i(TAG, "start read");
				FileUtils.DebugAddToFile("usb start read \r\n",
						FileUtils.sdcard_dump_txt);
				try {
					while (true) {
						/*
						 * if (bThreadWait) { bReadPaused = true; synchronized
						 * (oThreadControl) { oThreadControl.wait(); } }
						 * bReadPaused = false;
						 */
						rec = ReadIsAvaliable(buffer, buffer.length);
						if (rec > 0) {
							for (int i = 0; i < rec; i++)
								USBRXBuffer.PutByte(buffer[i]);
							OnRecv(buffer, 0, rec);
						} else if (rec < 0) {
							FileUtils.DebugAddToFile(
									"usb read error. ReadIsAvaliable return code:"
											+ rec + "\r\n",
									FileUtils.sdcard_dump_txt);
							break;
						}

						Thread.sleep(100);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				FileUtils.DebugAddToFile("usb stop read\r\n",
						FileUtils.sdcard_dump_txt);
				Close();
				break;
			}

			}
		}
	}

	/*
	 * public static void PauseRead() { bThreadWait = true; while (!bReadPaused)
	 * ; }
	 * 
	 * public static void ResumeRead() { bThreadWait = false; synchronized
	 * (oThreadControl) { oThreadControl.notifyAll(); } }
	 */
	public static boolean Open(USBPort port, TTYTermios serial) {
		boolean result = false;
		// SLOCK.lock();
		result = _Open(port, serial);
		// SLOCK.unlock();
		return result;
	}

	private static boolean _Open(USBPort port, TTYTermios serial) {
		boolean valid = false;
		try {
			if (pl2303.probe(port, PL2303Driver.id) == RTNCode.OK) {
				if (pl2303.attach(port) == RTNCode.OK) {
					if (pl2303.open(port, serial) == RTNCode.OK) {
						USBRWThread.port = port;
						USBRWThread.serial = serial;
						valid = true;
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			valid = false;
		}

		// 如果成功了，则发起读命令

		if (valid) {
			isOpened = true;
			Message msg = usbrwHandler.obtainMessage(RWHANDLER_READ);
			usbrwHandler.sendMessage(msg);
		} else {
			isOpened = false;
		}

		return valid;
	}

	public static void Close() {
		// SLOCK.lock();
		_Close();
		// SLOCK.unlock();
	}

	private static void _Close() {
		try {
			pl2303.close(port, serial);
			pl2303.release(port);
			pl2303.disconnect(port);
			port = null;
			serial = null;
			Log.v("USBRWThread Close", "Close Socket");
		} catch (Exception e) {
			e.printStackTrace();
		}
		isOpened = false;
	}

	public static boolean IsOpened() {
		boolean ret = false;
		// SLOCK.lock();
		ret = _IsOpened();
		// SLOCK.unlock();
		return ret;
	}

	private static boolean _IsOpened() {
		return isOpened;
	}

	public static int Write(byte[] buffer, int offset, int count) {
		int ret = 0;
		// SLOCK.lock();
		ret = _Write(buffer, offset, count);
		// SLOCK.unlock();
		return ret;
	}

	private static int _Write(byte[] buffer, int offset, int count) {
		int cnt = 0;
		try {
			cnt = pl2303.write(port, buffer, offset, count, 2000);
			if (cnt < 0) {
				cnt = 0;
				throw new Exception("write error");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			_Close();
		}

		return cnt;
	}

	/**
	 * 
	 * @param buffer
	 * @param byteOffset
	 * @param byteCount
	 * @param timeout
	 * @return 返回实际读取的字节数
	 */
	public static synchronized int Read(byte[] buffer, int byteOffset,
			int byteCount, int timeout) {
		int index = 0;
		long time = System.currentTimeMillis();
		while ((System.currentTimeMillis() - time) < timeout) {
			if (!IsEmpty()) {
				buffer[index++] = GetByte();
			}

			if (index == byteCount)
				break;
		}

		return index;
	}

	private static int ReadIsAvaliable(byte[] buffer, int maxCount)
			throws IOException {
		int ret = 0;
		// SLOCK.lock();
		ret = _ReadIsAvaliable(buffer, maxCount);
		// SLOCK.unlock();
		return ret;
	}

	private static int _ReadIsAvaliable(byte[] buffer, int maxCount)
			throws IOException {
		int rec;

		rec = pl2303.read(port, buffer, 0, maxCount, 1);
		if (-1 == rec)
			rec = 0;

		return rec;
	}

	private static void OnRecv(byte[] buffer, int byteOffset, int byteCount) {
		synchronized (NULLLOCK) {
			if (null != callBack)
				callBack.onRecv(buffer, byteOffset, byteCount);
		}
	}

	public static void SetOnRecvCallBack(RecvCallBack callback) {
		synchronized (NULLLOCK) {
			callBack = callback;
		}
	}

	public static boolean Request(byte sendbuf[], int sendlen, int requestlen,
			byte recbuf[], Integer reclen, int timeout) {
		int Retry = 3;

		while ((Retry--) > 0) {
			ClrRec();
			Write(sendbuf, 0, sendlen);
			reclen = Read(recbuf, 0, requestlen, timeout);
			if (requestlen == reclen)
				return true;
		}
		return false;
	}

	public static void ClrRec() {
		USBRXBuffer.ClrRec();
	}

	public static boolean IsEmpty() {
		return USBRXBuffer.IsEmpty();
	}

	public static byte GetByte() {
		return USBRXBuffer.GetByte();
	}

	public static synchronized void Quit() {
		try {
			if (null != mLooper) {
				mLooper.quit();
				mLooper = null;
			}
			usbrwThread = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
