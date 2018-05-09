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
    private Integer phaseCounter = 0;
    long MAX_PHASE_LENGTH = 5400000L;
    long MIN_PHASE_LENGTH = 3600000L;
    long MAX_CORRECTION_LENGTH = 600000L;
    long MIN_CORRECTION_LENGTH = 600000L;
    long DELTA_ERROR = 300000;
    private Calendar calendar;
    private Calendar phaseCalendar;
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
    private boolean stopRecording;
    // Инициализация переменных Таймера
    long DELAY_TIME = 1200000L; // Лучше всего 1200000 ms = 20 min
    private Timer mTimer;
    private MyTimerTask mMyTimerTask;
    private Timer dTimer;
    private dMyTimerTask dMyTimerTask;
    // Инициализация переменных модели
    private List<Classifier> mClassifiers = new ArrayList<>();
    private static final int MFCC_SIZE_HEIGHT = 20;
    private static final int MFCC_SIZE_WIDTH = 88;
    // Статистические величины
    double QUALITY_VALUE = -1;
    long FALL_VALUE = 0;
    int FALL_COUNTER = 0;
    long ALARM_START = 0;
    long ALARM_END = 0;
    boolean FALL_FIRST;
    long SLEEP_VALUE = 0;
    int SLEEP_COUNTER = 0;
    long PHASE_VALUE = 0;
    int PHASE_COUNTER = 0;
    long NEW_PHASE = 0;
    long PREV_PHASE = 0;
    long DAILY_SLEEP_LENGTH = 0;


    // Действия по инициализации активити
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (dTimer != null)
            dTimer.cancel();
        dTimer = new Timer();
        dMyTimerTask = new dMyTimerTask();
        dTimer.schedule(dMyTimerTask, 0, 86400000L);

        // Находим элементы
        setContentView(R.layout.activity_main);
        alarmTextView = (TextView) findViewById(R.id.phaseText);
        alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);
        Button start_alarm = (Button) findViewById(R.id.start_alarm);
        Button stop_alarm = (Button) findViewById(R.id.stop_alarm);
        Button statistic = (Button)  findViewById(R.id.statistic);
        Button options = (Button)  findViewById(R.id.options);
        Spinner spinner = (Spinner) findViewById(R.id.spinner);

        // Задаём контекст, спиннер
        this.context = this;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, lisRes);
        spinner.setAdapter(adapter);

        final Intent myIntent = new Intent(this.context, AlarmReceiver.class);

        // Создаём будильник
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        // Задаём будильниу выбранное время
        calendar = Calendar.getInstance();
        phaseCalendar = Calendar.getInstance();

        // tensorflow
        loadModel();

        // Листенер на кнопку "Запуска будильника"
        start_alarm.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                stopRecording = false;
                FALL_FIRST = true;
                // Считываем значения в необходимом формате
                final int hour = alarmTimePicker.getCurrentHour();
                final int minute = alarmTimePicker.getCurrentMinute();;
                String s_hour = getCorrectString(hour);
                String s_minute = getCorrectString(minute);
                // Создаём задачу в календаре
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                ALARM_START = calendar.getTimeInMillis();
                // Передаём в наше Намерение
                myIntent.putExtra("extra", "yes");
                myIntent.putExtra("song", String.valueOf(pos));
                pending_intent = PendingIntent.getBroadcast(MainActivity.this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                // Задаём будильник (с возможным смещением на сутки)
                if (calendar.before(Calendar.getInstance())) calendar.add(Calendar.DAY_OF_MONTH, 1);
                // Выводим результат
                setAlarmText("Время будильника " + s_hour + ":" + s_minute);
                // Ожидаем таймером номер 2
                // Запускаем таймер 1
                if (calendar.getTimeInMillis() - Calendar.getInstance().getTimeInMillis() < MIN_PHASE_LENGTH) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pending_intent);
                    stopRecording = true;
                }
                else {
                    if (mTimer != null)
                        mTimer.cancel();
                    mTimer = new Timer();
                    mMyTimerTask = new MyTimerTask();
                    mTimer.schedule(mMyTimerTask, DELAY_TIME, SAMPLE_DURATION_MS);
                    startRecording();
                }
            }
        });

        // Листенер на кнопку "Остановка будильника"
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

        // Листенер на кнопку перехода к окну статистики
        statistic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StatisticActivity.class);
                intent.putExtra("qualityValue", String.valueOf((int)QUALITY_VALUE));
                intent.putExtra("fallValue", String.valueOf(FALL_VALUE));
                intent.putExtra("sleepValue", String.valueOf(SLEEP_VALUE));
                intent.putExtra("phaseValue", String.valueOf(PHASE_VALUE));
                startActivity(intent);
            }
        });

        // Листенер на кнопку перехода к окну опций
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, OptionActivity.class);
                intent.putExtra("delayValue", String.valueOf(DELAY_TIME));
                intent.putExtra("lowerLimitValue", String.valueOf(MIN_CORRECTION_LENGTH));
                intent.putExtra("upperLimitValue", String.valueOf(MAX_CORRECTION_LENGTH));
                intent.putExtra("lowerPhaseValue", String.valueOf(MIN_PHASE_LENGTH));
                intent.putExtra("upperPhaseValue", String.valueOf(MAX_PHASE_LENGTH));
                startActivityForResult(intent, 999);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        if (requestCode == 999 && resultCode == RESULT_OK) {
            DELAY_TIME = Long.valueOf(dataIntent.getStringExtra("delayValue")).longValue();
            MIN_CORRECTION_LENGTH = Long.valueOf(dataIntent.getStringExtra("lowerLimitValue")).longValue();
            MAX_CORRECTION_LENGTH = Long.valueOf(dataIntent.getStringExtra("upperLimitValue")).longValue();
            MIN_PHASE_LENGTH = Long.valueOf(dataIntent.getStringExtra("lowerPhaseValue")).longValue();
            MAX_PHASE_LENGTH = Long.valueOf(dataIntent.getStringExtra("upperPhaseValue")).longValue();
        }
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
            String prediction = stopRecording();
            if (prediction == "Sleep") {
                Calendar tempCalendar = Calendar.getInstance();
                if (!FALL_FIRST) {
                    PHASE_VALUE = (PHASE_VALUE * PHASE_COUNTER + NEW_PHASE - PREV_PHASE) / (PHASE_COUNTER + 1);
                    PHASE_COUNTER++;
                    if (tempCalendar.getTimeInMillis() - NEW_PHASE > MIN_PHASE_LENGTH) {
                        PREV_PHASE = NEW_PHASE;
                        NEW_PHASE = tempCalendar.getTimeInMillis();
                    }
                    updateQuality();

                }
                if (FALL_FIRST) {
                    FALL_VALUE = (FALL_VALUE + (NEW_PHASE - ALARM_START)) / (FALL_COUNTER + 1);
                    FALL_COUNTER++;
                    FALL_FIRST = false;
                    NEW_PHASE = ALARM_START;
                }
                if (NEW_PHASE > phaseCalendar.getTimeInMillis() - DELTA_ERROR) {
                    phaseCalendar = (Calendar) tempCalendar.clone();
                    int minutes = (int)(MIN_PHASE_LENGTH / 60000L);
                    phaseCalendar.add(Calendar.MINUTE, minutes);
                    alarmCorrect();
                }
            }
        }
        if (stopRecording == true) {
            mTimer.cancel();
            return;
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
        Log.e("prediction", prediction);
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
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) throws IOException {

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

    // Основные реакции MainActivity
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
            // Запуск новой записи
            startRecording();
        }
    }

    // Создание задачи для таймера
    class dMyTimerTask extends TimerTask {
        @Override
        public void run() {
            if (QUALITY_VALUE != -1) {
                long diff = Math.abs(DAILY_SLEEP_LENGTH - 30600000L);
                double f = -((double) diff - 4500000L) / 1350000;
                if (diff < 1800000L)
                    QUALITY_VALUE += 2;
                else if (diff > 7200000L)
                    QUALITY_VALUE -= 2;
                else
                    QUALITY_VALUE += f;
                DAILY_SLEEP_LENGTH = 0;
                if (QUALITY_VALUE > 100)
                    QUALITY_VALUE = 100;
                if (QUALITY_VALUE < 0)
                    QUALITY_VALUE = 0;
            }
            else {
                QUALITY_VALUE = 60;
                long diff = Math.abs(DAILY_SLEEP_LENGTH - 30600000L);
                double f = (-((double) diff - 4500000L) / 1350000) * 20;
                if (diff < 1800000L)
                    QUALITY_VALUE += 40;
                else if (diff > 7200000L)
                    QUALITY_VALUE -= 40;
                else
                    QUALITY_VALUE += f;
                DAILY_SLEEP_LENGTH = 0;
            }
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
                                    "opt_dreamNet.pb", "labels.txt", MFCC_SIZE_HEIGHT, MFCC_SIZE_WIDTH,
                                    "input", "FC/output", 1.0f));
                } catch (final Exception e) {
                    // Ошибка, если не найдено
                    throw new RuntimeException("Error initializing classifiers!", e);
                }
            }
        }).start();
    }

    // Получение предсказания при помощи НН
    private String getPrediction(String inFilename) {
        FileInputStream in = null;
        byte[] data = new byte[RECORDING_LENGTH];
        byte[] resdata = new byte[RECORDING_LENGTH];
        try {
            in = new FileInputStream(inFilename);
            while(in.read(data) != -1){
                resdata = data;// Ничего не делать
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
        float[] mfccInput = mfccConvert.process(doubleInputBuffer);
        //Log.e("mfccInput", "" + mfccInput.length);
        String prediction = "Не удалось вычислить предсказание";
        // Классификация записи
        for (Classifier classifier : mClassifiers) {
            // Распознаем
            final Classification res = classifier.recognize(mfccInput);
            // если не может классифицировать - выводит вопросительный знак
            if (res.getLabel() == null) {
                prediction = "Не удалось получить точнй результат";
            } else {
                // иначе - имя выходного слоя
                prediction = res.getLabel();
            }
        }
        return prediction;
    }

    // Корректировка времени будильника
    private void alarmCorrect() {
        String TAG = "Correct";
        final long alarmTime = calendar.getTimeInMillis();
        final long correctTime = phaseCalendar.getTimeInMillis();
        long diff = alarmTime - correctTime;
        if (alarmTime > correctTime) {
            if (alarmTime - correctTime > MAX_PHASE_LENGTH) {
                // Ничего не делать - не корректировать время
                Log.e(TAG,"нет необходимости");
                return;
            }
            else {
                if (alarmTime - correctTime > MAX_CORRECTION_LENGTH) {
                    // Убавить -20 минут от будильника
                    Log.e(TAG,"убрано n минут");
                    calendar.add(Calendar.MINUTE, -(int) MIN_CORRECTION_LENGTH);
                    stopRecording = true;
                }
                else {
                    // Будить по новому времени
                    Log.e(TAG,"время изменено");
                    calendar.set(Calendar.MINUTE, phaseCalendar.get(Calendar.MINUTE));
                    calendar.set(Calendar.HOUR, phaseCalendar.get(Calendar.HOUR));
                    stopRecording = true;
                }
            }
        }
        else {
            if (correctTime - alarmTime > MAX_CORRECTION_LENGTH) {
                // Добавить время +20 минут к будильнику
                Log.e(TAG,"добавлено n минут");
                calendar.add(Calendar.MINUTE, (int) MAX_CORRECTION_LENGTH);
                stopRecording = true;
            }
            else {
                // Будить по новому времени
                Log.e(TAG,"время изменено");
                calendar.set(Calendar.MINUTE, phaseCalendar.get(Calendar.MINUTE));
                calendar.set(Calendar.HOUR, phaseCalendar.get(Calendar.HOUR));
                stopRecording = true;
            }
        }
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pending_intent);
        ALARM_END = calendar.getTimeInMillis();
        DAILY_SLEEP_LENGTH += (ALARM_END - ALARM_START);
        SLEEP_VALUE = (SLEEP_VALUE * SLEEP_COUNTER + (ALARM_END - ALARM_START)) / (SLEEP_COUNTER + 1);
        SLEEP_COUNTER++;
    }
    private void updateQuality() {
        if (QUALITY_VALUE != -1) {
            long diff = Math.abs(NEW_PHASE - PREV_PHASE - PHASE_VALUE);
            long perc = diff / PHASE_VALUE;
            double f = (0.5 - perc) * 4;
            if (diff >= 1)
                QUALITY_VALUE -= 2.0;
            else if (diff < 0.1)
                QUALITY_VALUE += 2.0;
            else
                QUALITY_VALUE += f;
            if (QUALITY_VALUE > 100)
                QUALITY_VALUE = 100;
            if (QUALITY_VALUE < 0)
                QUALITY_VALUE = 0;
        }
    }
}