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

# Глобальные параметры
PATH_TRAIN = 'E:\Documents\Dataset\Test'
PATH_TEST = 'E:\Documents\Dataset\Train'
BATCH_SIZE = 50
ITERATIONS = 20
EPOCHS = 2
ITERATIONS_TEST = 10
EVAL_EVERY = 1
HEIGHT = 20
WIDTH = 88
NUM_LABELS = 0
LEARNING_RATE = 1E-6
LOGDIR = 'log/'
TEST_LOGDIR = 'log_test/'
LABEL_TO_INDEX_MAP = {}

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

# Модель сети
'''
Топология:
    1. Свёрточный слой
        Relu-активация
        Dropout
        Max Pooling
    2. Свёрточный слой
        Relu-активация
        Dropout
    3. Полносвязный слой
        MatMul - перемножение матриц
'''
def get_model(input, dropout):
    # 1. Свёрточный слой
    with tf.name_scope('Conv1'):
        input_4D = tf.reshape(input, [-1, HEIGHT, WIDTH, 1])
        w1 = tf.Variable(tf.truncated_normal([12, 8, 1, 44], stddev=0.01), name='W')
        b1 = tf.Variable(tf.zeros([44]), name='B')
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
        w2 = tf.Variable(tf.truncated_normal([6, 4, 44, 44], stddev=0.01), name='W')
        b2 = tf.Variable(tf.zeros([44]), name='B')
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
        w3 = tf.Variable(tf.truncated_normal([count, NUM_LABELS], stddev=0.01))
        b3 = tf.Variable(tf.zeros([NUM_LABELS]))
        fc = tf.add(tf.matmul(flat_output, w3), b3)
        tf.summary.histogram('weights', w3)
        tf.summary.histogram('biases', b3)
    
    return fc

# Точка входа 
def main():
    # Запуска графа и сессии в Tensorflow
    tf.reset_default_graph()
    sess = tf.Session()
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
    saver = tf.train.Saver()
    sess.run(tf.global_variables_initializer())
    writer = tf.summary.FileWriter(LOGDIR)
    writer.add_graph(sess.graph)
    test_writer = tf.summary.FileWriter(TEST_LOGDIR)
    # Обучение модели
    print('Starting training\n')
    batch = get_batch(BATCH_SIZE, PATH_TRAIN)
    #start_time = time.time()
    for epoch in range(1, EPOCHS):
        epoch_acc = 0
        for i in range(1, ITERATIONS + 1):
            begin = time.time()
            X, Y = next(batch)
            if i % EVAL_EVERY == 0:
                # ОТЛИЧАЕТСЯ ОТ ВИДЕО!!!
                [train_accuracy, train_loss, s] = sess.run([accuracy, loss, summ], feed_dict={x: X,y: Y, dropout: 0.75})
                acc_and_loss = [i, train_loss, train_accuracy * 100]
                epoch_acc = train_accuracy
                writer.add_summary(s, i)
                print(acc_and_loss)
            if i % (EVAL_EVERY * 20) == 0:
                tf.train.write_graph(sess.graph_def, '.', 'dreamNet.pbtxt')
                train_confusion_matrix = sess.run([confusion_matrix], feed_dict={x: X, y: Y, dropout: 1.0})
                header = LABEL_TO_INDEX_MAP.keys()
                #df = pd.DataFrame(np.reshape(train_confusion_matrix, (NUM_LABELS, NUM_LABELS)), index=??)
                saver.save(sess, os.path.join(LOGDIR, 'dreamNet.ckpt'), i)
            sess.run(train_step, feed_dict={x: X, y: Y, dropout: 0.75})
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

    MODEL_NAME = 'dreamNet'

    input_graph_path = MODEL_NAME + '.pbtxt'
    checkpoint_path = MODEL_NAME + '.ckpt'
    input_saver_def_path = ''
    input_binary = False
    output_node_names = 'result'
    restore_op_name = 'save/restore_all'
    filename_tensor_name = 'save/Const:0'
    ouput_frozen_graph_name = 'frozen ' + MODEL_NAME + '.pb'
    clear_devices = True

    tf.python.tools.freeze_graph(input_graph_path, input_saver_def_path, input_binary, 
    checkpoint_path, output_node_names, restore_op_name, filename_tensor_name, 
    ouput_frozen_graph_name, clear_devices, "")

    input_graph_def = tf.GraphDef()
    with tf.gfile.Open(ouput_frozen_graph_name, 'r') as f:
        data = f.read()
        input_graph_def.ParseFromString(data)

    output_graph_def = tf.python.tools.optimize_for_inference_lib.optimize_for_inference(
        input_graph_def,
        ['I'],
        ['result'],
        tf.float32.as_datatype_enum)

    f = tf.gfile.FastGFile(ouput_frozen_graph_name, 'w')
    f.write(output_graph_def.SerializeToString())



    '''
    # Экспортирование модели
    export_path_base = sys.argv[-1]
    export_path = os.path.join(compat.as_bytes(export_path_base), compat.as_bytes(str(FLAGS.model_version)))
    print('Exporting trained model to', export_path)

    builder = saved_model_builder.SavedModelBuilder(export_path)

    # Уточнить параметры
    classification_inputs = utils.build_tensor_info('serialized_tf_example')
    classification_outputs_classes = utils.build_tensor_info('prediction_classes')
    classification_outputs_scores = utils.build_tensor_info('values')

    classification_signature = signature_def_utils.build_signature_def(
        inputs={signature_constants.CLASSIFY_INPUTS: classification_inputs},
        outputs={signature_constants.CLASSIFY_OUTPUT_CLASSES: classification_outputs_classes, signature_constants.CLASSIFY_OUTPUT_SCORES: classification_outputs_scores},
        method_name=signature_constants.CLASSIFY_METHOD_NAME)
    
    tensor_info_x = utils.build_tensor_info(x)
    tensor_info_y = utils.build_tensor_info(y)

    prediction_signature = signature_def_utils.build_signature_def(
        inputs={'sounds': tensor_info_x},
        outputs={'scores': tensor_info_y},
        method_name=signature_constants.PREDICT_METHOD_NAME)
    
    legacy_init_op = tf.group(tf.tables_initializer(), name='legacy_init_op')

    builder.add_meta_graph_and_variables(
        sess, [tag_constants.SERVING],
        signature_def_map={'predict_sounds': prediction_signature, signature_constants.DEFAULT_SERVING_SIGNATURE_DEF_KEY: classification_signature},
        legacy_init_op=legacy_init_op)

    builder.save()
    '''


if __name__ == '__main__':
    init(PATH_TRAIN)
    main()
    print(dir(tf))

