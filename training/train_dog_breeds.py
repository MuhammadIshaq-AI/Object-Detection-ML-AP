"""Train the dog breed classifier used by the app on the Stanford Dogs dataset.

Stanford Dogs: 120 breeds, 12,000 train / 8,580 test images, with a bounding box
per dog. We train on the box crops because on-device the classifier only ever
sees crops from the EfficientDet "dog" detections.

Approach: an ImageNet-pretrained MobileNetV3-Large backbone is kept frozen, its
pooled features are extracted once, and a small softmax head is trained on top.
This runs on a CPU in minutes. The combined model is exported to TFLite.

Usage:
    python train_dog_breeds.py --data <dir> --out <app/src/main/assets>

<dir> must contain the extracted Images/, Annotation/ and train_list.mat /
test_list.mat from http://vision.stanford.edu/aditya86/ImageNetDogs/
"""

import argparse
import os
import xml.etree.ElementTree as ET

import numpy as np
import scipy.io
import tensorflow as tf
from PIL import Image

IMG_SIZE = 224
BOX_PADDING = 0.1  # detector boxes are rarely tight, so pad the ground-truth boxes a little


def breed_name(folder: str) -> str:
    """'n02099601-golden_retriever' -> 'Golden Retriever'."""
    name = folder.split("-", 1)[1].replace("_", " ")
    return " ".join(w[:1].upper() + w[1:] for w in name.split(" "))


def load_split(data_dir: str, split: str):
    mat = scipy.io.loadmat(os.path.join(data_dir, f"{split}_list.mat"))
    files = [f[0][0] for f in mat["file_list"]]
    labels = mat["labels"].flatten().astype(np.int64) - 1
    return files, labels


def crop_dog(data_dir: str, rel_path: str) -> Image.Image:
    image = Image.open(os.path.join(data_dir, "Images", rel_path)).convert("RGB")
    ann = ET.parse(os.path.join(data_dir, "Annotation", rel_path[:-4])).getroot()
    # Use the largest box when an image has several dogs.
    boxes = [
        [int(float(o.find("bndbox").find(k).text)) for k in ("xmin", "ymin", "xmax", "ymax")]
        for o in ann.findall("object")
    ]
    x0, y0, x1, y1 = max(boxes, key=lambda b: (b[2] - b[0]) * (b[3] - b[1]))
    pad_x, pad_y = (x1 - x0) * BOX_PADDING, (y1 - y0) * BOX_PADDING
    box = (
        max(0, x0 - pad_x),
        max(0, y0 - pad_y),
        min(image.width, x1 + pad_x),
        min(image.height, y1 + pad_y),
    )
    return image.crop(box).resize((IMG_SIZE, IMG_SIZE), Image.BILINEAR)


def extract_features(backbone, data_dir, files, flip=False, batch_size=64):
    features = []
    for start in range(0, len(files), batch_size):
        batch = np.stack(
            [np.asarray(crop_dog(data_dir, f), dtype=np.float32) for f in files[start:start + batch_size]]
        )
        if flip:
            batch = batch[:, :, ::-1, :]
        features.append(backbone.predict_on_batch(batch))
        print(f"\r  {min(start + batch_size, len(files))}/{len(files)}", end="", flush=True)
    print()
    return np.concatenate(features)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--epochs", type=int, default=30)
    args = parser.parse_args()

    class_names = [breed_name(d) for d in sorted(os.listdir(os.path.join(args.data, "Images")))]
    train_files, train_labels = load_split(args.data, "train")
    test_files, test_labels = load_split(args.data, "test")

    backbone = tf.keras.applications.MobileNetV3Large(
        input_shape=(IMG_SIZE, IMG_SIZE, 3),
        include_top=False,
        pooling="avg",
        weights="imagenet",
        include_preprocessing=True,  # model takes raw 0..255 RGB
    )
    backbone.trainable = False

    cache = os.path.join(args.data, "features.npz")
    if os.path.exists(cache):
        cached = np.load(cache)
        x_train, x_test = cached["x_train"], cached["x_test"]
    else:
        print("Extracting train features")
        x_train = extract_features(backbone, args.data, train_files)
        print("Extracting flipped train features")
        x_train = np.concatenate([x_train, extract_features(backbone, args.data, train_files, flip=True)])
        print("Extracting test features")
        x_test = extract_features(backbone, args.data, test_files)
        np.savez(cache, x_train=x_train, x_test=x_test)
    y_train = np.concatenate([train_labels, train_labels])

    head = tf.keras.Sequential([
        tf.keras.Input(shape=x_train.shape[1:]),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(len(class_names), activation="softmax",
                              kernel_regularizer=tf.keras.regularizers.l2(1e-4)),
    ])
    head.compile(
        optimizer=tf.keras.optimizers.Adam(1e-3),
        loss="sparse_categorical_crossentropy",
        metrics=["accuracy"],
    )
    head.fit(
        x_train, y_train,
        validation_data=(x_test, test_labels),
        epochs=args.epochs,
        batch_size=128,
        callbacks=[tf.keras.callbacks.EarlyStopping(patience=4, restore_best_weights=True)],
        verbose=2,
    )
    _, accuracy = head.evaluate(x_test, test_labels, verbose=0)
    print(f"Test accuracy: {accuracy:.4f}")

    inputs = tf.keras.Input(shape=(IMG_SIZE, IMG_SIZE, 3), batch_size=1)
    model = tf.keras.Model(inputs, head(backbone(inputs, training=False)))

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()

    os.makedirs(args.out, exist_ok=True)
    with open(os.path.join(args.out, "dog_breeds.tflite"), "wb") as f:
        f.write(tflite_model)
    with open(os.path.join(args.out, "dog_breeds_labels.txt"), "w", encoding="utf-8") as f:
        f.write("\n".join(class_names) + "\n")
    print(f"Wrote {len(tflite_model) / 1e6:.1f} MB model and {len(class_names)} labels to {args.out}")


if __name__ == "__main__":
    main()
