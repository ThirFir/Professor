package com.example.ggwave;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ggwave.databinding.ActivityMainBinding;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ShortBuffer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private AttendanceAdapter adapter;

    private rtNoiseReducer rtNoiseReducer;

    private String kMessageToSend = generateRandomKey();
    private CapturingThread mCapturingThread;
    private PlaybackThread mPlaybackThread;
    private static final int REQUEST_RECORD_AUDIO = 13;

    // TODO : BASE URL
    private static final String BASE_URL = "http://15.165.236.170:5000/professor/";
    private OkHttpClient client;
    private Retrofit retrofit;
    private ServerApi api;

    private String id = "PROF001";
    private String currentLecture;

    // Native interface:
    private native void initNative();
    private native void processCaptureData(short[] data);
    private native void sendMessage(String message);

    // Native callbacks:
    private void onNativeReceivedMessage(byte c_message[]) {
        String message = new String(c_message);
        Log.v("ggwave", "Received message: " + message);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
    }

    private void onNativeMessageEncoded(short c_message[]) {
        Log.v("ggwave", "Playing encoded message ..");

        mPlaybackThread = new PlaybackThread(c_message, new PlaybackListener() {
            @Override
            public void onProgress(int progress) {
                // todo : progress updates
            }
            @Override
            public void onCompletion() {
                mPlaybackThread.stopPlayback();
                String key = kMessageToSend;
                sendMessage(key);
                mPlaybackThread.startPlayback(getApplicationContext(), key);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        System.loadLibrary("test-cpp");
        initNative();
        adapter = new AttendanceAdapter();
        initRetrofitApi();
        initLectureList();

        mCapturingThread = new CapturingThread(new AudioDataReceivedListener() {
            @Override
            public void onAudioDataReceived(short[] data) {
                processCaptureData(data);
            }
        });

        binding.lottiePlay.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlaybackThread == null || !mPlaybackThread.playing()) {
                    if(api != null) {
                        api.getRandomKey().enqueue(new Callback<KeyResp>() {
                            @Override
                            public void onResponse(Call<KeyResp> call, Response<KeyResp> response) {
                                if (response.isSuccessful()) {
                                    KeyResp keyResp = response.body();
                                    if (keyResp != null) {
                                        kMessageToSend = keyResp.getKey();
                                        binding.textViewMessageToSend.setText(kMessageToSend);
                                        sendMessage(kMessageToSend);
                                        mPlaybackThread.startPlayback(getApplicationContext(), kMessageToSend);
                                    }
                                } else {
                                    try {
                                        Log.e("ggwave", "Error: " + response.errorBody().string());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<KeyResp> call, Throwable t) {
                                Log.e("ggwave", "Error: " + t.getMessage());
                            }
                        });
                    } else {
                        kMessageToSend = generateRandomKey();
                        sendMessage(kMessageToSend);
                        mPlaybackThread.startPlayback(getApplicationContext(), kMessageToSend);
                        binding.textViewMessageToSend.setText(kMessageToSend);
                    }
                } else {
                    mPlaybackThread.stopPlayback();
                }

                if (mPlaybackThread.playing()) {
                    binding.lottiePlay.playAnimation();
                    binding.textViewStatusOut.setText("Status: Playing audio");
                } else {
                    binding.lottiePlay.cancelAnimation();
                    binding.lottiePlay.setProgress(0);
                    binding.textViewStatusOut.setText("Status: Idle");
                }
            }
        });
    }

    private void startAudioCapturingSafe() {
        Log.i("ggwave", "startAudioCapturingSafe");

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Log.i("ggwave", " - record permission granted");
            mCapturingThread.startCapturing();
        } else {
            Log.i("ggwave", " - record permission NOT granted");
            requestMicrophonePermission();
        }
    }

    private void requestMicrophonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
            new AlertDialog.Builder(this)
                    .setTitle("Microphone Access Requested")
                    .setMessage("Microphone access is required in order to receive audio messages")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                                    android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            mCapturingThread.stopCapturing();
        }
    }

    private String generateRandomKey() {
        int leftLimit = 48; // 0
        int rightLimit = 122; // z
        int targetStringLength = 8;
        Random random = new Random();
        String generatedString = "";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            generatedString = random.ints(leftLimit, rightLimit + 1)
                    .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                    .limit(targetStringLength)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
        }

        return generatedString;
    }

    private void initRetrofitApi() {
        client = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        retrofit = new Retrofit.Builder()
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .build();
        api = retrofit.create(ServerApi.class);
    }

    void initRTNR(){
        try {
            rtNoiseReducer = new rtNoiseReducer(this);
        } catch (IOException e) {
            Log.d("class", "Failed to create noise reduction");
        }
    }

    private void initLectureList() {
        Log.e("ggwave1111", "initLectureList");
        api.getLectureList(id).enqueue(new Callback<List<LectureResp>>() {
            @Override
            public void onResponse(Call<List<LectureResp>> call, Response<List<LectureResp>> response) {
                if (response.isSuccessful()) {
                    List<LectureResp> lectureList = response.body();
                    if (lectureList != null) {
                        for (LectureResp lecture : lectureList) {
                            LocalTime now = LocalTime.now();
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                            LocalTime startTime = LocalTime.parse(lecture.getStartTime(), formatter);
                            LocalTime endTime = LocalTime.parse(lecture.getEndTime(), formatter);
                            if ((now.isAfter(startTime) || now.equals(startTime)) && now.isBefore(endTime)) {
                                currentLecture = lecture.getLectureCode();
                                Log.e("ggwave1111", "Current lecture: " + currentLecture);
                                break;
                            }
                        }
                    }
                    for(LectureResp lecture : lectureList) {
                        Log.e("ggwave1111", "Lecture: " + lecture.getLectureCode() + " " + lecture.getLectureName() + " " + lecture.getProfessorId() + " " + lecture.getStartTime() + " " + lecture.getEndTime());
                    }
                } else {
                    try {
                        Log.e("ggwave1111", "Error: " + response.errorBody().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<LectureResp>> call, Throwable t) {
                Log.e("ggwave1111", "Error: " + t.getMessage());
            }
        });
    }
}

interface AudioDataReceivedListener {
    void onAudioDataReceived(short[] data);
}

class CapturingThread {
    private static final String LOG_TAG = CapturingThread.class.getSimpleName();
    private static final int SAMPLE_RATE = 48000;

    public CapturingThread(AudioDataReceivedListener listener) {
        mListener = listener;
    }

    private boolean mShouldContinue;
    private AudioDataReceivedListener mListener;
    private Thread mThread;

    public boolean capturing() {
        return mThread != null;
    }

    public void startCapturing() {
        if (mThread != null)
            return;

        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                capture();
            }
        });
        mThread.start();
    }

    public void stopCapturing() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;
    }

    private void capture() {
        Log.v(LOG_TAG, "Start");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }
        bufferSize = 4*1024;

        short[] audioBuffer = new short[bufferSize / 2];

        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        Log.d("ggwave", "buffer size = " + bufferSize);
        Log.d("ggwave", "Sample rate = " + record.getSampleRate());

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();

        Log.v(LOG_TAG, "Start capturing");

        long shortsRead = 0;
        while (mShouldContinue) {
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            shortsRead += numberOfShort;

            mListener.onAudioDataReceived(audioBuffer);
        }

        record.stop();
        record.release();

        Log.v(LOG_TAG, String.format("Capturing stopped. Samples read: %d", shortsRead));
    }
}

interface PlaybackListener {
    void onProgress(int progress);
    void onCompletion();
}

class PlaybackThread {
    static final int SAMPLE_RATE = 48000;
    private static final String LOG_TAG = PlaybackThread.class.getSimpleName();

    public PlaybackThread(short[] samples, PlaybackListener listener) {
        mSamples = ShortBuffer.wrap(samples);
        mNumSamples = samples.length;
        mListener = listener;
    }

    private Thread mThread;
    private boolean mShouldContinue;
    private ShortBuffer mSamples;
    private int mNumSamples;
    private PlaybackListener mListener;

    public boolean playing() {
        return mThread != null;
    }

    public void startPlayback(Context applicationContext, String key) {
        if (mThread != null)
            return;

        // Start streaming in a thread
        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    play(applicationContext, key);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        mThread.start();
    }

    public void stopPlayback() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;
    }

    private void play(Context applicationContext, String key) throws FileNotFoundException {
        int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        bufferSize = 16*1024;

        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);

        audioTrack.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
            @Override
            public void onPeriodicNotification(AudioTrack track) {
                if (mListener != null && track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    mListener.onProgress((track.getPlaybackHeadPosition() * 1000) / SAMPLE_RATE);
                }
            }
            @Override
            public void onMarkerReached(AudioTrack track) {
                Log.v(LOG_TAG, "Audio file end reached");
                track.release();
                if (mListener != null) {
                    mListener.onCompletion();
                }
            }
        });
        audioTrack.setPositionNotificationPeriod(SAMPLE_RATE / 30); // 30 times per second
        audioTrack.setNotificationMarkerPosition(mNumSamples);

        audioTrack.play();

        Log.v(LOG_TAG, "Audio streaming started");

//        FileOutputStream out = new FileOutputStream(applicationContext.getFilesDir() + "/" + key + ".wav");
        long totalAudioLen = mNumSamples * 2L;
        long totalDataLen = totalAudioLen + 36;
//        try {
//            addWavHeader(out, totalAudioLen, totalDataLen);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        short[] buffer = new short[bufferSize];
        mSamples.rewind();
        int limit = mNumSamples;
        int totalWritten = 0;
        while (mSamples.position() < limit && mShouldContinue) {
            int numSamplesLeft = limit - mSamples.position();
            int samplesToWrite;
            if (numSamplesLeft >= buffer.length) {
                mSamples.get(buffer);
                samplesToWrite = buffer.length;
            } else {
                for(int i = numSamplesLeft; i < buffer.length; i++) {
                    buffer[i] = 0;
                }
                mSamples.get(buffer, 0, numSamplesLeft);
                samplesToWrite = numSamplesLeft;
            }
            totalWritten += samplesToWrite;
            audioTrack.write(buffer, 0, samplesToWrite);


            byte[] byteArray = shortArrayToByteArray(buffer);
//            try {
//                out.write(byteArray);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }
//        try {
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        if (!mShouldContinue) {
            audioTrack.release();
        }

        Log.v(LOG_TAG, "Audio streaming finished. Samples written: " + totalWritten);
    }

    private void addWavHeader(FileOutputStream out, long totalAudioLen, long totalDataLen) throws IOException {
        int sampleRate = SAMPLE_RATE;
        short channels = 1;
        int bitsPerSample = 16;
        long byteRate = SAMPLE_RATE * channels * bitsPerSample / 8;
        long blockAlign = (long) (channels * bitsPerSample / 8);

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0);
        header[32] = (byte) (blockAlign);  // block align
        header[33] = 0;
        header[34] = (byte) bitsPerSample;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);

    }

    private static byte[] shortArrayToByteArray(short[] shortArray) {
        int shortArrayLength = shortArray.length;
        byte[] byteArray = new byte[shortArrayLength * 2]; // short는 2바이트이므로 * 2

        for (int i = 0; i < shortArrayLength; i++) {
            // 리틀 엔디안 방식으로 변환
            byteArray[i * 2] = (byte) (shortArray[i] & 0xFF);
            byteArray[i * 2 + 1] = (byte) ((shortArray[i] >> 8) & 0xFF);

            // 빅 엔디안 방식으로 변환 (주석을 해제하면 됨)
            // byteArray[i * 2] = (byte) ((shortArray[i] >> 8) & 0xFF);
            // byteArray[i * 2 + 1] = (byte) (shortArray[i] & 0xFF);
        }

        return byteArray;
    }
}
