# Бибилиотеки проекта
import sys
# Библиотека для работы с Операционной Системой
import os
# Библиотека для работы со времененм
import time
# Библиотека случайных чисел
import random
# Математическая библиотека
import numpy as np
# Библиотека анализа данныз
import pandas as pd
# Библиотека анализа аудиофайлов
import librosa
# Библиотека программирования нейросети
import tensorflow as tf
#import my_freeze_graph
import freeze_graph
from tensorflow.python.tools import optimize_for_inference_lib

# Глобальные параметры
PATH_TRAIN = 'E:/Documents/Dataset/Test'
PATH_TEST = 'E:/Documents/Dataset/Train'
BATCH_SIZE = 10
ITERATIONS = 2
EPOCHS = 2
ITERATIONS_TEST = 1
EVAL_EVERY = 1
HEIGHT = 20
WIDTH = 88
NUM_LABELS = 0
LEARNING_RATE = 0.000000008
LOGDIR = 'log/'
TEST_LOGDIR = 'logtest/'
LABEL_TO_INDEX_MAP = {}
MODEL_NAME = 'dreamNet'

tf.app.flags.DEFINE_integer('training_iteratiom', 1000, 'number of training iterations.')
tf.app.flags.DEFINE_integer('model_version', 1, 'version number of the model.')
tf.app.flags.DEFINE_string('work_dir', '/tmp', 'Working directory.')
FLAGS = tf.app.flags.FLAGS

# Создание маппинга меток с индексами и наоборот
def init(path):
    labels = os.listdir(path)
    index = 0
    for label in labels:
        LABEL_TO_INDEX_MAP[label] = index
        index += 1
    global NUM_LABELS
    NUM_LABELS = len(LABEL_TO_INDEX_MAP)

# Конвертация лейбла к вектору типа {1, 0, 0, ..., 0}
def one_hot_encoding(label):
    encoding = [0] * len(LABEL_TO_INDEX_MAP)
    encoding[LABEL_TO_INDEX_MAP[label]] = 1
    return encoding

# Представление звука в виде Mel-frequency cepstrum - энергетического спектра
def get_mfcc(wave_path, PAD_WIDTH=WIDTH):
    wave, sr = librosa.load(wave_path, mono=True)
    mfccs = librosa.feature.mfcc(y=wave, sr=sr, n_mfcc=HEIGHT)
    mfccs = np.pad(mfccs, ((0, 0), (0, PAD_WIDTH - len(mfccs[0]))), mode='constant')
    return mfccs

# Функция получения батча меток и аудио-характеристик
def get_batch(batch_size, path):
    X = []
    Y = []
    #random.seed(5896)
    path = os.path.join(path, '*', '*.wav')
    waves = tf.gfile.Glob(path)
    while True:
        random.shuffle(waves)
        for wave_path in waves:
            _, label = os.path.split(os.path.dirname(wave_path))
            X.append(get_mfcc(wave_path))
            Y.append(one_hot_encoding(label))
            if (len(X) == batch_size):
                yield X, Y
                X = []
                Y = []

def get_model(input, dropout):
    # 1. Свёрточный слой
    with tf.name_scope('Conv1'):
        input_4D = tf.reshape(input, [-1, HEIGHT, WIDTH, 1])
        w1 = tf.Variable(tf.truncated_normal([12, 8, 1, 44], stddev=0.01), name='W1')
        b1 = tf.Variable(tf.zeros([44]), name='B1')
        conv1 = tf.nn.conv2d(input_4D, w1, strides=[1, 1, 1, 1], padding='SAME')
        act1 = tf.nn.relu(conv1 + b1)
        drop1 = tf.nn.dropout(act1, dropout)
        max_pool1 = tf.nn.max_pool(drop1, ksize=[1, 2, 2, 1], strides=[1, 2, 2, 1], padding='SAME')
        tf.summary.histogram('weights', w1)
        tf.summary.histogram('biases', b1)
        tf.summary.histogram('activations', act1)
        tf.summary.histogram('dropouts', drop1)
    # 2. Свёрточный слой
    with tf.name_scope('Conv2'):
        w2 = tf.Variable(tf.truncated_normal([6, 4, 44, 44], stddev=0.01), name='W2')
        b2 = tf.Variable(tf.zeros([44]), name='B2')
        conv2 = tf.nn.conv2d(max_pool1, w2, strides=[1, 1, 1, 1], padding='SAME')
        act2 = tf.nn.relu(conv2 + b2)
        drop2 = tf.nn.dropout(act2, dropout)
        tf.summary.histogram('weights', w2)
        tf.summary.histogram('biases', b2)
        tf.summary.histogram('activations', act2)
        tf.summary.histogram('dropouts', drop2)
        # Решейпинг для полносвязного слоя
        conv_shape = drop2.get_shape()
        count = int(conv_shape[1] * conv_shape[2] * conv_shape[3])
        flat_output = tf.reshape(drop2, [-1, count])
    # Полносвязный слой
    with tf.name_scope('FC'):
        w3 = tf.Variable(tf.truncated_normal([count, NUM_LABELS], stddev=0.01), name='W3')
        b3 = tf.Variable(tf.zeros([NUM_LABELS]), name='B3')
        fc = tf.add(tf.matmul(flat_output, w3), b3)
        tf.summary.histogram('weights', w3)
        tf.summary.histogram('biases', b3)
    return fc

# Точка входа 
def main():
    # Запуска графа и сессии в Tensorflow
    tf.reset_default_graph()
    with tf.Session() as sess:
        # Входные данные
        x = tf.placeholder(tf.float32, shape=[None, HEIGHT, WIDTH], name = 'input')
        # Метки
        y = tf.placeholder(tf.float32, shape=[None, NUM_LABELS], name = 'label')
        # Шанс срабатывания dropout
        dropout = tf.placeholder(tf.float32, name='dropout')
        # Модель нейронной сети
        logits = get_model(x, dropout)
        # Loss-функция
        with tf.name_scope('loss'):
            # ОТЛИЧАЕТСЯ ОТ ВИДЕО!!!
            loss = tf.reduce_mean(tf.nn.softmax_cross_entropy_with_logits(logits=logits, labels=y))
            tf.summary.scalar('loss', loss)
        # Оптимайзер Loss-функции
        with tf.name_scope('train'):
            train_step = tf.train.AdamOptimizer(LEARNING_RATE).minimize(loss)
        # Функция вычисления точности
        with tf.name_scope('accuracy'):
            predicided = tf.argmax(logits, 1)
            truth = tf.argmax(y, 1)
            correct_prediction = tf.equal(predicided, truth)
            accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))
            confusion_matrix = tf.confusion_matrix(truth, predicided, num_classes=NUM_LABELS)
            tf.summary.scalar('accuracy', accuracy)
        # Настройка тензорборда
        summ = tf.summary.merge_all()
        sess.run(tf.global_variables_initializer())
        tf.train.write_graph(sess.graph_def, 'out', MODEL_NAME + '.pbtxt', True)
        writer = tf.summary.FileWriter(LOGDIR)
        writer.add_graph(sess.graph)
        test_writer = tf.summary.FileWriter(TEST_LOGDIR)
        saver = tf.train.Saver()
        # Обучение модели
        print('Starting training\n')
        batch = get_batch(BATCH_SIZE, PATH_TRAIN)
        for epoch in range(1, EPOCHS):
            epoch_acc = 0
            for i in range(1, ITERATIONS + 1):
                begin = time.time()
                X, Y = next(batch)
                if i % EVAL_EVERY == 0:
                    # ОТЛИЧАЕТСЯ ОТ ВИДЕО!!!
                    [train_accuracy, train_loss, s] = sess.run([accuracy, loss, summ], feed_dict={x: X,y: Y, dropout: 1.0})
                    acc_and_loss = [i, train_loss, train_accuracy * 100]
                    epoch_acc = train_accuracy
                    writer.add_summary(s, i)
                    print(acc_and_loss)
                if i % (EVAL_EVERY * 20) == 0:
                    train_confusion_matrix = sess.run([confusion_matrix], feed_dict={x: X, y: Y, dropout: 1.0})
                    header = LABEL_TO_INDEX_MAP.keys()
                sess.run(train_step, feed_dict={x: X, y: Y, dropout: 0.7})
        # Тестирование модели
        batch = get_batch(BATCH_SIZE, PATH_TEST)
        total_accuracy = 0
        for i in range(ITERATIONS_TEST):
            X, Y = next(batch, PATH_TEST)
            test_accuracy, s = sess.run([accuracy, summ], feed_dict={x: X, y: Y, dropout: 1.0})
            total_accuracy += (test_accuracy/ITERATIONS_TEST)
            test_writer.add_summary(s, i)
            print(test_accuracy)
        print(total_accuracy)
        # Сохранение модели
        saver.save(sess, 'out/' + MODEL_NAME + '.chkp')
        s = ''
        for n in tf.get_default_graph().as_graph_def().node:
            s += (n.name + ',')
        s = s[:-1]
        freeze_graph.freeze_graph('C:/Temp/AD_001/model/out', s, MODEL_NAME)
    input_graph_def = tf.GraphDef()
    with tf.gfile.Open('out/' + MODEL_NAME + '.pb', "rb") as f:
        input_graph_def.ParseFromString(f.read())
    input_node_name = 'input'
    output_node_name = 'output'
    output_graph_def = optimize_for_inference_lib.optimize_for_inference(input_graph_def, [input_node_name], [output_node_name], tf.float32.as_datatype_enum)
    with tf.gfile.FastGFile('out/opt_' + MODEL_NAME + '.pb', "wb") as f:
        f.write(output_graph_def.SerializeToString())

if __name__ == '__main__':
    init(PATH_TRAIN)
    main()