# from https://github.com/EdjeElectronics/TensorFlow-Lite-Object-Detection-on-Android-and-Raspberry-Pi/blob/master/util_scripts/train_val_test_split.py

from pathlib import Path
import random
import os
import sys

# Set the paths for the image directories
image_path = '/content/dataset/all'
train_path = '/content/dataset/train'
val_path = '/content/dataset/validation'
test_path = '/content/dataset/test'

# Gather all image files with different extensions from the source folder
jpeg_file_list = [path for path in Path(image_path).rglob('*.jpeg')]
jpg_file_list = [path for path in Path(image_path).rglob('*.jpg')]
png_file_list = [path for path in Path(image_path).rglob('*.png')]
bmp_file_list = [path for path in Path(image_path).rglob('*.bmp')]

# On Linux systems, also include uppercase file extensions (case sensitive)
if sys.platform == 'linux':
    JPEG_file_list = [path for path in Path(image_path).rglob('*.JPEG')]
    JPG_file_list = [path for path in Path(image_path).rglob('*.JPG')]
    file_list = jpg_file_list + JPG_file_list + png_file_list + bmp_file_list + JPEG_file_list + jpeg_file_list
else:
    file_list = jpg_file_list + png_file_list + bmp_file_list + jpeg_file_list

# Count the total number of image files found
file_num = len(file_list)
print('Total images: %d' % file_num)

# Define the dataset split ratios: 70% for training, 15% for validation, and 15% for testing
train_percent = 0.7
val_percent = 0.15
test_percent = 0.15

# Calculate the exact number of images for each subset
train_num = int(file_num * train_percent)
val_num = int(file_num * val_percent)
test_num = file_num - train_num - val_num
print('Images moving to train: %d' % train_num)
print('Images moving to validation: %d' % val_num)
print('Images moving to test: %d' % test_num)

# Randomly select and move 70% of the images along with their annotation files to the training folder
for i in range(train_num):
    move_me = random.choice(file_list)
    fn = move_me.name
    base_fn = move_me.stem
    parent_path = move_me.parent
    xml_fn = base_fn + '.xml'
    os.rename(move_me, os.path.join(train_path, fn))
    os.rename(os.path.join(parent_path, xml_fn), os.path.join(train_path, xml_fn))
    file_list.remove(move_me)

# Randomly select and move 15% of the remaining images with annotations to the validation folder
for i in range(val_num):
    move_me = random.choice(file_list)
    fn = move_me.name
    base_fn = move_me.stem
    parent_path = move_me.parent
    xml_fn = base_fn + '.xml'
    os.rename(move_me, os.path.join(val_path, fn))
    os.rename(os.path.join(parent_path, xml_fn), os.path.join(val_path, xml_fn))
    file_list.remove(move_me)

# Move all remaining images and their annotation files to the testing folder
for i in range(test_num):
    move_me = random.choice(file_list)
    fn = move_me.name
    base_fn = move_me.stem
    parent_path = move_me.parent
    xml_fn = base_fn + '.xml'
    os.rename(move_me, os.path.join(test_path, fn))
    os.rename(os.path.join(parent_path, xml_fn), os.path.join(test_path, xml_fn))
    file_list.remove(move_me)
