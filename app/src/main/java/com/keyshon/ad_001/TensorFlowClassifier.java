package com.keyshon.ad_001;

import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;


// Класс - классификатор
public class TensorFlowClassifier implements Classifier {

    // Возвращает результат, если уверенность в ответе больше порогового значения
    private static final float THRESHOLD = 0.5f;

    private TensorFlowInferenceInterface tfHelper;

    private String name;
    private String inputName;
    private String outputName;
    private int inputSize;
    private boolean feedKeepProb;

    private List<String> labels;
    private float[] output;
    private String[] outputNames;

    // Получает сохранённую можель, считывание лейблов модели в список
    private static List<String> readLabels(AssetManager am, String fileName) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(am.open(fileName)));

        String line;
        List<String> labels = new ArrayList<>();
        while ((line = br.readLine()) != null) {
            labels.add(line);
        }

        br.close();
        return labels;
    }

    // Записывает в модель лейблы и метаданные модели в том числе предсказания
    public static TensorFlowClassifier create(AssetManager assetManager, String name,
                                              String modelPath, String labelFile, int inputSize, String inputName, String outputName,
                                              boolean feedKeepProb) throws IOException {
        // Инициализация классификатора
        TensorFlowClassifier c = new TensorFlowClassifier();

        // Лейблы первого и последнего слоя, а также название модели
        c.name = name;
        c.inputName = inputName;
        c.outputName = outputName;

        // Считывание лейблов
        c.labels = readLabels(assetManager, labelFile);

        // Задание пути до модели и где хранятся ассеты
        c.tfHelper = new TensorFlowInferenceInterface(assetManager, modelPath);
        int numClasses = 2;

        // Размер входного массива
        c.inputSize = inputSize;

        // Инициализация остальных переменных модели
        c.outputNames = new String[]{outputName};
        c.outputName = outputName;
        c.output = new float[numClasses];
        c.feedKeepProb = feedKeepProb;

        return c;
    }

    // Возвращает имя модели
    @Override
    public String name() {
        return name;
    }

    // Классификация данных
    @Override
    public Classification recognize(final float[] pixels) {

        // При помощи интерфейса происходит передача имени входного слоя, данных и размера
        tfHelper.feed(inputName, pixels, 1, inputSize, inputSize, 1);

        // Вероятности
        if (feedKeepProb) {
            tfHelper.feed("keep_prob", new float[]{1});
        }
        // Прогон данных по модели
        tfHelper.run(outputNames);

        // Считывание выходного слоя
        tfHelper.fetch(outputName, output);

        // Вычисление результата классификации (для каждого класса
        Classification ans = new Classification();
        for (int i = 0; i < output.length; ++i) {
            System.out.println(output[i]);
            System.out.println(labels.get(i));
            if (output[i] > THRESHOLD && output[i] > ans.getConf()) {
                ans.update(output[i], labels.get(i));
            }
        }

        return ans;
    }
}
