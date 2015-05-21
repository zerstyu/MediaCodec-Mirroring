package com.example.mediacodec_client;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

public class VideoDecoderThread extends Thread {
	private MainActivity mMainActivity;

	private static final String VIDEO = "video/";
	private static final String MIME_TYPE = "video/avc"; // H.264

	private static final String TAG = "VideoDecoder";

	// private MediaExtractor mExtractor;
	private MediaCodec mDecoder;

	private boolean eosReceived;
	public static int flag = 0, flag1 = 0;
	int count = 0;

	public boolean init(Surface surface) {
		eosReceived = false;
		try {

			MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE,
					mMainActivity.width, mMainActivity.height);
			mDecoder = MediaCodec.createDecoderByType(MIME_TYPE);
			

			try {
				Log.d(TAG, "format : " + format);
				
				/*
				 * 1. MediaFormat : ������ ������ MediaFormat�� ������(MediaExtractor�� �̿��� ���
				 * �ش� MediaFormat ������ return �޾ƿ� �� �ִ�) 2. Surface : ���ڵ������� ����ϸ� ȭ����
				 * �׸��� ���� View�� �Ѱ��ָ� �ȴ�. 3. MediaCrypto : ��ȣȭ�� Media Data�� �ٷ� �� ����ϴ�
				 * �ɼ��̴�. �Ϲ������� null�� �ʱ�ȭ�ؼ� ��� 4. flags : ���ڵ��ÿ��� ������
				 * ��(MediaCodec.CONFIGURE_FLAG_ENCODE)�� �ʱ�ȭ�ϰ�, �� �ܿ��� 0���� �ʱ�ȭ�Ѵ�.
				 */
				
				mDecoder.configure(format, surface, null, 0 /* Decoder */);

			} catch (IllegalStateException e) {
				Log.e(TAG, "codec failed configuration. " + e);
				return false;
			}

			mDecoder.start();

			// mExtractor = new MediaExtractor();
			// mExtractor.setDataSource(filePath);

			/*
			 * for (int i = 0; i < mExtractor.getTrackCount(); i++) {
			 * MediaFormat format = mExtractor.getTrackFormat(i);
			 * 
			 * String mime = format.getString(MediaFormat.KEY_MIME); if
			 * (mime.startsWith(VIDEO)) { mExtractor.selectTrack(i); mDecoder =
			 * MediaCodec.createDecoderByType(mime); try { Log.d(TAG,
			 * "format : " + format); mDecoder.configure(format, surface, null,
			 * 0 Decoder );
			 * 
			 * } catch (IllegalStateException e) { Log.e(TAG, "codec '" + mime +
			 * "' failed configuration. " + e); return false; }
			 * 
			 * mDecoder.start(); break; } }
			 */

		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public void run() {
		BufferInfo info = new BufferInfo();
		ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();

		// mDecoder.getOutputBuffers();

		boolean isInput = true;
		boolean first = false;
		long startWhen = 0;

		while (!eosReceived) {
			if (flag1 == 0) {

				if (isInput) {
					try {

						int inputIndex = mDecoder.dequeueInputBuffer(10000);
						if (inputIndex >= 0) {
							// fill inputBuffers[inputBufferIndex] with valid
							// data
							ByteBuffer inputBuffer = inputBuffers[inputIndex];
							inputBuffer.clear();
							inputBuffer.put(MainActivity.resBytes);

							mDecoder.queueInputBuffer(inputIndex, 0,
									MainActivity.resBytes.length, count++, 0);

							// int sampleSize =
							// mExtractor.readSampleData(inputBuffer, 0);

							/*
							 * if (mExtractor.advance() && sampleSize > 0) {
							 * mDecoder.queueInputBuffer(inputIndex, 0,
							 * sampleSize, mExtractor.getSampleTime(), 0);
							 * 
							 * } else { Log.d(TAG,
							 * "InputBuffer BUFFER_FLAG_END_OF_STREAM");
							 * mDecoder.queueInputBuffer(inputIndex, 0, 0, 0,
							 * MediaCodec.BUFFER_FLAG_END_OF_STREAM); isInput =
							 * false; }
							 */
						}
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
				}

				int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
				switch (outIndex) {
				case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
					// ���⿡���� MediaCodec�� Buffer ���°� ����� �� �ִ�. �Ʒ��� ���� �ڵ带 �� �־����
					// �Ѵ�.
					Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
					mDecoder.getOutputBuffers();
					break;

				case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
					// ���⿡���� MediaMuxer�� �̿��ؼ� ������ ����� �� ����� �� �ִ�. ����� Andorid
					// 4.3 �̻󿡼��� �׻� ȣ�������
					// 4.3 ���� ���� (4.2, 4.1) ������ ȣ��� ���� �ȵ� ���� �ִ�. ���� Mediamuxer��
					// ����Ѵٸ� �Ʒ��� ���� �ϸ� �ȴ�.
					Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : "
							+ mDecoder.getOutputFormat());
					break;

				case MediaCodec.INFO_TRY_AGAIN_LATER:
					// ����� MediaCodec���� ������ Buffer�� ���� ��� ������ ���� ��쿡 ���.
					// �� ��Ȳ�� ���� ���� ȣ��� �� �մ�.

					// Log.d(TAG, "INFO_TRY_AGAIN_LATER");
					break;

				default:
					// ���Ⱑ �������� ��쿡 ȣ��Ǵ� ���
					if (!first) {
						startWhen = System.currentTimeMillis();
						first = true;
					}
					try {
						long sleepTime = (info.presentationTimeUs / 1000)
								- (System.currentTimeMillis() - startWhen);
						Log.d(TAG, "info.presentationTimeUs : "
								+ (info.presentationTimeUs / 1000)
								+ " playTime: "
								+ (System.currentTimeMillis() - startWhen)
								+ " sleepTime : " + sleepTime);

						if (sleepTime > 0)
							Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					mDecoder.releaseOutputBuffer(outIndex, true /* Surface init */);
					break;
				}

				// All decoded frames have been rendered, we can stop playing
				// now
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
					break;
				}

				flag = 1;
				flag1 = 1;
			}

			mDecoder.stop();
			mDecoder.release();
			// mExtractor.release();
		}
	}

	public void close() {
		eosReceived = true;
	}
}
