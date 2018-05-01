package com.keyshon.ad_001;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.media.MediaRecorder;
import android.util.Log;
import android.os.Environment;
import android.os.CountDownTimer;
import android.media.AudioManager;
import android.media.AudioDeviceInfo;
import android.media.MediaPlayer;
//import edu.cmu.sphinx.tools.feature;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    // Инициализация переменных БУДИЛЬНИКА
    AlarmManager alarmManager;
    private PendingIntent pending_intent;
    private AlarmReceiver alarm;
    private TimePicker alarmTimePicker;
    private TextView alarmTextView;
    MainActivity inst;
    Context context;
    String[] lisRes = {"Случайно", "Звук 1", "Звук 2", "Звук 3", "Звук 4", "Звук 5", "Звук 6", "Звук 7", "Звук 8", "Звук 9"};
    Integer pos = 0;
    // Инициализация переменных АУДИОРЕКОРДЕРА
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 22050;
    private static final int SAMPLE_DURATION_MS = 2000;
    private static final int RECORDING_LENGTH = (int) (RECORDER_SAMPLERATE * SAMPLE_DURATION_MS / 1000);
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    // Инициализация переменных Таймера
    private static long delayTime = 1000; // Лучше всего 1200000 ms = 20 min
    private Timer mTimer;
    private MyTimerTask mMyTimerTask;
    // Инициализация переменных модели
    private List<Classifier> mClassifiers = new ArrayList<>();
    private static final int MFCC_SIZE_HEIGHT = 20;
    private static final int MFCC_SIZE_WIDTH = 87;

    // Действия по инициализации активити
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        // Находим элементы
        setContentView(R.layout.activity_main);
        alarmTextView = (TextView) findViewById(R.id.alarmText);
        alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);
        Button start_alarm = (Button) findViewById(R.id.start_alarm);
        Button stop_alarm = (Button) findViewById(R.id.stop_alarm);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        // Задаём контекст, спиннер
        this.context = this;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lisRes);
        spinner.setAdapter(adapter);

        final Intent myIntent = new Intent(this.context, AlarmReceiver.class);

        // Создаём будильник
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        // Задаём будильниу выбранное время
        final Calendar calendar = Calendar.getInstance();

        // tensorflow
        loadModel();

        // Листенер на кнопку "Запуска будильника"
        start_alarm.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                // Считываем значения в необходимом формате
                final int hour = alarmTimePicker.getCurrentHour();
                final int minute = alarmTimePicker.getCurrentMinute();;
                String s_hour = getCorrectString(hour);
                String s_minute = getCorrectString(minute);
                // Создаём задачу в календаре
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                // Передаём в наше Намерение
                myIntent.putExtra("extra", "yes");
                myIntent.putExtra("song", String.valueOf(pos));
                pending_intent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                // Задаём будильник (с возможным смещением на сутки)
                long timeOffset = calendar.getTimeInMillis();
                if (calendar.before(Calendar.getInstance())) timeOffset+= 86400000L;
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeOffset, pending_intent);
                // Выводим результат
                setAlarmText("Время будильника " + s_hour + ":" + s_minute);
                // Ожидаем таймером номер 2
                // Запускаем таймер 1
                if (mTimer != null)
                    mTimer.cancel();
                mTimer = new Timer();
                mMyTimerTask = new MyTimerTask();

                mTimer.schedule(mMyTimerTask, delayTime, 2000);
                startRecording();
            }
        });
        stop_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Отправка выключения будильника
                myIntent.putExtra("extra", "no");
                sendBroadcast(myIntent);
                alarmManager.cancel(pending_intent);
                // Обновление текста
                setAlarmText("Будильник отменён");
                // Выключаем таймер 1
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
                // Выключение записи
                stopRecording();
            }
        });
        // Листенер на выпадающий список
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onNothingSelected(AdapterView<?> p0) {
                pos = 0;
            }

            @Override
            public void onItemSelected(AdapterView<?> p0, View p1, int p2, long p3) {
                pos = p2;
            }
        });
    }
    // Получить имя файла
    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + "sleep_sound" + AUDIO_RECORDER_FILE_EXT_WAV);
    }
    // Получить имя временного файла
    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }
    // Начать запись
    private void startRecording(){
        // Остановка записи
        if (recorder != null && isRecording) {
            stopRecording();
        }
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);
        int i = recorder.getState();
        if(i==1)
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }
    // Сохранить в файл
    private void writeAudioDataToFile(){
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read = 0;

        if(null != os){
            while(isRecording){
                read = recorder.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    // Остановить запись
    private String stopRecording(){
        if(recorder != null && isRecording){
            isRecording = false;

            int i = recorder.getState();
            if(i==1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        String prediction = getPrediction(getTempFilename());
        copyWaveFile(getTempFilename(),getFilename());
        deleteTempFile();
        return prediction;
    }
    // Удалить временный файл
    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }
    // Скопировать wav-файл
    private void copyWaveFile(String inFilename, String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Записать wav-заголовок для правильного сохранения
    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
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
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;
        header[34] = RECORDER_BPP;
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
    // Установить текст в поле
    public void setAlarmText(String alarmText) {
        alarmTextView.setText(alarmText);
    }
    // Приводим число к корректному строковому представлению
    private String getCorrectString(Integer num) {
        if (num < 10)
            return "0" + String.valueOf(num);
        else
            return String.valueOf(num);
    }
    @Override
    public void onStart() {
        super.onStart();
        inst = this;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Создание задачи для таймера
    class MyTimerTask extends TimerTask {
        @Override
        public void run() {
            if (recorder != null) {
                String result = stopRecording();
                // Изменение state
                Log.e("Запись", "Завершена");
            }
            // Запуск новой записи
            startRecording();
        }
    }
    // Создание модели и сохранение тензорфлоу модели в формате .pb с обученными весами
    private void loadModel() {
        // Интерфейс для прогона данных
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Добавление 2 классификаторов (я скорее всего буду использвать 1
                    mClassifiers.add(
                            TensorFlowClassifier.create(getAssets(), "TensorFlow",
                                    "opt_mnist_convnet-tf.pb", "labels.txt", MFCC_SIZE_HEIGHT, MFCC_SIZE_WIDTH,
                                    "input", "output", true));
//                    mClassifiers.add(
//                            TensorFlowClassifier.create(getAssets(), "Keras",
//                                    "opt_mnist_convnet-keras.pb", "labels.txt", MFCC_SIZE,
//                                    "conv2d_1_input", "dense_2/Softmax", false));
                } catch (final Exception e) {
                    // Ошибка, если не найдено
                    throw new RuntimeException("Error initializing classifiers!", e);
                }
            }
        }).start();
    }
    private String getPrediction(String inFilename) {
        FileInputStream in = null;
        byte[] data = new byte[RECORDING_LENGTH];
        try {
            in = new FileInputStream(inFilename);
            while(in.read(data) != -1){
                // Ничего не делать
            }
            in.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Преобразование записи в mfcc
        double[] doubleInputBuffer = new double[RECORDING_LENGTH];
        for (int i = 0; i < RECORDING_LENGTH; ++i) {
            doubleInputBuffer[i] = data[i] / 32767.0;
        }
        MFCC mfccConvert = new MFCC();
        Log.e("doubleInputBuffer", "" + doubleInputBuffer.length);
        float[] mfccInput = mfccConvert.process(doubleInputBuffer);
        Log.e("mfccInput", "" + mfccInput.length);
        String prediction = "Не удалось вычислить предсказание";
        // Классификация записи
        for (Classifier classifier : mClassifiers) {
            // Распознаем
            final Classification res = classifier.recognize(mfccInput);
            // если не может классифицировать - выводит вопросительный знак
            if (res.getLabel() == null) {
                prediction = "Не удалось получить точнй результат";
                Log.e("Нет данных о лейблах", classifier.name() + ": ?\n");
            } else {
                // иначе - имя выходного слоя
                prediction = res.getLabel();
                //Log.e("Успешно - результат", String.format("%s: %s, %f\n", classifier.name(), res.getLabel(), res.getConf()));
            }
        }
        return prediction;
    }
}
