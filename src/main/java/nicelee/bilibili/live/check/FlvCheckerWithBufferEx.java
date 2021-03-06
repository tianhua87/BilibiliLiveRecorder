package nicelee.bilibili.live.check;

import java.io.File;
import java.io.IOException;

import nicelee.bilibili.util.Logger;

public class FlvCheckerWithBufferEx extends FlvCheckerWithBuffer {

	public static void main(String[] args) throws IOException {

		FlvCheckerWithBufferEx fChecker = new FlvCheckerWithBufferEx();
		boolean splitScripts = false;
		String saveFolder = null;
		if(args != null) {
			if(args.length >= 4) {
				saveFolder = args[3];
				File f = new File(saveFolder);
				if(!f.exists())
					f.mkdirs();
			}
			if(args.length >= 3 && "false".equals(args[2]))
				Logger.debug = false;
			
			if(args.length >= 2)
				splitScripts = "true".equals(args[1]);
			
			if (args.length >= 1) {
				System.out.println("校对时间戳开始...");
				fChecker.check(args[0], false, splitScripts, saveFolder);
				System.out.println("校对时间戳完毕。");
			} else {
				System.out.println("请输入正确的文件路径");
			}
		}
	}
	
	private long count = 0;
	private int firstTimeStamp = 0;
	private int lastTimeStamp = 0;
	/**
	 * 不再对时间戳进行额外处理，也不再分音视频进行处理，基本上完整保留原来的相对时间戳大小
	 */

	@Override
	protected boolean dealTimestamp(RafWBuffered raf, int timestamp, int tagType) throws IOException {
		// 只考虑从前10帧获取最初的timestamp
		if (count < 10) {
			// 如果时间差大于30s，当前时间可以算是初始时间戳，但由于前面可能还有几帧数据，时间戳应该非严格?递增，写入的时间戳最小只能是lastTimeStamp
			// 本来 timestamp -> ▲0, 现在timestamp -> ▲lastTimeStamp, 即
			if (timestamp - lastTimeStamp > 30 * 1000) {
				firstTimeStamp = timestamp - lastTimeStamp;
			}
		}

		//要写入的时间戳为当前相对于第一帧的差值
		lastTimeStamp = timestamp - firstTimeStamp;
		lastTimestampWrite[0] = lastTimeStamp;
		// 低于0xffffff部分
		int lowCurrenttime = lastTimeStamp & 0xffffff;
		raf.write(int2Bytes(lowCurrenttime), 1, 3);
		// 高于0xffffff部分
		int highCurrenttime = lastTimeStamp >> 24;
		raf.write(highCurrenttime);
		Logger.print(" ,读取timestamps 为：" + timestamp);
		Logger.print(" ,写入timestamps 为：" + lastTimeStamp);
		Logger.println();
		count++;
//		if(count > 20)
//			System.exit(1);
		return true;
	}
}
